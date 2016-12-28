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
package com.optimizely.ab.android.sdk;


import com.optimizely.ab.android.shared.Client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DataFileClient}
 */
@RunWith(JUnit4.class)
public class DataFileClientTest {

    private DataFileClient dataFileClient;
    private Logger logger;
    private Client client;
    private HttpURLConnection urlConnection;

    @Before
    public void setup() {
        client = mock(Client.class);
        logger = mock(Logger.class);
        dataFileClient = new DataFileClient(client, logger);
        urlConnection = mock(HttpURLConnection.class);
    }

    @Test
    public void request200() throws IOException {
        URL url = new URL(DataFileService.getDatafileUrl("1"));
        when(client.openConnection(url)).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(client.readStream(urlConnection)).thenReturn("{}");

        dataFileClient.request(url.toString());

        ArgumentCaptor<Client.Request> captor1 = ArgumentCaptor.forClass(Client.Request.class);
        ArgumentCaptor<Integer> captor2 = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> captor3 = ArgumentCaptor.forClass(Integer.class);
        verify(client).execute(captor1.capture(), captor2.capture(), captor3.capture());
        assertEquals(Integer.valueOf(2), captor2.getValue());
        assertEquals(Integer.valueOf(3), captor3.getValue());
        Object response = captor1.getValue().execute();
        assertTrue(String.class.isInstance(response));
        assertEquals("{}", response);

        verify(logger).info("Requesting data file from {}", url);
        verify(client).saveLastModified(urlConnection);
        verify(client).readStream(urlConnection);
        verify(urlConnection).disconnect();
    }

    @Test
    public void request201() throws IOException {
        URL url = new URL(DataFileService.getDatafileUrl("1"));
        when(client.openConnection(url)).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(201);
        when(client.readStream(urlConnection)).thenReturn("{}");

        dataFileClient.request(url.toString());

        ArgumentCaptor<Client.Request> captor1 = ArgumentCaptor.forClass(Client.Request.class);
        ArgumentCaptor<Integer> captor2 = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> captor3 = ArgumentCaptor.forClass(Integer.class);
        verify(client).execute(captor1.capture(), captor2.capture(), captor3.capture());
        assertEquals(Integer.valueOf(2), captor2.getValue());
        assertEquals(Integer.valueOf(3), captor3.getValue());
        Object response = captor1.getValue().execute();
        assertTrue(String.class.isInstance(response));
        assertEquals("{}", response);

        verify(logger).info("Requesting data file from {}", url);
        verify(client).saveLastModified(urlConnection);
        verify(client).readStream(urlConnection);
        verify(urlConnection).disconnect();
    }

    @Test
    public void request299() throws IOException {
        URL url = new URL(DataFileService.getDatafileUrl("1"));
        when(client.openConnection(url)).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(299);
        when(client.readStream(urlConnection)).thenReturn("{}");

        dataFileClient.request(url.toString());

        ArgumentCaptor<Client.Request> captor1 = ArgumentCaptor.forClass(Client.Request.class);
        ArgumentCaptor<Integer> captor2 = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> captor3 = ArgumentCaptor.forClass(Integer.class);
        verify(client).execute(captor1.capture(), captor2.capture(), captor3.capture());
        assertEquals(Integer.valueOf(2), captor2.getValue());
        assertEquals(Integer.valueOf(3), captor3.getValue());
        Object response = captor1.getValue().execute();
        assertTrue(String.class.isInstance(response));
        assertEquals("{}", response);

        verify(logger).info("Requesting data file from {}", url);
        verify(client).saveLastModified(urlConnection);
        verify(client).readStream(urlConnection);
        verify(urlConnection).disconnect();
    }

    @Test
    public void request300() throws IOException {
        URL url = new URL(DataFileService.getDatafileUrl("1"));
        when(client.openConnection(url)).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(300);

        dataFileClient.request(url.toString());
        ArgumentCaptor<Client.Request> captor1 = ArgumentCaptor.forClass(Client.Request.class);
        ArgumentCaptor<Integer> captor2 = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> captor3 = ArgumentCaptor.forClass(Integer.class);
        verify(client).execute(captor1.capture(), captor2.capture(), captor3.capture());
        assertEquals(Integer.valueOf(2), captor2.getValue());
        assertEquals(Integer.valueOf(3), captor3.getValue());
        Object response = captor1.getValue().execute();
        assertNull(response);

        verify(logger).error("Unexpected response from data file cdn, status: {}", 300);
        verify(urlConnection).disconnect();
    }

    @Test
    public void handlesIOException() throws IOException {
        URL url = new URL(DataFileService.getDatafileUrl("1"));
        when(client.openConnection(url)).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        doThrow(new IOException()).when(urlConnection).connect();

        dataFileClient.request(url.toString());
        ArgumentCaptor<Client.Request> captor1 = ArgumentCaptor.forClass(Client.Request.class);
        ArgumentCaptor<Integer> captor2 = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> captor3 = ArgumentCaptor.forClass(Integer.class);
        verify(client).execute(captor1.capture(), captor2.capture(), captor3.capture());
        assertEquals(Integer.valueOf(2), captor2.getValue());
        assertEquals(Integer.valueOf(3), captor3.getValue());
        Object response = captor1.getValue().execute();
        assertNull(response);

        verify(logger).error(contains("Error making request"), any(IOException.class));
        verify(urlConnection).disconnect();
        verify(urlConnection).disconnect();
    }

    @Test
    public void handlesNullResponse() throws MalformedURLException {
        URL url = new URL(DataFileService.getDatafileUrl("1"));
        when(client.execute(any(Client.Request.class), eq(2), eq(3))).thenReturn(null);
        assertNull(dataFileClient.request(url.toString()));
    }

    @Test
    public void handlesEmptyStringResponse() throws MalformedURLException {
        URL url = new URL(DataFileService.getDatafileUrl("1"));
        when(client.execute(any(Client.Request.class), eq(2), eq(3))).thenReturn("");
        assertEquals("", dataFileClient.request(url.toString()));
    }
}
