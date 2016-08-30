package com.optimizely.ab.android.event_handler;


import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

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
@RunWith(AndroidJUnit4.class)
public class EventClientTest {

    Logger logger;
    Event event;
    HttpURLConnection urlConnection;

    EventClient eventClient;

    @Before
    public void setupEventClient() throws IOException {
        logger = mock(Logger.class);
        event = mock(Event.class);
        urlConnection = mock(HttpURLConnection.class);

        this.eventClient = new EventClient(logger);
        when(event.toString()).thenReturn("http://www.foo.com");
    }

    @Test
    public void sendEvents200() throws IOException {
        when(event.send()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        InputStream inputStream = mock(InputStream.class);
        when(urlConnection.getInputStream()).thenReturn(inputStream);

        assertTrue(eventClient.sendEvent(event));
        verify(logger).info("Dispatching event: {}", event);
    }

    @Test
    public void sendEvents201() throws IOException {
        when(event.send()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(201);
        InputStream inputStream = mock(InputStream.class);
        when(urlConnection.getInputStream()).thenReturn(inputStream);

        assertTrue(eventClient.sendEvent(event));
        verify(logger).info("Dispatching event: {}", event);
    }

    @Test
    public void sendEvents300() throws IOException {
        when(event.send()).thenReturn(urlConnection);
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
        when(event.send()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(urlConnection.getInputStream()).thenThrow(IOException.class);

        assertFalse(eventClient.sendEvent(event));
        verify(logger).info("Dispatching event: {}", event);

    }

    @SuppressWarnings("unchecked")
    @Test()
    public void sendEventsIoExceptionOpenConnection() throws IOException {
        when(event.send()).thenThrow(IOException.class);

        assertFalse(eventClient.sendEvent(event));
        verify(logger).info("Dispatching event: {}", event);

    }
}
