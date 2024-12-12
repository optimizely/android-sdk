/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
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

import androidx.annotation.RequiresApi;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.optimizely.ab.android.shared.Client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DefaultEventHandler}
 */
@RunWith(AndroidJUnit4.class)
public class EventClientTest {

    @Mock
    Logger logger = mock(Logger.class);
    @Mock
    Client client = mock(Client.class);
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
    public void testEventClient() {
        eventClient.sendEvent(event);

        verify(logger).debug("SendEvent completed: {}", event);
    }

    @Test
    public void testConnectionAndReadTimeout() throws IOException {
        when(client.openConnection(event.getURL())).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        doThrow(new IOException()).when(urlConnection).getOutputStream();

        eventClient.sendEvent(event);


        ArgumentCaptor<Client.Request> captor1 = ArgumentCaptor.forClass(Client.Request.class);
        verify(client).execute(captor1.capture(), anyInt(), anyInt());
        Object response = captor1.getValue().execute();

        verify(logger).error(contains("Unable to send event"), any(Event.class), any(IOException.class));
        verify(urlConnection).setConnectTimeout(10*1000);
        verify(urlConnection).setReadTimeout(60*1000);
        verify(urlConnection).disconnect();
    }
}
