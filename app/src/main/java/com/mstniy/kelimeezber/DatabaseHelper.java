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
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "kelimeezber_db";
    private static final String TABLE_NAME = "kelimeler";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_FIRST = "first";
    private static final String COLUMN_SECOND = "second";
    private static final String COLUMN_HARDNESS = "hardness";


    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_FIRST + " TEXT,"
                + COLUMN_SECOND + " TEXT,"
                + COLUMN_HARDNESS + " REAL"
                + ")";


    private SQLiteDatabase db;

    public DatabaseHelper(Context context) {
        String DATABASE_FILE_PATH = context.getExternalFilesDir(null).getPath() + '/' + DATABASE_NAME;
        db = SQLiteDatabase.openDatabase(DATABASE_FILE_PATH, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY);
        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '"
                + TABLE_NAME + "'", null);
        if (cursor.getCount() == 0)
            create();
    }

    // Creating Tables
    public void create() {
        // create notes table
        db.execSQL(CREATE_TABLE);
    }

    public long insertPair(Pair p) {

        ContentValues values = new ContentValues();
        // `id` will be inserted automatically.
        // no need to add them
        values.put(COLUMN_FIRST, p.first);
        values.put(COLUMN_SECOND, p.second);
        values.put(COLUMN_HARDNESS, p.hardness);

        // insert row
        long id = db.insert(TABLE_NAME, null, values);
        p.id = id;

        // close db connection
        //db.close();

        // return newly inserted row id
        return id;
    }

    public int removePair(Pair p) {
        String[] whereArgs = new String[]{String.valueOf(p.id)};
        return db.delete(TABLE_NAME, COLUMN_ID + "=?", whereArgs);
    }

    public int updatePair(Pair p) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_FIRST, p.first);
        values.put(COLUMN_SECOND, p.second);
        values.put(COLUMN_HARDNESS, p.hardness);

        String[] whereArgs = new String[]{String.valueOf(p.id)};
        return db.update(TABLE_NAME, values, COLUMN_ID + "=?", whereArgs);
    }

    public Pair[] getPairs() {
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NAME,
                    new String[]{COLUMN_ID, COLUMN_FIRST, COLUMN_SECOND, COLUMN_HARDNESS},
                    null,
                    null, null, null, null, null);
        }
        catch (SQLiteException e) {
            e.printStackTrace();
            // Probably the DB doesn't have the hardness field (old versions did not have that).
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD " + COLUMN_HARDNESS + " REAL DEFAULT 0");
            cursor = db.query(TABLE_NAME,
                    new String[]{COLUMN_ID, COLUMN_FIRST, COLUMN_SECOND, COLUMN_HARDNESS},
                    null,
                    null, null, null, null, null);
        }

        if (cursor == null)
            return new Pair[0];

        Pair[] pairs = new Pair[cursor.getCount()];

        final int ciId = cursor.getColumnIndexOrThrow(COLUMN_ID);
        final int ciFirst = cursor.getColumnIndexOrThrow(COLUMN_FIRST);
        final int ciSecond = cursor.getColumnIndexOrThrow(COLUMN_SECOND);
        final int ciHardness = cursor.getColumnIndexOrThrow(COLUMN_HARDNESS);

        cursor.moveToFirst();

        for (int i=0; !cursor.isAfterLast(); i++, cursor.moveToNext())
            pairs[i] = new Pair(cursor.getLong(ciId), cursor.getString(ciFirst), cursor.getString(ciSecond), cursor.getDouble(ciHardness));

        cursor.close();

        return pairs;
    }

}