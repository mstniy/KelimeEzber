package com.mstniy.kelimeezber;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;

class StampedEstimate implements Comparable<StampedEstimate>{
    public long timestamp;
    public int estimate;

    public StampedEstimate(long _timestamp, int _estimate) {
        timestamp = _timestamp;
        estimate = _estimate;
    }

    @Override
    public int compareTo(StampedEstimate o) {
        if (timestamp > o.timestamp)
            return 1;
        if (timestamp < o.timestamp)
            return -1;
        return 0;
    }
}

class DatabaseHelper {
    private static final String TAG = DatabaseHelper.class.getName();
    private static final int DB_VERSION = 6;
    private static final int ERESULTS_QUEUE_LENGTH = 75;
    private static final int MAX_ESTIMATE_COUNT = 365*10; // One estimate per day -> 10 years

    private static final String COLUMN_CURRENT_PAIRQUEUE_INDEX = "current_pairqueue_index";
    private static final String COLUMN_FIRST = "first";
    private static final String COLUMN_SECOND = "second";
    private static final String COLUMN_FROM = "from_lang";
    private static final String COLUMN_TO = "to_lang";
    private static final String COLUMN_TO_ISO639 = "to_iso639";
    private static final String COLUMN_TO_ISO3166 = "to_iso3166";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_MAPPED_ID = "mapped_id";
    private static final String COLUMN_PERIOD = "period";
    private static final String COLUMN_NEXT = "next";
    private static final String COLUMN_ROUND_ID = "round_id";
    private static final String COLUMN_ERESULTS_INDEX = "eresults_index";
    private static final String COLUMN_RESULT = "result";
    private static final String COLUMN_AUDIO_DATASET_PATH = "audio_dataset_path";
    private static final String COLUMN_TIME = "time";
    private static final String COLUMN_ESTIMATE = "estimate";
    private static final String CREATE_KELIMELER = "CREATE TABLE IF NOT EXISTS kelimeler(id INTEGER PRIMARY KEY AUTOINCREMENT,first TEXT,second TEXT,period INTEGER,next INTEGER DEFAULT -1)";
    private static final String CREATE_VARIABLES = "CREATE TABLE IF NOT EXISTS variables(id INTEGER PRIMARY KEY AUTOINCREMENT,audio_dataset_path TEXT, round_id INTEGER, eresults_index INTEGER)";
    private static final String CREATE_CONSTS = "CREATE TABLE IF NOT EXISTS constants(id INTEGER PRIMARY KEY AUTOINCREMENT,from_lang TEXT, to_lang TEXT, to_iso639 TEXT, to_iso3166 TEXT)";
    private static final String CREATE_EXERCISE_RESULTS = "CREATE TABLE IF NOT EXISTS eresults(id INTEGER PRIMARY KEY AUTOINCREMENT,result BOOLEAN)";
    private static final String CREATE_ESTIMATES = "CREATE TABLE IF NOT EXISTS estimates(time INTEGER PRIMARY KEY,estimate INTEGER)";
    private static final String TABLE_KELIMELER = "kelimeler";
    private static final String TABLE_PAIRQUEUE = "pair_queue";
    private static final String TABLE_CONSTS = "constants";
    private static final String TABLE_VARIABLES = "variables";
    private static final String TABLE_EXERCISE_RESULTS = "eresults";
    private static final String TABLE_ESTIMATES = "estimates";

    private static final String TABLE_CONFUSION = "confusion";
    private static final String CREATE_CONFUSION = "CREATE TABLE IF NOT EXISTS confusion(pair1 INTEGER REFERENCES kelimeler(id) ON DELETE CASCADE, pair2 INTEGER REFERENCES kelimeler(id) ON DELETE CASCADE, counter INTEGER, PRIMARY KEY(pair1, pair2)); CREATE INDEX confusion_pair1 ON confusion(pair1); CREATE INDEX confusion_pair2 ON confusion(pair2);";
    private static final String COLUMN_PAIR1 = "pair1";
    private static final String COLUMN_PAIR2 = "pair2";
    private static final String COLUMN_COUNTER = "counter";
    private static final int MAX_CONFUSION_PAIRS_PER_PAIR = 5;
    private static final int MAX_CONFUSION_COUNTER = 5; // Confusion counter saturates at this value

    private SQLiteDatabase db;
    private String dbPath;

    private int eresults_index;
    private int eresults_length;

