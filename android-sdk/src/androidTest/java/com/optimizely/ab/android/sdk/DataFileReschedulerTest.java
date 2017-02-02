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

package com.optimizely.ab.android.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.test.InstrumentationRegistry;

import com.optimizely.ab.android.shared.Cache;

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
 * Tests for {@link DataFileRescheduler}
 */
@RunWith(JUnit4.class)
@Ignore
// Tests pass locally but not on travis
// probably starting too many services
public class DataFileReschedulerTest {

    private DataFileRescheduler dataFileRescheduler;
    private Logger logger;

    @Before
    public void setup() {
        dataFileRescheduler = new DataFileRescheduler();
        logger = mock(Logger.class);
        dataFileRescheduler.logger = logger;
    }

    @Test
    public void receivingNullContext() {
        dataFileRescheduler.onReceive(null, mock(Intent.class));
        verify(logger).warn("Received invalid broadcast to data file rescheduler");
    }

    @Test
    public void receivingNullIntent() {
        dataFileRescheduler.onReceive(mock(Context.class), null);
        verify(logger).warn("Received invalid broadcast to data file rescheduler");
    }

    @Test
    public void receivedActionBootCompleted() {
        Context context = InstrumentationRegistry.getTargetContext();
        Intent intent = mock(Intent.class);
        when(intent.getAction()).thenReturn(Intent.ACTION_BOOT_COMPLETED);
        dataFileRescheduler.onReceive(context, intent);
        verify(logger).info("Received intent with action {}", Intent.ACTION_BOOT_COMPLETED);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Test
    public void receivedActionMyPackageReplaced() {
        Context context = InstrumentationRegistry.getTargetContext();
        Intent intent = mock(Intent.class);
        when(intent.getAction()).thenReturn(Intent.ACTION_MY_PACKAGE_REPLACED);
        dataFileRescheduler.onReceive(context, intent);
        verify(logger).info("Received intent with action {}", Intent.ACTION_MY_PACKAGE_REPLACED);
    }

    @Test
    public void dispatchingOne() {
        Context mockContext = mock(Context.class);
        Cache cache = new Cache(InstrumentationRegistry.getTargetContext(), logger);
        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(cache, logger);
        backgroundWatchersCache.setIsWatching("1", true);
        Logger logger = mock(Logger.class);
        DataFileRescheduler.Dispatcher dispatcher = new DataFileRescheduler.Dispatcher(mockContext, backgroundWatchersCache, logger);
        Intent intent = new Intent(mockContext, DataFileService.class);
        dispatcher.dispatch(intent);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext).startService(captor.capture());
        assertEquals("1", captor.getValue().getStringExtra(DataFileService.EXTRA_PROJECT_ID));
        verify(logger).info("Rescheduled data file watching for project {}", "1");
        cache.delete(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME);
    }

    @Test
    public void dispatchingMany() {
        Context mockContext = mock(Context.class);
        Cache cache = new Cache(InstrumentationRegistry.getTargetContext(), logger);
        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(cache, logger);
        backgroundWatchersCache.setIsWatching("1", true);
        backgroundWatchersCache.setIsWatching("2", true);
        backgroundWatchersCache.setIsWatching("3", true);
        Logger logger = mock(Logger.class);
        DataFileRescheduler.Dispatcher dispatcher = new DataFileRescheduler.Dispatcher(mockContext, backgroundWatchersCache, logger);
        Intent intent = new Intent(mockContext, DataFileService.class);
        dispatcher.dispatch(intent);
        verify(mockContext, times(3)).startService(any(Intent.class));
        cache.delete(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME);
    }
}
