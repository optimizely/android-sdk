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

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.OptlyStorage;
import com.optimizely.ab.android.shared.ServiceScheduler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link EventDispatcher}
 */
@RunWith(AndroidJUnit4.class)
public class EventDispatcherTest {

    private EventDispatcher eventDispatcher;
    private OptlyStorage optlyStorage;
    private EventDAO eventDAO;
    private EventClient eventClient;
    private ServiceScheduler serviceScheduler;
    private Logger logger;
    private Context context;
    private Client client;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        logger = mock(Logger.class);
        client = mock(Client.class);
        eventDAO = EventDAO.getInstance(context, "1", logger);
        eventClient = new EventClient(client, logger);
        serviceScheduler = mock(ServiceScheduler.class);
        optlyStorage = mock(OptlyStorage.class);

        eventDispatcher = new EventDispatcher(context, optlyStorage, eventDAO, eventClient, serviceScheduler, logger);
    }

    @After
    public void tearDown() {
        context.deleteDatabase(String.format(EventSQLiteOpenHelper.DB_NAME, "1"));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void handleIntentSchedulesWhenEventsLeftInStorage() throws IOException {
        Event event1 = new Event(new URL("http://www.foo1.com"), "");
        Event event2 = new Event(new URL("http://www.foo2.com"), "");
        Event event3= new Event(new URL("http://www.foo3.com"), "");
        eventDAO.storeEvent(event1);
        eventDAO.storeEvent(event2);
        eventDAO.storeEvent(event3);

        final HttpURLConnection goodConnection = getGoodConnection();
        when(client.openConnection(event1.getURL())).thenReturn(goodConnection);
        when(client.openConnection(event2.getURL())).thenReturn(goodConnection);
        final HttpURLConnection badConnection = getBadConnection();
        when(client.openConnection(event3.getURL())).thenReturn(badConnection);

        Intent mockIntent = mock(Intent.class);
        when(mockIntent.getLongExtra(EventIntentService.EXTRA_INTERVAL, -1)).thenReturn(-1L);
        when(optlyStorage.getLong(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR)).thenReturn(AlarmManager.INTERVAL_HOUR);
        eventDispatcher.dispatch(mockIntent);

        verify(serviceScheduler).schedule(mockIntent, -1);
        verify(optlyStorage).saveLong(EventIntentService.EXTRA_INTERVAL, -1);

        verify(logger).info("Scheduled events to be dispatched");
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void handleIntentSchedulesWhenNewEventFailsToSend() throws IOException {
        Event event = new Event(new URL("http://www.foo.com"), "");

        Intent intent = new Intent(context, EventIntentService.class);
        intent.putExtra(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR);
        intent.putExtra(EventIntentService.EXTRA_URL, event.getURL().toString());
        intent.putExtra(EventIntentService.EXTRA_REQUEST_BODY, event.getRequestBody());

        final HttpURLConnection badConnection = getBadConnection();
        when(client.openConnection(event.getURL())).thenReturn(badConnection);

        eventDispatcher.dispatch(intent);
        verify(serviceScheduler).schedule(intent, AlarmManager.INTERVAL_HOUR);
        verify(optlyStorage).saveLong(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR);

        verify(logger).info("Scheduled events to be dispatched");
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void unschedulesServiceWhenNoEventsToFlush() {
        Intent mockIntent = mock(Intent.class);
        eventDispatcher.dispatch(mockIntent);
        verify(serviceScheduler).unschedule(mockIntent);
        verify(logger).info("Unscheduled event dispatch");
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void handleMalformedURL() throws MalformedURLException {
        String url= "foo";

        Intent mockIntent = mock(Intent.class);
        when(mockIntent.hasExtra(EventIntentService.EXTRA_URL)).thenReturn(true);
        when(mockIntent.getStringExtra(EventIntentService.EXTRA_URL)).thenReturn(url);

        eventDispatcher.dispatch(mockIntent);

        verify(logger).error(contains("Received a malformed URL in event handler service"), any(MalformedURLException.class));
    }

    private HttpURLConnection getGoodConnection() throws IOException {
        HttpURLConnection httpURLConnection = getConnection();
        when(httpURLConnection.getResponseCode()).thenReturn(200);

        return httpURLConnection;
    }

    private HttpURLConnection getBadConnection() throws IOException {
        HttpURLConnection httpURLConnection = getConnection();
        when(httpURLConnection.getResponseCode()).thenReturn(400);
        return httpURLConnection;
    }

    private HttpURLConnection getConnection() throws IOException {
        HttpURLConnection httpURLConnection = mock(HttpURLConnection.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(httpURLConnection.getOutputStream()).thenReturn(outputStream);
        InputStream inputStream = mock(InputStream.class);
        when(httpURLConnection.getInputStream()).thenReturn(inputStream);

        return httpURLConnection;
    }
}