    public DatabaseHelper(LanguageDB langDB, boolean create_if_necessary) {
        dbPath = langDB.dbPath;
        db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE | (create_if_necessary ? SQLiteDatabase.CREATE_IF_NECESSARY : 0));
        db.rawQuery("PRAGMA foreign_keys = ON", null);
        if (db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = 'kelimeler'", null).getCount() == 0)
            CreateDB(langDB);
        else if (db.getVersion() != DB_VERSION)
            UpgradeDB(db.getVersion());
        else
            Log.d(TAG, "No need to upgrade the existing DB, already at version " + DB_VERSION);

        eresults_index = getEResultsIndex();
        eresults_length = getEResultsLength();
    }

    private void CreateDB(LanguageDB langDB) {
        Log.d(TAG, "Creating DB");

        db.execSQL(CREATE_KELIMELER);

        db.execSQL(CREATE_VARIABLES);
        db.execSQL("INSERT INTO " + TABLE_VARIABLES + "(audio_dataset_path, round_id, eresults_index) VALUES (0, 0, 0)");

        db.execSQL(CREATE_CONSTS);
        ContentValues values = new ContentValues();
        values.put(COLUMN_FROM, langDB.from);
        values.put(COLUMN_TO, langDB.to);
        values.put(COLUMN_TO_ISO639, langDB.to_iso639);
        values.put(COLUMN_TO_ISO3166, langDB.to_iso3166);
        db.insert(TABLE_CONSTS, null, values);

        db.execSQL(CREATE_EXERCISE_RESULTS);
        values = new ContentValues();
        values.putNull("result");
        for (int i=0; i<ERESULTS_QUEUE_LENGTH; i++)
            db.insert(TABLE_EXERCISE_RESULTS, null, values);

        db.execSQL(CREATE_ESTIMATES);

        db.setVersion(DB_VERSION);
    }

    private void UpgradeDB(int version) {
        if (version == 0) {
            From0to1();
            version = 1;
        }
        if (version == 1) {
            From1to2();
            version = 2;
        }
        if (version == 2) {
            From2to3();
            version = 3;
        }
        if (version == 3) {
            From3to4();
            version = 4;
        }
        if (version == 4) {
            From4to5();
            version = 5;
        }
        if (version == 5) {
            From5to6();
            version = 6;
        }
        if (version == 6)
            return ;
        else
            throw new RuntimeException("Unrecognized DB version.");
    }

    private void From0to1() {
        Log.d(TAG, "Upgrading the existing DB from version 0 to 1");
        db.execSQL("ALTER TABLE " + TABLE_VARIABLES + " ADD COLUMN " + COLUMN_AUDIO_DATASET_PATH + " TEXT");
        db.setVersion(1);
    }

