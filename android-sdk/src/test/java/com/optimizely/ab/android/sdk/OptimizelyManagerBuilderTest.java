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
import com.optimizely.ab.android.datafile_handler.DefaultDatafileHandler;
import com.optimizely.ab.android.event_handler.DefaultEventHandler;
import com.optimizely.ab.android.shared.WorkerScheduler;
import com.optimizely.ab.android.user_profile.DefaultUserProfileService;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.event.BatchEventProcessor;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.odp.ODPManager;
import com.optimizely.ab.odp.ODPSegmentManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.sql.Time;
import java.util.concurrent.TimeUnit;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("jdk.internal.reflect.*")
@PrepareForTest({OptimizelyManager.class, BatchEventProcessor.class, DefaultEventHandler.class})
public class OptimizelyManagerBuilderTest {

    private String testProjectId = "7595190003";
    private String testSdkKey = "1234";
    private Logger logger;

    private String minDatafile = "{\n" +
            "experiments: [ ],\n" +
            "version: \"2\",\n" +
            "audiences: [ ],\n" +
            "groups: [ ],\n" +
            "attributes: [ ],\n" +
            "projectId: \"" + testProjectId + "\",\n" +
            "accountId: \"6365361536\",\n" +
            "events: [ ],\n" +
            "revision: \"1\"\n" +
            "}";

    private Context mockContext;
    private DefaultDatafileHandler mockDatafileHandler;

    @Before
    public void setup() throws Exception {
        mockContext = mock(Context.class);
        mockDatafileHandler = mock(DefaultDatafileHandler.class);
    }

    /**
     * Verify that building the {@link OptimizelyManager} with a polling interval greater than 60
     * seconds is properly registered.
     */
    @Test
    public void testBuildWithValidPollingInterval() {
        Long interval = 16L;
        TimeUnit timeUnit = TimeUnit.MINUTES;

        OptimizelyManager manager = OptimizelyManager.builder()
                .withSDKKey(testSdkKey)
                .withDatafileDownloadInterval(interval, timeUnit)
                .build(mockContext);

        assertEquals(interval * 60L, manager.getDatafileDownloadInterval().longValue());
    }

    @Test
    public void testBuildWithEventHandler() {
        EventHandler eventHandler = mock(EventHandler.class);
        OptimizelyManager manager = OptimizelyManager.builder()
                .withSDKKey(testSdkKey)
                .withDatafileDownloadInterval(901L, TimeUnit.SECONDS)
                .withEventHandler(eventHandler)
                .build(mockContext);

        assertEquals(901L, manager.getDatafileDownloadInterval().longValue());
        assertEquals(manager.getEventHandler(mockContext), eventHandler);
    }

    @Test
    public void testBuildWithErrorHandler() {
        ErrorHandler errorHandler = mock(ErrorHandler.class);
        OptimizelyManager manager = OptimizelyManager.builder()
                .withSDKKey(testSdkKey)
                .withDatafileDownloadInterval(61L, TimeUnit.SECONDS)
                .withErrorHandler(errorHandler)
                .build(mockContext);

        manager.initialize(mockContext, minDatafile);

        assertEquals(manager.getErrorHandler(mockContext), errorHandler);
    }

    @Test
    public void testBuildWithDatafileHandler() {
        DefaultDatafileHandler dfHandler = mock(DefaultDatafileHandler.class);
        OptimizelyManager manager = OptimizelyManager.builder()
                .withSDKKey(testSdkKey)
                .withDatafileDownloadInterval(61L, TimeUnit.SECONDS)
                .withDatafileHandler(dfHandler)
                .build(mockContext);

        manager.initialize(mockContext, minDatafile);

        assertEquals(manager.getDatafileHandler(), dfHandler);
    }

    @Test
    public void testBuildWithUserProfileService() {
        Context appContext = mock(Context.class);
        DefaultUserProfileService ups = mock(DefaultUserProfileService.class);
        OptimizelyManager manager = OptimizelyManager.builder()
                .withSDKKey(testSdkKey)
                .withDatafileDownloadInterval(61L, TimeUnit.SECONDS)
                .withUserProfileService(ups)
                .build(appContext);

        manager.initialize(appContext, minDatafile);

        assertEquals(manager.getUserProfileService(), ups);
    }

    // BackgroundDatafile worker tests

