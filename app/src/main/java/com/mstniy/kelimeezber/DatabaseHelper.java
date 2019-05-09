package com.mstniy.kelimeezber;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper{

    private static final String TAG = DatabaseHelper.class.getName();
    static final int DEFAULT_PAIRQUEUE_LENGTH = 512;
    private static final String DATABASE_NAME = "kelimeezber_db";
    private static final String TABLE_KELIMELER = "kelimeler";
    private static final String TABLE_PAIRQUEUE = "pair_queue";
    private static final String TABLE_VARIABLES = "variables";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_FIRST = "first";
    private static final String COLUMN_SECOND = "second";
    private static final String COLUMN_HARDNESS = "hardness";
    private static final String COLUMN_PERIOD = "period";
    private static final String COLUMN_MAPPED_ID = "mapped_id";
    private static final String COLUMN_CURRENT_PAIRQUEUE_INDEX = "current_pairqueue_index";


    private static final String CREATE_PAIRQUEUE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_PAIRQUEUE + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_MAPPED_ID + " INTEGER,"
                    + "FOREIGN KEY (" + COLUMN_MAPPED_ID + ") REFERENCES " + TABLE_KELIMELER + "(" + COLUMN_ID + ") ON UPDATE SET NULL ON DELETE SET NULL"
                    + ")";

    private static final String CREATE_VARIABLES =
            "CREATE TABLE IF NOT EXISTS " + TABLE_VARIABLES + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_CURRENT_PAIRQUEUE_INDEX + " INTEGER"
                    + ")";

    private static final String CREATE_KELIMELER =
            "CREATE TABLE IF NOT EXISTS " + TABLE_KELIMELER+ "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_FIRST + " TEXT,"
                    + COLUMN_SECOND + " TEXT,"
                    + COLUMN_HARDNESS + " REAL,"
                    + COLUMN_PERIOD + " INTEGER"
                    + ")";


    private SQLiteDatabase db;

    public DatabaseHelper(Context context) {
        String DATABASE_FILE_PATH = context.getExternalFilesDir(null).getPath() + '/' + DATABASE_NAME;
        db = SQLiteDatabase.openDatabase(DATABASE_FILE_PATH, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY);
        db.rawQuery("PRAGMA foreign_keys = ON", null);
        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '"
                + TABLE_KELIMELER + "'", null);
        if (cursor.getCount() == 0)
            CreateDB();
        else
            MaybeUpgradeDB();
    }

    private void CreateDB() {
        Log.d(TAG, "Creating DB");
        db.execSQL(CREATE_KELIMELER);
        db.execSQL(CREATE_VARIABLES);
        db.execSQL(CREATE_PAIRQUEUE);
        for (int i=0; i<DEFAULT_PAIRQUEUE_LENGTH; i++)
            setPairQueueElement(i, (long)0);
        setCurrentQueueIndex(0);
    }

    private void MaybeUpgradeDB() {
        Cursor cursor = db.query(TABLE_KELIMELER,
                null,
                null,
                null, null, null, null, "0");
        if (cursor.getColumnIndex(COLUMN_PERIOD) != -1) {
            Log.d(TAG, "DB exists, no need to upgrade");
            return;
        }
        Log.d(TAG, "Upgrading DB");
        db.execSQL(CREATE_VARIABLES);
        db.execSQL(CREATE_PAIRQUEUE);
        String alterKelimeler = "ALTER TABLE " + TABLE_KELIMELER + " ADD " + COLUMN_PERIOD + " INTEGER DEFAULT 0;";
        db.execSQL(alterKelimeler);
        for (int i=0; i<DEFAULT_PAIRQUEUE_LENGTH; i++)
            setPairQueueElement(i, (long)0);
        setCurrentQueueIndex(0);
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

        // close db connection
        //db.close();

        // return newly inserted row id
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
                    new String[]{COLUMN_ID, COLUMN_FIRST, COLUMN_SECOND, COLUMN_HARDNESS, COLUMN_PERIOD},
                    null,
                    null, null, null, null, null);

        if (cursor == null)
            return new Pair[0];

        Pair[] pairs = new Pair[cursor.getCount()];

        final int ciId = cursor.getColumnIndexOrThrow(COLUMN_ID);
        final int ciFirst = cursor.getColumnIndexOrThrow(COLUMN_FIRST);
        final int ciSecond = cursor.getColumnIndexOrThrow(COLUMN_SECOND);
        final int ciHardness = cursor.getColumnIndexOrThrow(COLUMN_HARDNESS);
        final int ciPeriod = cursor.getColumnIndexOrThrow(COLUMN_PERIOD);

        cursor.moveToFirst();

        for (int i=0; !cursor.isAfterLast(); i++, cursor.moveToNext())
            pairs[i] = new Pair(cursor.getLong(ciId), cursor.getString(ciFirst), cursor.getString(ciSecond), cursor.getDouble(ciHardness), (int)cursor.getLong(ciPeriod));

        cursor.close();

        return pairs;
    }

    public long[] getPairQueueIds(){
        Cursor cursor = db.query(TABLE_PAIRQUEUE,
                new String[]{COLUMN_ID, COLUMN_MAPPED_ID},
                null,
                null, null, null, null, null);

        if (cursor == null)
            throw new RuntimeException("Failed to get a cursor");

        long[] pairQueueIds = new long[cursor.getCount()];

        final int ciId = cursor.getColumnIndexOrThrow(COLUMN_ID);
        final int ciMappedId = cursor.getColumnIndexOrThrow(COLUMN_MAPPED_ID);

        cursor.moveToFirst();

        for (int i=0; !cursor.isAfterLast(); i++, cursor.moveToNext()) {
            if (cursor.getLong(ciId) != i+1)
                throw new RuntimeException("PairQueue table ID's are not continuous!");
            if (cursor.isNull(ciMappedId) == false)
                pairQueueIds[i] = cursor.getLong(ciMappedId);
            else
                pairQueueIds[i] = 0;
        }

        cursor.close();

        return pairQueueIds;
    }

    public int getCurrentQueueIndex() {
        Cursor cursor = db.query(TABLE_VARIABLES,
                new String[]{COLUMN_ID, COLUMN_CURRENT_PAIRQUEUE_INDEX},
                null,
                null, null, null, null, null);

        if (cursor == null || cursor.getCount() == 0)
            return 0;

        final int ciId = cursor.getColumnIndexOrThrow(COLUMN_ID);
        final int ciCurrentPairQueueIndex = cursor.getColumnIndexOrThrow(COLUMN_CURRENT_PAIRQUEUE_INDEX);

        cursor.moveToFirst();

        int currentQueueIndex = (int)cursor.getLong(ciCurrentPairQueueIndex);

        cursor.close();

        return currentQueueIndex;
    }

    public void setCurrentQueueIndex(int index) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_CURRENT_PAIRQUEUE_INDEX, index);

        String[] whereArgs = new String[]{"1"};
        if (0 == db.update(TABLE_VARIABLES, values, COLUMN_ID + "=?", whereArgs))
            db.insert(TABLE_VARIABLES, null, values);
    }

    public void setPairQueueElement(int index, Long mappedId) {
        ContentValues values = new ContentValues();
        if (mappedId != 0)
            values.put(COLUMN_MAPPED_ID, mappedId);
        else
            values.putNull(COLUMN_MAPPED_ID);

        String[] whereArgs = new String[]{String.valueOf(index+1)};
        if (0 == db.update(TABLE_PAIRQUEUE, values, COLUMN_ID + "=?", whereArgs))
            db.insert(TABLE_PAIRQUEUE, null, values);
    }
}