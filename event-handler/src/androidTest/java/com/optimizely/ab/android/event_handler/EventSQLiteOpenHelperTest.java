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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link EventSQLiteOpenHelper}
 */
@RunWith(AndroidJUnit4.class)
public class EventSQLiteOpenHelperTest {

    private Context context;
    private Logger logger;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        logger = mock(Logger.class);
    }

    @After
    public void teardown() {
        context.deleteDatabase(String.format(EventSQLiteOpenHelper.DB_NAME, "1"));
    }

    @Test
    public void onCreateMakesTables() {
        EventSQLiteOpenHelper eventSQLiteOpenHelper =
                new EventSQLiteOpenHelper(context, "1", null, 1, logger);
        SQLiteDatabase db = eventSQLiteOpenHelper.getWritableDatabase();
        String[] projection = {
                EventTable.Column._ID,
                EventTable.Column.URL,
        };
        Cursor cursor = db.query(EventTable.NAME, projection, null, null, null, null, null);
        assertEquals(2, cursor.getColumnCount());
        cursor.close();

        verify(logger).info("Created event table with SQL: {}", EventSQLiteOpenHelper.SQL_CREATE_EVENT_TABLE);
    }

}
