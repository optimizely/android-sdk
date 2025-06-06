/****************************************************************************
 * Copyright 2017-2021, 2023 Optimizely, Inc. and contributors              *
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
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.optimizely.ab.android.datafile_handler.DatafileHandler;
import com.optimizely.ab.android.datafile_handler.DatafileLoadedListener;
import com.optimizely.ab.android.datafile_handler.DefaultDatafileHandler;
import com.optimizely.ab.android.event_handler.DefaultEventHandler;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.android.user_profile.DefaultUserProfileService;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.config.DatafileProjectConfig;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.EventProcessor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link OptimizelyManager}
 */
@RunWith(AndroidJUnit4.class)
public class OptimizelyManagerTest {

    private String testProjectId = "7595190003";
    private String testSdkKey = "EQRZ12XAR22424";
    private ExecutorService executor;
    private Logger logger;
    private OptimizelyManager optimizelyManager;
    private DefaultDatafileHandler defaultDatafileHandler;
    private String defaultDatafile;

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

    @Before
    public void setup() throws Exception {
        logger = mock(Logger.class);
        executor = Executors.newSingleThreadExecutor();
        defaultDatafileHandler = mock(DefaultDatafileHandler.class);
        EventHandler eventHandler = mock(DefaultEventHandler.class);
        EventProcessor eventProcessor = mock(EventProcessor.class);
        optimizelyManager = OptimizelyManager.builder(testProjectId)
                .withLogger(logger)
                .withDatafileDownloadInterval(3600L, TimeUnit.SECONDS)
                .withDatafileHandler(defaultDatafileHandler)
                .withEventDispatchInterval(3600L, TimeUnit.SECONDS)
                .withEventHandler(eventHandler)
                .withEventProcessor(eventProcessor)
                .build(InstrumentationRegistry.getInstrumentation().getTargetContext());
        defaultDatafile = optimizelyManager.getDatafile(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.raw.datafile);
        ProjectConfig config = new DatafileProjectConfig.Builder().withDatafile(defaultDatafile).build();

        when(defaultDatafileHandler.getConfig()).thenReturn(config);
    }

    @Test
    public void initializeIntUseForcedVariation() {
        optimizelyManager.initialize(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.raw.datafile);


        assertTrue(optimizelyManager.getOptimizely().setForcedVariation("android_experiment_key", "1", "var_1"));
        Variation variation = optimizelyManager.getOptimizely().getForcedVariation("android_experiment_key", "1");
        assertEquals(variation.getKey(), "var_1");
        assertTrue(optimizelyManager.getOptimizely().setForcedVariation("android_experiment_key", "1", null));
    }

