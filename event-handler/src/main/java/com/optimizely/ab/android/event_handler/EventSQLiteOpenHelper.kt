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

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.provider.BaseColumns
import androidx.annotation.RequiresApi
import com.optimizely.ab.android.event_handler.EventTable.Column
import org.slf4j.Logger

/**
 * Handles transactions for the events db
 */
internal class EventSQLiteOpenHelper @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB) constructor(private val context: Context, private val projectId: String, factory: CursorFactory?, version: Int, private val logger: Logger) : SQLiteOpenHelper(context, String.format(DB_NAME, projectId), factory, version) {
    /**
     * @hide
     * @see SQLiteOpenHelper.onCreate
     */
    override fun onCreate(db: SQLiteDatabase) {
        try {
            // Deletes the old events db that stored events for all projects
            context.deleteDatabase("optly-events")
            db.execSQL(SQL_CREATE_EVENT_TABLE)
            logger.info("Created event table with SQL: {}", SQL_CREATE_EVENT_TABLE)
        } catch (e: Exception) {
            logger.error("Error creating optly-events table.", e)
        }
    }

    /**
     * @hide
     * @see SQLiteOpenHelper.onUpgrade
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    val dbName: String
        get() = String.format(DB_NAME, projectId)

    companion object {
        const val VERSION = 1
        const val DB_NAME = "optly-events-%s"
        const val SQL_CREATE_EVENT_TABLE = "CREATE TABLE " + EventTable.NAME + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY, " +
                Column.URL + " TEXT NOT NULL," +
                Column.REQUEST_BODY + " TEXT NOT NULL" +
                ")"
        private const val SQL_DELETE_EVENT_TABLE = "DROP TABLE IF EXISTS " + EventTable.NAME
    }
}