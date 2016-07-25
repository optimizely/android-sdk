package com.optimizely.android;


import org.junit.Before;
import org.junit.Test;
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
public class EventClientTest {

    EventClient eventClient;
    Logger logger;
    HttpURLConnection urlConnection;
    URLProxy url;

    @Before
    public void setupEventClient() throws IOException {
        logger = mock(Logger.class);
        this.eventClient = new EventClient(logger);
        url = mock(URLProxy.class);
        when(url.toString()).thenReturn("http://www.foo.com");
        urlConnection = mock(HttpURLConnection.class);
    }

    @Test
    public void sendEvents200() throws IOException {
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        InputStream inputStream = mock(InputStream.class);
        when(urlConnection.getInputStream()).thenReturn(inputStream);

        assertTrue(eventClient.sendEvent(url));
        verify(logger).info("Dispatching event: {}", url);
    }

    @Test
    public void sendEvents201() throws IOException {
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(201);
        InputStream inputStream = mock(InputStream.class);
        when(urlConnection.getInputStream()).thenReturn(inputStream);

        assertTrue(eventClient.sendEvent(url));
        verify(logger).info("Dispatching event: {}", url);
    }

    @Test
    public void sendEvents300() throws IOException {
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(300);
        InputStream inputStream = mock(InputStream.class);
        when(urlConnection.getInputStream()).thenReturn(inputStream);

        assertFalse(eventClient.sendEvent(url));
        verify(logger).info("Dispatching event: {}", url);
        verify(logger).error("Unexpected response from event endpoint, status: 300");
    }

    @SuppressWarnings("unchecked")
    @Test()
    public void sendEventsIoExceptionGetInputStream() throws IOException {
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(urlConnection.getInputStream()).thenThrow(IOException.class);

        assertFalse(eventClient.sendEvent(url));
        verify(logger).info("Dispatching event: {}", url);

    }

    @SuppressWarnings("unchecked")
    @Test()
    public void sendEventsIoExceptionOpenConnection() throws IOException {
        when(url.openConnection()).thenThrow(IOException.class);

        assertFalse(eventClient.sendEvent(url));
        verify(logger).info("Dispatching event: {}", url);

    }
}