    private void From1to2() {
        Log.d(TAG, "Upgrading the existing DB from version 1 to 2");
        db.beginTransaction();
        try {
            db.execSQL("ALTER TABLE " + TABLE_VARIABLES + " ADD COLUMN " + COLUMN_ROUND_ID + " INTEGER");
            db.execSQL("CREATE TABLE kelimeler_tmp(id INTEGER PRIMARY KEY AUTOINCREMENT,first TEXT,second TEXT,period INTEGER,next INTEGER DEFAULT -1)");
            db.execSQL("INSERT INTO kelimeler_tmp(id, first, second, period) SELECT id,first,second,period FROM kelimeler");
            long[] pairQueue = getPairQueueIds();
            int queueIndex = getCurrentQueueIndex();
            for (int i=0; i<pairQueue.length; i++) {
                if (pairQueue[i] == 0)
                    continue;
                ContentValues values = new ContentValues();
                int moddedIndex = i-queueIndex;
                if (moddedIndex < 0)
                    moddedIndex += pairQueue.length;
                values.put(COLUMN_NEXT, moddedIndex);
                String[] whereArgs = new String[]{String.valueOf(pairQueue[i])};
                if (db.update("kelimeler_tmp", values, COLUMN_ID + "=?", whereArgs) != 1)
                    throw new RuntimeException("Failed to set the next field");
                Log.d(TAG, "upgraded " + pairQueue[i] + " with next " + moddedIndex);
            }
            db.execSQL("DROP TABLE " + TABLE_PAIRQUEUE);
            db.execSQL("DROP TABLE kelimeler");
            db.execSQL("ALTER TABLE kelimeler_tmp RENAME TO kelimeler");
            db.setVersion(2);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

    private void From2to3() {
        Log.d(TAG, "Upgrading the existing DB from version 2 to 3");
        db.beginTransaction();
        try {
            db.execSQL(CREATE_EXERCISE_RESULTS);
            ContentValues values = new ContentValues();
            // `id` will be inserted automatically, no need to add it
            values.putNull("result");
            for (int i=0; i<ERESULTS_QUEUE_LENGTH; i++)
                db.insert(TABLE_EXERCISE_RESULTS, null, values);

            db.execSQL("CREATE TABLE variables_tmp(id INTEGER PRIMARY KEY AUTOINCREMENT,audio_dataset_path TEXT, round_id INTEGER, eresults_index INTEGER)");
            db.execSQL("INSERT INTO variables_tmp(id, audio_dataset_path, round_id) SELECT id,audio_dataset_path,round_id FROM variables");
            /*values = new ContentValues();
            values.put(COLUMN_ERESULTS_INDEX, 0);
            db.update("variables_tmp", values, COLUMN_ID + "=?", new String[]{"1"});*/

            db.execSQL("DROP TABLE " + TABLE_VARIABLES);
            db.execSQL("ALTER TABLE variables_tmp RENAME TO variables");
            db.setVersion(3);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

    private void From3to4() {
        Log.d(TAG, "Upgrading the existing DB from version 3 to 4");
        db.beginTransaction();
        try {
            db.execSQL(CREATE_CONSTS);
            ContentValues values = new ContentValues();
            values.put(COLUMN_FROM, "English"); // We have to use default values because db versions before 4 did not include language info.
            values.put(COLUMN_TO, "Swedish");
            values.put(COLUMN_TO_ISO639, "sv");
            values.put(COLUMN_TO_ISO3166, "SE");
            db.insert(TABLE_CONSTS, null, values);

            db.setVersion(4);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

    private void From4to5() {
        Log.d(TAG, "Upgrading the existing DB from version 4 to 5");
        db.beginTransaction();
        try {
            db.execSQL(CREATE_ESTIMATES);

            db.setVersion(5);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

    private void From5to6() {
        Log.d(TAG, "Upgrading the existing DB from version 5 to 6");
        db.beginTransaction();
        try {
            db.execSQL(CREATE_CONFUSION);

            db.setVersion(6);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

    public long insertPair(Pair p) {
        ContentValues values = new ContentValues();
        // `id` will be inserted automatically.
        // no need to add them
        values.put(COLUMN_FIRST, p.first);
        values.put(COLUMN_SECOND, p.second);
        values.put(COLUMN_PERIOD, p.period);

        // insert row
        long id = db.insert(TABLE_KELIMELER, null, values);
        p.id = id;

        // return the newly inserted row id
        return id;
    }

    public int removePair(Pair p) {
        String[] whereArgs = new String[]{String.valueOf(p.id)};
        return db.delete(TABLE_KELIMELER, COLUMN_ID + "=?", whereArgs);
    }

    public int updatePair(Pair p) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_FIRST, p.first);
        values.put(COLUMN_SECOND, p.second);
        values.put(COLUMN_PERIOD, p.period);
        values.put(COLUMN_NEXT, p.next);

        String[] whereArgs = new String[]{String.valueOf(p.id)};
        return db.update(TABLE_KELIMELER, values, COLUMN_ID + "=?", whereArgs);
    }

    public Pair[] getPairs() {
        Cursor cursor = db.query(TABLE_KELIMELER,
                new String[]{COLUMN_ID, COLUMN_FIRST, COLUMN_SECOND, COLUMN_PERIOD, COLUMN_NEXT}, null, null, null, null, null, null);
        if (cursor == null) {
            return new Pair[0];
        }
        Pair[] pairs = new Pair[cursor.getCount()];

        final int ciId = cursor.getColumnIndexOrThrow(COLUMN_ID);
        final int ciFirst = cursor.getColumnIndexOrThrow(COLUMN_FIRST);
        final int ciSecond = cursor.getColumnIndexOrThrow(COLUMN_SECOND);
        final int ciPeriod = cursor.getColumnIndexOrThrow(COLUMN_PERIOD);
        final int ciNext = cursor.getColumnIndexOrThrow(COLUMN_NEXT);
        cursor.moveToFirst();
        for(int i=0; !cursor.isAfterLast(); i++, cursor.moveToNext())
            pairs[i] = new Pair(cursor.getLong(ciId), cursor.getString(ciFirst), cursor.getString(ciSecond), (int) cursor.getLong(ciPeriod), (int) cursor.getLong(ciNext));

        cursor.close();

        return pairs;
    }

    public long[] getPairQueueIds() {
        Cursor cursor = db.query(TABLE_PAIRQUEUE, new String[]{COLUMN_ID, COLUMN_MAPPED_ID}, null, null, null, null, null, null);
        if (cursor != null) {
            long[] pairQueueIds = new long[cursor.getCount()];
            int ciId = cursor.getColumnIndexOrThrow(COLUMN_ID);
            int ciMappedId = cursor.getColumnIndexOrThrow(COLUMN_MAPPED_ID);
            cursor.moveToFirst();
            for(int i=0; !cursor.isAfterLast(); i++, cursor.moveToNext()) {
                if (cursor.getLong(ciId) != ((long) (i + 1)))
                    throw new RuntimeException("PairQueue table ID's are not continuous!");
                if (!cursor.isNull(ciMappedId))
                    pairQueueIds[i] = cursor.getLong(ciMappedId);
                else
                    pairQueueIds[i] = 0;
            }
            cursor.close();
            return pairQueueIds;
        }
        throw new RuntimeException("Failed to get a cursor");
    }

    public int getCurrentQueueIndex() {
        Cursor cursor = db.query(TABLE_VARIABLES, new String[]{COLUMN_ID, COLUMN_CURRENT_PAIRQUEUE_INDEX}, null, null, null, null, null, null);
        int ciCurrentPairQueueIndex = cursor.getColumnIndexOrThrow(COLUMN_CURRENT_PAIRQUEUE_INDEX);
        cursor.moveToFirst();
        int currentQueueIndex = (int) cursor.getLong(ciCurrentPairQueueIndex);
        cursor.close();
        return currentQueueIndex;
    }

    public LanguageDB getLanguageDB() {
        LanguageDB ldb = new LanguageDB();
        ldb.dbPath = dbPath;
        Cursor cursor = db.query(TABLE_CONSTS, new String[]{COLUMN_FROM, COLUMN_TO, COLUMN_TO_ISO639, COLUMN_TO_ISO3166}, null, null, null, null, null, null);
        cursor.moveToFirst();
        ldb.from = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FROM));
        ldb.to = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TO));
        ldb.to_iso639 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TO_ISO639));
        ldb.to_iso3166 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TO_ISO3166));
        cursor.close();
        return ldb;
    }

