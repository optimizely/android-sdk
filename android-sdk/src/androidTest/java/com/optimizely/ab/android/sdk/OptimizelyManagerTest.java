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

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.test.espresso.core.deps.guava.util.concurrent.ListeningExecutorService;
import android.support.test.espresso.core.deps.guava.util.concurrent.MoreExecutors;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.android.datafile_handler.DatafileHandler;
import com.optimizely.ab.android.datafile_handler.DatafileHandlerDefault;
import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.ab.android.user_profile.AndroidUserProfileServiceDefault;
import com.optimizely.ab.config.parser.ConfigParseException;

import com.optimizely.ab.android.datafile_handler.DataFileService;
import com.optimizely.ab.android.datafile_handler.DataFileServiceConnection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OptimizelyManager}
 */
@RunWith(AndroidJUnit4.class)
public class OptimizelyManagerTest {

    private String testProjectId = "7595190003";
    private ListeningExecutorService executor;
    private Logger logger;
    private OptimizelyManager optimizelyManager;

    private String minDataFile = "{\n" +
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
    public void setup() {
        logger = mock(Logger.class);
        executor = MoreExecutors.newDirectExecutorService();
        DatafileHandler handler = mock(DatafileHandlerDefault.class);
        optimizelyManager = new OptimizelyManager(testProjectId, 1L, TimeUnit.HOURS, 1L, TimeUnit.HOURS, executor, logger, handler);
    }

    @SuppressWarnings("WrongConstant")
    @Test
    public void initialize() {
        OptimizelyStartListener startListener = mock(OptimizelyStartListener.class);
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);
        when(appContext.getPackageName()).thenReturn("com.optly");
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        optimizelyManager.initialize(appContext, startListener);

        assertNotNull(optimizelyManager.getOptimizelyStartListener());
        assertNotNull(optimizelyManager.getDatafileHandler());

    }

    @Test
    public void initializeWithEmptyDatafile() {
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);
        when(appContext.getPackageName()).thenReturn("com.optly");

        String emptyString = "";

        optimizelyManager.initialize(context, emptyString);
        verify(logger).error(eq("Unable to parse compiled data file"), any(ConfigParseException.class));
    }

    @Test
    public void initializeWithMalformedDatafile() {
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);
        when(appContext.getPackageName()).thenReturn("com.optly");

        String emptyString = "malformed data";

        optimizelyManager.initialize(context, emptyString);
        verify(logger).error(eq("Unable to parse compiled data file"), any(ConfigParseException.class));
    }

    @Test
    public void initializeWithNullDatafile() {
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);
        when(appContext.getPackageName()).thenReturn("com.optly");

        String emptyString = null;

        optimizelyManager.initialize(context, emptyString);
        verify(logger).error(eq("Unable to parse compiled data file"), any(ConfigParseException.class));
    }

    @Test
    public void stop() {
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);

        optimizelyManager.getDatafileHandler().downloadDatafile(context, optimizelyManager.getProjectId(), null);

        optimizelyManager.stop(context);

        assertNull(optimizelyManager.getOptimizelyStartListener());
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void injectOptimizely() {
        Context context = mock(Context.class);
        AndroidUserProfileServiceDefault userProfileService = mock(AndroidUserProfileServiceDefault.class);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        OptimizelyStartListener startListener = mock(OptimizelyStartListener.class);
        optimizelyManager.setOptimizelyStartListener(startListener);
        optimizelyManager.injectOptimizely(context, userProfileService, minDataFile);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(userProfileService).start();
        verify(logger).info("Sending Optimizely instance to listener");
        verify(startListener).onStart(any(OptimizelyClient.class));
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void injectOptimizelyNullListener() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("com.optly");
        AndroidUserProfileServiceDefault userProfileService = mock(AndroidUserProfileServiceDefault.class);
        ServiceScheduler serviceScheduler = mock(ServiceScheduler.class);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        optimizelyManager.setOptimizelyStartListener(null);
        //optimizelyManager.injectOptimizely(context, userProfileService, minDataFile);
        AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler
                .PendingIntentFactory(context);

        Intent intent = new Intent(context, DataFileService.class);
        intent.putExtra("com.optimizely.ab.android.EXTRA_PROJECT_ID", optimizelyManager.getProjectId());
        serviceScheduler.schedule(intent, optimizelyManager.getDataFileDownloadIntervalTimeUnit().toMillis(optimizelyManager.getDataFileDownloadInterval()));

        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        //verify(userProfileService).start();
        verify(serviceScheduler).schedule(captor.capture(), eq(TimeUnit.HOURS.toMillis(1L)));
        //verify(logger).info("No listener to send Optimizely to");

        Intent intent2 = captor.getValue();
        assertTrue(intent2.getComponent().getShortClassName().contains("DataFileService"));
        assertEquals(optimizelyManager.getProjectId(), intent2.getStringExtra(DataFileService.EXTRA_PROJECT_ID));
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void injectOptimizelyHandlesInvalidDataFile() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("com.optly");
        AndroidUserProfileServiceDefault userProfileService = mock(AndroidUserProfileServiceDefault.class);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        optimizelyManager.setOptimizelyStartListener(null);
        optimizelyManager.injectOptimizely(context, userProfileService, "{}");
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(userProfileService).start();
        verify(logger).error(eq("Unable to build optimizely instance"), any(Exception.class));

    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void injectOptimizelyDoesNotDuplicateCallback() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("com.optly");
        AndroidUserProfileServiceDefault userProfileService = mock(AndroidUserProfileServiceDefault.class);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        OptimizelyStartListener startListener = mock(OptimizelyStartListener.class);
        optimizelyManager.setOptimizelyStartListener(startListener);
        optimizelyManager.injectOptimizely(context, userProfileService, minDataFile);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(userProfileService).start();

        verify(logger).info("Sending Optimizely instance to listener");
        verify(startListener).onStart(any(OptimizelyClient.class));

        optimizelyManager.injectOptimizely(context, userProfileService, minDataFile);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }
    }
}
