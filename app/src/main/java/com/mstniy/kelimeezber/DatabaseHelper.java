package com.mstniy.kelimeezber;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

class Fraction {
    int a;
    int b;

    public Fraction(int _a, int _b) {
        a = _a;
        b = _b;
    }
}

class DatabaseHelper {
    private static final String TAG = DatabaseHelper.class.getName();
    static final int DEFAULT_PAIRQUEUE_LENGTH = 512;
    private static final int DB_VERSION = 1;

    private static final String COLUMN_CURRENT_PAIRQUEUE_INDEX = "current_pairqueue_index";
    private static final String COLUMN_FIRST = "first";
    private static final String COLUMN_HARDNESS = "hardness";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_MAPPED_ID = "mapped_id";
    private static final String COLUMN_PERIOD = "period";
    private static final String COLUMN_RANDOM_PASS_COUNTER = "random_pass_counter";
    private static final String COLUMN_RANDOM_PASS_OUTOF = "random_pass_outof";
    private static final String COLUMN_SECOND = "second";
    private static final String COLUMN_AUDIO_DATASET_PATH = "audio_dataset_path";
    private static final String CREATE_KELIMELER = "CREATE TABLE IF NOT EXISTS kelimeler(id INTEGER PRIMARY KEY AUTOINCREMENT,first TEXT,second TEXT,hardness REAL,period INTEGER)";
    private static final String CREATE_PAIRQUEUE = "CREATE TABLE IF NOT EXISTS pair_queue(id INTEGER PRIMARY KEY AUTOINCREMENT,mapped_id INTEGER,FOREIGN KEY (mapped_id) REFERENCES kelimeler(id) ON UPDATE SET NULL ON DELETE SET NULL)";
    private static final String CREATE_VARIABLES = "CREATE TABLE IF NOT EXISTS variables(id INTEGER PRIMARY KEY AUTOINCREMENT,current_pairqueue_index INTEGER,random_pass_counter INTEGER,random_pass_outof INTEGER,audio_dataset_path TEXT)";
    private static final String DATABASE_NAME = "kelimeezber_db";
    private static final String TABLE_KELIMELER = "kelimeler";
    private static final String TABLE_PAIRQUEUE = "pair_queue";
    private static final String TABLE_VARIABLES = "variables";

    private SQLiteDatabase db;

