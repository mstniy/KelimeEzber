package com.mstniy.kelimeezber;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;

import static java.lang.Math.max;
import static java.lang.Math.min;

class MistakeQueueElement {
    Pair p = null;
    // This counter keeps how many times the user failed on this pair (since it was introduced to the mistake queue).
    // Passes on exercises decrease this counter (unless it was already 0), fails increase it.
    // If this counter is 0 if and only if p==null.
    // For example, if the user fails on a pair, it'll be "added" to the mistake queue (p set to that pair) with the counter set to 1, and it'll be shown again MistakeQueueLength rounds later.
    // If the user passes this round, the pair is "erased" from the mistake queue (p set to null), since the counter will drop to 0.
    // Otherwise, it stays in the mistake queue and the counter increases to 2.
    int mistakeCnt = 0;
}

public class MyApplication extends Application {

    private static final String TAG = MyApplication.class.getName();
    HashSet<Pair> wlist = new HashSet<>();
    HashSet<Runnable> wlistObservers = new HashSet<>(); // Java (or Android or JavaRX) doesn't have an ObservableSet, so we implement it by ourselves.
    HashMap<String, HashSet<String>> wordTranslationsFwd = new HashMap<>();
    HashMap<String, HashSet<String>> wordTranslationsBwd = new HashMap<>();
    final int MistakeQueueLength=4;
    static final int MaxMistakeQueueCounter =3; // The maximum value of MistakeQueueElement.mistakeCnt
    // Keeps track of hardness on the short term. When the user fails on an exercise, the relevant pair is added to the queue to be shown MistakeQueueLength rounds later.
    // For more details, see MistakeQueueElement.
    // If mistakeQueue[i].p == null, that spot is empty.
    MistakeQueueElement mistakeQueue[]=new MistakeQueueElement[MistakeQueueLength];
    int currentQueueIndex = MistakeQueueLength-1;
    MutableLiveData<Pair> currentPair = new MutableLiveData<>();
    DatabaseHelper helper = null;
    int roundId = 0;
    MutableLiveData<Boolean> sortByHardness = new MutableLiveData<>();

    public MyApplication() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sortByHardness.setValue(true);
        helper = new DatabaseHelper(this);
        for (int i=0; i<MistakeQueueLength; i++)
            mistakeQueue[i] = new MistakeQueueElement();
        SyncWords();
        NewRound();
    }

    private void NotifyWListObservers() {
        for (Runnable r : wlistObservers)
            r.run();
    }

    void HardnessChanged(Pair p) {
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
        wlist.remove(p);
        NotifyWListObservers();
        wordTranslationsFwd.remove(p.first);
        wordTranslationsBwd.remove(p.second);
        for (int i=0; i<MistakeQueueLength; i++)
            if (mistakeQueue[i].p == p) {
                mistakeQueue[i].p = null;
                mistakeQueue[i].mistakeCnt = 0;
            }
        if (currentPair.getValue() == p)
            NewRound();
        helper.removePair(p);
    }

    boolean SyncWords()
    {
        wlist.clear();
        wordTranslationsFwd.clear();
        wordTranslationsBwd.clear();
        currentQueueIndex = MistakeQueueLength-1;
        for (int i=0;i<MistakeQueueLength;i++) {
            mistakeQueue[i].p = null;
            mistakeQueue[i].mistakeCnt = 0;
        }
        currentPair.setValue(null);

        Pair[] pairs = helper.getPairs();
        if (pairs.length == 0) { // We just created the db, add the mock words
            pairs = new Pair[5];
            pairs[0] = new Pair(0, "sedan", "since", 0); // AddPair will set proper id's once these Pair's are added to the db.
            pairs[1] = new Pair(0, "annars", "otherwise", 0);
            pairs[2] = new Pair(0, "även om", "even if", 0);
            pairs[3] = new Pair(0, "snygg", "nice", 0);
            pairs[4] = new Pair(0, "trevlig", "nice", 0);
            for (Pair p : pairs)
                AddPair(p); // AddPair also adds them to the DB
        }
        else {
            for (Pair p : pairs)
                AddPairToAppState(p);
        }
        return true;
    }

    void NewRound() {
        roundId++;
        currentQueueIndex=(currentQueueIndex+1)%MistakeQueueLength;
        if (mistakeQueue[currentQueueIndex].p != null)
            currentPair.setValue(mistakeQueue[currentQueueIndex].p);
        else
            currentPair.setValue(PairChooser.ChoosePairSmart(this));
    }

    void FinishRound(boolean isMC, boolean isPass) {
        final Pair currentPairVal = currentPair.getValue();
        final double oldScore = currentPairVal.hardness;
        double newScore = oldScore;
        MistakeQueueElement currentMQE = mistakeQueue[currentQueueIndex];
        if (isPass) {
            newScore -= isMC ? 0.3 : 0.5;
            if (currentMQE.p != null) {
                assert currentMQE.mistakeCnt > 0;
                currentMQE.mistakeCnt--;
                if (currentMQE.mistakeCnt == 0)
                    currentMQE.p = null;
            }
        }
        else {
            newScore += isMC ? 0.7 : 0.6;
            if (currentMQE.p == null) {
                assert currentMQE.mistakeCnt == 0;
                currentMQE.p = currentPairVal;
            }
            currentMQE.mistakeCnt++;
            currentMQE.mistakeCnt = Math.max(currentMQE.mistakeCnt, MyApplication.MaxMistakeQueueCounter);
        }
        newScore = min(newScore, 2.0);
        newScore = max(newScore, -1.33);

        currentPairVal.hardness = newScore; // Update the score of the current word
        HardnessChanged(currentPairVal);
        NewRound();
    }
}
