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

package com.optimizely.ab.android.datafile_handler;

import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.DatafileConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DatafileClient}
 */
@RunWith(JUnit4.class)
public class DatafileClientTest {

    private DatafileClient datafileClient;
    private Logger logger;
    private Client client;
    private HttpURLConnection urlConnection;

    @Before
    public void setup() {
        client = mock(Client.class);
        logger = mock(Logger.class);
        datafileClient = new DatafileClient(client, logger);
        urlConnection = mock(HttpURLConnection.class);
    }

    @Test
    public void request200() throws IOException {
        URL url = new URL(new DatafileConfig("1", null).getUrl());
        when(client.openConnection(url)).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(client.readStream(urlConnection)).thenReturn("{}");

        datafileClient.request(url.toString());

        ArgumentCaptor<Client.Request> captor1 = ArgumentCaptor.forClass(Client.Request.class);
        ArgumentCaptor<Integer> captor2 = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> captor3 = ArgumentCaptor.forClass(Integer.class);
        verify(client).execute(captor1.capture(), captor2.capture(), captor3.capture());
        assertEquals(Integer.valueOf(DatafileClient.REQUEST_BACKOFF_TIMEOUT), captor2.getValue());
        assertEquals(Integer.valueOf(DatafileClient.REQUEST_RETRIES_POWER), captor3.getValue());
        Object response = captor1.getValue().execute();
        assertTrue(String.class.isInstance(response));
        assertEquals("{}", response);

        verify(logger).info("Requesting data file from {}", url);
        verify(client).saveLastModified(urlConnection);
        verify(client).readStream(urlConnection);
        verify(urlConnection).disconnect();
    }

    /**
     * testLastModified - This is a test to see if given two projects, the last modified for datafile download is project specific.
     * Two URLs url1 and url2 are both datafile urls, url1 is requested from the data client twice, while url2 is only asked for once.
     * The first time the last modified is 0 and the second time, if it is non-zero, then it is the current last modified and a 304 is returned.
     *
     * @throws IOException
     */
    @Test
    public void testLastModified() throws IOException {
        final URL url1 = new URL(new DatafileConfig("1", null).getUrl());
        final URL url2 = new URL(new DatafileConfig("2", null).getUrl());
        HttpURLConnection urlConnection2 = mock(HttpURLConnection.class);
        when(urlConnection.getURL()).thenReturn(url1);
        when(urlConnection2.getURL()).thenReturn(url2);
        when(urlConnection.getLastModified()).thenReturn(0L);
        when(urlConnection2.getLastModified()).thenReturn(0L);
        when(client.openConnection(url1)).thenReturn(urlConnection);
        Answer<Integer> answer = new Answer<Integer>() {
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                HttpURLConnection connection = (HttpURLConnection) invocation.getMock();
                URL url = connection.getURL();
                if (url == url1) {
                    if (connection.getLastModified() == 0L) {
                        when(connection.getLastModified()).thenReturn(300L);
                        return 200;
                    }
                    else {
                        assertEquals(connection.getLastModified(), 300L);
                        return 304;
                    }
                }
                else if (url == url2) {
                    if (connection.getLastModified() == 0L) {
                        when(connection.getLastModified()).thenReturn(200L);
                        return 200;
                    }
                    else {
                        assertEquals(connection.getLastModified(), 200L);
                        return 304;
                    }
                }
                //Object[] arguments = invocation.getArguments();
                //String string = (String) arguments[0];
                return 0;
            }
        };

        when(urlConnection.getResponseCode()).thenAnswer(answer);
        when(urlConnection2.getResponseCode()).thenAnswer(answer);

        when(client.openConnection(url2)).thenReturn(urlConnection2);
        when(client.readStream(urlConnection)).thenReturn("{}");
        when(client.readStream(urlConnection2)).thenReturn("{}");

        // first call returns the project file {}
        datafileClient.request(url1.toString());

        ArgumentCaptor<Client.Request> captor1 = ArgumentCaptor.forClass(Client.Request.class);
        ArgumentCaptor<Integer> captor2 = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> captor3 = ArgumentCaptor.forClass(Integer.class);
        verify(client).execute(captor1.capture(), captor2.capture(), captor3.capture());
        assertEquals(Integer.valueOf(DatafileClient.REQUEST_BACKOFF_TIMEOUT), captor2.getValue());
        assertEquals(Integer.valueOf(DatafileClient.REQUEST_RETRIES_POWER), captor3.getValue());
        Object response = captor1.getValue().execute();
        assertTrue(String.class.isInstance(response));
        assertEquals("{}", response);

        verify(logger).info("Requesting data file from {}", url1);
        verify(client).saveLastModified(urlConnection);
        verify(client).readStream(urlConnection);
        verify(urlConnection).disconnect();

        // second call returns 304 so the response is a empty string.
        datafileClient.request(url1.toString());

        captor1 = ArgumentCaptor.forClass(Client.Request.class);
        captor2 = ArgumentCaptor.forClass(Integer.class);
        captor3 = ArgumentCaptor.forClass(Integer.class);
        verify(client, times(2)).execute(captor1.capture(), captor2.capture(), captor3.capture());
        assertEquals(Integer.valueOf(DatafileClient.REQUEST_BACKOFF_TIMEOUT), captor2.getValue());
        assertEquals(Integer.valueOf(DatafileClient.REQUEST_RETRIES_POWER), captor3.getValue());
        response = captor1.getValue().execute();
        assertTrue(String.class.isInstance(response));
        assertEquals("", response);

