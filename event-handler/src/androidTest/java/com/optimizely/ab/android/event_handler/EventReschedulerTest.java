// Copyright 2016,2021,2023, Optimizely, Inc. and contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.optimizely.ab.android.event_handler;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.test.ext.junit.runners.AndroidJUnit4;

/**
 * Unit tests for {@link EventRescheduler}
 */
@RunWith(AndroidJUnit4.class)
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
        rescheduler = spy(new EventRescheduler());
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
    public void onReceiveWhenRescheduleWithException() {
        when(intent.getAction()).thenThrow(new IllegalStateException());
        rescheduler.onReceive(context, intent);
        verify(logger).warn(matches("WorkScheduler failed to reschedule an event service.*"));
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
        when(intent.getAction()).thenReturn(WifiManager.WIFI_STATE_CHANGED_ACTION);
        NetworkInfo info = mock(NetworkInfo.class);
        when(info.isConnected()).thenReturn(true);
        when(intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).thenReturn(info);

        rescheduler.reschedule(context, intent);
        verify(logger).info("Preemptively flushing events since wifi became available");
    }
}
