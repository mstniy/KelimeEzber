package com.mstniy.kelimeezber;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;

public class MyApplication extends Application {

    private static final String TAG = MyApplication.class.getName();
    HashSet<Pair> wlist = new HashSet<>();
    HashSet<Runnable> wlistObservers = new HashSet<>(); // Java (or Android or JavaRX) doesn't have an ObservableSet, so we implement it by ourselves.
    HashMap<String, HashSet<String>> wordTranslationsFwd = new HashMap<>();
    HashMap<String, HashSet<String>> wordTranslationsBwd = new HashMap<>();
    final int MistakeQueueLength=4;
    Pair mistakeQueue[]=new Pair[MistakeQueueLength];
    int currentQueueIndex = MistakeQueueLength-1;
    MutableLiveData<Pair> currentPair = new MutableLiveData<>();

    public MyApplication() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SyncWords();
        NewRound();
    }

    private void NotifyWListObservers() {
        for (Runnable r : wlistObservers)
            r.run();
    }

    void HardnessChanged(Pair p) {
        DatabaseHelper helper = new DatabaseHelper(this);
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
        DatabaseHelper helper = new DatabaseHelper(this);
        helper.insertPair(p);
    }

    void RemovePair(Pair p) {
        wlist.remove(p);
        NotifyWListObservers();
        wordTranslationsFwd.remove(p.first);
        wordTranslationsBwd.remove(p.second);
        for (int i=0; i<MistakeQueueLength; i++)
            if (mistakeQueue[i] == p)
                mistakeQueue[i] = null;
        if (currentPair.getValue() == p)
            NewRound();
        DatabaseHelper helper = new DatabaseHelper(this);
        helper.removePair(p);
    }

    boolean SyncWords()
    {
        wlist.clear();
        wordTranslationsFwd.clear();
        wordTranslationsBwd.clear();
        currentQueueIndex = MistakeQueueLength-1;
        for (int i=0;i<MistakeQueueLength;i++)
            mistakeQueue[i] = null;
        currentPair.setValue(null);

        DatabaseHelper helper = new DatabaseHelper(this);
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
        currentQueueIndex=(currentQueueIndex+1)%MistakeQueueLength;
        if (mistakeQueue[currentQueueIndex] != null)
            currentPair.setValue(mistakeQueue[currentQueueIndex]);
        else
            currentPair.setValue(PairChooser.ChoosePairSmart(this));
        mistakeQueue[currentQueueIndex]=null;
    }
}
