package com.optimizely.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Pair;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.optimizely.android.EventContract.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jdeffibaugh on 7/21/16 for Optimizely.
 *
 * Handles interactions with the {@link SQLiteDatabase} that stores
 * built event {@link URL}s.
 */
public class EventDAO {

    @NonNull SQLiteDatabase db;
    @NonNull Logger logger;

    private EventDAO(@NonNull SQLiteDatabase db, @NonNull Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    static EventDAO getInstance(@NonNull Context context, @NonNull Logger logger) {
        EventSQLiteOpenHelper sqLiteOpenHelper = new EventSQLiteOpenHelper(context, EventSQLiteOpenHelper.DB_NAME, null, EventSQLiteOpenHelper.VERSION, null, LoggerFactory.getLogger(EventSQLiteOpenHelper.class));
        SQLiteDatabase sqLiteDatabase = sqLiteOpenHelper.getWritableDatabase();
        return new EventDAO(sqLiteDatabase, logger);
    }

    boolean storeEvent(@NonNull URLProxy event) {
        ContentValues values = new ContentValues();
        values.put(Event.COLUMN_NAME_URL, event.toString());

        // Since we are setting the "null column hack" param to null empty values will not be inserted
        // at all instead of inserting null.
        long newRowId;
        newRowId = db.insert(Event.TABLE_NAME, null, values);

        logger.info("Inserted {} into db", event);

        return newRowId != -1;
    }

    List<Pair<Long, URLProxy>> getEvents() {
        List<Pair<Long, URLProxy>> events = new ArrayList<>();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                Event._ID,
                Event.COLUMN_NAME_URL
        };

        Cursor cursor = db.query(
                Event.TABLE_NAME,           // The table to query
                projection,                 // The columns to return
                null,                       // The columns for the WHERE clause
                null,                       // The values for the WHERE clause
                null,                       // don't group the rows
                null,                       // don't filter by row groups
                null                        // The sort order
        );

        cursor.moveToFirst();
        do {
            long itemId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(Event._ID)
            );
            String urlString = cursor.getString(
                    cursor.getColumnIndexOrThrow(Event.COLUMN_NAME_URL)
            );
            try {
                events.add(new Pair<>(itemId, new URLProxy(new URL(urlString))));
            } catch (MalformedURLException e) {
                logger.error("Retreived a malformed URL from storage", e);

            }
        } while (cursor.moveToNext());

        cursor.close();

        logger.info("Got events from db");

        return events;
    }

    boolean removeEvent(long eventId) {
        // Define 'where' part of query.
        String selection = Event._ID + " = ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = { String.valueOf(eventId) };
        // Issue SQL statement.
        int numRowsDeleted = db.delete(Event.TABLE_NAME, selection, selectionArgs);

        if (numRowsDeleted > 0) {
            logger.info("Removed event with id {} from db", eventId);
            return true;
        } else {
            logger.error("Tried to remove an event id {} that does not exist", eventId);
        }

        return numRowsDeleted > 0;
    }
}
