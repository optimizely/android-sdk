package com.optimizely.android;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 7/25/16 for Optimizely.
 *
 * Tests {@link EventDAO}
 */
@RunWith(AndroidJUnit4.class)
public class EventDAOTest {

    EventDAO eventDAO;
    Logger logger;
    Context context;

    @Before
    public void setupEventDAO() {
        logger = mock(Logger.class);
        context = InstrumentationRegistry.getTargetContext();
        eventDAO = EventDAO.getInstance(context, logger);
    }

    @After
    public void tearDownEventDAO() {
        context.deleteDatabase(EventSQLiteOpenHelper.DB_NAME);
    }

    @Test
    public void storeEvent() {
        Event event = mock(Event.class);
        when(event.toString()).thenReturn("http://www.foo.com?bar=baz");
        assertTrue(eventDAO.storeEvent(event));
        verify(logger).info("Inserted {} into db", event);
    }

    @Test
    public void getEvents() {
        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);
        Event event3 = mock(Event.class);

        when(event1.toString()).thenReturn("http://www.foo1.com?bar1=baz1");
        when(event2.toString()).thenReturn("http://www.foo2.com?bar2=baz2");
        when(event3.toString()).thenReturn("http://www.foo3.com?bar3=baz3");

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

        assertEquals("http://www.foo1.com?bar1=baz1", pair1.second.toString());
        assertEquals("http://www.foo2.com?bar2=baz2", pair2.second.toString());
        assertEquals("http://www.foo3.com?bar3=baz3", pair3.second.toString());

        verify(logger).info("Got events from db");
    }

    @Test
    public void removeEventSuccess() {
        Event event = mock(Event.class);
        when(event.toString()).thenReturn("http://www.foo.com?bar=baz");
        assertTrue(eventDAO.storeEvent(event));
        assertTrue(eventDAO.removeEvent(1));
        verify(logger).info("Removed event with id {} from db", 1L);
    }

    @Test
    public void removeEventInvalid() {
        Event event = mock(Event.class);
        when(event.toString()).thenReturn("http://www.foo.com?bar=baz");
        assertTrue(eventDAO.storeEvent(event));
        assertFalse(eventDAO.removeEvent(2));
        verify(logger).error("Tried to remove an event id {} that does not exist", 2L);
    }
}
