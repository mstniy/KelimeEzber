package com.mstniy.kelimeezber;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper{

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "kelimeezber_db";
    private static final String TABLE_NAME = "kelimeler";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_FIRST = "first";
    private static final String COLUMN_SECOND = "second";


    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_FIRST + " TEXT,"
                + COLUMN_SECOND + " TEXT"
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

        // insert row
        long id = db.insert(TABLE_NAME, null, values);

        // close db connection
        //db.close();

        // return newly inserted row id
        return id;
    }

    public Pair[] getPairs() {
        Cursor cursor = db.query(TABLE_NAME,
                new String[]{COLUMN_FIRST, COLUMN_SECOND},
                null,
                null, null, null, null, null);

        if (cursor == null)
            return new Pair[0];

        Pair[] pairs = new Pair[cursor.getCount()];

        final int ciFirst = cursor.getColumnIndexOrThrow(COLUMN_FIRST);
        final int ciSecond = cursor.getColumnIndexOrThrow(COLUMN_SECOND);

        cursor.moveToFirst();

        for (int i=0; !cursor.isAfterLast(); i++, cursor.moveToNext())
            pairs[i] = new Pair(cursor.getString(ciFirst), cursor.getString(ciSecond));

        cursor.close();

        return pairs;
    }
}