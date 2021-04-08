/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                            *
 * *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.android.event_handler

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.util.Pair
import androidx.annotation.RequiresApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URL
import java.util.*

/**
 * Handles interactions with the SQLiteDatabase that store [Event] instances.
 * This is the event queue for Android.
 */
class EventDAO
/**
 * Private constructor
 *
 * @param dbHelper helper for SQLite calls.
 * @param logger where to log errors and warnings.
 */ private constructor(private val dbHelper: EventSQLiteOpenHelper, val logger: Logger) {
    /**
     * Store an event in SQLite
     * @param event to store
     * @return true if successful
     */
    fun storeEvent(event: Event): Boolean {
        logger.info("Inserting {} into db", event)
        val values = ContentValues()
        values.put(EventTable.Column.URL, event.uRL.toString())
        values.put(EventTable.Column.REQUEST_BODY, event.requestBody)

        // Since we are setting the "null column hack" param to null empty values will not be inserted
        // at all instead of inserting null.
        try {
            val newRowId: Long
            newRowId = dbHelper.writableDatabase.insert(EventTable.NAME, null, values)
            logger.info("Inserted {} into db", event)
            return newRowId != -1L
        } catch (e: Exception) {
            logger.error("Error inserting Optimizely event into db.", e)
        }
        return false
    }// The table to query
    // The columns to return
    // The columns for the WHERE clause
    // The values for the WHERE clause
    // don't group the rows
    // don't filter by row groups
    // The sort order
// Define a projection that specifies which columns from the database
    // you will actually use after this query.
    /**
     * Get all the events in the SQLite queue
     * @return a list of events.
     */
    val events: List<Pair<Long, Event>>
        get() {
            val events: MutableList<Pair<Long, Event>> = LinkedList()

            // Define a projection that specifies which columns from the database
            // you will actually use after this query.
            val projection = arrayOf(
                    EventTable.Column._ID,
                    EventTable.Column.URL,
                    EventTable.Column.REQUEST_BODY)
            var cursor: Cursor? = null
            try {
                cursor = dbHelper.readableDatabase.query(
                        EventTable.NAME,  // The table to query
                        projection,  // The columns to return
                        null,  // The columns for the WHERE clause
                        null,  // The values for the WHERE clause
                        null,  // don't group the rows
                        null,  // don't filter by row groups
                        null // The sort order
                )
                logger.info("Opened database")
            } catch (e: Exception) {
                logger.error("Failed to open database.", e)
            }
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        val itemId = cursor.getLong(
                                cursor.getColumnIndexOrThrow(EventTable.Column._ID)
                        )
                        val url = cursor.getString(
                                cursor.getColumnIndexOrThrow(EventTable.Column.URL)
                        )
                        val requestBody = cursor.getString(
                                cursor.getColumnIndexOrThrow(EventTable.Column.REQUEST_BODY)
                        )
                        try {
                            events.add(Pair(itemId, Event(URL(url), requestBody)))
                        } catch (e: MalformedURLException) {
                            logger.error("Retrieved a malformed event from storage", e)
                        }
                    } while (cursor.moveToNext())
                    logger.info("Got events from SQLite")
                }
            } catch (e: Exception) {
                logger.error("Error reading events db cursor", e)
            } finally {
                if (cursor != null && !cursor.isClosed) {
                    try {
                        cursor.close()
                        logger.info("Closed database")
                    } catch (e: Exception) {
                        logger.error("Error closing db cursor", e)
                    }
                }
            }
            return events
        }

    /**
     * Remove an event from SQLite db.
     * @param eventId id of the event to remove
     * @return true on success.
     */
    fun removeEvent(eventId: Long): Boolean {
        // Define 'where' part of query.
        val selection = EventTable.Column._ID + " = ?"
        // Specify arguments in placeholder order.
        val selectionArgs = arrayOf(eventId.toString())
        try {
            // Issue SQL statement.
            val numRowsDeleted = dbHelper.writableDatabase.delete(EventTable.NAME, selection, selectionArgs)
            if (numRowsDeleted > 0) {
                logger.info("Removed event with id {} from db", eventId)
                return true
            } else {
                logger.error("Tried to remove an event id {} that does not exist", eventId)
            }
            return numRowsDeleted > 0
        } catch (e: Exception) {
            logger.error("Could not open db.", e)
        }
        return false
    }

    /**
     * Close the SQLite DB.
     */
    fun closeDb() {
        try {
            dbHelper.close()
        } catch (e: Exception) {
            logger.warn("Error closing db.", e)
        }
    }

    companion object {
        /**
         * Static initializer for EventDAO.
         * @param context current context
         * @param projectId current project id
         * @param logger where to log errors and warnings.
         * @return a new instance of EventDAO.
         */
        @JvmStatic
        @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
        fun getInstance(context: Context, projectId: String, logger: Logger): EventDAO {
            val sqLiteOpenHelper = EventSQLiteOpenHelper(context, projectId, null, EventSQLiteOpenHelper.VERSION, LoggerFactory.getLogger(EventSQLiteOpenHelper::class.java))
            return EventDAO(sqLiteOpenHelper, logger)
        }
    }
}