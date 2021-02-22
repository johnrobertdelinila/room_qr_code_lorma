package com.example.johnrobertdelinila.roomqrcode.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.johnrobertdelinila.roomqrcode.utils.Upload;

import java.util.Date;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TABLE_NAME = "uploads_table";
    private static final String COL_ID = "id";
    private static final String COL_IMEI = "imei";
    private static final String COL_TIMESTAMP = "timestamp";
    private static final String COL_RAW_VALUE = "raw_value";
    private static final String COL_TIMEIN = "timein";
    private static final String COL_ONLINE = "online";

    public DatabaseHelper(Context context) {
        super(context, TABLE_NAME, null, 3);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + "("+ COL_ID +" INTEGER PRIMARY KEY AUTOINCREMENT, " + COL_IMEI + " TEXT, " +
                COL_TIMESTAMP + " TEXT, " + COL_RAW_VALUE + " TEXT, "+ COL_TIMEIN + " TINYINT, " + COL_ONLINE + " TINYINT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean addUpload(String imei, String timestamp, String raw_value, Boolean isTimeIn, Boolean isOnline) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_IMEI, imei); // 1
        contentValues.put(COL_TIMESTAMP, timestamp); // 2
        contentValues.put(COL_RAW_VALUE, raw_value); // 3
        contentValues.put(COL_TIMEIN, (isTimeIn ? 1 : 0)); // 4
        contentValues.put(COL_ONLINE, (isOnline ? 1 : 0)); // 5

        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public boolean deleteUpload(int id) {
        boolean isDeleted;

        SQLiteDatabase db = this.getWritableDatabase();
        isDeleted = db.delete(TABLE_NAME, COL_ID + "='" + id + "'", null) > 0;

        return isDeleted;
    }

    public Cursor getData() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME;
        return db.rawQuery(query, null);
    }

    public Upload getTheLastUpload() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(query, null);
        data.moveToLast();

        String key = String.valueOf(data.getInt(0));
        String imei = data.getString(1);
        String timestamp = data.getString(2);
        Date offlineDate = new Date(Long.parseLong(timestamp));
        String rawValue = data.getString(3);
        int timeIn = data.getInt(4);
        int online = data.getInt(5);

        boolean isTimeIn = true;
        if (timeIn == 0) {
            isTimeIn = false;
        }
        boolean isOnline = true;
        if (online == 0) {
            isOnline = false;
        }

        Upload upload = new Upload(rawValue, imei, offlineDate, isTimeIn, isOnline);
        upload.setKey(key);
        return upload;
    }

}
