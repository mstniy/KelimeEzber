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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;

enum SelectionMethod {
    SMART, NEW, RANDOM, LISTENING
}

public class MyApplication extends Application implements OnInitListener {
    static final int MaxWordPeriod = 256;
    private static final String TAG = MyApplication.class.getName();
    final double FWD_PROBABILITY = 0.5;
    final double MC_PROBABILITY = 0.5;
    boolean currentFwd;
    Pair currentPair = null;
    int currentQueueIndex;
    ExerciseFragment exerciseFragment;
    ExerciseType exerciseType;
    DatabaseHelper helper = null;
    boolean isPass;
    Pair[] pairQueue;
    Fraction randomPassFraction;
    int roundId = 0;
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

    void HardnessPeriodChanged(Pair p) {
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
        int i = 0;
        while (true) {
            Pair[] pairArr = pairQueue;
            if (i >= pairArr.length) {
                break;
            }
            if (pairArr[i] == p) {
                pairArr[i] = null;
                helper.setPairQueueElement(i, Long.valueOf(0));
            }
            i++;
        }
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
            Pair pair = new Pair(0, "sedan", "since", 0.0, 1);
            Pair pair2 = new Pair(0, "annars", "otherwise", 0.0, 1);
            Pair pair3 = new Pair(0, "även om", "even if", 0.0, 1);
            Pair pair4 = new Pair(0, "snygg", "nice", 0.0, 1);
            Pair pair5 = new Pair(0, "trevlig", "nice", 0.0, 1);
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
        long[] pairQueueIds = helper.getPairQueueIds();
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("Length of pairQueueIds: ");
        sb.append(pairQueueIds.length);
        Log.d(str, sb.toString());
        pairQueue = new Pair[pairQueueIds.length];
        for (int i = 0; i < pairQueueIds.length; i++) {
            if (pairQueueIds[i] != 0) {
                pairQueue[i] = pairReverseLookup.get(Long.valueOf(pairQueueIds[i]));
                if (pairQueue[i] == null) {
                    throw new RuntimeException("Invalid pairQueueId in the pair queue!");
                }
            } else {
                pairQueue[i] = null;
            }
        }
        currentQueueIndex = helper.getCurrentQueueIndex();
        randomPassFraction = helper.getRandomPassFraction();
        audioDatasetPath = helper.getAudioDatasetPath();
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
                Pair[] pairArr = pairQueue;
                int i2 = currentQueueIndex;
                if (pairArr[i2] == null)
                    currentPair = PairChooser.ChoosePairRandom(this);
                else
                    currentPair = pairArr[i2];
            }
            else if (selectionMethod == SelectionMethod.NEW)
                currentPair = PairChooser.ChoosePairNew(this);
            else if (selectionMethod == SelectionMethod.RANDOM)
                currentPair = PairChooser.ChoosePairRandom(this);
            currentFwd = new Random().nextDouble() <= FWD_PROBABILITY;
            isPass = true;
        }
        if (exerciseFragment != null && exerciseFragment.isAdded()) {
            exerciseFragment.ChangeExercise(currentPair, exerciseType);
        }
    }

    void InsertToPairQueue(int index, Pair p) {
        long id = p == null ? 0 : p.id;
        Pair[] pairArr = pairQueue;
        if (pairArr[index] == null) {
            pairArr[index] = p;
            helper.setPairQueueElement(index, Long.valueOf(id));
            return;
        }
        int currentIndex = (index + 1) % pairArr.length;
        while (currentIndex != index) {
            Pair[] pairArr2 = pairQueue;
            if (pairArr2[currentIndex] == null) {
                pairArr2[currentIndex] = p;
                helper.setPairQueueElement(currentIndex, Long.valueOf(id));
                return;
            }
            currentIndex = (currentIndex + 1) % pairArr2.length;
        }
        Log.w(TAG, "No empty spot left in the pair queue.");
    }

    void FinishRound() {
        if (exerciseType != ExerciseType.Listening) {
            if (isPass) {
                currentPair.period *= 2;
                if (currentPair.period > MaxWordPeriod) {
                    currentPair.period = 0;
                }
            } else if (currentPair.period == 0) {
                currentPair.period = MaxWordPeriod;
            } else if (currentPair.period > 1) {
                currentPair.period /= 2;
            }
            HardnessPeriodChanged(currentPair);
            if (selectionMethod == SelectionMethod.RANDOM || pairQueue[currentQueueIndex] == null) {
                randomPassFraction.a += isPass ? 1 : 0;
                randomPassFraction.b++;
                helper.setRandomPassFraction(randomPassFraction);
            }
            if (selectionMethod == SelectionMethod.SMART) {
                Pair[] pairArr = pairQueue;
                int i = currentQueueIndex;
                pairArr[i] = null;
                helper.setPairQueueElement(i, Long.valueOf(0));
                if (currentPair.period != 0) {
                    InsertToPairQueue((currentQueueIndex + currentPair.period) % pairQueue.length, currentPair);
                }
                currentQueueIndex = (currentQueueIndex + 1) % pairQueue.length;
                helper.setCurrentQueueIndex(currentQueueIndex);
            }
        }
        roundId++;
        StartRound();
    }

    void speak(String s) {
        if (ttsSupported && !isMuted) {
            tts.speak(s, 0, null);
        }
    }

    void playAudio(Uri uri) {
        if (!isMuted) {
            mediaPlayer.reset();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mediaPlayer.setDataSource(this, uri);
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
}
