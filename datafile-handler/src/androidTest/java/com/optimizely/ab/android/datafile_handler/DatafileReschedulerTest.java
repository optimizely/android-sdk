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

package com.optimizely.ab.android.datafile_handler;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.test.platform.app.InstrumentationRegistry;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.DatafileConfig;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DatafileRescheduler}
 */
@RunWith(JUnit4.class)
@Ignore
// Tests pass locally but not on travis
// probably starting too many services
public class DatafileReschedulerTest {

    private DatafileRescheduler datafileRescheduler;
    private Logger logger;

    @Before
    public void setup() {
        datafileRescheduler = new DatafileRescheduler();
        logger = mock(Logger.class);
        datafileRescheduler.logger = logger;
    }

    @Test
    public void receivingNullContext() {
        datafileRescheduler.onReceive(null, mock(Intent.class));
        verify(logger).warn("Received invalid broadcast to data file rescheduler");
    }

    @Test
    public void receivingNullIntent() {
        datafileRescheduler.onReceive(mock(Context.class), null);
        verify(logger).warn("Received invalid broadcast to data file rescheduler");
    }

    @Test
    public void receivedActionBootCompleted() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = mock(Intent.class);
        when(intent.getAction()).thenReturn(Intent.ACTION_BOOT_COMPLETED);
        datafileRescheduler.onReceive(context, intent);
        verify(logger).info("Received intent with action {}", Intent.ACTION_BOOT_COMPLETED);
    }

    @Test
    public void receivedActionMyPackageReplaced() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = mock(Intent.class);
        when(intent.getAction()).thenReturn(Intent.ACTION_MY_PACKAGE_REPLACED);
        datafileRescheduler.onReceive(context, intent);
        verify(logger).info("Received intent with action {}", Intent.ACTION_MY_PACKAGE_REPLACED);
    }

    @Test
    public void dispatchingOneWithoutEnvironment() {
        Context mockContext = mock(Context.class);
        Cache cache = new Cache(InstrumentationRegistry.getInstrumentation().getTargetContext(), logger);
        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(cache, logger);
        backgroundWatchersCache.setIsWatching(new DatafileConfig("1", null), true);
        Logger logger = mock(Logger.class);
        DatafileRescheduler.Dispatcher dispatcher = new DatafileRescheduler.Dispatcher(mockContext, backgroundWatchersCache, logger);
        dispatcher.dispatch();
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext).startService(captor.capture());
        assertEquals(new DatafileConfig("1", null).toJSONString(), captor.getValue().getStringExtra(DatafileService.EXTRA_DATAFILE_CONFIG));
        verify(logger).info("Rescheduled data file watching for project {}", "1");
        cache.delete(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME);
    }

    @Test
    public void dispatchingOneWithEnvironment() {
        Context mockContext = mock(Context.class);
        Cache cache = new Cache(InstrumentationRegistry.getInstrumentation().getTargetContext(), logger);
        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(cache, logger);
        backgroundWatchersCache.setIsWatching(new DatafileConfig("1", "2"), true);
        Logger logger = mock(Logger.class);
        DatafileRescheduler.Dispatcher dispatcher = new DatafileRescheduler.Dispatcher(mockContext, backgroundWatchersCache, logger);
        dispatcher.dispatch();
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext).startService(captor.capture());
        assertEquals(new DatafileConfig("1", "2").toJSONString(), captor.getValue().getStringExtra(DatafileService.EXTRA_DATAFILE_CONFIG));
        verify(logger).info("Rescheduled data file watching for project {}", "2");
        cache.delete(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME);
    }

    @Test
    public void dispatchingManyWithoutEnvironment() {
        Context mockContext = mock(Context.class);
        Cache cache = new Cache(InstrumentationRegistry.getInstrumentation().getTargetContext(), logger);
        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(cache, logger);
        backgroundWatchersCache.setIsWatching(new DatafileConfig("1", null), true);
        backgroundWatchersCache.setIsWatching(new DatafileConfig("2", null), true);
        backgroundWatchersCache.setIsWatching(new DatafileConfig("3", null), true);
        Logger logger = mock(Logger.class);
        DatafileRescheduler.Dispatcher dispatcher = new DatafileRescheduler.Dispatcher(mockContext, backgroundWatchersCache, logger);
        dispatcher.dispatch();
        verify(mockContext, times(3)).startService(any(Intent.class));
        cache.delete(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME);
    }

    @Test
    public void dispatchingManyWithEnvironment() {
        Context mockContext = mock(Context.class);
        Cache cache = new Cache(InstrumentationRegistry.getInstrumentation().getTargetContext(), logger);
        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(cache, logger);
        backgroundWatchersCache.setIsWatching(new DatafileConfig("1", "1"), true);
        backgroundWatchersCache.setIsWatching(new DatafileConfig("2", "1"), true);
        backgroundWatchersCache.setIsWatching(new DatafileConfig("3", "1"), true);
        Logger logger = mock(Logger.class);
        DatafileRescheduler.Dispatcher dispatcher = new DatafileRescheduler.Dispatcher(mockContext, backgroundWatchersCache, logger);
        dispatcher.dispatch();
        verify(mockContext, times(3)).startService(any(Intent.class));
        cache.delete(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME);
    }
}
