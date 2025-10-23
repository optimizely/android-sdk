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

import com.optimizely.ab.android.datafile_handler.DatafileHandler;
import com.optimizely.ab.android.datafile_handler.DefaultDatafileHandler;
import com.optimizely.ab.android.event_handler.DefaultEventHandler;
import com.optimizely.ab.android.odp.DefaultODPApiManager;
import com.optimizely.ab.android.odp.ODPEventClient;
import com.optimizely.ab.android.odp.ODPSegmentClient;
import com.optimizely.ab.android.odp.VuidManager;
import com.optimizely.ab.android.sdk.cmab.CMABClient;
import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.android.user_profile.DefaultUserProfileService;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.event.BatchEventProcessor;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.EventProcessor;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.odp.ODPEventManager;
import com.optimizely.ab.odp.ODPManager;
import com.optimizely.ab.odp.ODPSegmentManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OptimizelyManager.class, BatchEventProcessor.class, DefaultEventHandler.class, ODPManager.class, ODPSegmentManager.class, ODPEventManager.class, VuidManager.class})
public class OptimizelyManagerBuilderTest {

    private String testProjectId = "7595190003";
    private String testSdkKey = "1234";
    private Logger logger;

    private VuidManager mockVuidManager;

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

        mockStatic(VuidManager.class);
        VuidManager.Companion mockCompanion = PowerMockito.mock(VuidManager.Companion.class);
        mockVuidManager = PowerMockito.mock(VuidManager.class);
        PowerMockito.doReturn(mockVuidManager).when(mockCompanion).getInstance();
        Whitebox.setInternalState(
            VuidManager.class, "Companion",
            mockCompanion
        );
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
    public void testBuildWithCustomSdkNameAndVersion() throws Exception {
        OptimizelyManager manager = OptimizelyManager.builder()
            .withSDKKey(testSdkKey)
            .withClientInfo("test-sdk", "test-version")
            .withVuid("any-to-avoid-generate")
            .build(mockContext);
        assertEquals(manager.getSdkName(mockContext), "test-sdk");
        assertEquals(manager.getSdkVersion(), "test-version");
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
            eq("test-vuid"),
            any(),
            any());
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
            eq("test-vuid"),
            any(),
            any());
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

    ODPManager.Builder getMockODPManagerBuilder() {
        ODPManager.Builder mockBuilder = PowerMockito.mock(ODPManager.Builder.class);
        when(mockBuilder.withApiManager(any())).thenReturn(mockBuilder);
        when(mockBuilder.withSegmentCacheSize(any())).thenReturn(mockBuilder);
        when(mockBuilder.withSegmentCacheTimeout(any())).thenReturn(mockBuilder);
        when(mockBuilder.withSegmentManager(any())).thenReturn(mockBuilder);
        when(mockBuilder.withEventManager(any())).thenReturn(mockBuilder);
        when(mockBuilder.withUserCommonData(any())).thenReturn(mockBuilder);
        when(mockBuilder.withUserCommonIdentifiers(any())).thenReturn(mockBuilder);
        return mockBuilder;
    }

    @Test
    public void testBuildWithVuidDisabled() throws Exception {
        mockStatic(ODPManager.class);
        ODPManager.Builder mockBuilder = getMockODPManagerBuilder();
        when(mockBuilder.build()).thenReturn(mock(ODPManager.class));
        when(ODPManager.builder()).thenReturn(mockBuilder);

        OptimizelyManager manager = OptimizelyManager.builder()
            .withSDKKey(testSdkKey)
            .build(mockContext);

        verify(mockVuidManager, times(1)).configure(eq(false), any(Context.class));

        ArgumentCaptor<Map<String, String>> identifiersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockBuilder).withUserCommonIdentifiers(identifiersCaptor.capture());
        Map<String, String> identifiers = identifiersCaptor.getValue();
        assertFalse(identifiers.containsKey("vuid"));

        when(ODPManager.builder()).thenCallRealMethod();
    }

    @Test
    public void testBuildWithVuidEnabled() throws Exception {
        mockStatic(ODPManager.class);
        ODPManager.Builder mockBuilder = getMockODPManagerBuilder();
        when(mockBuilder.build()).thenReturn(mock(ODPManager.class));
        when(ODPManager.builder()).thenReturn(mockBuilder);

        when(mockVuidManager.getVuid()).thenReturn("vuid_test");

        OptimizelyManager manager = OptimizelyManager.builder()
            .withSDKKey(testSdkKey)
            .withVuidEnabled()
            .build(mockContext);

        verify(mockVuidManager, times(1)).configure(eq(true), any(Context.class));

        ArgumentCaptor<Map<String, String>> identifiersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockBuilder).withUserCommonIdentifiers(identifiersCaptor.capture());
        Map<String, String> identifiers = identifiersCaptor.getValue();
        assertEquals(identifiers.get("vuid"), "vuid_test");

        when(ODPManager.builder()).thenCallRealMethod();
    }

    @Test
    public void testCmabServiceConfigurationValidation() throws Exception {
        // Custom configuration values
        int customCacheSize = 500;
        int customTimeoutMinutes = 45;
        int expectedTimeoutSeconds = customTimeoutMinutes * 60; // 45 min = 2700 sec
        CMABClient mockCmabClient = mock(CMABClient.class);

        // Create mocks for the CMAB service creation chain
        Object mockDefaultLRUCache = PowerMockito.mock(Class.forName("com.optimizely.ab.cache.DefaultLRUCache"));
        Object mockCmabServiceOptions = PowerMockito.mock(Class.forName("com.optimizely.ab.cmab.CmabServiceOptions"));
        Object mockDefaultCmabService = PowerMockito.mock(Class.forName("com.optimizely.ab.cmab.DefaultCmabService"));

        // Mock the construction chain with parameter validation
        whenNew(Class.forName("com.optimizely.ab.cache.DefaultLRUCache"))
            .thenReturn(mockDefaultLRUCache);

        whenNew(Class.forName("com.optimizely.ab.cmab.CmabServiceOptions"))
            .thenReturn(mockCmabServiceOptions);

        whenNew(Class.forName("com.optimizely.ab.cmab.DefaultCmabService"))
            .thenReturn(mockDefaultCmabService);

        // Use PowerMock to verify OptimizelyManager constructor is called with CMAB service
        whenNew(OptimizelyManager.class).withAnyArguments().thenReturn(mock(OptimizelyManager.class));

        OptimizelyManager manager = OptimizelyManager.builder(testProjectId)
                .withCmabCacheSize(customCacheSize)
                .withCmabCacheTimeout(customTimeoutMinutes, TimeUnit.MINUTES)
                .withCmabClient(mockCmabClient)
                .build(mockContext);

        verifyNew(Class.forName("com.optimizely.ab.cache.DefaultLRUCache"))
            .withArguments(eq(customCacheSize), eq(expectedTimeoutSeconds));

        verifyNew(Class.forName("com.optimizely.ab.cmab.CmabServiceOptions"))
            .withArguments(any(), eq(mockDefaultLRUCache), eq(mockCmabClient));

        verifyNew(Class.forName("com.optimizely.ab.cmab.DefaultCmabService"))
            .withArguments(eq(mockCmabServiceOptions));

        // Verify OptimizelyManager constructor was called with the mocked CMAB service
        verifyNew(OptimizelyManager.class).withArguments(
            any(),                      // projectId
            any(),                      // sdkKey
            any(),                      // datafileConfig
            any(),                      // logger
            anyLong(),                  // datafileDownloadInterval
            any(),                      // datafileHandler
            any(),                      // errorHandler
            anyLong(),                  // eventDispatchRetryInterval
            any(),                      // eventHandler
            any(),                      // eventProcessor
            any(),                      // userProfileService
            any(),                      // notificationCenter
            any(),                      // defaultDecideOptions
            any(),                      // odpManager
            eq(mockDefaultCmabService), // cmabService - Should be our mocked service
            any(),                      // vuid
            any(),                      // customSdkName
            any()                       // customSdkVersion
        );

        assertNotNull("Manager should be created successfully", manager);
    }

    @Test
    public void testCmabServiceDefaultConfigurationValidation() throws Exception {
        // Default configuration values
        int defaultCacheSize = 100;
        int defaultTimeoutSeconds = 30 * 60; // 30 minutes = 1800 seconds

        // Create mocks for the CMAB service creation chain
        Object mockDefaultLRUCache = PowerMockito.mock(Class.forName("com.optimizely.ab.cache.DefaultLRUCache"));
        Object mockDefaultCmabClient = PowerMockito.mock(Class.forName("com.optimizely.ab.cmab.DefaultCmabClient"));
        Object mockCmabServiceOptions = PowerMockito.mock(Class.forName("com.optimizely.ab.cmab.CmabServiceOptions"));
        Object mockDefaultCmabService = PowerMockito.mock(Class.forName("com.optimizely.ab.cmab.DefaultCmabService"));

        // Mock the construction chain with parameter validation
        whenNew(Class.forName("com.optimizely.ab.cache.DefaultLRUCache"))
            .thenReturn(mockDefaultLRUCache);

        whenNew(Class.forName("com.optimizely.ab.cmab.DefaultCmabClient"))
            .thenReturn(mockDefaultCmabClient);

        whenNew(Class.forName("com.optimizely.ab.cmab.CmabServiceOptions"))
            .thenReturn(mockCmabServiceOptions);

        whenNew(Class.forName("com.optimizely.ab.cmab.DefaultCmabService"))
            .thenReturn(mockDefaultCmabService);

        // Use PowerMock to verify OptimizelyManager constructor is called with CMAB service
        whenNew(OptimizelyManager.class).withAnyArguments().thenReturn(mock(OptimizelyManager.class));

        // Build OptimizelyManager with NO CMAB configuration to test defaults
        OptimizelyManager manager = OptimizelyManager.builder(testProjectId)
                .build(mockContext);

        verifyNew(Class.forName("com.optimizely.ab.cache.DefaultLRUCache"))
            .withArguments(eq(defaultCacheSize), eq(defaultTimeoutSeconds));

        // Verify DefaultCmabClient is created with default parameters
        verifyNew(Class.forName("com.optimizely.ab.cmab.DefaultCmabClient"))
            .withNoArguments();

        // Verify CmabServiceOptions is created with logger, cache, and default client
        verifyNew(Class.forName("com.optimizely.ab.cmab.CmabServiceOptions"))
            .withArguments(any(), eq(mockDefaultLRUCache), eq(mockDefaultCmabClient));

        verifyNew(Class.forName("com.optimizely.ab.cmab.DefaultCmabService"))
            .withArguments(eq(mockCmabServiceOptions));

        // Verify OptimizelyManager constructor was called with the mocked CMAB service
        verifyNew(OptimizelyManager.class).withArguments(
            any(),                      // projectId
            any(),                      // sdkKey
            any(),                      // datafileConfig
            any(),                      // logger
            anyLong(),                  // datafileDownloadInterval
            any(),                      // datafileHandler
            any(),                      // errorHandler
            anyLong(),                  // eventDispatchRetryInterval
            any(),                      // eventHandler
            any(),                      // eventProcessor
            any(),                      // userProfileService
            any(),                      // notificationCenter
            any(),                      // defaultDecideOptions
            any(),                      // odpManager
            eq(mockDefaultCmabService), // cmabService - Should be our mocked service
            any(),                      // vuid
            any(),                      // customSdkName
            any()                       // customSdkVersion
        );

        // This test validates:
        // 1. DefaultLRUCache created with default cache size (100) and timeout (1800 seconds)
        // 2. DefaultCmabClient created with no arguments (using defaults)
        // 3. CmabServiceOptions created with logger, mocked cache, and default client
        // 4. DefaultCmabService created with the mocked service options
        // 5. OptimizelyManager constructor receives the exact mocked CMAB service
        // 6. All default parameters flow correctly through the creation chain
        assertNotNull("Manager should be created successfully", manager);
    }

}