    @Test
    public void initializeInt() {

        optimizelyManager.initialize(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.raw.datafile);

        assertEquals(optimizelyManager.isDatafileCached(InstrumentationRegistry.getInstrumentation().getTargetContext()), false);

        assertEquals(optimizelyManager.getDatafileUrl(), "https://cdn.optimizely.com/json/7595190003.json" );

        verify(optimizelyManager.getDatafileHandler()).startBackgroundUpdates(eq(InstrumentationRegistry.getInstrumentation().getTargetContext()), eq(new DatafileConfig(testProjectId, null)), eq(3600L), any(DatafileLoadedListener.class));
        assertNotNull(optimizelyManager.getOptimizely());
        assertNotNull(optimizelyManager.getDatafileHandler());

    }
    @Test
    public void initializeSyncWithoutEnvironment() {
        /*
         * Scenario#1: when datafile is not Empty
         * Scenario#2: when datafile is Empty
         */
        optimizelyManager.initialize(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.raw.datafile);

        assertEquals(optimizelyManager.isDatafileCached(InstrumentationRegistry.getInstrumentation().getTargetContext()), false);

        assertEquals(optimizelyManager.getDatafileUrl(), "https://cdn.optimizely.com/json/7595190003.json" );

        assertNotNull(optimizelyManager.getOptimizely());
        assertNotNull(optimizelyManager.getDatafileHandler());

        optimizelyManager.initialize(InstrumentationRegistry.getInstrumentation().getTargetContext(),(Integer) null);
        verify(logger).error(eq("Invalid datafile resource ID."));
    }
    @Test
    public void initializeSyncWithEnvironment() {
        Logger logger = mock(Logger.class);
        DatafileHandler datafileHandler = mock(DefaultDatafileHandler.class);
        EventHandler eventHandler = mock(DefaultEventHandler.class);
        EventProcessor eventProcessor = mock(EventProcessor.class);
        OptimizelyManager optimizelyManager = new OptimizelyManager(testProjectId, testSdkKey, null, logger, 3600L, datafileHandler, null, 3600L,
                eventHandler, eventProcessor, null, null, null, null, null, null, null);
        /*
         * Scenario#1: when datafile is not Empty
         * Scenario#2: when datafile is Empty
        */
        optimizelyManager.initialize(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.raw.datafile);

        assertEquals(optimizelyManager.isDatafileCached(InstrumentationRegistry.getInstrumentation().getTargetContext()), false);

        assertEquals(optimizelyManager.getDatafileUrl(), String.format((DatafileConfig.defaultHost + DatafileConfig.environmentUrlSuffix), testSdkKey));

        assertNotNull(optimizelyManager.getOptimizely());
        assertNotNull(optimizelyManager.getDatafileHandler());

        optimizelyManager.initialize(InstrumentationRegistry.getInstrumentation().getTargetContext(),(Integer) null);
        verify(logger).error(eq("Invalid datafile resource ID."));
    }
    @Test
    public void initializeSyncWithEmptyDatafile() {
        //for this case to pass empty the data file or enter any garbage data given on R.raw.emptydatafile this path
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);
        when(appContext.getPackageName()).thenReturn("com.optly");
        when(defaultDatafileHandler.getConfig()).thenReturn(null);
        optimizelyManager.initialize(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.raw.emptydatafile);
        assertFalse(optimizelyManager.getOptimizely().isValid());
    }
    @Test
    public void getEmptyDatafile() {
        //for this case to pass empty the data file or enter any garbage data given on R.raw.emptydatafile this path
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);
        when(appContext.getPackageName()).thenReturn("com.optly");

        String datafile= optimizelyManager.getDatafile(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.raw.emptydatafile);
        assertNotNull(datafile,"");
    }
    @Test
    public void getDatafile() {
        /*
         * Scenario#1: when datafile is Cached
         *  Scenario#2: when datafile is not cached and raw datafile is not empty
        */
        assertEquals(optimizelyManager.isDatafileCached(InstrumentationRegistry.getInstrumentation().getTargetContext()), false);
        String datafile =  optimizelyManager.getDatafile(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.raw.datafile);
        assertEquals(optimizelyManager.getDatafileUrl(), String.format("https://cdn.optimizely.com/json/%s.json", testProjectId) );
        assertNotNull(datafile);
        assertNotNull(optimizelyManager.getDatafileHandler());
    }

    @Test
    public void initializeAsyncWithEnvironment() {
        Logger logger = mock(Logger.class);
        DatafileHandler datafileHandler = mock(DefaultDatafileHandler.class);
        EventHandler eventHandler = mock(DefaultEventHandler.class);
        EventProcessor eventProcessor = mock(EventProcessor.class);
        final OptimizelyManager optimizelyManager = new OptimizelyManager(testProjectId, testSdkKey, null, logger, 3600L, datafileHandler, null, 3600L,
                eventHandler, eventProcessor, null, null, null, null, null, null, null);

        /*
         * Scenario#1: when datafile is not Empty
         * Scenario#2: when datafile is Empty
        */

        doAnswer(
                new Answer<Object>() {
                    public Object answer(InvocationOnMock invocation) {
                        ((DatafileLoadedListener) invocation.getArguments()[2]).onDatafileLoaded(null);
                        return null;
                    }
                }).when(optimizelyManager.getDatafileHandler()).downloadDatafile(any(Context.class), any(DatafileConfig.class),
                any(DatafileLoadedListener.class));

        OptimizelyStartListener listener = new OptimizelyStartListener() {
            @Override
            public void onStart(OptimizelyClient optimizely) {
                assertNotNull(optimizelyManager.getOptimizely());
                assertNotNull(optimizelyManager.getDatafileHandler());
                assertNull(optimizelyManager.getOptimizelyStartListener());
            }
        };
        optimizelyManager.initialize(InstrumentationRegistry.getInstrumentation().getContext(), R.raw.datafile, listener);

        verify(optimizelyManager.getDatafileHandler()).startBackgroundUpdates(any(Context.class), eq(new DatafileConfig(testProjectId, testSdkKey)), eq(3600L), any(DatafileLoadedListener.class));


        assertEquals(optimizelyManager.isDatafileCached(InstrumentationRegistry.getInstrumentation().getTargetContext()), false);

        assertEquals(optimizelyManager.getDatafileUrl(), String.format((DatafileConfig.defaultHost + DatafileConfig.environmentUrlSuffix), testSdkKey) );
    }

    @Test
    public void initializeAsyncWithoutEnvironment() {
        /*
         * Scenario#1: when datafile is not Empty
         * Scenario#2: when datafile is Empty
         */
        optimizelyManager.initialize(InstrumentationRegistry.getInstrumentation().getContext(), R.raw.datafile, new OptimizelyStartListener() {
            @Override
            public void onStart(OptimizelyClient optimizely) {
                assertNotNull(optimizelyManager.getOptimizely());
                assertNotNull(optimizelyManager.getDatafileHandler());
                assertNull(optimizelyManager.getOptimizelyStartListener());
            }
        });

        assertEquals(optimizelyManager.isDatafileCached(InstrumentationRegistry.getInstrumentation().getTargetContext()), false);

        assertEquals(optimizelyManager.getDatafileUrl(), "https://cdn.optimizely.com/json/7595190003.json" );
    }

    @Test
    public void initializeWithEmptyDatafile() {
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);
        when(appContext.getPackageName()).thenReturn("com.optly");
        when(defaultDatafileHandler.getConfig()).thenReturn(null);

        String emptyString = "";

        optimizelyManager.initialize(context, emptyString);
        assertFalse(optimizelyManager.getOptimizely().isValid());
    }

    @Test
    public void initializeWithMalformedDatafile() {
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);
        when(appContext.getPackageName()).thenReturn("com.optly");
        when(defaultDatafileHandler.getConfig()).thenReturn(null);

        String emptyString = "malformed data";

        optimizelyManager.initialize(context, emptyString);
        assertFalse(optimizelyManager.getOptimizely().isValid());
    }

    @Test
    public void initializeWithNullDatafile() {
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);
        when(appContext.getPackageName()).thenReturn("com.optly");

        String emptyString = null;

        optimizelyManager.initialize(context, emptyString);
        verify(logger).error(eq("Invalid datafile"));
    }

    @Test
    public void initializeAsyncWithNullDatafile() {
        optimizelyManager.initialize(InstrumentationRegistry.getInstrumentation().getContext(), null, new OptimizelyStartListener() {
            @Override
            public void onStart(OptimizelyClient optimizely) {
                assertNotNull(optimizely);
            }
        });
    }

    @Test
    public void load() {
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);
        when(appContext.getPackageName()).thenReturn("com.optly");

        String emptyString = null;

        optimizelyManager.initialize(context, emptyString);
        verify(logger).error(eq("Invalid datafile"));
    }

    @Test
    public void stop() {
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);

        optimizelyManager.getDatafileHandler().downloadDatafile(context, optimizelyManager.getDatafileConfig(), null);

        optimizelyManager.stop(context);

        assertNull(optimizelyManager.getOptimizelyStartListener());
    }

    @Test
    public void injectOptimizely() {
        Context context = mock(Context.class);
        UserProfileService userProfileService = mock(UserProfileService.class);
        OptimizelyStartListener startListener = mock(OptimizelyStartListener.class);

        optimizelyManager.setOptimizelyStartListener(startListener, true);
        optimizelyManager.injectOptimizely(context, userProfileService, minDatafile);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(logger).info("Sending Optimizely instance to listener");
        verify(startListener).onStart(any(OptimizelyClient.class));
        verify(optimizelyManager.getDatafileHandler()).startBackgroundUpdates(eq(context), eq(new DatafileConfig(testProjectId, null)), eq(3600L), any(DatafileLoadedListener.class));

    }

    @Test
    public void injectOptimizelyWithDatafileListener() {
        Context context = mock(Context.class);
        UserProfileService userProfileService = mock(UserProfileService.class);
        OptimizelyStartListener startListener = mock(OptimizelyStartListener.class);

        optimizelyManager.setOptimizelyStartListener(startListener);
        optimizelyManager.injectOptimizely(context, userProfileService, minDatafile);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(optimizelyManager.getDatafileHandler()).startBackgroundUpdates(eq(context), eq(new DatafileConfig(testProjectId, null)), eq(3600L), any(DatafileLoadedListener.class));
        verify(logger).info("Sending Optimizely instance to listener");
        verify(startListener).onStart(any(OptimizelyClient.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void injectOptimizelyNullListener() {
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);

        when(context.getPackageName()).thenReturn("com.optly");
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getApplicationContext().getPackageManager()).thenReturn(packageManager);
        try {
            when(packageManager.getPackageInfo("com.optly", 0)).thenReturn(mock(PackageInfo.class));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        UserProfileService userProfileService = mock(UserProfileService.class);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<DefaultUserProfileService.StartCallback> callbackArgumentCaptor =
                ArgumentCaptor.forClass(DefaultUserProfileService.StartCallback.class);
        optimizelyManager.setOptimizelyStartListener(null);

        optimizelyManager.injectOptimizely(context, userProfileService, minDatafile);

        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(logger).info("No listener to send Optimizely to");
    }

    @Test
    public void injectOptimizelyHandlesInvalidDatafile() {
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);

        when(context.getPackageName()).thenReturn("com.optly");
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getApplicationContext().getPackageManager()).thenReturn(packageManager);
        try {
            when(packageManager.getPackageInfo("com.optly", 0)).thenReturn(mock(PackageInfo.class));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        DefaultUserProfileService userProfileService = mock(DefaultUserProfileService.class);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<DefaultUserProfileService.StartCallback> callbackArgumentCaptor =
                ArgumentCaptor.forClass(DefaultUserProfileService.StartCallback.class);

        when(defaultDatafileHandler.getConfig()).thenReturn(null);
        optimizelyManager.setOptimizelyStartListener(null);
        optimizelyManager.injectOptimizely(context, userProfileService, "{}");
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }
        assertFalse(optimizelyManager.getOptimizely().isValid());
    }

    @Test
    public void injectOptimizelyDoesNotDuplicateCallback() {
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);

        when(context.getPackageName()).thenReturn("com.optly");
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getApplicationContext().getPackageManager()).thenReturn(packageManager);
        try {
            when(packageManager.getPackageInfo("com.optly", 0)).thenReturn(mock(PackageInfo.class));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        UserProfileService userProfileService = mock(UserProfileService.class);

        OptimizelyStartListener startListener = mock(OptimizelyStartListener.class);
        optimizelyManager.setOptimizelyStartListener(startListener);
        optimizelyManager.injectOptimizely(context, userProfileService, minDatafile);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(logger).info("Sending Optimizely instance to listener");
        verify(startListener).onStart(any(OptimizelyClient.class));
    }

    // Init Sync Flows

    @Test
    public void initializeSyncWithUpdateOnNewDatafileDisabled() {
        boolean downloadToCache = true;
        boolean updateConfigOnNewDatafile = false;
        int pollingInterval = 0;   // disable polling

        DefaultDatafileHandler datafileHandler = spy(new DefaultDatafileHandler());
        Logger logger = mock(Logger.class);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        OptimizelyManager manager = new OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval, datafileHandler, null, 0,
                null, null, null, null, null, null, null, null, null);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<DatafileConfig> configCaptor = ArgumentCaptor.forClass(DatafileConfig.class);
        ArgumentCaptor<DatafileLoadedListener> listenerCaptor = ArgumentCaptor.forClass(DatafileLoadedListener.class);

        doAnswer(invocation -> {
            Context capturedContext = contextCaptor.getValue();
            DatafileConfig capturedConfig = configCaptor.getValue();
            DatafileLoadedListener capturedListener = listenerCaptor.getValue();

            String newDatafile = manager.getDatafile(capturedContext, R.raw.datafile_api);
            datafileHandler.saveDatafile(capturedContext, capturedConfig, newDatafile);

            return datafileHandler;
        }).when(manager.getDatafileHandler()).downloadDatafile(contextCaptor.capture(), configCaptor.capture(), listenerCaptor.capture());

        OptimizelyClient client = manager.initialize(context, defaultDatafile, downloadToCache, updateConfigOnNewDatafile);

        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //
        }

        assertEquals(client.getOptimizelyConfig().getRevision(), "7");
    }

    @Test
    public void initializeSyncWithUpdateOnNewDatafileEnabled() {
        boolean downloadToCache = true;
        boolean updateConfigOnNewDatafile = true;
        int pollingInterval = 0;   // disable polling

        DefaultDatafileHandler datafileHandler = spy(new DefaultDatafileHandler());
        Logger logger = mock(Logger.class);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        OptimizelyManager manager = new OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval, datafileHandler, null, 0,
                null, null, null, null, null, null, null, null, null);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<DatafileConfig> configCaptor = ArgumentCaptor.forClass(DatafileConfig.class);
        ArgumentCaptor<DatafileLoadedListener> listenerCaptor = ArgumentCaptor.forClass(DatafileLoadedListener.class);

        doAnswer(invocation -> {
            Context capturedContext = contextCaptor.getValue();
            DatafileConfig capturedConfig = configCaptor.getValue();
            DatafileLoadedListener capturedListener = listenerCaptor.getValue();

            String newDatafile = manager.getDatafile(capturedContext, R.raw.datafile_api);
            datafileHandler.saveDatafile(capturedContext, capturedConfig, newDatafile);

            return datafileHandler;
        }).when(manager.getDatafileHandler()).downloadDatafile(contextCaptor.capture(), configCaptor.capture(), listenerCaptor.capture());

        OptimizelyClient client = manager.initialize(context, defaultDatafile, downloadToCache, updateConfigOnNewDatafile);

        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //
        }

        assertEquals(client.getOptimizelyConfig().getRevision(), "241");
    }

    @Test
    public void initializeSyncWithDownloadToCacheDisabled() {
        boolean downloadToCache = false;
        boolean updateConfigOnNewDatafile = true;
        int pollingInterval = 0;   // disable polling

        DefaultDatafileHandler datafileHandler = spy(new DefaultDatafileHandler());
        Logger logger = mock(Logger.class);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        OptimizelyManager manager = new OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval, datafileHandler, null, 0,
                null, null, null, null, null, null, null, null, null);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<DatafileConfig> configCaptor = ArgumentCaptor.forClass(DatafileConfig.class);
        ArgumentCaptor<DatafileLoadedListener> listenerCaptor = ArgumentCaptor.forClass(DatafileLoadedListener.class);

        doAnswer(invocation -> {
            Context capturedContext = contextCaptor.getValue();
            DatafileConfig capturedConfig = configCaptor.getValue();
            DatafileLoadedListener capturedListener = listenerCaptor.getValue();

            String newDatafile = manager.getDatafile(capturedContext, R.raw.datafile_api);
            datafileHandler.saveDatafile(capturedContext, capturedConfig, newDatafile);

            return datafileHandler;
        }).when(manager.getDatafileHandler()).downloadDatafile(contextCaptor.capture(), configCaptor.capture(), listenerCaptor.capture());

        OptimizelyClient client = manager.initialize(context, defaultDatafile, downloadToCache, updateConfigOnNewDatafile);

        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //
        }

        assertEquals(client.getOptimizelyConfig().getRevision(), "7");
    }

    @Test
    public void initializeSyncWithUpdateOnNewDatafileDisabledWithPeriodicPollingEnabled() {
        boolean downloadToCache = true;
        boolean updateConfigOnNewDatafile = false;
        int pollingInterval = 30;   // enable polling

        DefaultDatafileHandler datafileHandler = spy(new DefaultDatafileHandler());
        Logger logger = mock(Logger.class);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        OptimizelyManager manager = new OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval, datafileHandler, null, 0,
                null, null, null, null, null, null, null, null, null);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<DatafileConfig> configCaptor = ArgumentCaptor.forClass(DatafileConfig.class);
        ArgumentCaptor<DatafileLoadedListener> listenerCaptor = ArgumentCaptor.forClass(DatafileLoadedListener.class);

        doAnswer(invocation -> {
            Context capturedContext = contextCaptor.getValue();
            DatafileConfig capturedConfig = configCaptor.getValue();
            DatafileLoadedListener capturedListener = listenerCaptor.getValue();

            String newDatafile = manager.getDatafile(capturedContext, R.raw.datafile_api);
            datafileHandler.saveDatafile(capturedContext, capturedConfig, newDatafile);

            return datafileHandler;
        }).when(manager.getDatafileHandler()).downloadDatafile(contextCaptor.capture(), configCaptor.capture(), listenerCaptor.capture());

        OptimizelyClient client = manager.initialize(context, defaultDatafile, downloadToCache, updateConfigOnNewDatafile);

        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //
        }

        // when periodic polling enabled, project config always updated on cache datafile update (regardless of "updateConfigOnNewDatafile" setting)
        assertEquals(client.getOptimizelyConfig().getRevision(), "241"); // wait for first download.
    }

    @Test
    public void initializeSyncWithUpdateOnNewDatafileEnabledWithPeriodicPollingEnabled() {
        boolean downloadToCache = true;
        boolean updateConfigOnNewDatafile = true;
        int pollingInterval = 30;   // enable polling

        DefaultDatafileHandler datafileHandler = spy(new DefaultDatafileHandler());
        Logger logger = mock(Logger.class);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        OptimizelyManager manager = new OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval, datafileHandler, null, 0,
                null, null, null, null, null, null, null, null, null);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<DatafileConfig> configCaptor = ArgumentCaptor.forClass(DatafileConfig.class);
        ArgumentCaptor<DatafileLoadedListener> listenerCaptor = ArgumentCaptor.forClass(DatafileLoadedListener.class);

        doAnswer(invocation -> {
            Context capturedContext = contextCaptor.getValue();
            DatafileConfig capturedConfig = configCaptor.getValue();
            DatafileLoadedListener capturedListener = listenerCaptor.getValue();

            String newDatafile = manager.getDatafile(capturedContext, R.raw.datafile_api);
            datafileHandler.saveDatafile(capturedContext, capturedConfig, newDatafile);

            return datafileHandler;
        }).when(manager.getDatafileHandler()).downloadDatafile(contextCaptor.capture(), configCaptor.capture(), listenerCaptor.capture());

        OptimizelyClient client = manager.initialize(context, defaultDatafile, downloadToCache, updateConfigOnNewDatafile);

        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //
        }

        assertEquals(client.getOptimizelyConfig().getRevision(), "241");
    }

    @Test
    public void initializeSyncWithUpdateOnNewDatafileDisabledWithPeriodicPollingDisabled() {
        boolean downloadToCache = true;
        boolean updateConfigOnNewDatafile = false;
        int pollingInterval = 0;   // disable polling

        DefaultDatafileHandler datafileHandler = spy(new DefaultDatafileHandler());
        Logger logger = mock(Logger.class);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        OptimizelyManager manager = new OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval, datafileHandler, null, 0,
                null, null, null, null, null, null, null, null, null);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<DatafileConfig> configCaptor = ArgumentCaptor.forClass(DatafileConfig.class);
        ArgumentCaptor<DatafileLoadedListener> listenerCaptor = ArgumentCaptor.forClass(DatafileLoadedListener.class);

        doAnswer(invocation -> {
            Context capturedContext = contextCaptor.getValue();
            DatafileConfig capturedConfig = configCaptor.getValue();
            DatafileLoadedListener capturedListener = listenerCaptor.getValue();

            String newDatafile = manager.getDatafile(capturedContext, R.raw.datafile_api);
            datafileHandler.saveDatafile(capturedContext, capturedConfig, newDatafile);

            return datafileHandler;
        }).when(manager.getDatafileHandler()).downloadDatafile(contextCaptor.capture(), configCaptor.capture(), listenerCaptor.capture());
        
        OptimizelyClient client = manager.initialize(context, defaultDatafile, downloadToCache, updateConfigOnNewDatafile);

        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //
        }

        // when periodic polling enabled, project config always updated on cache datafile update (regardless of "updateConfigOnNewDatafile" setting)
        assertEquals(client.getOptimizelyConfig().getRevision(), "7"); // wait for first download.
    }

    @Test
    public void initializeSyncWithUpdateOnNewDatafileEnabledWithPeriodicPollingDisabled() {
        boolean downloadToCache = true;
        boolean updateConfigOnNewDatafile = true;
        int pollingInterval = 0;   // disable polling

        DefaultDatafileHandler datafileHandler = spy(new DefaultDatafileHandler());
        Logger logger = mock(Logger.class);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        OptimizelyManager manager = new OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval, datafileHandler, null, 0,
                null, null, null, null, null, null, null, null, null);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<DatafileConfig> configCaptor = ArgumentCaptor.forClass(DatafileConfig.class);
        ArgumentCaptor<DatafileLoadedListener> listenerCaptor = ArgumentCaptor.forClass(DatafileLoadedListener.class);

        doAnswer(invocation -> {
            Context capturedContext = contextCaptor.getValue();
            DatafileConfig capturedConfig = configCaptor.getValue();
            DatafileLoadedListener capturedListener = listenerCaptor.getValue();

            String newDatafile = manager.getDatafile(capturedContext, R.raw.datafile_api);
            datafileHandler.saveDatafile(capturedContext, capturedConfig, newDatafile);

            return datafileHandler;
        }).when(manager.getDatafileHandler()).downloadDatafile(contextCaptor.capture(), configCaptor.capture(), listenerCaptor.capture());

        OptimizelyClient client = manager.initialize(context, defaultDatafile, downloadToCache, updateConfigOnNewDatafile);

        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //
        }

        assertEquals(client.getOptimizelyConfig().getRevision(), "241");
    }

    @Test
    public void initializeSyncWithResourceDatafileNoCache() {
        boolean downloadToCache = true;
        boolean updateConfigOnNewDatafile = true;
        int pollingInterval = 30;   // enable polling

        DefaultDatafileHandler datafileHandler = spy(new DefaultDatafileHandler());
        Logger logger = mock(Logger.class);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        OptimizelyManager manager = spy(new OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval, datafileHandler, null, 0,
                null, null, null, null, null, null, null, null, null));

        datafileHandler.removeSavedDatafile(context, manager.getDatafileConfig());
        OptimizelyClient client = manager.initialize(context, R.raw.datafile, downloadToCache, updateConfigOnNewDatafile);

        verify(manager).initialize(eq(context), eq(defaultDatafile), eq(downloadToCache),  eq(updateConfigOnNewDatafile));
    }

    @Test
    public void initializeSyncWithResourceDatafileNoCacheWithDefaultParams() {
        int pollingInterval = 30;   // enable polling

        DefaultDatafileHandler datafileHandler = spy(new DefaultDatafileHandler());
        Logger logger = mock(Logger.class);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        OptimizelyManager manager = spy(new OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval, datafileHandler, null, 0,
                null, null, null, null, null, null, null, null, null));

        datafileHandler.removeSavedDatafile(context, manager.getDatafileConfig());
        OptimizelyClient client = manager.initialize(context, R.raw.datafile);

        verify(manager).initialize(eq(context), eq(defaultDatafile), eq(true),  eq(false));
    }

    @Test
    public void initializeAsyncCallbackInBackgroundThread() throws InterruptedException {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder(testProjectId)
            .build(InstrumentationRegistry.getInstrumentation().getTargetContext());

        CountDownLatch latch = new CountDownLatch(1);

        // by default, async init returns in main thread.
        // this parameter should be set to false to overrule it.
        boolean returnInMainThread = false;

        optimizelyManager.initialize(
            InstrumentationRegistry.getInstrumentation().getContext(),
            null,
            returnInMainThread,
            (client) -> {
                Log.d("Optly", "[TESTING] " + Thread.currentThread().getName());
                try {
                    assertNotEquals(
                        "OptimizelyStartListener should be called in a background thread",
                        "main", Thread.currentThread().getName()
                    );
                    latch.countDown();
                } catch (AssertionError e) {
                    // we need catch and silence this assertion error, otherwise it will be caught in OptimizeManager,
                    // and give a wrong error message. The failure will be detected with the latch timeout below.
                }
            }
        );

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        if (!completed) {
            fail("OptimizelyStartListener thread checking failed");
        }
    }

    @Test
    public void initializeAsyncCallbackInMainThread() throws InterruptedException {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder(testProjectId)
            .build(InstrumentationRegistry.getInstrumentation().getTargetContext());

        CountDownLatch latch = new CountDownLatch(1);

        optimizelyManager.initialize(
            InstrumentationRegistry.getInstrumentation().getContext(),
            null,
            (client) -> {
                Log.d("Optly", "[TESTING] " + Thread.currentThread().getName());
                try {
                    assertEquals(
                        "OptimizelyStartListener should be called in a background thread",
                        "main", Thread.currentThread().getName()
                    );
                    latch.countDown();
                } catch (AssertionError e) {
                    // we need catch and silence this assertion error, otherwise it will be caught in OptimizeManager,
                    // and give a wrong error message. The failure will be detected with the latch timeout below.
                }
            }
        );

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        if (!completed) {
            fail("OptimizelyStartListener thread checking failed");
        }
    }

    // Utils

    void mockProjectConfig(DefaultDatafileHandler datafileHandler, String datafile) {
        ProjectConfig config = null;
        try {
            config = new DatafileProjectConfig.Builder().withDatafile(datafile).build();
            when(datafileHandler.getConfig()).thenReturn(config);
        } catch (ConfigParseException e) {
            e.printStackTrace();
        }
    }

}
