package com.optimizely.android;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by jdeffibaugh on 7/25/16 for Optimizely.
 *
 * Tests for {@link EventSQLiteOpenHelper}
 */
@RunWith(AndroidJUnit4.class)
public class EventSQLiteOpenHelperTest {

    Context context;
    Logger logger;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getTargetContext();
        logger = mock(Logger.class);
    }

    @After
    public void teardown() {
        context.deleteDatabase(EventSQLiteOpenHelper.DB_NAME);
    }

    @Test
    public void onCreateMakesTables() {
        EventSQLiteOpenHelper eventSQLiteOpenHelper =
                new EventSQLiteOpenHelper(context, EventSQLiteOpenHelper.DB_NAME, null, 1, logger);
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
