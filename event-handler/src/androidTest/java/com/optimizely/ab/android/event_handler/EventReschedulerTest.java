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

import android.content.Context;
import android.content.Intent;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 7/26/16 for Optimizely.
 *
 * Unit tests for {@link EventRescheduler}
 */
@RunWith(AndroidJUnit4.class)
public class EventReschedulerTest {

    Context context;
    Intent intent;
    Logger logger;
    EventRescheduler rescheduler;

    @Before
    public void setupEventRescheduler() {
        context = mock(Context.class);
        intent = mock(Intent.class);
        logger = mock(Logger.class);
        rescheduler = mock(EventRescheduler.class);
        rescheduler = new EventRescheduler();
        rescheduler.logger = logger;
    }

    @Test
    public void onReceiveNullIntent() {
        rescheduler.onReceive(context, null);
        verify(logger).warn("Received invalid broadcast to event rescheduler");
    }

    @Test
    public void onReceiveNullContext() {
        rescheduler.onReceive(null, intent);
        verify(logger).warn("Received invalid broadcast to event rescheduler");
    }

    @Test
    public void onReceiveInvalidAction() {
        when(intent.getAction()).thenReturn("invalid");
        rescheduler.onReceive(context, intent);
        verify(logger).warn("Received invalid broadcast to event rescheduler");
    }

    @Test
    public void onReceiveValidBootComplete() {
        when(intent.getAction()).thenReturn(Intent.ACTION_BOOT_COMPLETED);
        rescheduler.onReceive(context, intent);
        verify(logger).info("Rescheduling event flushing if necessary");
    }

    @Test
    public void onReceiveValidPackageReplaced() {
        when(intent.getAction()).thenReturn(Intent.ACTION_MY_PACKAGE_REPLACED);
        rescheduler.onReceive(context, intent);
        verify(logger).info("Rescheduling event flushing if necessary");
    }
}
