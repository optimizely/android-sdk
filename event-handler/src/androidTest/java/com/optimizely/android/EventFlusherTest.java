package com.optimizely.android;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 7/25/16 for Optimizely.
 *
 * Tests {@link EventFlusher}
 */
@RunWith(AndroidJUnit4.class)
public class EventFlusherTest {

    EventFlusher eventFlusher;
    EventDAO eventDAO;
    EventClient eventClient;
    EventScheduler eventScheduler;
    Logger logger;

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getTargetContext();
        eventDAO = mock(EventDAO.class);
        eventClient = mock(EventClient.class);
        logger = mock(Logger.class);
        eventScheduler = mock(EventScheduler.class);

        eventFlusher = new EventFlusher(eventDAO, eventClient, eventScheduler, logger);
    }

    @Test
    public void handleIntentSchedulesWhenEventsLeftInStorage() throws MalformedURLException {
        Event event1 = new Event(new URL("http://www.foo1.com"));
        Event event2 = new Event(new URL("http://www.foo2.com"));
        Event event3= new Event(new URL("http://www.foo3.com"));
        when(eventDAO.getEvents()).thenReturn(new LinkedList<>(Arrays.asList(
                new Pair<>(1L, event1),
                new Pair<>(2L, event2),
                new Pair<>(3L, event3))));

        when(eventClient.sendEvent(event1)).thenReturn(true);
        when(eventClient.sendEvent(event2)).thenReturn(true);
        when(eventClient.sendEvent(event3)).thenReturn(false);

        when(eventDAO.removeEvent(1L)).thenReturn(false);
        when(eventDAO.removeEvent(2L)).thenReturn(true);
        when(eventDAO.removeEvent(3L)).thenReturn(true);

        Intent intent = mock(Intent.class);
        eventFlusher.flush(intent);

        verify(logger).warn("Unable to delete an event from local storage that was sent to successfully");
        verify(logger).info("Scheduled events to be flushed");
    }

    @Test
    public void handleIntentSchedulesWhenNewEventFailsToSend() throws MalformedURLException {
        String url= "http://www.foo.com";
        Event event = new Event(new URL(url));

        when(eventDAO.getEvents()).thenReturn(new LinkedList<Pair<Long, Event>>());

        Intent intent = mock(Intent.class);
        when(intent.hasExtra(EventIntentService.EXTRA_URL)).thenReturn(true);
        when(intent.getStringExtra(EventIntentService.EXTRA_URL)).thenReturn(url);
        when(eventClient.sendEvent(event)).thenReturn(false);
        when(eventDAO.storeEvent(event)).thenReturn(true);

        eventFlusher.flush(intent);
        verify(logger).info("Scheduled events to be flushed");
    }

    @Test
    public void handleIntentLogsWhenUnableToSendOrStoreEvent() throws MalformedURLException {
        String url= "http://www.foo.com";
        Event event = new Event(new URL(url));

        when(eventDAO.getEvents()).thenReturn(new LinkedList<Pair<Long, Event>>());

        Intent intent = mock(Intent.class);
        when(intent.hasExtra(EventIntentService.EXTRA_URL)).thenReturn(true);
        when(intent.getStringExtra(EventIntentService.EXTRA_URL)).thenReturn(url);
        when(eventClient.sendEvent(event)).thenReturn(false);
        when(eventDAO.storeEvent(event)).thenReturn(false);

        eventFlusher.flush(intent);
        verify(logger).error("Unable to send or store event {}", event);
        verify(logger, never()).info("Scheduled events to be flushed");
    }

    @Test
    public void handleMalformedURL() throws MalformedURLException {
        String url= "foo";

        when(eventDAO.getEvents()).thenReturn(new LinkedList<Pair<Long, Event>>());

        Intent intent = mock(Intent.class);
        when(intent.hasExtra(EventIntentService.EXTRA_URL)).thenReturn(true);
        when(intent.getStringExtra(EventIntentService.EXTRA_URL)).thenReturn(url);

        eventFlusher.flush(intent);

        verify(logger).error(contains("Received a malformed URL in event handler service"), any(MalformedURLException.class));
    }
}
