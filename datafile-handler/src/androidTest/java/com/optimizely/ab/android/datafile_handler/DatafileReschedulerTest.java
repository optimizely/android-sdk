/****************************************************************************
 * Copyright 2016, 2021-2022, Optimizely, Inc. and contributors              *
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

import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.DatafileConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link DatafileRescheduler}
 */
@RunWith(JUnit4.class)
public class DatafileReschedulerTest {

    private DatafileRescheduler datafileRescheduler;
    private Logger logger;
    private Context context;
    private Cache cache;
    private BackgroundWatchersCache backgroundWatchersCache;
    private DatafileRescheduler.Dispatcher dispatcher;
    private ArgumentCaptor<DatafileConfig> captor;

    @Before
    public void setup() {
        context = mock(Context.class);
        logger = mock(Logger.class);
        cache = new Cache(InstrumentationRegistry.getInstrumentation().getTargetContext(), logger);
        cache.delete(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME);
        backgroundWatchersCache = new BackgroundWatchersCache(cache, logger);
        captor = ArgumentCaptor.forClass(DatafileConfig.class);

        dispatcher = spy(new DatafileRescheduler.Dispatcher(context, backgroundWatchersCache, logger));
        doNothing().when(dispatcher).rescheduleService(any());

        datafileRescheduler = new DatafileRescheduler();
        datafileRescheduler.logger = logger;
    }

    @After
    public void teardown() {
        cache.delete(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME);
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
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N)
    public void dispatchingOneWithoutEnvironment() throws InterruptedException {
        // projectId: number string
        // sdkKey: alphabet string
        backgroundWatchersCache.setIsWatching(new DatafileConfig("1", null), true);
        dispatcher.dispatch();
        TimeUnit.SECONDS.sleep(1);

        verify(dispatcher).rescheduleService(captor.capture());
        assertEquals(new DatafileConfig("1", null), captor.getValue());
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N)
    public void dispatchingOneWithEnvironment() throws InterruptedException {
        // projectId: number string
        // sdkKey: alphabet string
        backgroundWatchersCache.setIsWatching(new DatafileConfig(null, "A"), true);
        dispatcher.dispatch();
        TimeUnit.SECONDS.sleep(1);

        verify(dispatcher).rescheduleService(captor.capture());
        assertEquals(new DatafileConfig(null, "A"), captor.getValue());
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N)
    public void dispatchingManyForLegacy() throws InterruptedException {
        backgroundWatchersCache.setIsWatching(new DatafileConfig("1", null), true);
        backgroundWatchersCache.setIsWatching(new DatafileConfig("2", "A"), true);
        backgroundWatchersCache.setIsWatching(new DatafileConfig(null, "B"), true);
        backgroundWatchersCache.setIsWatching(new DatafileConfig("3", null), true);
        dispatcher.dispatch();
        TimeUnit.SECONDS.sleep(1);

        verify(dispatcher, times(4)).rescheduleService(captor.capture());
        List array = new ArrayList<String>();
        for(DatafileConfig config : captor.getAllValues()) {
            array.add(config.toString());
        }
        assert(array.contains(new DatafileConfig("1", null).toString()));
        assert(array.contains(new DatafileConfig(null, "A").toString()));
        assert(array.contains(new DatafileConfig(null, "B").toString()));
        assert(array.contains(new DatafileConfig("3", null).toString()));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    public void dispatchingManyForJobScheduler() throws InterruptedException {
        backgroundWatchersCache.setIsWatching(new DatafileConfig("1", null), true);
        backgroundWatchersCache.setIsWatching(new DatafileConfig(null, "A"), true);
        dispatcher.dispatch();
        TimeUnit.SECONDS.sleep(1);

        verify(dispatcher, never()).rescheduleService(captor.capture());
    }

}
