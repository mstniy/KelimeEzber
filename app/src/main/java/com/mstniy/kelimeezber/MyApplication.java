package com.mstniy.kelimeezber;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;

enum SelectionMethod {
    SMART, NEW, RANDOM, LISTENING
}

public class MyApplication extends Application implements OnInitListener {
    static final int MaxWordPeriod = 1<<16;
    static final int WordDropPeriod = 128;
    private static final String TAG = MyApplication.class.getName();
    final double MC_PROBABILITY = 0.5;
    Pair currentPair = null;
    ExerciseFragment exerciseFragment;
    DrawerActivity drawerActivity;
    ExerciseType exerciseType;
    DatabaseHelper helper = null;
    boolean isCurrentPairRandom;
    Fraction randomPassFraction;
    int roundId;
    SelectionMethod selectionMethod = SelectionMethod.SMART;
    MutableLiveData<Boolean> sortByPeriod = new MutableLiveData<>();
    TextToSpeech tts;
    boolean isMuted = true;
    boolean ttsSupported = false;
    HashSet<Pair> wlist = new HashSet<>();
    HashMap<String, HashSet<String>> wordTranslationsBwd = new HashMap<>();
    HashMap<String, HashSet<String>> wordTranslationsFwd = new HashMap<>();
    String audioDatasetPath = null;
    MediaPlayer mediaPlayer = new MediaPlayer();

    public void onInit(int status) {
        if (status != 0 || tts.setLanguage(new Locale("sv", "SE")) == -2) {
            ttsSupported = false;
        } else {
            ttsSupported = true;
        }
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("TextToSpeech in Swedish is ");
        sb.append(ttsSupported ? "" : "not ");
        sb.append("supported.");
        Log.i(str, sb.toString());
    }

    public void onCreate() {
        super.onCreate();
        sortByPeriod.setValue(Boolean.valueOf(true));
        helper = new DatabaseHelper(this);
        SyncStateWithDB();
        tts = new TextToSpeech(this, this);
        StartRound();
    }

    void UpdatePair(Pair p) {
        helper.updatePair(p);
    }

    private void AddPairToAppState(Pair p) {
        wlist.add(p);
        if (!wordTranslationsFwd.containsKey(p.first)) {
            wordTranslationsFwd.put(p.first, new HashSet());
        }
        if (!wordTranslationsBwd.containsKey(p.second)) {
            wordTranslationsBwd.put(p.second, new HashSet());
        }
        (wordTranslationsFwd.get(p.first)).add(p.second);
        (wordTranslationsBwd.get(p.second)).add(p.first);
    }

    void AddPair(Pair p) {
        AddPairToAppState(p);
        helper.insertPair(p);
    }

    void RemovePair(Pair p) {
        wlist.remove(p);
        (wordTranslationsFwd.get(p.first)).remove(p.second);
        if ((wordTranslationsFwd.get(p.first)).isEmpty()) {
            wordTranslationsFwd.remove(p.first);
        }
        (wordTranslationsBwd.get(p.second)).remove(p.first);
        if ((wordTranslationsBwd.get(p.second)).isEmpty()) {
            wordTranslationsBwd.remove(p.second);
        }
        helper.removePair(p);
        if (currentPair == p) {
            drawerActivity.discardSavedExerciseState();
            StartRound();
        }
    }

    boolean SyncStateWithDB() {
        wlist.clear();
        wordTranslationsFwd.clear();
        wordTranslationsBwd.clear();
        currentPair = null;
        Pair[] pairs = helper.getPairs();
        if (pairs.length == 0) {
            Pair pair = new Pair(0, "sedan", "since",1, -1);
            Pair pair2 = new Pair(0, "annars", "otherwise",1, -1);
            Pair pair3 = new Pair(0, "även om", "even if", 1, -1);
            Pair pair4 = new Pair(0, "snygg", "nice", 1, -1);
            Pair pair5 = new Pair(0, "trevlig", "nice", 1, -1);
            pairs = new Pair[]{pair, pair2, pair3, pair4, pair5};
            for (Pair p : pairs) {
                AddPair(p);
            }
        }
        HashMap<Long, Pair> pairReverseLookup = new HashMap<>();
        for (Pair p2 : pairs) {
            AddPairToAppState(p2);
            pairReverseLookup.put(Long.valueOf(p2.id), p2);
        }
        randomPassFraction = helper.getRandomPassFraction();
        audioDatasetPath = helper.getAudioDatasetPath();
        roundId = helper.getRoundID();
        return true;
    }