    public DatabaseHelper(Context context) {
        String DATABASE_FILE_PATH = context.getExternalFilesDir(null).getPath() + '/' + DATABASE_NAME;
        db = SQLiteDatabase.openDatabase(DATABASE_FILE_PATH, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY);
        db.rawQuery("PRAGMA foreign_keys = ON", null);
        if (db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = 'kelimeler'", null).getCount() == 0)
            CreateDB();
        else if (db.needUpgrade(DB_VERSION))
            UpgradeDB(db.getVersion());
        else
            Log.v(TAG, "No need to upgrade the existing DB, already at version " + DB_VERSION);
    }

    private void CreateDB() {
        Log.v(TAG, "Creating DB");
        db.execSQL(CREATE_KELIMELER);
        db.execSQL(CREATE_VARIABLES);
        db.execSQL(CREATE_PAIRQUEUE);
        for (int i = 0; i < DEFAULT_PAIRQUEUE_LENGTH; i++) {
            setPairQueueElement(i, (long)0);
        }
        setCurrentQueueIndex(0);
        db.setVersion(DB_VERSION);
    }

    private void UpgradeDB(int oldVersion) {
        if (oldVersion == 0) {
            From0to1();
            oldVersion = 1;
        }
        if (oldVersion == 1)
            return ;
        else
            throw new RuntimeException("Unrecognized DB version.");
    }

    private void From0to1() {
        Log.v(TAG, "Upgrading the existing DB from version 0 to 1");
        db.execSQL("ALTER TABLE " + TABLE_VARIABLES + " ADD COLUMN " + COLUMN_AUDIO_DATASET_PATH + " TEXT");
        db.setVersion(1);
    }

    public long insertPair(Pair p) {
        ContentValues values = new ContentValues();
        // `id` will be inserted automatically.
        // no need to add them
        values.put(COLUMN_FIRST, p.first);
        values.put(COLUMN_SECOND, p.second);
        values.put(COLUMN_HARDNESS, p.hardness);
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
        values.put(COLUMN_HARDNESS, p.hardness);
        values.put(COLUMN_PERIOD, p.period);

        String[] whereArgs = new String[]{String.valueOf(p.id)};
        return db.update(TABLE_KELIMELER, values, COLUMN_ID + "=?", whereArgs);
    }

    public Pair[] getPairs() {
        Cursor cursor = db.query(TABLE_KELIMELER,
                new String[]{COLUMN_ID, COLUMN_FIRST, COLUMN_SECOND, COLUMN_HARDNESS, COLUMN_PERIOD}, null, null, null, null, null, null);
        if (cursor == null) {
            return new Pair[0];
        }
        Pair[] pairs = new Pair[cursor.getCount()];

        final int ciId = cursor.getColumnIndexOrThrow(COLUMN_ID);
        final int ciFirst = cursor.getColumnIndexOrThrow(COLUMN_FIRST);
        final int ciSecond = cursor.getColumnIndexOrThrow(COLUMN_SECOND);
        final int ciHardness = cursor.getColumnIndexOrThrow(COLUMN_HARDNESS);
        final int ciPeriod = cursor.getColumnIndexOrThrow(COLUMN_PERIOD);
        cursor.moveToFirst();
        for(int i=0; !cursor.isAfterLast(); i++, cursor.moveToNext())
            pairs[i] = new Pair(cursor.getLong(ciId), cursor.getString(ciFirst), cursor.getString(ciSecond), cursor.getDouble(ciHardness), (int) cursor.getLong(ciPeriod));

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

    public Fraction getRandomPassFraction() {
        Cursor cursor = db.query(TABLE_VARIABLES, new String[]{COLUMN_ID, COLUMN_RANDOM_PASS_COUNTER, COLUMN_RANDOM_PASS_OUTOF}, null, null, null, null, null, null);
        int ciRandomPassCounter = cursor.getColumnIndexOrThrow(COLUMN_RANDOM_PASS_COUNTER);
        int ciRandomPassOutOf = cursor.getColumnIndexOrThrow(COLUMN_RANDOM_PASS_OUTOF);
        cursor.moveToFirst();
        int counter = (int) cursor.getLong(ciRandomPassCounter);
        int outof = (int) cursor.getLong(ciRandomPassOutOf);
        cursor.close();
        return new Fraction(counter, outof);
    }

    public String getAudioDatasetPath() {
        Cursor cursor = db.query(TABLE_VARIABLES, new String[]{COLUMN_ID, COLUMN_AUDIO_DATASET_PATH}, null, null, null, null, null, null);
        int ciADP = cursor.getColumnIndexOrThrow(COLUMN_AUDIO_DATASET_PATH);
        cursor.moveToFirst();
        String path = cursor.getString(ciADP);
        cursor.close();
        return path;
    }

    public void setCurrentQueueIndex(int index) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_CURRENT_PAIRQUEUE_INDEX, Integer.valueOf(index));
        if (db.update(TABLE_VARIABLES, values, COLUMN_ID + "=?", new String[]{"1"}) == 0)
            db.insert(TABLE_VARIABLES, null, values);
    }

    public void setRandomPassFraction(Fraction fraction) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_RANDOM_PASS_COUNTER, fraction.a);
        values.put(COLUMN_RANDOM_PASS_OUTOF, fraction.b);
        if (db.update(TABLE_VARIABLES, values, COLUMN_ID + "=?", new String[]{"1"}) == 0)
            db.insert(TABLE_VARIABLES, null, values);
    }

    public void setPairQueueElement(int index, Long mappedId) {
        ContentValues values = new ContentValues();
        if (mappedId != 0)
            values.put(COLUMN_MAPPED_ID, mappedId);
        else
            values.putNull(COLUMN_MAPPED_ID);
        String[] whereArgs = new String[]{String.valueOf(index + 1)};
        if (0 == db.update(TABLE_PAIRQUEUE, values, COLUMN_ID + "=?", whereArgs))
            db.insert(TABLE_PAIRQUEUE, null, values);
    }

    public void setAudioDatasetPath(String path) {
        Log.v(TAG, "Setting audio dataset path to \"" + path + "\"");
        ContentValues values = new ContentValues();
        values.put(COLUMN_AUDIO_DATASET_PATH, path);
        if (db.update(TABLE_VARIABLES, values, COLUMN_ID + "=?", new String[]{"1"}) != 1)
            throw new RuntimeException("setAudioDatasetPath failed");
    }
}
