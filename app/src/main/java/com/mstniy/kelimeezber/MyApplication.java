package com.mstniy.kelimeezber;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

import com.opencsv.CSVReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum SelectionMethod {
    SMART, NEW, RANDOM
}

public class MyApplication extends Application implements OnInitListener {
    static final int MaxWordPeriod = 1<<16;
    static final int WordDropPeriod = 128;
    private static final String TAG = MyApplication.class.getName();
    ExerciseFragment exerciseFragment;
    ListeningFragment listeningFragment;
    DrawerActivity drawerActivity;
    DatabaseHelper helper = null;
    MutableLiveData<Boolean> sortByPeriod = new MutableLiveData<>();
    TextToSpeech tts;
    boolean isMuted = true;
    boolean ttsSupported = false;
    HashSet<Pair> wlist = new HashSet<>();
    HashMap<String, HashSet<String>> wordTranslationsBwd = new HashMap<>();
    HashMap<String, HashSet<String>> wordTranslationsFwd = new HashMap<>();
    String audioDatasetPath = null;
    MediaPlayer mediaPlayer = new MediaPlayer();
    int roundId;
    ArrayList<AudioAndWords> ats;

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

    @Override
    public void onTerminate() {
        super.onTerminate();
        helper.close();
    }

    public void onCreate() {
        super.onCreate();
        sortByPeriod.setValue(Boolean.valueOf(true));
        helper = new DatabaseHelper(this);
        SyncStateWithDB();
        tts = new TextToSpeech(this, this);
    }


    private void AddPairToAppState(Pair p) {
        wlist.add(p);
        if (!wordTranslationsFwd.containsKey(p.first))
            wordTranslationsFwd.put(p.first, new HashSet());
        if (!wordTranslationsBwd.containsKey(p.second))
            wordTranslationsBwd.put(p.second, new HashSet());
        wordTranslationsFwd.get(p.first).add(p.second);
        wordTranslationsBwd.get(p.second).add(p.first);
    }

    void AddPair(Pair p) {
        AddPairToAppState(p);
        helper.insertPair(p);
    }

    void UpdatePair(Pair p) {
        helper.updatePair(p);
    }

    void RemovePair(Pair p) {
        wlist.remove(p);
        (wordTranslationsFwd.get(p.first)).remove(p.second);
        if (wordTranslationsFwd.get(p.first).isEmpty()) {
            wordTranslationsFwd.remove(p.first);
        }
        wordTranslationsBwd.get(p.second).remove(p.first);
        if (wordTranslationsBwd.get(p.second).isEmpty())
            wordTranslationsBwd.remove(p.second);
        helper.removePair(p);
    }

    boolean SyncStateWithDB() {
        wlist.clear();
        wordTranslationsFwd.clear();
        wordTranslationsBwd.clear();
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
        audioDatasetPath = helper.getAudioDatasetPath();
        roundId = helper.getRoundID();
        MaybeReadAudioDataset();
        return true;
    }

    ArrayList<String> tokenizeWords(String sentence) {
        sentence = sentence.replace(",", "");
        sentence = sentence.replace(".", "");
        sentence = sentence.replace("!", "");
        sentence = sentence.replace("?", "");
        sentence = sentence.replace(":", "");
        sentence = sentence.replace(";", "");
        final Pattern word_etractor = Pattern.compile("(^| )([^ ]+)");
        Matcher word_matcher = word_etractor.matcher(sentence);
        ArrayList<String> list = new ArrayList<>();
        while (word_matcher.find())
            list.add(word_matcher.group(2).toLowerCase());
        return list;
    }

    void MaybeReadAudioDataset() {
        ats = new ArrayList<>();
        try {
            CSVReader reader = new CSVReader(new FileReader(audioDatasetPath + "/validated.tsv"), '\t');
            for (String[] line : reader) {
                String sentence = line[2];
                ArrayList<String> words = tokenizeWords(sentence);
                if (words.size() < ListeningFragment.MINIMUM_SENTENCE_LENGTH_IN_WORDS)
                    continue;
                String path = audioDatasetPath + "/clips/" + line[1];
                ats.add(new AudioAndWords(path, words, sentence));
            }
        }
        catch (FileNotFoundException e) {
            Log.w(TAG, "validated.tsv not found in the audio dataset path: " + audioDatasetPath);
            audioDatasetPath = null;
            helper.setAudioDatasetPath(null);
        }
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
}
