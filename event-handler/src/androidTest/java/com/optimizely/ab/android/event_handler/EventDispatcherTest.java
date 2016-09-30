/**
 * Copyright 2016, Optimizely
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.android.event_handler;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import com.optimizely.ab.android.shared.OptlyStorage;
import com.optimizely.ab.android.shared.ServiceScheduler;

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
 * Tests {@link EventDispatcher}
 */
@RunWith(AndroidJUnit4.class)
public class EventDispatcherTest {

    EventDispatcher eventDispatcher;
    OptlyStorage optlyStorage;
    EventDAO eventDAO;
    EventClient eventClient;
    ServiceScheduler serviceScheduler;
    Logger logger;
    Context context;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getTargetContext();
        eventDAO = mock(EventDAO.class);
        eventClient = mock(EventClient.class);
        logger = mock(Logger.class);
        serviceScheduler = mock(ServiceScheduler.class);
        optlyStorage = mock(OptlyStorage.class);

        eventDispatcher = new EventDispatcher(context, optlyStorage, eventDAO, eventClient, serviceScheduler, logger);
    }

    @Test
    public void handleIntentSchedulesWhenEventsLeftInStorage() throws MalformedURLException {
        Event event1 = new Event(new URL("http://www.foo1.com"), "");
        Event event2 = new Event(new URL("http://www.foo2.com"), "");
        Event event3= new Event(new URL("http://www.foo3.com"), "");
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

        Intent mockIntent = mock(Intent.class);
        when(mockIntent.getLongExtra(EventIntentService.EXTRA_INTERVAL, -1)).thenReturn(-1L);
        when(optlyStorage.getLong(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR)).thenReturn(AlarmManager.INTERVAL_HOUR);
        eventDispatcher.dispatch(mockIntent);

        verify(serviceScheduler).schedule(mockIntent, AlarmManager.INTERVAL_HOUR);
        verify(optlyStorage).saveLong(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR);
        verify(logger).warn("Unable to delete an event from local storage that was sent to successfully");
        verify(logger).info("Scheduled events to be dispatched");
    }

    @Test
    public void handleIntentSchedulesWhenNewEventFailsToSend() throws MalformedURLException {
        String url= "http://www.foo.com";
        Event event = new Event(new URL(url), "");

        when(eventDAO.getEvents()).thenReturn(new LinkedList<Pair<Long, Event>>());

        Intent mockIntent = mock(Intent.class);
        when(mockIntent.hasExtra(EventIntentService.EXTRA_URL)).thenReturn(true);
        when(mockIntent.getStringExtra(EventIntentService.EXTRA_URL)).thenReturn(url);
        when(mockIntent.getLongExtra(EventIntentService.EXTRA_INTERVAL, -1)).thenReturn(AlarmManager.INTERVAL_HOUR);
        when(eventClient.sendEvent(event)).thenReturn(false);
        when(eventDAO.storeEvent(event)).thenReturn(true);

        eventDispatcher.dispatch(mockIntent);
        verify(serviceScheduler).schedule(mockIntent, AlarmManager.INTERVAL_HOUR);
        verify(optlyStorage).saveLong(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR);
        verify(logger).info("Scheduled events to be dispatched");
    }

    @Test
    public void getIntervalFromIntent() throws MalformedURLException {
        String url= "http://www.foo.com";

        when(eventDAO.getEvents()).thenReturn(new LinkedList<Pair<Long, Event>>());

        Intent mockIntent = mock(Intent.class);
        when(mockIntent.hasExtra(EventIntentService.EXTRA_URL)).thenReturn(true);
        when(mockIntent.getStringExtra(EventIntentService.EXTRA_URL)).thenReturn(url);

    }

    @Test
    public void handleIntentLogsWhenUnableToSendOrStoreEvent() throws MalformedURLException {
        String url= "http://www.foo.com";
        String requestBody = "param1=123";
        Event event = new Event(new URL(url), requestBody);

        when(eventDAO.getEvents()).thenReturn(new LinkedList<Pair<Long, Event>>());

        Intent mockIntent = mock(Intent.class);
        when(mockIntent.hasExtra(EventIntentService.EXTRA_URL)).thenReturn(true);
        when(mockIntent.hasExtra(EventIntentService.EXTRA_REQUEST_BODY)).thenReturn(true);
        when(mockIntent.getStringExtra(EventIntentService.EXTRA_URL)).thenReturn(url);
        when(mockIntent.getStringExtra(EventIntentService.EXTRA_REQUEST_BODY)).thenReturn(requestBody);
        when(eventClient.sendEvent(event)).thenReturn(false);
        when(eventDAO.storeEvent(event)).thenReturn(false);

        eventDispatcher.dispatch(mockIntent);
        verify(logger).error("Unable to send or store event {}", event);
        verify(logger, never()).info("Scheduled events to be dispatched");
    }

    @Test
    public void unschedulesServiceWhenNoEventsToFlush() {
        when(eventDAO.getEvents()).thenReturn(new LinkedList<Pair<Long, Event>>());
        Intent mockIntent = mock(Intent.class);
        eventDispatcher.dispatch(mockIntent);
        verify(serviceScheduler).unschedule(mockIntent);
        verify(logger).info("Unscheduled event dispatch");
    }

    @Test
    public void handleMalformedURL() throws MalformedURLException {
        String url= "foo";

        when(eventDAO.getEvents()).thenReturn(new LinkedList<Pair<Long, Event>>());

        Intent mockIntent = mock(Intent.class);
        when(mockIntent.hasExtra(EventIntentService.EXTRA_URL)).thenReturn(true);
        when(mockIntent.getStringExtra(EventIntentService.EXTRA_URL)).thenReturn(url);

        eventDispatcher.dispatch(mockIntent);

        verify(logger).error(contains("Received a malformed URL in event handler service"), any(MalformedURLException.class));
    }
}
