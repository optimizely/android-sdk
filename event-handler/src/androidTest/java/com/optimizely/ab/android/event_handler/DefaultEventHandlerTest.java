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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.EventBatch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link DefaultEventHandler}
 */
@RunWith(AndroidJUnit4.class)
public class DefaultEventHandlerTest {
    private Context context;
    private Logger logger;

    private DefaultEventHandler eventHandler;
    private String url = "http://www.foo.com";

    @Before
    public void setupEventHandler() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        logger = mock(Logger.class);
        eventHandler = DefaultEventHandler.getInstance(context);
        eventHandler.logger = logger;

        eventHandler.setDispatchInterval(60L);
    }

    @Test
    public void dispatchEventSuccess() throws MalformedURLException {
        eventHandler.dispatchEvent(new LogEvent(LogEvent.RequestMethod.POST, url, new HashMap<String, String>(), new EventBatch()));
        verify(logger).info("Sent url {} to the event handler service", "http://www.foo.com");
    }

    @Test
    public void dispatchEmptyUrlString() {
        eventHandler.dispatchEvent(new LogEvent(LogEvent.RequestMethod.POST, "", new HashMap<String, String>(), new EventBatch()));
        verify(logger).error("Event dispatcher received an empty url");
    }

    @Test
    public void dispatchEmptyParams() {
        eventHandler.dispatchEvent(new LogEvent(LogEvent.RequestMethod.POST, url, new HashMap<String, String>(), new EventBatch()));
        //verify(context).startService(any(Intent.class));
        verify(logger).info("Sent url {} to the event handler service", "http://www.foo.com");
    }

}