    public int getRoundID() {
        Cursor cursor = db.query(TABLE_VARIABLES, new String[]{COLUMN_ID, COLUMN_ROUND_ID}, null, null, null, null, null, null);
        int ciCRoundID = cursor.getColumnIndexOrThrow(COLUMN_ROUND_ID);
        cursor.moveToFirst();
        int roundId = (int) cursor.getLong(ciCRoundID);
        cursor.close();
        return roundId;
    }

    private int getEResultsIndex() {
        Cursor cursor = db.query(TABLE_VARIABLES, new String[]{COLUMN_ID, COLUMN_ERESULTS_INDEX}, null, null, null, null, null, null);
        int ciEResultsIndex = cursor.getColumnIndexOrThrow(COLUMN_ERESULTS_INDEX);
        cursor.moveToFirst();
        int roundId = (int) cursor.getLong(ciEResultsIndex);
        cursor.close();
        return roundId;
    }

    public Double getSuccessFraction() {
        Cursor cursor = db.query(TABLE_EXERCISE_RESULTS, new String[]{COLUMN_ID, COLUMN_RESULT}, null, null, null, null, null, null);
        int ciCResult = cursor.getColumnIndexOrThrow(COLUMN_RESULT);
        cursor.moveToFirst();
        double a=0, b=0;
        int id=0;
        while (cursor.isAfterLast() == false) {
            if (cursor.isNull(ciCResult) == false) {
                if (eresults_length <= ERESULTS_QUEUE_LENGTH || // If the eresults table in the DB is larger than ERESULTS_QUEUE_LENGTH, take into account only the last ERESULTS_QUEUE_LENGTH results.
                        (id>=eresults_index-ERESULTS_QUEUE_LENGTH && id<eresults_index) ||
                        id-eresults_length>=eresults_index-ERESULTS_QUEUE_LENGTH) {
                    boolean result = cursor.getInt(ciCResult) != 0;
                    if (result)
                        a++;
                    b++;
                }
            }
            cursor.moveToNext();
            id++;
        }
        cursor.close();
        if (b != 0)
            return a/b;
        else
            return null;
    }

