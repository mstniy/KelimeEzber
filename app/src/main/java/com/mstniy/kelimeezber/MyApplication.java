package com.mstniy.kelimeezber;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteException;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLClientInfoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LanguageDB {
    public String from, to, to_iso639, to_iso3166, dbPath;
}

public class MyApplication extends Application implements OnInitListener {
    static final int MaxWordPeriod = 1<<16;
    static final int WordDropPeriod = 128;

    private static final String DB_PATH_COLUMN = "db_path";
    private static final String PREFERENCES_NAME = "mypref";

    private static final String TAG = MyApplication.class.getName();
    SharedPreferences preferences;
    ArrayList<LanguageDB> dbs;
    LanguageDB currentDB;
    ExerciseFragment exerciseFragment;
    WordListFragment wordListFragment;
    ListeningFragment listeningFragment;
    DrawerActivity drawerActivity;
    DatabaseHelper helper = null;
    MutableLiveData<Boolean> sortByPeriod = new MutableLiveData<>();
    TextToSpeech tts;
    boolean isMuted = true;
    boolean ttsSupported = false;
    HashSet<Pair> wlist;
    HashMap<Long, Pair> pairsById;
    HashMap<String, HashSet<String>> wordTranslationsBwd;
    HashMap<String, HashSet<String>> wordTranslationsFwd;
    ArrayList<StampedEstimate> estimates;
    String audioDatasetPath = null;
    MediaPlayer mediaPlayer = new MediaPlayer();
    int roundId;
    ArrayList<AudioAndWords> ats;
    List<TextToSpeech.EngineInfo> ttsEngines;
    int ttsEngineIndex = 0;
    String defaultTTSEngine;

