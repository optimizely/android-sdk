/****************************************************************************
 * Copyright 2016,2021, Optimizely, Inc. and contributors                   *
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
import android.net.wifi.WifiManager;
import android.os.Build;
import androidx.annotation.RequiresApi;

import com.optimizely.ab.android.shared.ServiceScheduler;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EventRescheduler}
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore
public class EventReschedulerTest {

    private Context context;
    private Intent intent;
    private Logger logger;
    private EventRescheduler rescheduler;

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
        verify(logger).warn("Received unsupported broadcast action to event rescheduler");
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

    @Test
    public void flushOnWifiConnectionIfScheduled() {
        final Intent eventServiceIntent = mock(Intent.class);
        ServiceScheduler serviceScheduler = mock(ServiceScheduler.class);
        when(intent.getAction()).thenReturn(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        when(intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)).thenReturn(true);
        when(serviceScheduler.isScheduled(eventServiceIntent)).thenReturn(true);
        rescheduler.reschedule(context, intent, eventServiceIntent, serviceScheduler);
        verify(context).startService(eventServiceIntent);
        verify(logger).info("Preemptively flushing events since wifi became available");
    }
}