    public ArrayList<StampedEstimate> getEstimates() {
        Cursor cursor = db.query(TABLE_ESTIMATES, new String[]{COLUMN_TIME, COLUMN_ESTIMATE}, null, null, null, null, null, null);
        int ciTime = cursor.getColumnIndexOrThrow(COLUMN_TIME);
        int ciEstimate = cursor.getColumnIndexOrThrow(COLUMN_ESTIMATE);
        cursor.moveToFirst();

        ArrayList<StampedEstimate> res = new ArrayList<>();
        while (cursor.isAfterLast() == false) {
            res.add(new StampedEstimate(cursor.getLong(ciTime), cursor.getInt(ciEstimate)));
            cursor.moveToNext();
        }
        cursor.close();
        return res;
    }

    public void pushEstimate(int estimate, ArrayList<StampedEstimate> estimates) {
        int numEstimates = (int)DatabaseUtils.queryNumEntries(db, TABLE_ESTIMATES);
        if (numEstimates >= MAX_ESTIMATE_COUNT) { // Don't forget that we'll push one value
            db.execSQL("DELETE FROM " + TABLE_ESTIMATES + " WHERE " + COLUMN_TIME + " IN ( " +
                    "SELECT " + COLUMN_TIME + " FROM " + TABLE_ESTIMATES + " ORDER BY " + COLUMN_TIME + " LIMIT " + (numEstimates-MAX_ESTIMATE_COUNT+1)
            );
            for (int i=0; i< numEstimates-MAX_ESTIMATE_COUNT+1; i++) // Remove the first few elements
                estimates.remove(0);
        }
        long timestamp = System.currentTimeMillis()/1000;
        ContentValues values = new ContentValues();
        values.put(COLUMN_TIME, timestamp);
        values.put(COLUMN_ESTIMATE, estimate);
        if (-1 == db.insertWithOnConflict(TABLE_ESTIMATES, null, values, SQLiteDatabase.CONFLICT_REPLACE))
            throw new RuntimeException("pushEstimate failed");
        estimates.add(new StampedEstimate(timestamp, estimate));
    }

    public int getEResultsLength() {
        Cursor cursor = db.query(TABLE_EXERCISE_RESULTS, new String[]{COLUMN_ID}, null, null, null, null, null, null);
        int length = cursor.getCount();
        cursor.close();
        return length;
    }

    public String getAudioDatasetPath() {
        Cursor cursor = db.query(TABLE_VARIABLES, new String[]{COLUMN_ID, COLUMN_AUDIO_DATASET_PATH}, null, null, null, null, null, null);
        int ciADP = cursor.getColumnIndexOrThrow(COLUMN_AUDIO_DATASET_PATH);
        cursor.moveToFirst();
        String path = cursor.getString(ciADP);
        cursor.close();
        return path;
    }

    public void setAudioDatasetPath(String path) {
        Log.d(TAG, "Setting audio dataset path to \"" + path + "\"");
        ContentValues values = new ContentValues();
        values.put(COLUMN_AUDIO_DATASET_PATH, path);
        if (db.update(TABLE_VARIABLES, values, COLUMN_ID + "=?", new String[]{"1"}) != 1)
            throw new RuntimeException("setAudioDatasetPath failed");
    }