        verify(logger).info("Data file has not been modified on the cdn");
        verify(urlConnection, times(2)).disconnect();

        datafileClient.request(url2.toString());

        captor1 = ArgumentCaptor.forClass(Client.Request.class);
        captor2 = ArgumentCaptor.forClass(Integer.class);
        captor3 = ArgumentCaptor.forClass(Integer.class);
        verify(client, times(3)).execute(captor1.capture(), captor2.capture(), captor3.capture());
        assertEquals(Integer.valueOf(DatafileClient.REQUEST_BACKOFF_TIMEOUT), captor2.getValue());
        assertEquals(Integer.valueOf(DatafileClient.REQUEST_RETRIES_POWER), captor3.getValue());
        response = captor1.getValue().execute();
        assertTrue(String.class.isInstance(response));
        assertEquals("{}", response);

        verify(logger, times(2)).info("Requesting data file from {}", url1);
        verify(client).saveLastModified(urlConnection2);
        verify(client).readStream(urlConnection2);
        verify(urlConnection2).disconnect();


    }


    @Test
    public void request201() throws IOException {
        URL url = new URL(new DatafileConfig("1", null).getUrl());
        when(client.openConnection(url)).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(201);
        when(client.readStream(urlConnection)).thenReturn("{}");

        datafileClient.request(url.toString());

        ArgumentCaptor<Client.Request> captor1 = ArgumentCaptor.forClass(Client.Request.class);
        ArgumentCaptor<Integer> captor2 = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> captor3 = ArgumentCaptor.forClass(Integer.class);
        verify(client).execute(captor1.capture(), captor2.capture(), captor3.capture());
        assertEquals(Integer.valueOf(DatafileClient.REQUEST_BACKOFF_TIMEOUT), captor2.getValue());
        assertEquals(Integer.valueOf(DatafileClient.REQUEST_RETRIES_POWER), captor3.getValue());
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
        URL url = new URL(new DatafileConfig("1", null).getUrl());
        when(client.openConnection(url)).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(299);
        when(client.readStream(urlConnection)).thenReturn("{}");

        datafileClient.request(url.toString());

        ArgumentCaptor<Client.Request> captor1 = ArgumentCaptor.forClass(Client.Request.class);
        ArgumentCaptor<Integer> captor2 = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> captor3 = ArgumentCaptor.forClass(Integer.class);
        verify(client).execute(captor1.capture(), captor2.capture(), captor3.capture());
        assertEquals(Integer.valueOf(DatafileClient.REQUEST_BACKOFF_TIMEOUT), captor2.getValue());
        assertEquals(Integer.valueOf(DatafileClient.REQUEST_RETRIES_POWER), captor3.getValue());
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
        URL url = new URL(new DatafileConfig("1", null).getUrl());
        when(client.openConnection(url)).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(300);

        datafileClient.request(url.toString());
        ArgumentCaptor<Client.Request> captor1 = ArgumentCaptor.forClass(Client.Request.class);
        ArgumentCaptor<Integer> captor2 = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> captor3 = ArgumentCaptor.forClass(Integer.class);
        verify(client).execute(captor1.capture(), captor2.capture(), captor3.capture());
        assertEquals(Integer.valueOf(DatafileClient.REQUEST_BACKOFF_TIMEOUT), captor2.getValue());
        assertEquals(Integer.valueOf(DatafileClient.REQUEST_RETRIES_POWER), captor3.getValue());
        Object response = captor1.getValue().execute();
        assertNull(response);

        verify(logger).error("Unexpected response from data file cdn, status: {}", 300);
        verify(urlConnection).disconnect();
    }

    @Test
    public void handlesIOException() throws IOException {
        URL url = new URL(new DatafileConfig("1", null).getUrl());
        when(client.openConnection(url)).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        doThrow(new IOException()).when(urlConnection).connect();

        datafileClient.request(url.toString());
        ArgumentCaptor<Client.Request> captor1 = ArgumentCaptor.forClass(Client.Request.class);
        ArgumentCaptor<Integer> captor2 = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> captor3 = ArgumentCaptor.forClass(Integer.class);
        verify(client).execute(captor1.capture(), captor2.capture(), captor3.capture());
        assertEquals(Integer.valueOf(DatafileClient.REQUEST_BACKOFF_TIMEOUT), captor2.getValue());
        assertEquals(Integer.valueOf(DatafileClient.REQUEST_RETRIES_POWER), captor3.getValue());
        Object response = captor1.getValue().execute();
        assertNull(response);

        verify(logger).error(contains("Error making request"), any(IOException.class));
        verify(urlConnection).disconnect();
        verify(urlConnection).disconnect();
    }

    @Test
    public void handlesNullResponse() throws MalformedURLException {
        URL url = new URL(new DatafileConfig("1", null).getUrl());
        when(client.execute(any(Client.Request.class), eq(DatafileClient.REQUEST_BACKOFF_TIMEOUT), eq(DatafileClient.REQUEST_RETRIES_POWER))).thenReturn(null);
        assertNull(datafileClient.request(url.toString()));
    }

    @Test
    public void handlesEmptyStringResponse() throws MalformedURLException {
        URL url = new URL(new DatafileConfig("1", null).getUrl());
        when(client.execute(any(Client.Request.class), eq(DatafileClient.REQUEST_BACKOFF_TIMEOUT), eq(DatafileClient.REQUEST_RETRIES_POWER))).thenReturn("");
        assertEquals("", datafileClient.request(url.toString()));
    }
}