    public void onInit(int status) {
        Log.d(TAG, "onInit");

        if (status != 0 || tts.setLanguage(new Locale(currentDB.to_iso639, currentDB.to_iso3166)) < 0) {

            boolean instantiatedTTS = false;

            if (ttsEngines == null) {
                ttsEngines = tts.getEngines();
                defaultTTSEngine = tts.getDefaultEngine();
                Log.i(TAG, "The default TTS engine does not support " + currentDB.to_iso639 + "-" + currentDB.to_iso3166 + ". Trying the other engines.");
            }
            while (ttsEngineIndex < ttsEngines.size()) {
                String newCandidate = ttsEngines.get(ttsEngineIndex).name;
                if (newCandidate.equals(defaultTTSEngine)) {
                    Log.i(TAG, "Skipping the default TTS engine: " + defaultTTSEngine);
                    ttsEngineIndex++;
                    continue;
                } else {
                    tts = new TextToSpeech(getApplicationContext(),this,ttsEngines.get(ttsEngineIndex).name); // This will result in a call to onInit
                    instantiatedTTS = true;
                    ttsEngineIndex += 1;
                    break;
                }
            }
            if (instantiatedTTS == false) { // This means that we have exhausted all the options
                ttsSupported = false;
                Log.w(TAG, "None of the installed TTS engines support " + currentDB.to_iso639 + "-" + currentDB.to_iso3166);
            }
        } else {
            ttsSupported = true;
            if (ttsEngines == null)
                Log.i(TAG, "TTS in " + currentDB.to_iso639 + "-" + currentDB.to_iso3166 + " is supported by the default TTS engine " + tts.getDefaultEngine());
            else // This means that we have enumerated the installed TTS engines
                Log.i(TAG, "TTS in " + currentDB.to_iso639 + "-" + currentDB.to_iso3166 + " is supported by the non-default TTS engine " + ttsEngines.get(ttsEngineIndex-1).name);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        helper.close();
    }

    public void onCreate() {
        super.onCreate();
        sortByPeriod.setValue(Boolean.valueOf(true));
        preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        dbs = discoverDBs();
        LanguageDB ldb = new LanguageDB();
        ldb.dbPath = preferences.getString(DB_PATH_COLUMN, null);
        if (ldb.dbPath == null) {
            if (dbs.size() == 0) { // No DB has been created so far
                ldb.dbPath = getExternalFilesDir(null).getPath() + '/' + "0_english_swedish.db";
                ldb.from = "English";
                ldb.to = "Swedish";
                ldb.to_iso639 = "sv";
                ldb.to_iso3166 = "SE";
                dbs.add(ldb);
                changeDB(ldb, true);
            }
            else {
                ldb = dbs.get(0);
                changeDB(ldb, true);
            }
        }
        else {
            try {
                changeDB(ldb, false); // changeDB will take care of filling out the missing fields
            }
            catch (SQLiteCantOpenDatabaseException e) { // The db in the shared preferences probably does not exist anymore
                changeDB(dbs.get(0), true); // Fall back to the first db that we know of
            }
        }
    }

    public void changeDB(LanguageDB ldb, boolean create_if_necessary) {
        DatabaseHelper newhelper = new DatabaseHelper(ldb, create_if_necessary);
        if (helper != null) // Delay closing the existing helper in case the creation of the new one fails
            helper.close();
        helper = newhelper;
        currentDB = helper.getLanguageDB(); // Note that this may be different from ldb if, for example, some fields are missing from ldb.
        preferences.edit().putString(DB_PATH_COLUMN, currentDB.dbPath).apply();
        SyncStateWithDB();
        if (currentDB.to_iso639 == null ||  currentDB.to_iso3166 == null)
            ttsSupported = false;
        else {
            ttsEngines = null; // Thanks to the weird TTS api, the control flow depends on this null-ness.
            ttsEngineIndex = 0;
            tts = new TextToSpeech(this, this);
        }
        if (drawerActivity != null)
            drawerActivity.dropFragmentStates();
        if (exerciseFragment != null && exerciseFragment.isAdded())
            exerciseFragment.dbChanged();
        if (wordListFragment != null && wordListFragment.isAdded())
            wordListFragment.dbChanged();
    }

    private ArrayList<LanguageDB> discoverDBs() {
        String DATABASE_PATH = getExternalFilesDir(null).getPath();
        File[] files = new File(DATABASE_PATH).listFiles();
        ArrayList<LanguageDB> dbs = new ArrayList<>();
        for (int i=0; i<files.length; i++) {
            if (files[i].getName().endsWith(".db") == false) // SQLite creates auxiliary files. Ignore them.
                continue;
            LanguageDB dummyldb = new LanguageDB();
            dummyldb.dbPath = files[i].getAbsolutePath(); // Other fields in LanguageDB (*from* and *to*) are only used in case the DB is not found
            DatabaseHelper helper = new DatabaseHelper(dummyldb, true);
            dbs.add(helper.getLanguageDB());
            helper.close();
        }
        return dbs;
    }


    private void AddPairToAppState(Pair p) {
        wlist.add(p);
        assert(p.id != 0);
        pairsById.put(p.id, p);
        if (!wordTranslationsFwd.containsKey(p.first))
            wordTranslationsFwd.put(p.first, new HashSet());
        if (!wordTranslationsBwd.containsKey(p.second))
            wordTranslationsBwd.put(p.second, new HashSet());
        wordTranslationsFwd.get(p.first).add(p.second);
        wordTranslationsBwd.get(p.second).add(p.first);
    }

    void AddPair(Pair p) {
        helper.insertPair(p); // Note that this sets p.id
        AddPairToAppState(p); // And this uses it
    }

    void UpdatePair(Pair p) {
        helper.updatePair(p);
    }

    void RemovePair(Pair p) {
        wlist.remove(p);
        pairsById.remove(p.id);
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
        wlist = new HashSet<>();
        pairsById = new HashMap<>();
        wordTranslationsFwd = new HashMap<>();
        wordTranslationsBwd = new HashMap<>();
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
        estimates = helper.getEstimates();
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

    void pushEstimate(int estimate) {
        helper.pushEstimate(estimate, estimates);
    }

    Integer getKnownEstimate() {
        Double successFraction = helper.getSuccessFraction();
        if (successFraction == null) {
            return null;
        } else {
            return (int)Math.round(wlist.size() * successFraction);
        }
    }

    void forceSpeak(String s) {
        if (ttsSupported) {
            tts.speak(s, 0, null);
        }
    }

    void speak(String s) {
        if (!isMuted) {
            forceSpeak(s);
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

    boolean isACorrectAnswer(Pair p, String s, boolean currentFwd) {
        if (currentFwd)
            return (wordTranslationsFwd.get(p.first)).contains(s);
        else
            return (wordTranslationsBwd.get(p.second)).contains(s);
    }
}