    void StartRound() {
        if (selectionMethod == SelectionMethod.LISTENING) {
            exerciseType = ExerciseType.Listening;
            currentPair = null;
        }
        else {
            double randomDouble = new Random().nextDouble();
            if (randomDouble < MC_PROBABILITY)
                exerciseType = ExerciseType.MC;
            else
                exerciseType = ExerciseType.Writing;
            if (selectionMethod == SelectionMethod.SMART) {
                ArrayList<Pair> candidates = new ArrayList<>();
                int smallestNext = -1;
                for (Pair p : wlist) {
                    if (p.next == -1)
                        continue;
                    if (smallestNext == -1 || p.next < smallestNext)
                        smallestNext = p.next;
                }
                Log.d(TAG, "smallestNext: " + smallestNext);
                if (smallestNext != -1 && smallestNext <= roundId) {
                    for (Pair p : wlist) {
                        if (p.next == smallestNext)
                            candidates.add(p);
                    }
                }
                Log.d(TAG, "candidatesSize: " + candidates.size());
                if (candidates.size() == 0) {
                    isCurrentPairRandom = true;
                    currentPair = PairChooser.ChoosePairRandom(this);
                }
                else {
                    isCurrentPairRandom = false;
                    currentPair = candidates.get(new Random().nextInt(candidates.size()));
                }
            }
            else if (selectionMethod == SelectionMethod.NEW) {
                currentPair = PairChooser.ChoosePairNew(this);
                isCurrentPairRandom = false;
            }
            else if (selectionMethod == SelectionMethod.RANDOM) {
                currentPair = PairChooser.ChoosePairRandom(this);
                isCurrentPairRandom = true;
            }
        }
        if (exerciseFragment != null && exerciseFragment.isAdded()) {
            exerciseFragment.ChangeExercise(currentPair, exerciseType);
        }
    }

    void FinishRound(boolean isPass) {
        if (exerciseType != ExerciseType.Listening) {
            if (isPass) {
                currentPair.period *= 2;
                if (currentPair.period > MaxWordPeriod)
                    currentPair.period = 0;
            }
            else {
                if (currentPair.period == 0  || currentPair.period > WordDropPeriod)
                    currentPair.period = WordDropPeriod;
                else if (currentPair.period > 1)
                    currentPair.period /= 2;
            }
            if (selectionMethod == SelectionMethod.SMART) {
                if (currentPair.period != 0)
                    currentPair.next = roundId + currentPair.period;
                else
                    currentPair.next = -1;
                roundId++;
                helper.setRoundID(roundId);
            }
            UpdatePair(currentPair);
            if (isCurrentPairRandom) {
                randomPassFraction.a += isPass ? 1 : 0;
                randomPassFraction.b++;
                helper.setRandomPassFraction(randomPassFraction);
            }
        }
        StartRound();
    }

    void speak(String s) {
        if (ttsSupported && !isMuted) {
            tts.speak(s, 0, null);
        }
    }

    void playAudio(String path) {
        if (!isMuted) {
            mediaPlayer.reset();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mediaPlayer.setDataSource(this, Uri.parse(path));
                mediaPlayer.prepare();
            }
            catch (IOException e) {
                Log.e(TAG, e.getMessage());
                Log.e(TAG, e.getStackTrace().toString());
                return ;
            }
            mediaPlayer.start();
        }
    }

    void stopPlayingAudio() {
        mediaPlayer.stop();
    }

    boolean isACorrectAnswer(String s, boolean currentFwd) {
        if (currentFwd)
            return (wordTranslationsFwd.get(currentPair.first)).contains(s);
        else
            return (wordTranslationsBwd.get(currentPair.second)).contains(s);
    }
}
