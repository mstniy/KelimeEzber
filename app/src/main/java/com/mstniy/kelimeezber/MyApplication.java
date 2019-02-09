package com.mstniy.kelimeezber;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.databinding.ObservableArrayList;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Observable;

public class MyApplication extends Application {

    private static final String TAG = MyApplication.class.getName();

    //ArrayList<Integer> uncoveredPairs = new ArrayList<Integer>();
    ObservableArrayList<Pair> wlist = new ObservableArrayList<>();
    HashMap<String, HashSet<String>> wordTranslationsFwd = new HashMap<>();
    HashMap<String, HashSet<String>> wordTranslationsBwd = new HashMap<>();
    ArrayList<Double> hardness = new ArrayList<>(); // A hardness score for each pair in wlist
    final int MistakeQueueLength=4;
    int mistakeQueue[]=new int[MistakeQueueLength];
    int currentQueueIndex = MistakeQueueLength-1;
    MutableLiveData<Integer> currentPairIndex = new MutableLiveData<>();

    public MyApplication() {
        //SyncWords(); // Doing DB operations in the Application constructor is just asking for trouble.
        //NewRound();
        currentPairIndex.setValue(0);
    }

    private void AddPairToAppState(Pair p) {
        wlist.add(p);
        if (wordTranslationsFwd.containsKey(p.first) == false)
            wordTranslationsFwd.put(p.first, new HashSet<String>());
        if (wordTranslationsBwd.containsKey(p.second) == false)
            wordTranslationsBwd.put(p.second, new HashSet<String>());
        wordTranslationsFwd.get(p.first).add(p.second);
        wordTranslationsBwd.get(p.second).add(p.first);
        //uncoveredPairs.add(uncoveredPairs.size());
        hardness.add(0.0);
    }

    void AddPair(Pair p){
        AddPairToAppState(p);
        DatabaseHelper helper = new DatabaseHelper(this);
        helper.insertPair(p);
    }

    void RemovePair(int index) {
        Pair p = wlist.get(index);
        wlist.remove(index);
        wordTranslationsFwd.remove(p.first);
        wordTranslationsBwd.remove(p.second);
        hardness.remove(index);
        for (int i=0; i<MistakeQueueLength; i++)
            if (mistakeQueue[i] == index)
                mistakeQueue[i] = -1;
        if (currentPairIndex.getValue() == index)
            NewRound();
        // TODO: Remove the pair from the DB, too. -> Requires that we keep the database id in Pair, too.
    }

    boolean SyncWords()
    {
        wlist.clear();
        wordTranslationsFwd.clear();
        wordTranslationsBwd.clear();
        hardness.clear();
        currentQueueIndex = MistakeQueueLength-1;
        for (int i=0;i<MistakeQueueLength;i++)
            mistakeQueue[i] = -1;
        currentPairIndex.setValue(0);

        DatabaseHelper helper = new DatabaseHelper(this);
        Pair[] pairs = helper.getPairs();
        if (pairs.length == 0) { // We just created the db, add the mock words
            pairs = new Pair[5];
            pairs[0] = new Pair("sedan", "since");
            pairs[1] = new Pair("annars", "otherwise");
            pairs[2] = new Pair("även om", "even if");
            pairs[3] = new Pair("snygg", "nice");
            pairs[4] = new Pair("trevlig", "nice");
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
        if (mistakeQueue[currentQueueIndex] != -1)
            currentPairIndex.setValue(mistakeQueue[currentQueueIndex]);
        else
            currentPairIndex.setValue(PairChooser.ChoosePair(this));
        mistakeQueue[currentQueueIndex]=-1;
    }
}
