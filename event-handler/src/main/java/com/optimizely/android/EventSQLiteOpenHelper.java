package com.optimizely.android;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.optimizely.android.EventContract.Event;

/**
 * Created by jdeffibaugh on 7/21/16 for Optimizely.
 *
 * Handles transactions for the events db
 */
public class EventSQLiteOpenHelper extends SQLiteOpenHelper {

    static final int VERSION = 1;
    static final String DB_NAME = "optly-events";

    private static final String SQL_CREATE_EVENT_TABLE =
            "CREATE TABLE " + Event.TABLE_NAME + " (" +
                    Event._ID + " INTEGER PRIMARY KEY, " +
                    Event.COLUMN_NAME_URL + "TEXT NOT NULL" +
            ")";

    private static final String SQL_DELETE_EVENT_TABLE =
            "DROP TABLE IF EXISTS " + Event.TABLE_NAME;

    public EventSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public EventSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_EVENT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Create an upgrade strategy once we actually upgrade the schema.
    }
}
