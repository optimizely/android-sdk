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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkManager;
import androidx.work.testing.TestWorkerBuilder;

import com.optimizely.ab.android.shared.OptlyStorage;
import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Tests {@link EventWorker}
 */
@RunWith(AndroidJUnit4.class)
public class EventWorkerTest {
    private Context context;
    private Executor executor;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        executor = Executors.newSingleThreadExecutor();
    }

    @Test
    public void testEventWorker_dispatchSuccess() {
        String host = "host-1";
        String body = "body-1";
        Long retryInterval = 123L;
        EventWorker worker = mockEventWorker(host, body, retryInterval);

        EventDispatcher dispatcher = mock(EventDispatcher.class);
        when(dispatcher.dispatch(host, body)).thenReturn(true);    // dispatched successfully
        worker.eventDispatcher = dispatcher;

        ListenableWorker.Result result = worker.doWork();

        assertThat(result, is(ListenableWorker.Result.success()));  // success
        verify(worker.eventDispatcher).dispatch(host, body);
    }

    @Test
    public void testEventWorker_noRetryOnDispatchError() {
        String host = "host-1";
        String body = "body-1";
        Long retryInterval = -1L;  // no retry
        EventWorker worker = mockEventWorker(host, body, retryInterval);

        EventDispatcher dispatcher = mock(EventDispatcher.class);
        when(dispatcher.dispatch(host, body)).thenReturn(false);    // dispatch failed
        worker.eventDispatcher = dispatcher;

        ListenableWorker.Result result = worker.doWork();

        assertThat(result, is(ListenableWorker.Result.success()));  // always success (no retry)
        verify(worker.eventDispatcher).dispatch(host, body);
    }

    @Test
    public void testEventWorker_retryOnDispatchError() {
        String host = "host-1";
        String body = "body-1";
        Long retryInterval = 123L;  // retry
        EventWorker worker = mockEventWorker(host, body, retryInterval);

        EventDispatcher dispatcher = mock(EventDispatcher.class);
        when(dispatcher.dispatch(host, body)).thenReturn(false);    // dispatch failed
        worker.eventDispatcher = dispatcher;

        ListenableWorker.Result result = worker.doWork();

        assertThat(result, is(ListenableWorker.Result.retry()));  // retry
        verify(worker.eventDispatcher).dispatch(host, body);
    }

    // Helpers

    EventWorker mockEventWorker(String host, String body, Long retryInterval) {
        LogEvent event = mock(LogEvent.class);
        when(event.getEndpointUrl()).thenReturn(host);
        when(event.getBody()).thenReturn(body);

        Data inputData = EventWorker.getData(event, retryInterval);

        return (EventWorker) TestWorkerBuilder.from(context, EventWorker.class, executor)
                .setInputData(inputData)
                .build();
    }

}
