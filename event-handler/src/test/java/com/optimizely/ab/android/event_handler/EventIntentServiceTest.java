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

import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link EventIntentService}
 */
@RunWith(MockitoJUnitRunner.class)
public class EventIntentServiceTest {

    private EventIntentService service;
    private Logger logger;

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