    public void setRoundID(int roundId) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ROUND_ID, roundId);
        if (db.update(TABLE_VARIABLES, values, COLUMN_ID + "=?", new String[]{"1"}) != 1)
            throw new RuntimeException("setRoundID failed");
    }

    private void setEResultsIndex(int eresults_index) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ERESULTS_INDEX, eresults_index);
        if (db.update(TABLE_VARIABLES, values, COLUMN_ID + "=?", new String[]{"1"}) != 1)
            throw new RuntimeException("setEResultsIndex failed");
    }

    public void pushExerciseResult(boolean result) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_RESULT, result);
        if (db.update(TABLE_EXERCISE_RESULTS, values, COLUMN_ID + "=?", new String[]{String.valueOf(eresults_index+1)}) != 1) // SQLite id's are 1-based, so we add 1 here
            throw new RuntimeException("pushExerciseResult failed");
        eresults_index = (eresults_index+1)%eresults_length;
        setEResultsIndex(eresults_index);
    }

    public void increaseConfusion(Long pair1, Long pair2) {
        if (pair1 == pair2) // One does not confuse a word with itself
            return ;
        if (pair1 > pair2) {
            // Swap the pairs such that pair1 has the lower id
            Long tmp = pair1;
            pair1 = pair2;
            pair2 = tmp; // I love Java
        }
        Cursor cur_exists = db.rawQuery("SELECT counter FROM " + TABLE_CONFUSION + " WHERE pair1 = ? AND pair2 = ?", new String[]{String.valueOf(pair1), String.valueOf(pair2)});
        cur_exists.moveToFirst();
        if (cur_exists.getCount() == 0) { // This confusion is new, add it to the DB
            Cursor cur1 = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CONFUSION + " WHERE pair1 = ?", new String[]{String.valueOf(pair1)});
            cur1.moveToFirst();
            int count_pair1 = cur1.getInt(0);
            cur1.close();
            if (count_pair1 > MAX_CONFUSION_PAIRS_PER_PAIR) { // If the confusion list for the first pair is full...
                Cursor cur2 = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CONFUSION + " WHERE pair2 = ?", new String[]{String.valueOf(pair2)});
                cur2.moveToFirst();
                int count_pair2 = cur2.getInt(0);
                cur2.close();
                if (count_pair2 > MAX_CONFUSION_PAIRS_PER_PAIR) { // If the confusion list for the second pair is also full...
                    // Delete the confusion entry with the lowest counter from either pair1 or pair2
                    Cursor cur_del = db.query(TABLE_CONFUSION, new String[]{COLUMN_PAIR1, COLUMN_PAIR2}, "pair1 = ? OR pair2 = ?", new String[]{String.valueOf(pair1), String.valueOf(pair2)}, null, null, "counter", "1");
                    db.delete(TABLE_CONFUSION, "pair1 = ? AND pair2 = ?", new String[]{String.valueOf(cur_del.getInt(0)), String.valueOf(cur_del.getInt(1))});
                    cur_del.close();
                }
            }
            ContentValues values = new ContentValues(); // Insert the new confusion
            values.put(COLUMN_PAIR1, pair1);
            values.put(COLUMN_PAIR2, pair2);
            values.put(COLUMN_COUNTER, 3);
            db.insert(TABLE_CONFUSION, null, values);
        }
        else { // This confusion is not new. Increase its counter, if it is not saturated
            int oldCounter = cur_exists.getInt(0);
            if (oldCounter < MAX_CONFUSION_COUNTER) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_PAIR1, pair1);
                values.put(COLUMN_PAIR2, pair2);
                values.put(COLUMN_COUNTER, oldCounter + 1);
                db.update(TABLE_CONFUSION, values, "pair1 = ? AND pair2 = ?", new String[]{String.valueOf(pair1), String.valueOf(pair2)});
            }
        }
        cur_exists.close();
    }

    public void decreaseConfusion(Long pair1, Long pair2) {
        if (pair1 > pair2) {
            // Swap the pairs such that pair1 has the lower id
            Long tmp = pair1;
            pair1 = pair2;
            pair2 = tmp; // I love Java
        }
        Cursor cur_exists = db.rawQuery("SELECT counter FROM " + TABLE_CONFUSION + " WHERE pair1 = ? AND pair2 = ?", new String[]{String.valueOf(pair1), String.valueOf(pair2)});
        if (cur_exists.getCount() == 0)
            return ;
        cur_exists.moveToFirst();
        int oldCounter = cur_exists.getInt(cur_exists.getColumnIndexOrThrow("counter"));
        if (oldCounter <= 1) {
            db.delete(TABLE_CONFUSION, "pair1 = ? AND pair2 = ?", new String[]{String.valueOf(pair1), String.valueOf(pair2)});
        }
        else {
            ContentValues values = new ContentValues();
            values.put(COLUMN_COUNTER, oldCounter - 1);
            db.update(TABLE_CONFUSION, values, "pair1 = ? AND pair2 = ?", new String[]{String.valueOf(pair1), String.valueOf(pair2)});
        }
        cur_exists.close();
    }

    public ArrayList<Long> getConfusionsForPair(Long pair) {
        HashSet<Long> results = new HashSet<>();
        Cursor cur = db.query(TABLE_CONFUSION, new String[]{"pair1", "pair2"}, "pair1 = ? OR pair2 = ?", new String[]{String.valueOf(pair), String.valueOf(pair)}, null, null, null);
        if (cur.moveToFirst()) {
            do {
                results.add(cur.getLong(0));
                results.add(cur.getLong(1));
            } while (cur.moveToNext());
        }
        results.remove(pair);
        return new ArrayList<>(results);
    }

    public void close() {
        db.close();
    }
}
