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
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jdeffibaugh on 7/21/16 for Optimizely.
 *
 * Handles interactions with the {@link SQLiteDatabase} that store {@link Event} instances.
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

    public boolean storeEvent(@NonNull Event event) {
        ContentValues values = new ContentValues();
        values.put(EventTable.Column.URL, event.toString());

        // Since we are setting the "null column hack" param to null empty values will not be inserted
        // at all instead of inserting null.
        long newRowId;
        newRowId = db.insert(EventTable.NAME, null, values);

        logger.info("Inserted {} into db", event);

        return newRowId != -1;
    }

    public List<Pair<Long, Event>> getEvents() {
        List<Pair<Long, Event>> events = new LinkedList<>();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                EventTable.Column._ID,
                EventTable.Column.URL
        };

        Cursor cursor = db.query(
                EventTable.NAME,           // The table to query
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
                    cursor.getColumnIndexOrThrow(EventTable._ID)
            );
            String url = cursor.getString(
                    cursor.getColumnIndexOrThrow(EventTable.Column.URL)
            );
            try {
                events.add(new Pair<>(itemId, new Event(new URL(url))));
            } catch (MalformedURLException e) {
                logger.error("Retreived a malformed event from storage", e);

            }
        } while (cursor.moveToNext());

        cursor.close();

        logger.info("Got events from db");

        return events;
    }

    public boolean removeEvent(long eventId) {
        // Define 'where' part of query.
        String selection = EventTable._ID + " = ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = { String.valueOf(eventId) };
        // Issue SQL statement.
        int numRowsDeleted = db.delete(EventTable.NAME, selection, selectionArgs);

        if (numRowsDeleted > 0) {
            logger.info("Removed event with id {} from db", eventId);
            return true;
        } else {
            logger.error("Tried to remove an event id {} that does not exist", eventId);
        }

        return numRowsDeleted > 0;
    }
}
