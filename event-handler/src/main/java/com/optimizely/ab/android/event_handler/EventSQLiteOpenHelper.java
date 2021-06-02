/****************************************************************************
 * Copyright 2016,2021, Optimizely, Inc. and contributors                   *
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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.slf4j.Logger;

/**
 * Handles transactions for the events db
 */
class EventSQLiteOpenHelper extends SQLiteOpenHelper {

    static final int VERSION = 1;
    static final String DB_NAME = "optly-events-%s";

    static final String SQL_CREATE_EVENT_TABLE =
            "CREATE TABLE " + EventTable.NAME + " (" +
                    EventTable._ID + " INTEGER PRIMARY KEY, " +
                    EventTable.Column.URL + " TEXT NOT NULL," +
                    EventTable.Column.REQUEST_BODY + " TEXT NOT NULL" +
            ")";

    private static final String SQL_DELETE_EVENT_TABLE =
            "DROP TABLE IF EXISTS " + EventTable.NAME;

    @NonNull private final Logger logger;
    @NonNull private final String projectId;
    @NonNull private final Context context;

    EventSQLiteOpenHelper(@NonNull Context context, @NonNull String projectId, @Nullable SQLiteDatabase.CursorFactory factory, int version, @NonNull Logger logger) {
        super(context, String.format(DB_NAME, projectId), factory, version);
        this.logger = logger;
        this.projectId = projectId;
        this.context = context;
    }

    /**
     * @hide
     * @see SQLiteOpenHelper#onCreate(SQLiteDatabase)
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            // Deletes the old events db that stored events for all projects
            context.deleteDatabase("optly-events");
            db.execSQL(SQL_CREATE_EVENT_TABLE);
            logger.info("Created event table with SQL: {}", SQL_CREATE_EVENT_TABLE);
        } catch (Exception e) {
            logger.error("Error creating optly-events table.", e);
        }
    }

    /**
     * @hide
     * @see SQLiteOpenHelper#onUpgrade(SQLiteDatabase, int, int)
     */

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public String getDbName() {
        return String.format(DB_NAME, projectId);
    }
}
