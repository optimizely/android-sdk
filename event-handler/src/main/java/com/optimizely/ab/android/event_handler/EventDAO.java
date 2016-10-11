/*
 * Copyright 2016, Optimizely
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.android.event_handler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * Handles interactions with the {@link SQLiteDatabase} that store {@link Event} instances.
 */
class EventDAO {

    @NonNull Logger logger;
    @NonNull private EventSQLiteOpenHelper dbHelper;

    private EventDAO(@NonNull EventSQLiteOpenHelper dbHelper, @NonNull Logger logger) {
        this.dbHelper = dbHelper;
        this.logger = logger;
    }

    static EventDAO getInstance(@NonNull Context context, @NonNull Logger logger) {
        EventSQLiteOpenHelper sqLiteOpenHelper = new EventSQLiteOpenHelper(context, EventSQLiteOpenHelper.DB_NAME, null, EventSQLiteOpenHelper.VERSION, null, LoggerFactory.getLogger(EventSQLiteOpenHelper.class));
        return new EventDAO(sqLiteOpenHelper, logger);
    }

    boolean storeEvent(@NonNull Event event) {
        ContentValues values = new ContentValues();
        values.put(EventTable.Column.URL, event.toString());
        values.put(EventTable.Column.REQUEST_BODY, event.getRequestBody());

        // Since we are setting the "null column hack" param to null empty values will not be inserted
        // at all instead of inserting null.
        long newRowId;
        newRowId = dbHelper.getWritableDatabase().insert(EventTable.NAME, null, values);

        logger.info("Inserted {} into db", event);

        return newRowId != -1;
    }

    List<Pair<Long, Event>> getEvents() {
        List<Pair<Long, Event>> events = new LinkedList<>();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                EventTable.Column._ID,
                EventTable.Column.URL,
                EventTable.Column.REQUEST_BODY,
        };

        Cursor cursor = dbHelper.getReadableDatabase().query(
                EventTable.NAME,           // The table to query
                projection,                 // The columns to return
                null,                       // The columns for the WHERE clause
                null,                       // The values for the WHERE clause
                null,                       // don't group the rows
                null,                       // don't filter by row groups
                null                        // The sort order
        );

        if (cursor.moveToFirst()) {
            do {
                long itemId = cursor.getLong(
                        cursor.getColumnIndexOrThrow(EventTable._ID)
                );
                String url = cursor.getString(
                        cursor.getColumnIndexOrThrow(EventTable.Column.URL)
                );
                String requestBody = cursor.getString(
                        cursor.getColumnIndexOrThrow(EventTable.Column.REQUEST_BODY)
                );
                try {
                    events.add(new Pair<>(itemId, new Event(new URL(url), requestBody)));
                } catch (MalformedURLException e) {
                    logger.error("Retrieved a malformed event from storage", e);

                }
            } while (cursor.moveToNext());

            cursor.close();

            logger.info("Got events from SQLite");
        }


        return events;
    }

    boolean removeEvent(long eventId) {
        // Define 'where' part of query.
        String selection = EventTable._ID + " = ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = {String.valueOf(eventId)};
        // Issue SQL statement.
        int numRowsDeleted = dbHelper.getWritableDatabase().delete(EventTable.NAME, selection, selectionArgs);

        if (numRowsDeleted > 0) {
            logger.info("Removed event with id {} from db", eventId);
            return true;
        } else {
            logger.error("Tried to remove an event id {} that does not exist", eventId);
        }

        return numRowsDeleted > 0;
    }

    void closeDb() {
        dbHelper.close();
    }
}
