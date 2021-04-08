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
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.slf4j.Logger

/**
 * Tests for [EventSQLiteOpenHelper]
 */
@RunWith(AndroidJUnit4::class)
class EventSQLiteOpenHelperTest {
    private var context: Context? = null
    private var logger: Logger? = null
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        logger = Mockito.mock(Logger::class.java)
    }

    @After
    fun teardown() {
        context!!.deleteDatabase(String.format(EventSQLiteOpenHelper.DB_NAME, "1"))
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    fun onCreateMakesTables() {
        val eventSQLiteOpenHelper = EventSQLiteOpenHelper(context!!, "1", null, 1, logger!!)
        val db = eventSQLiteOpenHelper.writableDatabase
        val projection = arrayOf(
                EventTable.Column._ID,
                EventTable.Column.URL)
        val cursor = db.query(EventTable.NAME, projection, null, null, null, null, null)
        Assert.assertEquals(2, cursor.columnCount)
        cursor.close()
        Mockito.verify(logger)?.info("Created event table with SQL: {}", EventSQLiteOpenHelper.SQL_CREATE_EVENT_TABLE)
    }
}