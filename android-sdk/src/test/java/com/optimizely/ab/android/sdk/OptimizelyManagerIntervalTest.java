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

package com.optimizely.ab.android.sdk;

import android.content.Context;

import com.optimizely.ab.android.datafile_handler.DatafileHandler;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.android.user_profile.DefaultUserProfileService;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.event.BatchEventProcessor;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.EventProcessor;
import com.optimizely.ab.notification.NotificationCenter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;


@RunWith(PowerMockRunner.class)
@PrepareForTest({OptimizelyManager.class, BatchEventProcessor.class})
public class OptimizelyManagerIntervalTest {

    private Logger logger;

    // DatafileDownloadInterval

    @Test
    public void testBuildWithDatafileDownloadInterval() throws Exception {
        whenNew(OptimizelyManager.class).withAnyArguments().thenReturn(mock(OptimizelyManager.class));

        Context appContext = mock(Context.class);
        when(appContext.getApplicationContext()).thenReturn(appContext);

        long goodNumber = 27;
        OptimizelyManager manager = OptimizelyManager.builder("1")
                .withLogger(logger)
                .withDatafileDownloadInterval(goodNumber, TimeUnit.MINUTES)
                .build(appContext);

        verifyNew(OptimizelyManager.class).withArguments(anyString(),
                anyString(),
                any(DatafileConfig.class),
                any(Logger.class),
                eq(goodNumber * 60L),                   // seconds
                any(DatafileHandler.class),
                any(ErrorHandler.class),
                anyLong(),
                any(EventHandler.class),
                any(EventProcessor.class),
                any(UserProfileService.class),
                any(NotificationCenter.class),
                anyList());
    }

    @Test
    public void testBuildWithDatafileDownloadIntervalDeprecated() throws Exception {
        whenNew(OptimizelyManager.class).withAnyArguments().thenReturn(mock(OptimizelyManager.class));

        Context appContext = mock(Context.class);
        when(appContext.getApplicationContext()).thenReturn(appContext);

        long goodNumber = 1234L;
        OptimizelyManager manager = OptimizelyManager.builder("1")
                .withLogger(logger)
                .withDatafileDownloadInterval(goodNumber)      // deprecated
                .build(appContext);

        verifyNew(OptimizelyManager.class).withArguments(anyString(),
                anyString(),
                any(DatafileConfig.class),
                any(Logger.class),
                eq(goodNumber),                             // seconds
                any(DatafileHandler.class),
                any(ErrorHandler.class),
                anyLong(),
                any(EventHandler.class),
                any(EventProcessor.class),
                any(UserProfileService.class),
                any(NotificationCenter.class),
                anyList());
    }

    @Test
    public void testBuildWithEventDispatchInterval() throws Exception {
        whenNew(OptimizelyManager.class).withAnyArguments().thenReturn(mock(OptimizelyManager.class));
        whenNew(BatchEventProcessor.class).withAnyArguments().thenReturn(mock(BatchEventProcessor.class));

        Context appContext = mock(Context.class);
        when(appContext.getApplicationContext()).thenReturn(appContext);

        long goodNumber = 100L;
        OptimizelyManager manager = OptimizelyManager.builder("1")
                .withLogger(logger)
                .withEventDispatchInterval(goodNumber, TimeUnit.SECONDS)
                .build(appContext);

        verifyNew(BatchEventProcessor.class).withArguments(any(BlockingQueue.class),
                any(EventHandler.class),
                anyInt(),
                eq(goodNumber * 1000L),         // milliseconds
                anyLong(),
                any(ExecutorService.class),
                any(NotificationCenter.class),
                any(Object.class));

        verifyNew(OptimizelyManager.class).withArguments(anyString(),
                anyString(),
                any(DatafileConfig.class),
                any(Logger.class),
                anyLong(),
                any(DatafileHandler.class),
                any(ErrorHandler.class),
                eq(-1L),                        // milliseconds
                any(EventHandler.class),
                any(EventProcessor.class),
                any(UserProfileService.class),
                any(NotificationCenter.class),
                anyList());
    }

    @Test
    public void testBuildWithEventDispatchRetryInterval() throws Exception {
        whenNew(OptimizelyManager.class).withAnyArguments().thenReturn(mock(OptimizelyManager.class));
        whenNew(BatchEventProcessor.class).withAnyArguments().thenReturn(mock(BatchEventProcessor.class));

        Context appContext = mock(Context.class);
        when(appContext.getApplicationContext()).thenReturn(appContext);

        long goodNumber = 100L;
        long defaultEventFlushInterval = 30L;

        OptimizelyManager manager = OptimizelyManager.builder("1")
                .withLogger(logger)
                .withEventDispatchRetryInterval(goodNumber, TimeUnit.MINUTES)
                .build(appContext);

        verifyNew(BatchEventProcessor.class).withArguments(any(BlockingQueue.class),
                any(EventHandler.class),
                anyInt(),
                eq(defaultEventFlushInterval * 1000L),   // milliseconds
                anyLong(),
                any(ExecutorService.class),
                any(NotificationCenter.class),
                any(Object.class));

        verifyNew(OptimizelyManager.class).withArguments(anyString(),
                anyString(),
                any(DatafileConfig.class),
                any(Logger.class),
                anyLong(),
                any(DatafileHandler.class),
                any(ErrorHandler.class),
                eq(goodNumber * 1000L * 60L),     // milliseconds
                any(EventHandler.class),
                any(EventProcessor.class),
                any(UserProfileService.class),
                any(NotificationCenter.class),
                anyList());
    }

    @Test
    public void testBuildWithEventDispatchIntervalDeprecated() throws Exception {
        whenNew(OptimizelyManager.class).withAnyArguments().thenReturn(mock(OptimizelyManager.class));
        whenNew(BatchEventProcessor.class).withAnyArguments().thenReturn(mock(BatchEventProcessor.class));

        Context appContext = mock(Context.class);
        when(appContext.getApplicationContext()).thenReturn(appContext);

        long goodNumber = 1234L;
        OptimizelyManager manager = OptimizelyManager.builder("1")
                .withLogger(logger)
                .withEventDispatchInterval(goodNumber)      // deprecated
                .build(appContext);

        verifyNew(BatchEventProcessor.class).withArguments(any(BlockingQueue.class),
                any(EventHandler.class),
                anyInt(),
                eq(goodNumber),                             // milliseconds
                anyLong(),
                any(ExecutorService.class),
                any(NotificationCenter.class),
                any(Object.class));

        verifyNew(OptimizelyManager.class).withArguments(anyString(),
                anyString(),
                any(DatafileConfig.class),
                any(Logger.class),
                anyLong(),
                any(DatafileHandler.class),
                any(ErrorHandler.class),
                eq(goodNumber),                             // milliseconds
                any(EventHandler.class),
                any(EventProcessor.class),
                any(UserProfileService.class),
                any(NotificationCenter.class),
                anyList());
    }

}
