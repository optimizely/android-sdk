/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
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
import android.support.annotation.RequiresApi;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.Gson;
import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.OptlyStorage;
import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.ab.event.internal.payload.batch.*;
import com.optimizely.ab.event.internal.payload.batch.Event;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
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
    private Gson gson;

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Before
    public void setup() {
        context = InstrumentationRegistry.getTargetContext();
        logger = mock(Logger.class);
        client = mock(Client.class);
        eventDAO = EventDAO.getInstance(context, "1", logger);
        eventClient = new EventClient(client, logger);
        serviceScheduler = mock(ServiceScheduler.class);
        optlyStorage = mock(OptlyStorage.class);
        gson = new Gson();

        eventDispatcher = new EventDispatcher(context, optlyStorage, eventDAO, eventClient,
                serviceScheduler, gson, logger);
    }

    @After
    public void tearDown() {
        context.deleteDatabase(String.format(EventSQLiteOpenHelper.DB_NAME, "1"));
    }

    @Test
    public void merge() throws MalformedURLException {
        Batch batch1 = new Batch("1");
        Batch batch2 = new Batch("1");

        Visitor visitor1 = new Visitor("1");
        Visitor visitor2 = new Visitor("1");
        List<Visitor> visitors1 = Arrays.asList(visitor1);
        List<Visitor> visitors2 = Arrays.asList(visitor2);

        Decision decision1 = new Decision("1", "1", false, "1");
        Decision decision2 = new Decision("2", "2", false, "2");
        List<Decision> decisionList = Arrays.asList(decision1, decision2);

        com.optimizely.ab.event.internal.payload.batch.Event event1 = new Event("campaign_activated", System.currentTimeMillis(), UUID.randomUUID().toString());
        com.optimizely.ab.event.internal.payload.batch.Event event2 = new Event("campaign_activated", System.currentTimeMillis(), UUID.randomUUID().toString());
        List<com.optimizely.ab.event.internal.payload.batch.Event> events1 = Arrays.asList(event1);
        List<com.optimizely.ab.event.internal.payload.batch.Event> events2 = Arrays.asList(event2);

        Snapshot snapshot1 = new Snapshot(decisionList, events1);
        Snapshot snapshot2 = new Snapshot(decisionList, events2);
        List<Snapshot> snapshots1 = Arrays.asList(snapshot1);
        List<Snapshot> snapshots2 = Arrays.asList(snapshot2);

        visitor1.setSnapshots(snapshots1);
        visitor2.setSnapshots(snapshots2);

        batch1.setVisitors(visitors1);
        batch2.setVisitors(visitors2);

        Gson gson = new Gson();
        com.optimizely.ab.android.event_handler.Event eventReq1 = new com.optimizely.ab.android.event_handler.Event(new URL("http://www.optimizely.com"), gson.toJson(batch1, Batch.class));
        com.optimizely.ab.android.event_handler.Event eventReq2 = new com.optimizely.ab.android.event_handler.Event(new URL("http://www.optimizely.com"), gson.toJson(batch2, Batch.class));

        com.optimizely.ab.android.event_handler.Event mergedEventReq = eventDispatcher.mergeBatches(Arrays.asList(eventReq1, eventReq2));

        assertEquals(new URL("http://www.optimizely.com"), mergedEventReq.getURL());

        Batch mergedBatch = gson.fromJson(mergedEventReq.getRequestBody(), Batch.class);
        assertEquals("1", mergedBatch.getAccountId());
        assertEquals(1, mergedBatch.getVisitors().size());
        Visitor mergedVisitor = mergedBatch.getVisitors().get(0);
        assertEquals("1", mergedVisitor.getVisitorId());
        assertEquals(1, mergedVisitor.getSnapshots().size());
        Snapshot snapshot = mergedVisitor.getSnapshots().get(0);
        assertEquals(2, snapshot.getDecisions().size());
        List<Decision> decisions = snapshot.getDecisions();
        assertTrue(decisions.contains(decision1));
        assertTrue(decisions.contains(decision2));
        assertEquals(2, snapshot.getEvents().size());
        List<Event> events = snapshot.getEvents();
        assertTrue(events.contains(event1));
        assertTrue(events.contains(event2));
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
