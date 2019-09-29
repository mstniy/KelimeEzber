package com.mstniy.kelimeezber;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

class DatabaseHelper {
    private static final String TAG = DatabaseHelper.class.getName();
    private static final int DB_VERSION = 3;
    private static final int ERESULTS_QUEUE_LENGTH = 500;

    private static final String COLUMN_CURRENT_PAIRQUEUE_INDEX = "current_pairqueue_index";
    private static final String COLUMN_FIRST = "first";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_MAPPED_ID = "mapped_id";
    private static final String COLUMN_PERIOD = "period";
    private static final String COLUMN_NEXT = "next";
    private static final String COLUMN_ROUND_ID = "round_id";
    private static final String COLUMN_ERESULTS_INDEX = "eresults_index";
    private static final String COLUMN_RESULT = "result";
    private static final String COLUMN_SECOND = "second";
    private static final String COLUMN_AUDIO_DATASET_PATH = "audio_dataset_path";
    private static final String CREATE_KELIMELER = "CREATE TABLE IF NOT EXISTS kelimeler(id INTEGER PRIMARY KEY AUTOINCREMENT,first TEXT,second TEXT,period INTEGER,next INTEGER DEFAULT -1)";
    private static final String CREATE_VARIABLES = "CREATE TABLE IF NOT EXISTS variables(id INTEGER PRIMARY KEY AUTOINCREMENT,audio_dataset_path TEXT, round_id INTEGER, eresults_index INTEGER)";
    private static final String CREATE_EXERCISE_RESULTS = "CREATE TABLE IF NOT EXISTS eresults(id INTEGER PRIMARY KEY AUTOINCREMENT,result BOOLEAN)";
    private static final String DATABASE_NAME = "kelimeezber_db";
    private static final String TABLE_KELIMELER = "kelimeler";
    private static final String TABLE_PAIRQUEUE = "pair_queue";
    private static final String TABLE_VARIABLES = "variables";
    private static final String TABLE_EXERCISE_RESULTS = "eresults";

    private SQLiteDatabase db;

    private int eresults_index;
    private int eresults_length;

    public DatabaseHelper(Context context) {
        String DATABASE_FILE_PATH = context.getExternalFilesDir(null).getPath() + '/' + DATABASE_NAME;
        db = SQLiteDatabase.openDatabase(DATABASE_FILE_PATH, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY);
        db.rawQuery("PRAGMA foreign_keys = ON", null);
        if (db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = 'kelimeler'", null).getCount() == 0)
            CreateDB();
        else if (db.getVersion() != DB_VERSION)
            UpgradeDB(db.getVersion());
        else
            Log.v(TAG, "No need to upgrade the existing DB, already at version " + DB_VERSION);

        eresults_index = getEResultsIndex();
        eresults_length = getEResultsLength();
    }

    private void CreateDB() {
        Log.v(TAG, "Creating DB");

        db.execSQL(CREATE_KELIMELER);

        db.execSQL(CREATE_VARIABLES);
        db.execSQL("INSERT INTO " + TABLE_VARIABLES + "(audio_dataset_path, round_id, eresults_index) VALUES (0, 0, 0)");

        db.execSQL(CREATE_EXERCISE_RESULTS);
        ContentValues values = new ContentValues();
        // `id` will be inserted automatically, no need to add it
        values.putNull("result");
        for (int i=0; i<ERESULTS_QUEUE_LENGTH; i++)
            db.insert(TABLE_EXERCISE_RESULTS, null, values);

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
        if (version == 3)
            return ;
        else
            throw new RuntimeException("Unrecognized DB version.");
    }

    private void From0to1() {
        Log.v(TAG, "Upgrading the existing DB from version 0 to 1");
        db.execSQL("ALTER TABLE " + TABLE_VARIABLES + " ADD COLUMN " + COLUMN_AUDIO_DATASET_PATH + " TEXT");
        db.setVersion(1);
    }

    private void From1to2() {
        Log.v(TAG, "Upgrading the existing DB from version 1 to 2");
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
        Log.v(TAG, "Upgrading the existing DB from version 2 to 3");
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
        while (cursor.isAfterLast() == false) {
            if (cursor.isNull(ciCResult) == false) {
                boolean result = cursor.getInt(ciCResult) != 0;
                if (result)
                    a++;
                b++;
            }
            cursor.moveToNext();
        }
        cursor.close();
        if (b != 0)
            return a/b;
        else
            return null;
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
        Log.v(TAG, "Setting audio dataset path to \"" + path + "\"");
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

    public void close() {
        db.close();
    }
}
