/**
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
package com.optimizely.ab.android.event_handler;

import android.content.Intent;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by jdeffibaugh on 7/25/16 for Optimizely.
 *
 * Unit tests for {@link EventIntentService}
 */
@RunWith(AndroidJUnit4.class)
public class EventIntentServiceTest {

    EventIntentService service;
    Logger logger;

    @Before
    public void setup() {
        service = new EventIntentService();
        logger = mock(Logger.class);
        service.logger = logger;
    }

    @Test
    public void testHandleNullIntent() {
        service.onHandleIntent(null);
        verify(logger).warn("Handled a null intent");
    }

    @Test
    public void testHandleNullIntentHandler() {
        Intent intent = mock(Intent.class);
        service.onHandleIntent(intent);
        verify(logger).warn("Unable to create dependencies needed by intent handler");
    }

    @Test
    public void forwardsToIntentHandler() {
        Intent intent = mock(Intent.class);
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);
        service.eventDispatcher = eventDispatcher;
        service.onHandleIntent(intent);
        verify(eventDispatcher).dispatch(intent);
        verify(logger).info("Handled intent");
    }
}
