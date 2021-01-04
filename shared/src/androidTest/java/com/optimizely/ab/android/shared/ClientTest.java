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

package com.optimizely.ab.android.shared;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Tests for {@link com.optimizely.ab.android.shared.Client}
 */
@RunWith(AndroidJUnit4.class)
public class ClientTest {

    private Client client;
    private OptlyStorage optlyStorage;
    private Logger logger;

    @Before
    public void setup() {
        optlyStorage = mock(OptlyStorage.class);
        logger = mock(Logger.class);
        client = new Client(optlyStorage, logger);
    }

    @Test
    public void setIfModifiedSinceHasValueInStorage() {
        URL url = null;

        try {
            url = new URL("http://www.optimizely.com");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        when(optlyStorage.getLong(url.toString(), 0)).thenReturn(100L);
        URLConnection urlConnection = mock(URLConnection.class);
        when(urlConnection.getURL()).thenReturn(url);

        client.setIfModifiedSince(urlConnection);
        verify(urlConnection).setIfModifiedSince(100L);
    }

    @Test
    public void saveLastModifiedNoHeader() {
        URL url = null;

        try {
            url = new URL("http://www.optimizely.com");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        URLConnection urlConnection = mock(URLConnection.class);
        when(urlConnection.getURL()).thenReturn(url);

        when(urlConnection.getLastModified()).thenReturn(0L);
        client.saveLastModified(urlConnection);
        verify(logger).warn("CDN response didn't have a last modified header");
    }

    @Test
    public void saveLastModifiedWhenExists() {
        URL url = null;

        try {
            url = new URL("http://www.optimizely.com");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        URLConnection urlConnection = mock(URLConnection.class);
        when(urlConnection.getLastModified()).thenReturn(100L);
        when(urlConnection.getURL()).thenReturn(url);
        client.saveLastModified(urlConnection);
        verify(optlyStorage).saveLong(url.toString(), 100L);
    }

    @Test
    public void readStreamReturnsString() throws IOException {
        String foo = "foo";
        InputStream is = new ByteArrayInputStream(foo.getBytes());
        URLConnection urlConnection = mock(URLConnection.class);
        when(urlConnection.getInputStream()).thenReturn(is);
        String readFoo = client.readStream(urlConnection);
        assertEquals(foo, readFoo);
    }

    @Test
    public void testExpBackoffSuccess() {
        Client.Request request = mock(Client.Request.class);
        final Object expectedResponse = new Object();
        when(request.execute()).thenReturn(expectedResponse);
        Object response = client.execute(request, 2, 4);
        assertEquals(expectedResponse, response);
        verify(logger, never()).info(eq("Request failed, waiting {} seconds to try again"), any(Integer.class));
    }

    @Test
    public void testExpBackoffFailure() {
        Client.Request request = mock(Client.Request.class);
        when(request.execute()).thenReturn(null);
        assertNull(client.execute(request, 2, 4));
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(logger, times(4)).info(eq("Request failed, waiting {} seconds to try again"), captor.capture());
        List<Integer> timeouts = captor.getAllValues();
        assertTrue(timeouts.contains(2));
        assertTrue(timeouts.contains(4));
        assertTrue(timeouts.contains(8));
        assertTrue(timeouts.contains(16));
    }
}