    @Test
    public void testBuildWithDatafileDownloadInterval_workerScheduled() throws Exception {
        long goodNumber = 1;
        OptimizelyManager manager = OptimizelyManager.builder()
                .withSDKKey(testSdkKey)
                .withDatafileHandler(mockDatafileHandler)
                .withDatafileDownloadInterval(goodNumber, TimeUnit.MINUTES)
                .build(mockContext);
        OptimizelyManager spyManager = spy(manager);
        when(spyManager.isAndroidVersionSupported()).thenReturn(true);
        spyManager.initialize(mockContext, "");

        verify(mockDatafileHandler).stopBackgroundUpdates(any(), any());
        verify(mockDatafileHandler).startBackgroundUpdates(any(), any(), eq(goodNumber * 60L), any());
    }

    @Test
    public void testBuildWithDatafileDownloadInterval_workerCancelledWhenIntervalIsNotPositive() throws Exception {
        OptimizelyManager manager = OptimizelyManager.builder()
                .withSDKKey(testSdkKey)
                .withDatafileHandler(mockDatafileHandler)
                .withDatafileDownloadInterval(-1, TimeUnit.MINUTES)
                .build(mockContext);
        OptimizelyManager spyManager = spy(manager);
        when(spyManager.isAndroidVersionSupported()).thenReturn(true);
        spyManager.initialize(mockContext, "");

        verify(mockDatafileHandler).stopBackgroundUpdates(any(), any());
        verify(mockDatafileHandler, never()).startBackgroundUpdates(any(), any(), any(), any());
    }

    @Test
    public void testBuildWithDatafileDownloadInterval_workerCancelledWhenNoIntervalProvided() throws Exception {
        OptimizelyManager manager = OptimizelyManager.builder()
                .withSDKKey(testSdkKey)
                .withDatafileHandler(mockDatafileHandler)
                .build(mockContext);
        OptimizelyManager spyManager = spy(manager);
        when(spyManager.isAndroidVersionSupported()).thenReturn(true);
        spyManager.initialize(mockContext, "");

        verify(mockDatafileHandler).stopBackgroundUpdates(any(), any());
        verify(mockDatafileHandler, never()).startBackgroundUpdates(any(), any(), any(), any());
    }

    @Test
    public void testBuildWithDefaultODP() throws Exception {
        OptimizelyManager manager = OptimizelyManager.builder()
                .withSDKKey(testSdkKey)
                .build(mockContext);
        OptimizelyManager spyManager = spy(manager);
        spyManager.initialize(mockContext, "");

        ODPManager odpManager = spyManager.getOptimizely().getODPManager();
        ODPManager spyODPManager = spy(odpManager);
        String vuid = spyManager.getOptimizely().getVuid();

        // validate
        // - enabled
        // - default odpAPIManager
        // - default size
        // - default timeout
        // - default queue size
        // - common data
        // - common identifiers


        assertEquals(vuid, VuidManager.Companion.getShared(mockContext).getVuid());
    }

    @Test
    public void testBuildWithODPSegmentCacheSize() throws Exception {
        OptimizelyManager manager = OptimizelyManager.builder()
                .withSDKKey(testSdkKey)
                .withODPSegmentCacheSize(123)
                .build(mockContext);
        OptimizelyManager spyManager = spy(manager);
        spyManager.initialize(mockContext, "");

        ODPSegmentManager segmentManager = spyManager.getOptimizely().getOPDManager().getSegmentManager();
        // validate custom cache size

    }

    @Test
    public void testBuildWithODPSegmentTimeout() throws Exception {
        OptimizelyManager manager = OptimizelyManager.builder()
                .withSDKKey(testSdkKey)
                .withODPSegmentTimeout(1234, TimeUnit.SECONDS)
                .build(mockContext);
        OptimizelyManager spyManager = spy(manager);
        spyManager.initialize(mockContext, "");

        ODPSegmentManager segmentManager = spyManager.getOptimizely().getODPManager().getSegmentManager();
        // validate custom cache timeout

    }

    @Test
    public void testBuildWithODPDisabled() throws Exception {
        OptimizelyManager manager = OptimizelyManager.builder()
                .withSDKKey(testSdkKey)
                .withODPDisabled()
                .build(mockContext);
        OptimizelyManager spyManager = spy(manager);
        spyManager.initialize(mockContext, "");

        ODPManager odpManager = spyManager.getOptimizely().getODPManager();
        assertNull(odpManager);
    }

}
