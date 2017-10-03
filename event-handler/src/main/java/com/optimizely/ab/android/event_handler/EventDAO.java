/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.android.event_handler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * Handles interactions with the SQLiteDatabase that store {@link Event} instances.
 * This is the event queue for Android.
 */
class EventDAO {

    @NonNull
    final Logger logger;
    @NonNull private final EventSQLiteOpenHelper dbHelper;

    /**
     * Private constructor
     *
     * @param dbHelper helper for SQLite calls.
     * @param logger where to log errors and warnings.
     */
    private EventDAO(@NonNull EventSQLiteOpenHelper dbHelper, @NonNull Logger logger) {
        this.dbHelper = dbHelper;
        this.logger = logger;
    }

    /**
     * Static initializer for EventDAO.
     * @param context current context
     * @param projectId current project id
     * @param logger where to log errors and warnings.
     * @return a new instance of EventDAO.
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    static EventDAO getInstance(@NonNull Context context, @NonNull String projectId, @NonNull Logger logger) {
        EventSQLiteOpenHelper sqLiteOpenHelper = new EventSQLiteOpenHelper(context, projectId, null, EventSQLiteOpenHelper.VERSION, LoggerFactory.getLogger(EventSQLiteOpenHelper.class));
        return new EventDAO(sqLiteOpenHelper, logger);
    }

    /**
     * Store an event in SQLite
     * @param event to store
     * @return true if successful
     */
    boolean storeEvent(@NonNull Event event) {
        logger.info("Inserting {} into db", event);
        ContentValues values = new ContentValues();
        values.put(EventTable.Column.URL, event.getURL().toString());
        values.put(EventTable.Column.REQUEST_BODY, event.getRequestBody());

        // Since we are setting the "null column hack" param to null empty values will not be inserted
        // at all instead of inserting null.
        try {
            long newRowId;
            newRowId = dbHelper.getWritableDatabase().insert(EventTable.NAME, null, values);

            logger.info("Inserted {} into db", event);

            return newRowId != -1;
        } catch (Exception e) {
            logger.error("Error inserting Optimizely event into db.", e);
        }

        return false;
    }

    /**
     * Get all the events in the SQLite queue
     * @return a list of events.
     */
    List<Pair<Long, Event>> getEvents() {
        List<Pair<Long, Event>> events = new LinkedList<>();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                EventTable.Column._ID,
                EventTable.Column.URL,
                EventTable.Column.REQUEST_BODY,
        };

        Cursor cursor = null;
        try {
            cursor = dbHelper.getReadableDatabase().query(
                    EventTable.NAME,           // The table to query
                    projection,                 // The columns to return
                    null,                       // The columns for the WHERE clause
                    null,                       // The values for the WHERE clause
                    null,                       // don't group the rows
                    null,                       // don't filter by row groups
                    null                        // The sort order
            );
            logger.info("Opened database");
        } catch (Exception e) {
            logger.error("Failed to open database.", e);
        }

        try {
            if (cursor != null && cursor.moveToFirst()) {
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

                logger.info("Got events from SQLite");
            }
        } catch (Exception e) {
            logger.error("Error reading events db cursor", e);
        }
        finally {
            if (cursor != null && !cursor.isClosed()) {
                try {
                    cursor.close();
                    logger.info("Closed database");

                }
                catch (Exception e) {
                    logger.error("Error closing db cursor", e);
                }
            }
        }

        return events;
    }

    /**
     * Remove an event from SQLite db.
     * @param eventId id of the event to remove
     * @return true on success.
     */
    boolean removeEvent(long eventId) {
        // Define 'where' part of query.
        String selection = EventTable._ID + " = ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = {String.valueOf(eventId)};

        try {
            // Issue SQL statement.
            int numRowsDeleted = dbHelper.getWritableDatabase().delete(EventTable.NAME, selection, selectionArgs);

            if (numRowsDeleted > 0) {
                logger.info("Removed event with id {} from db", eventId);
                return true;
            } else {
                logger.error("Tried to remove an event id {} that does not exist", eventId);
            }

            return numRowsDeleted > 0;
        } catch (Exception e) {
            logger.error("Could not open db.", e);
        }

        return false;
    }

    /**
     * Close the SQLite DB.
     */
    void closeDb() {
        try {
            dbHelper.close();
        } catch (Exception e) {
            logger.warn("Error closing db.", e);
        }
    }
}
