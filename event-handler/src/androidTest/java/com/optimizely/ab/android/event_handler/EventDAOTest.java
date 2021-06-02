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
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.util.Pair;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link EventDAO}
 */
@RunWith(AndroidJUnit4.class)
public class EventDAOTest {

    private EventDAO eventDAO;
    private Logger logger;
    private Context context;

    @Before
    public void setupEventDAO() {
        logger = mock(Logger.class);
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        eventDAO = EventDAO.getInstance(context, "1", logger);
    }

    @After
    public void tearDownEventDAO() {
        assertTrue(context.deleteDatabase(String.format(EventSQLiteOpenHelper.DB_NAME , "1")));
    }

    @Test
    public void storeEvent() throws MalformedURLException {
        Event event = new Event(new URL("http://www.foo.com"), "bar1=baz1");
        assertTrue(eventDAO.storeEvent(event));
        verify(logger).info("Inserted {} into db", event);
    }

    @Test
    public void getEvents() throws MalformedURLException {
        Event event1 = new Event(new URL("http://www.foo1.com"), "bar1=baz1");
        Event event2 = new Event(new URL("http://www.foo2.com"), "bar2=baz2");
        Event event3 = new Event(new URL("http://www.foo3.com"), "bar3=baz3");

        assertTrue(eventDAO.storeEvent(event1));
        assertTrue(eventDAO.storeEvent(event2));
        assertTrue(eventDAO.storeEvent(event3));

        List<Pair<Long,Event>> events = eventDAO.getEvents();
        assertTrue(events.size() == 3);

        Pair<Long,Event> pair1 = events.get(0);
        Pair<Long,Event> pair2 = events.get(1);
        Pair<Long,Event> pair3 = events.get(2);

        assertEquals(1, pair1.first.longValue());
        assertEquals(2, pair2.first.longValue());
        assertEquals(3, pair3.first.longValue());

        assertEquals("http://www.foo1.com", pair1.second.getURL().toString());
        assertEquals("bar1=baz1", pair1.second.getRequestBody());
        assertEquals("http://www.foo2.com", pair2.second.getURL().toString());
        assertEquals("bar2=baz2", pair2.second.getRequestBody());
        assertEquals("http://www.foo3.com", pair3.second.getURL().toString());
        assertEquals("bar3=baz3", pair3.second.getRequestBody());

        verify(logger).info("Got events from SQLite");
    }

    @Test
    public void removeEventSuccess() throws MalformedURLException {
        Event event = new Event(new URL("http://www.foo.com"), "baz=baz");
        assertTrue(eventDAO.storeEvent(event));
        assertTrue(eventDAO.removeEvent(1));
        verify(logger).info("Removed event with id {} from db", 1L);
    }

    @Test
    public void removeEventInvalid() throws MalformedURLException {
        Event event = new Event(new URL("http://www.foo.com"), "baz=baz");
        assertTrue(eventDAO.storeEvent(event));
        assertFalse(eventDAO.removeEvent(2));
        verify(logger).error("Tried to remove an event id {} that does not exist", 2L);
    }
}
