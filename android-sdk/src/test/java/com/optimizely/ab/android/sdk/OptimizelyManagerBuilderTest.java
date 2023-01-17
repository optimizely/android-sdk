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

import android.content.Context;
import android.graphics.Path;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.android.datafile_handler.DatafileHandler;
import com.optimizely.ab.android.datafile_handler.DefaultDatafileHandler;
import com.optimizely.ab.android.event_handler.DefaultEventHandler;
import com.optimizely.ab.android.odp.DefaultODPApiManager;
import com.optimizely.ab.android.odp.ODPEventClient;
import com.optimizely.ab.android.odp.ODPSegmentClient;
import com.optimizely.ab.android.odp.VuidManager;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.android.shared.WorkerScheduler;
import com.optimizely.ab.android.user_profile.DefaultUserProfileService;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.event.BatchEventProcessor;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.EventProcessor;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.odp.ODPApiManager;
import com.optimizely.ab.odp.ODPEventManager;
import com.optimizely.ab.odp.ODPManager;
import com.optimizely.ab.odp.ODPSegmentManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.sql.Time;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("jdk.internal.reflect.*")
@PrepareForTest({OptimizelyManager.class, BatchEventProcessor.class, DefaultEventHandler.class, ODPManager.class, ODPSegmentManager.class, ODPEventManager.class})
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
                .withVuid("any-to-avoid-generate")
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
                .withVuid("any-to-avoid-generate")
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
                .withVuid("any-to-avoid-generate")
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
                .withVuid("any-to-avoid-generate")
                .build(mockContext);

        manager.initialize(mockContext, minDatafile);

        assertEquals(manager.getDatafileHandler(), dfHandler);
    }

    @Test
    public void testBuildWithUserProfileService() {
        DefaultUserProfileService ups = mock(DefaultUserProfileService.class);
        OptimizelyManager manager = OptimizelyManager.builder()
                .withSDKKey(testSdkKey)
                .withDatafileDownloadInterval(61L, TimeUnit.SECONDS)
                .withUserProfileService(ups)
                .withVuid("any-to-avoid-generate")
                .build(mockContext);

        manager.initialize(mockContext, minDatafile);

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
                .withVuid("any-to-avoid-generate")
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
                .withVuid("any-to-avoid-generate")
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
                .withVuid("any-to-avoid-generate")
                .build(mockContext);
        OptimizelyManager spyManager = spy(manager);
        when(spyManager.isAndroidVersionSupported()).thenReturn(true);
        spyManager.initialize(mockContext, "");

        verify(mockDatafileHandler).stopBackgroundUpdates(any(), any());
        verify(mockDatafileHandler, never()).startBackgroundUpdates(any(), any(), any(), any());
    }

    @Test
    public void testBuildWithDefaultODP_defaultEnabled() throws Exception {
        whenNew(OptimizelyManager.class).withAnyArguments().thenReturn(mock(OptimizelyManager.class));

        OptimizelyManager manager = OptimizelyManager.builder()
                .withSDKKey(testSdkKey)
                .withVuid("test-vuid")
                .build(mockContext);

        verifyNew(OptimizelyManager.class).withArguments(
            any(),
            anyString(),                          // nullable (String)
            any(DatafileConfig.class),
            any(Logger.class),
            anyLong(),
            any(DatafileHandler.class),
            any(),                          // nullable (ErrorHandler)
            anyLong(),
            any(EventHandler.class),
            any(EventProcessor.class),
            any(UserProfileService.class),
            any(NotificationCenter.class),
            any(),                         // nullable (DefaultDecideOptions)
            any(ODPManager.class),
            eq("test-vuid"));
    }

    @Test
    public void testBuildWithDefaultODP_disabled() throws Exception {
        whenNew(OptimizelyManager.class).withAnyArguments().thenReturn(mock(OptimizelyManager.class));

        OptimizelyManager manager = OptimizelyManager.builder()
            .withSDKKey(testSdkKey)
            .withODPDisabled()
            .withVuid("test-vuid")
            .build(mockContext);

        verifyNew(OptimizelyManager.class).withArguments(
            any(),
            anyString(),                          // nullable (String)
            any(DatafileConfig.class),
            any(Logger.class),
            anyLong(),
            any(DatafileHandler.class),
            any(),                          // nullable (ErrorHandler)
            anyLong(),
            any(EventHandler.class),
            any(EventProcessor.class),
            any(UserProfileService.class),
            any(NotificationCenter.class),
            any(),                         // nullable (DefaultDecideOptions)
            isNull(),
            eq("test-vuid"));
    }

    @Test
    public void testBuildWithODP_defaultCacheSizeAndTimeout() throws Exception {
        whenNew(ODPSegmentManager.class).withAnyArguments().thenReturn(mock(ODPSegmentManager.class));
        whenNew(ODPEventManager.class).withAnyArguments().thenReturn(mock(ODPEventManager.class));
        whenNew(ODPManager.class).withAnyArguments().thenReturn(mock(ODPManager.class));

        OptimizelyManager manager = OptimizelyManager.builder()
                .withSDKKey(testSdkKey)
                .withVuid("any-to-avoid-generate")
                .build(mockContext);

        verifyNew(ODPManager.class).withArguments(
            any(ODPSegmentManager.class),
            any(ODPEventManager.class),
            isNull()
        );

        verifyNew(ODPEventManager.class).withArguments(
            any(DefaultODPApiManager.class)
        );

        verifyNew(ODPSegmentManager.class).withArguments(
            any(DefaultODPApiManager.class),
            eq(100),
            eq(600)
        );
    }

    @Test
    public void testBuildWithODP_customSegmentCacheSize() throws Exception {
        whenNew(ODPSegmentManager.class).withAnyArguments().thenReturn(mock(ODPSegmentManager.class));

        OptimizelyManager manager = OptimizelyManager.builder()
            .withSDKKey(testSdkKey)
            .withODPSegmentCacheSize(1234)
            .withVuid("any-to-avoid-generate")
            .build(mockContext);

        verifyNew(ODPSegmentManager.class).withArguments(
            any(DefaultODPApiManager.class),
            eq(1234),
            eq(600)
        );
    }

    @Test
    public void testBuildWithODP_customSegmentCacheTimeout() throws Exception {
        whenNew(ODPSegmentManager.class).withAnyArguments().thenReturn(mock(ODPSegmentManager.class));

        OptimizelyManager manager = OptimizelyManager.builder()
            .withSDKKey(testSdkKey)
            .withODPSegmentCacheTimeout(20, TimeUnit.MINUTES)
            .withVuid("any-to-avoid-generate")
            .build(mockContext);

        verifyNew(ODPSegmentManager.class).withArguments(
            any(DefaultODPApiManager.class),
            eq(100),
            eq(20*60)
        );
    }

    @Test
    public void testBuildWithODP_defaultSegmentFetchTimeout() throws Exception {
        OptimizelyManager manager = OptimizelyManager.builder()
            .withSDKKey(testSdkKey)
            .withVuid("any-to-avoid-generate")
            .build(mockContext);

        assertEquals(ODPSegmentClient.Companion.getCONNECTION_TIMEOUT(), 10*1000);
        assertEquals(ODPEventClient.Companion.getCONNECTION_TIMEOUT(), 10*1000);
    }

    @Test
    public void testBuildWithODP_customSegmentFetchTimeout() throws Exception {
        OptimizelyManager manager = OptimizelyManager.builder()
            .withSDKKey(testSdkKey)
            .withTimeoutForODPSegmentFetch(20)
            .withTimeoutForODPEventDispatch(30)
            .withVuid("any-to-avoid-generate")
            .build(mockContext);

        assertEquals(ODPSegmentClient.Companion.getCONNECTION_TIMEOUT(), 20*1000);
        assertEquals(ODPEventClient.Companion.getCONNECTION_TIMEOUT(), 30*1000);
    }

    @Test
    public void testBuildWithODP_defaultCommonDataAndIdentifiers() throws Exception {
        ODPEventManager mockEventManager = mock(ODPEventManager.class);
        whenNew(ODPEventManager.class).withAnyArguments().thenReturn(mockEventManager);
        whenNew(ODPSegmentManager.class).withAnyArguments().thenReturn(mock(ODPSegmentManager.class));
        whenNew(ODPManager.class).withAnyArguments().thenReturn(mock(ODPManager.class));

        OptimizelyManager manager = OptimizelyManager.builder()
            .withSDKKey(testSdkKey)
            .withVuid("test-vuid")
            .build(mockContext);

        ArgumentCaptor<Map<String, Object>> captorData = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, String>> captorIdentifiers = ArgumentCaptor.forClass(Map.class);

        verify(mockEventManager).setUserCommonData(captorData.capture());
        verify(mockEventManager).setUserCommonIdentifiers(captorIdentifiers.capture());

        Map<String, Object> data = captorData.getValue();
        Map<String, String> identifiers = captorIdentifiers.getValue();

        // here we just validate if data is passed or not (all values are validated in other tests: OptimizelyDefaultAttributesTest)
        assertEquals(data.get("os"), "Android");
        assertEquals(data.size(), 4);

        assertEquals(identifiers.get("vuid"), "test-vuid");
        assertEquals(identifiers.size(), 1);
    }

}
