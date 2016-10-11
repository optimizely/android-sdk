/*
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

import com.optimizely.ab.android.shared.Client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 7/25/16 for Optimizely.
 *
 * Tests {@link EventClient}
 */
@RunWith(MockitoJUnitRunner.class)
public class EventClientTest {

    @Mock Logger logger;
    @Mock Client client;
    private HttpURLConnection urlConnection;
    private EventClient eventClient;
    private Event event;

    @Before
    public void setupEventClient() throws IOException {
        urlConnection = mock(HttpURLConnection.class);
        when(urlConnection.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(urlConnection.getInputStream()).thenReturn(mock(InputStream.class));
        this.eventClient = new EventClient(client, logger);
        URL url = new URL("http://www.foo.com");
        event = new Event(url, "");
    }

    @Test
    public void sendEvents200() throws IOException {
        when(client.openConnection(event.getURL())).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        InputStream inputStream = mock(InputStream.class);
        when(urlConnection.getInputStream()).thenReturn(inputStream);

        assertTrue(eventClient.sendEvent(event));
        verify(logger).info("Dispatching event: {}", event);
    }

    @Test
    public void sendEvents201() throws IOException {
        when(client.openConnection(event.getURL())).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(201);
        InputStream inputStream = mock(InputStream.class);
        when(urlConnection.getInputStream()).thenReturn(inputStream);

        assertTrue(eventClient.sendEvent(event));
        verify(logger).info("Dispatching event: {}", event);
    }

    @Test
    public void sendEvents300() throws IOException {
        when(client.openConnection(event.getURL())).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(300);
        InputStream inputStream = mock(InputStream.class);
        when(urlConnection.getInputStream()).thenReturn(inputStream);

        assertFalse(eventClient.sendEvent(event));
        verify(logger).info("Dispatching event: {}", event);
        verify(logger).error("Unexpected response from event endpoint, status: 300");
    }

    @SuppressWarnings("unchecked")
    @Test()
    public void sendEventsIoExceptionGetInputStream() throws IOException {
        when(client.openConnection(event.getURL())).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(urlConnection.getInputStream()).thenThrow(IOException.class);

        assertFalse(eventClient.sendEvent(event));
        verify(logger).info("Dispatching event: {}", event);

    }

    @SuppressWarnings("unchecked")
    @Test()
    public void sendEventsIoExceptionOpenConnection() throws IOException {
        when(client.openConnection(event.getURL())).thenThrow(IOException.class);

        assertFalse(eventClient.sendEvent(event));
        verify(logger).info("Dispatching event: {}", event);

    }
}
