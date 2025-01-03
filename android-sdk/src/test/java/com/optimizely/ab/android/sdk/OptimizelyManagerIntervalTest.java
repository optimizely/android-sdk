/****************************************************************************
 * Copyright 2017, 2023 Optimizely, Inc. and contributors                   *
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.content.Context;

import com.optimizely.ab.android.datafile_handler.DatafileHandler;
import com.optimizely.ab.android.event_handler.DefaultEventHandler;
import com.optimizely.ab.android.odp.VuidManager;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.event.BatchEventProcessor;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.EventProcessor;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.odp.ODPManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore("jdk.internal.reflect.*")
@PrepareForTest({OptimizelyManager.class, BatchEventProcessor.class, DefaultEventHandler.class, VuidManager.class})
public class OptimizelyManagerIntervalTest {

    private Logger logger;
    private Context mockContext;
    private DefaultEventHandler mockEventHandler;

    @Before
    public void setup() throws Exception {
        mockContext = mock(Context.class);

        whenNew(OptimizelyManager.class).withAnyArguments().thenReturn(mock(OptimizelyManager.class));
        whenNew(BatchEventProcessor.class).withAnyArguments().thenReturn(mock(BatchEventProcessor.class));

        mockEventHandler = mock(DefaultEventHandler.class);
        mockStatic(DefaultEventHandler.class);
        when(DefaultEventHandler.getInstance(any())).thenReturn(mockEventHandler);

        mockStatic(VuidManager.class);
        VuidManager.Companion mockCompanion = PowerMockito.mock(VuidManager.Companion.class);
        VuidManager mockVuidManager = PowerMockito.mock(VuidManager.class);
        doReturn(mockVuidManager).when(mockCompanion).getInstance();
        Whitebox.setInternalState(
            VuidManager.class, "Companion",
            mockCompanion
        );
    }

    // DatafileDownloadInterval

    @Test
    public void testBuildWithDatafileDownloadInterval() throws Exception {
        long goodNumber = 27;
        OptimizelyManager manager = OptimizelyManager.builder("1")
                .withVuid("any-to-avoid-generate")
                .withLogger(logger)
                .withDatafileDownloadInterval(goodNumber, TimeUnit.MINUTES)
                .build(mockContext);

        verifyNew(OptimizelyManager.class).withArguments(
                anyString(),
                any(),                          // nullable (String)
                any(DatafileConfig.class),
                any(Logger.class),
                eq(goodNumber * 60L),                   // seconds
                any(DatafileHandler.class),
                any(),                          // nullable (ErrorHandler)
                anyLong(),
                any(EventHandler.class),
                any(EventProcessor.class),
                any(UserProfileService.class),
                any(NotificationCenter.class),
                any(),                         // nullable (DefaultDecideOptions)
                any(ODPManager.class),
                anyString(),
                any(),
                any());
    }

    @Test
    public void testBuildWithDatafileDownloadIntervalDeprecated() throws Exception {
        long goodNumber = 1234L;
        OptimizelyManager manager = OptimizelyManager.builder("1")
                .withVuid("any-to-avoid-generate")
                .withLogger(logger)
                .withDatafileDownloadInterval(goodNumber)      // deprecated
                .build(mockContext);

        verifyNew(OptimizelyManager.class).withArguments(
                anyString(),
                any(),                          // nullable (String)
                any(DatafileConfig.class),
                any(Logger.class),
                eq(goodNumber),                   // seconds
                any(DatafileHandler.class),
                any(),                          // nullable (ErrorHandler)
                anyLong(),
                any(EventHandler.class),
                any(EventProcessor.class),
                any(UserProfileService.class),
                any(NotificationCenter.class),
                any(),                         // nullable (DefaultDecideOptions)
                any(ODPManager.class),
                anyString(),
                any(),
                any());
    }

    @Test
    public void testBuildWithEventDispatchInterval() throws Exception {
        long goodNumber = 100L;
        OptimizelyManager manager = OptimizelyManager.builder("1")
                .withVuid("any-to-avoid-generate")
                .withLogger(logger)
                .withEventDispatchInterval(goodNumber, TimeUnit.SECONDS)
                .build(mockContext);

        verifyNew(BatchEventProcessor.class).withArguments(
                any(BlockingQueue.class),
                any(EventHandler.class),
                anyInt(),
                eq(goodNumber * 1000L),         // milliseconds
                anyLong(),
                any(ExecutorService.class),
                any(NotificationCenter.class),
                any());                                 // PowerMock bug? requires an extra null at the end

        verify(mockEventHandler).setDispatchInterval(-1L);  // default

        verifyNew(OptimizelyManager.class).withArguments(
                anyString(),
                any(),                          // nullable (String)
                any(DatafileConfig.class),
                any(Logger.class),
                anyLong(),
                any(DatafileHandler.class),
                any(),                          // nullable (ErrorHandler)
                eq(-1L),                        // default
                any(EventHandler.class),
                any(EventProcessor.class),
                any(UserProfileService.class),
                any(NotificationCenter.class),
                any(),                         // nullable (DefaultDecideOptions)
                any(ODPManager.class),
                anyString(),
                any(),
                any());
    }

    @Test
    public void testBuildWithEventDispatchRetryInterval() throws Exception {
        long goodNumber = 100L;
        TimeUnit timeUnit =  TimeUnit.MINUTES;
        long defaultEventFlushInterval = 30L;   // seconds

        OptimizelyManager manager = OptimizelyManager.builder("1")
                .withVuid("any-to-avoid-generate")
                .withLogger(logger)
                .withEventDispatchRetryInterval(goodNumber, timeUnit)
                .build(mockContext);

        verifyNew(BatchEventProcessor.class).withArguments(
                any(BlockingQueue.class),
                any(EventHandler.class),
                anyInt(),
                eq(defaultEventFlushInterval * 1000L),   // milliseconds
                anyLong(),
                any(ExecutorService.class),
                any(NotificationCenter.class),
                any());                                 // PowerMock bug? requires an extra null at the end

        verify(mockEventHandler).setDispatchInterval(timeUnit.toMillis(goodNumber));  // milli-seconds

        verifyNew(OptimizelyManager.class).withArguments(
                anyString(),
                any(),                          // nullable (String)
                any(DatafileConfig.class),
                any(Logger.class),
                anyLong(),
                any(DatafileHandler.class),
                any(),                          // nullable (ErrorHandler)
                eq(goodNumber * 1000L * 60L),     // milliseconds
                any(EventHandler.class),
                any(EventProcessor.class),
                any(UserProfileService.class),
                any(NotificationCenter.class),
                any(),                         // nullable (DefaultDecideOptions)
                any(ODPManager.class),
                anyString(),
                any(),
                any());
    }

    @Test
    public void testBuildWithEventDispatchIntervalDeprecated() throws Exception {
        long goodNumber = 1234L;
        OptimizelyManager manager = OptimizelyManager.builder("1")
                .withVuid("any-to-avoid-generate")
                .withLogger(logger)
                .withEventDispatchInterval(goodNumber)      // deprecated
                .build(mockContext);

        verifyNew(BatchEventProcessor.class).withArguments(any(BlockingQueue.class),
                any(EventHandler.class),
                anyInt(),
                eq(goodNumber),                             // milliseconds
                anyLong(),
                any(ExecutorService.class),
                any(NotificationCenter.class),
                any());                                 // PowerMock bug? requires an extra null at the end

        verify(mockEventHandler).setDispatchInterval(-1L);  // deprecated api not change default retryInterval

        verifyNew(OptimizelyManager.class).withArguments(
                anyString(),
                any(),                          // nullable (String)
                any(DatafileConfig.class),
                any(Logger.class),
                anyLong(),
                any(DatafileHandler.class),
                any(),                          // nullable (ErrorHandler)
                eq(-1L),                             // deprecated api not change default retryInterval
                any(EventHandler.class),
                any(EventProcessor.class),
                any(UserProfileService.class),
                any(NotificationCenter.class),
                any(),                         // nullable (DefaultDecideOptions)
                any(ODPManager.class),
                anyString(),
                any(),
                any());
    }

}
