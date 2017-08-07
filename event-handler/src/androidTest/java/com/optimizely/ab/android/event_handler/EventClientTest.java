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

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.event.LogEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
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

        verify(logger).info("Successfully dispatched event: {}",
                event);
    }
}
