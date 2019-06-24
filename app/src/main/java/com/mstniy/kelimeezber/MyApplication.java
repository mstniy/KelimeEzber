package com.mstniy.kelimeezber;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class MyApplication extends Application {

    private static final String TAG = MyApplication.class.getName();
    final double FORWARD_PROBABILITY = 0.33;

    HashSet<Pair> wlist = new HashSet<>();
    HashSet<Runnable> wlistObservers = new HashSet<>(); // Java (or Android or JavaRX) doesn't have an ObservableSet, so we implement it ourselves.
    HashMap<String, HashSet<String>> wordTranslationsFwd = new HashMap<>();
    HashMap<String, HashSet<String>> wordTranslationsBwd = new HashMap<>();
    static final int MaxWordPeriod = 64;
    // If pairQueue[i].p == null, that spot is empty.
    Pair[] pairQueue;
    int currentQueueIndex;
    boolean currentFwd;
    MutableLiveData<Pair> currentPair = new MutableLiveData<>();
    DatabaseHelper helper = null;
    int roundId = 0;
    MutableLiveData<Boolean> sortByPeriod = new MutableLiveData<>();
    int exerciseType = 0;

    public MyApplication() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sortByPeriod.setValue(true);
        helper = new DatabaseHelper(this);
        SyncStateWithDB();
        StartRound();
    }

    private void NotifyWListObservers() {
        for (Runnable r : wlistObservers)
            r.run();
    }

    void HardnessPeriodChanged(Pair p) {
        helper.updatePair(p);
        NotifyWListObservers(); // TODO: This isn't the most efficient way to handle a single hardness value change.
    }

    private void AddPairToAppState(Pair p) {
        wlist.add(p);
        NotifyWListObservers();
        if (wordTranslationsFwd.containsKey(p.first) == false)
            wordTranslationsFwd.put(p.first, new HashSet<String>());
        if (wordTranslationsBwd.containsKey(p.second) == false)
            wordTranslationsBwd.put(p.second, new HashSet<String>());
        wordTranslationsFwd.get(p.first).add(p.second);
        wordTranslationsBwd.get(p.second).add(p.first);
        if (currentPair.getValue() == null)
            currentPair.setValue(p);
        //uncoveredPairs.add(uncoveredPairs.size());
    }

    void AddPair(Pair p){
        AddPairToAppState(p);
        helper.insertPair(p);
    }

    void RemovePair(Pair p) {
        for (int i=0; i<pairQueue.length; i++)
            if (pairQueue[i] == p) {
                pairQueue[i] = null; // We can't use InsertToPairQueue here because it'd just skip currentQueueIndex since it's not null. So we have to change the pairQueue and reflect the change to the DB manually.
                helper.setPairQueueElement(i, (long)0);
            }
        wlist.remove(p);
        NotifyWListObservers();
        wordTranslationsFwd.get(p.first).remove(p.second);
        if (wordTranslationsFwd.get(p.first).isEmpty())
            wordTranslationsFwd.remove(p.first);
        wordTranslationsBwd.get(p.second).remove(p.first);
        if (wordTranslationsBwd.get(p.second).isEmpty())
            wordTranslationsBwd.remove(p.second);
        helper.removePair(p);
        if (currentPair.getValue() == p)
            StartRound();
    }

    boolean SyncStateWithDB()
    {
        wlist.clear();
        wordTranslationsFwd.clear();
        wordTranslationsBwd.clear();
        currentPair.setValue(null);

        Pair[] pairs = helper.getPairs();
        if (pairs.length == 0) { // We just created the db, add some mock words
            pairs = new Pair[5];
            pairs[0] = new Pair(0, "sedan", "since", 0, 0); // AddPair will set proper id's once these Pair's are added to the db.
            pairs[1] = new Pair(0, "annars", "otherwise", 0, 0);
            pairs[2] = new Pair(0, "även om", "even if", 0, 0);
            pairs[3] = new Pair(0, "snygg", "nice", 0, 0);
            pairs[4] = new Pair(0, "trevlig", "nice", 0, 0);
            for (Pair p : pairs)
                AddPair(p); // AddPair also adds them to the DB
        }
        HashMap<Long, Pair> pairReverseLookup = new HashMap<>();
        for (Pair p : pairs) {
            AddPairToAppState(p);
            pairReverseLookup.put(p.id, p);
        }
        long[] pairQueueIds = helper.getPairQueueIds();
        Log.d(TAG, "Length of pairQueueIds: " + pairQueueIds.length);
        pairQueue = new Pair[pairQueueIds.length];
        for (int i=0; i<pairQueueIds.length; i++) {
            if (pairQueueIds[i] != 0) {
                pairQueue[i] = pairReverseLookup.get(pairQueueIds[i]);
                if (pairQueue[i] == null)
                    throw new RuntimeException("Invalid pairQueueId in the pair queue!");
            }
            else
                pairQueue[i] = null;
        }
        currentQueueIndex = helper.getCurrentQueueIndex();
        return true;
    }

    void StartRoundSmart() {
        if (pairQueue[currentQueueIndex] == null)
            InsertToPairQueue(currentQueueIndex, PairChooser.ChoosePairRandom(this));

        currentFwd = (new Random().nextDouble() <= FORWARD_PROBABILITY);
        currentPair.setValue(pairQueue[currentQueueIndex]);

        Log.d(TAG, "currentQueueIndex: " + currentQueueIndex);
        StringBuilder pairQueueDebugLine = new StringBuilder();
        for (int i=currentQueueIndex,j=0;j<10;j++,i=(i+1)%pairQueue.length) {
            if (pairQueue[i] == null)
                pairQueueDebugLine.append("null ");
            else
                pairQueueDebugLine.append("\""+pairQueue[i].first + "\" ");
        }
        Log.d(TAG, pairQueueDebugLine.toString());
    }

    void StartRoundNew() {
        currentPair.setValue(PairChooser.ChoosePairNew(this));
    }

    void StartRoundRandom() {
        currentPair.setValue(PairChooser.ChoosePairRandom(this));
    }

    void StartRound() {
        if (exerciseType == 0)
            StartRoundSmart();
        else if (exerciseType == 1)
            StartRoundNew();
        else if (exerciseType == 2)
            StartRoundRandom();
    }

    // Inserts the given pair to the given index in the pair queue. If the given index is not empty, looks for the first empty index after that, cyclically.
    void InsertToPairQueue(int index, Pair p)
    {
        long id = p==null?0:p.id;
        if (pairQueue[index] == null)
        {
            pairQueue[index] = p;
            helper.setPairQueueElement(index, id);
            return ;
        }
        for (int currentIndex = (index+1)%pairQueue.length; currentIndex != index; currentIndex=(currentIndex+1)%pairQueue.length)
            if (pairQueue[currentIndex] == null)
            {
                pairQueue[currentIndex] = p;
                helper.setPairQueueElement(currentIndex, id);
                return ;
            }
        Log.w(TAG, "No empty spot left in the pair queue.");
    }

    void FinishRound(boolean isMC, boolean isPass) {
        final Pair currentPairVal = currentPair.getValue();
        int oldPeriod = currentPairVal.period;
        double newScore = currentPairVal.hardness;
        if (isPass) {
            newScore -= isMC ? 0.3 : 0.5;
            currentPairVal.period *= 2;
            if (currentPairVal.period > MaxWordPeriod)
                currentPairVal.period = MaxWordPeriod;
        }
        else {
            newScore += isMC ? 0.7 : 0.6;
            currentPairVal.period /= 2;
            if (currentPairVal.period == 0)
                currentPairVal.period = 1;
        }
        newScore = min(newScore, 2.0);
        newScore = max(newScore, -1.33);

        currentPairVal.hardness = newScore; // Update the score of the current word
        HardnessPeriodChanged(currentPairVal);

        if (exerciseType == 0) {
            pairQueue[currentQueueIndex] = null; // We can't use InsertToPairQueue here because it'd just skip currentQueueIndex since it's not null. So we have to change the pairQueue and reflect the change to the DB manually.
            helper.setPairQueueElement(currentQueueIndex, (long) 0);
            if (currentPairVal.period != 0 && !(oldPeriod == MaxWordPeriod && currentPairVal.period == MaxWordPeriod))
                InsertToPairQueue((currentQueueIndex + currentPairVal.period) % pairQueue.length, currentPairVal);
            currentQueueIndex=(currentQueueIndex+1)%pairQueue.length;
            helper.setCurrentQueueIndex(currentQueueIndex);
        }
        roundId++;
        StartRound();
    }
}
