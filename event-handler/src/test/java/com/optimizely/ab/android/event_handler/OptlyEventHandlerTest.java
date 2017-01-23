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
import android.content.Intent;

import com.optimizely.ab.event.LogEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.util.HashMap;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link OptlyEventHandler}
 */
@RunWith(MockitoJUnitRunner.class)
public class OptlyEventHandlerTest {

    private Context context;
    private Logger logger;

    private OptlyEventHandler optlyEventHandler;
    private String url = "http://www.foo.com";
    private String requestBody = "key1=val1&key2=val2&key3=val3";

    @Before
    public void setupEventHandler() {
        context = mock(Context.class);
        logger = mock(Logger.class);
        optlyEventHandler = OptlyEventHandler.getInstance(context);
        optlyEventHandler.logger = logger;
    }

    @Test
    public void dispatchEventSuccess() throws MalformedURLException {
        optlyEventHandler.dispatchEvent(new LogEvent(LogEvent.RequestMethod.POST, url, new HashMap<String, String>(), requestBody));
        verify(context).startService(any(Intent.class));
        verify(logger).info("Sent url {} to the event handler service", "http://www.foo.com");
    }

    @Test public void dispatchEmptyUrlString() {
        optlyEventHandler.dispatchEvent(new LogEvent(LogEvent.RequestMethod.POST, "", new HashMap<String, String>(), requestBody));
        verify(logger).error("Event dispatcher received an empty url");
    }

    @Test public void dispatchEmptyParams() {
        optlyEventHandler.dispatchEvent(new LogEvent(LogEvent.RequestMethod.POST, url, new HashMap<String, String>(), requestBody));
        verify(context).startService(any(Intent.class));
        verify(logger).info("Sent url {} to the event handler service", "http://www.foo.com");
    }
}
