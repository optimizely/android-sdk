package com.optimizely.ab.android.project_watcher;


import com.optimizely.ab.android.shared.Client;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 8/2/16 for Optimizely.
 * <p/>
 * Tests for {@link DataFileClient}
 */
public class DataFileClientTest {

    DataFileClient dataFileClient;
    Logger logger;
    Client client;
    HttpURLConnection urlConnection;

    @Before
    public void setup() {
        client = mock(Client.class);
        logger = mock(Logger.class);
        dataFileClient = new DataFileClient(client, logger);
        urlConnection = mock(HttpURLConnection.class);
    }

    @Test
    public void request200() throws IOException {
        URL url = new URL(String.format(DataFileLoader.RequestDataFileFromClientTask.FORMAT_CDN_URL, "1"));
        when(client.openConnection(url)).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(client.readStream(urlConnection)).thenReturn("{}");

        String response = dataFileClient.request(url);
        assertEquals(response, "{}");

        verify(logger).info("Requesting data file from {}", url);
        verify(client).saveLastModified(urlConnection);
        verify(client).readStream(urlConnection);
    }

    @Test
    public void request201() throws IOException {
        URL url = new URL(String.format(DataFileLoader.RequestDataFileFromClientTask.FORMAT_CDN_URL, "1"));
        when(client.openConnection(url)).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(201);
        when(client.readStream(urlConnection)).thenReturn("{}");

        String response = dataFileClient.request(url);
        assertEquals(response, "{}");

        verify(logger).info("Requesting data file from {}", url);
        verify(client).saveLastModified(urlConnection);
        verify(client).readStream(urlConnection);
    }

    @Test
    public void request299() throws IOException {
        URL url = new URL(String.format(DataFileLoader.RequestDataFileFromClientTask.FORMAT_CDN_URL, "1"));
        when(client.openConnection(url)).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(299);
        when(client.readStream(urlConnection)).thenReturn("{}");

        String response = dataFileClient.request(url);
        assertEquals(response, "{}");

        verify(logger).info("Requesting data file from {}", url);
        verify(client).saveLastModified(urlConnection);
        verify(client).readStream(urlConnection);
    }

    @Test
    public void request300() throws IOException {
        URL url = new URL(String.format(DataFileLoader.RequestDataFileFromClientTask.FORMAT_CDN_URL, "1"));
        when(client.openConnection(url)).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(300);

        String response = dataFileClient.request(url);
        assertNull(response);

        verify(logger).error("Unexpected response from data file cdn, status: {}", 300);
    }

    @Test
    public void handlesIOException() throws IOException {
        URL url = new URL(String.format(DataFileLoader.RequestDataFileFromClientTask.FORMAT_CDN_URL, "1"));
        when(client.openConnection(url)).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        doThrow(new IOException()).when(client).readStream(urlConnection);

        String response = dataFileClient.request(url);
        assertNull(response);

        verify(logger).error(contains("Error making request"), any(IOException.class));
        verify(urlConnection).disconnect();
    }
}
