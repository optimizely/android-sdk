/*
 * Copyright 2016, Optimizely
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.android.sdk;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.core.deps.guava.util.concurrent.ListeningExecutorService;
import android.support.test.espresso.core.deps.guava.util.concurrent.MoreExecutors;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.ab.android.user_experiment_record.AndroidUserExperimentRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
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

    private ListeningExecutorService executor;
    private Logger logger;
    private OptimizelyManager optimizelyManager;

    private String minDataFile = "{\n" +
            "experiments: [ ],\n" +
            "version: \"2\",\n" +
            "audiences: [ ],\n" +
            "groups: [ ],\n" +
            "attributes: [ ],\n" +
            "projectId: \"7595190003\",\n" +
            "accountId: \"6365361536\",\n" +
            "events: [ ],\n" +
            "revision: \"1\"\n" +
            "}";

    @Before
    public void setup() {
        logger = mock(Logger.class);
        executor = MoreExecutors.newDirectExecutorService();
        optimizelyManager = new OptimizelyManager("7595190003", 1L, TimeUnit.HOURS, 1L, TimeUnit.HOURS, executor, logger);
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

        optimizelyManager.initialize(context, startListener);

        assertNotNull(optimizelyManager.getOptimizelyStartListener());
        assertNotNull(optimizelyManager.getDataFileServiceConnection());

        verify(appContext).bindService(captor.capture(), any(OptimizelyManager.DataFileServiceConnection.class), eq(Context.BIND_AUTO_CREATE));

        Intent intent = captor.getValue();
        assertTrue(intent.getComponent().getShortClassName().contains("DataFileService"));
    }

    @Test
    public void stop() {
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);

        OptimizelyManager.DataFileServiceConnection dataFileServiceConnection = new OptimizelyManager.DataFileServiceConnection(optimizelyManager);
        dataFileServiceConnection.setBound(true);
        optimizelyManager.setDataFileServiceConnection(dataFileServiceConnection);

        optimizelyManager.stop(context);

        assertNull(optimizelyManager.getOptimizelyStartListener());
        verify(appContext).unbindService(dataFileServiceConnection);
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void injectOptimizely() {
        Context context = mock(Context.class);
        AndroidUserExperimentRecord userExperimentRecord = mock(AndroidUserExperimentRecord.class);
        ServiceScheduler serviceScheduler = mock(ServiceScheduler.class);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        OptimizelyStartListener startListener = mock(OptimizelyStartListener.class);
        optimizelyManager.setOptimizelyStartListener(startListener);
        optimizelyManager.injectOptimizely(context, userExperimentRecord, serviceScheduler, minDataFile);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(userExperimentRecord).start();
        verify(serviceScheduler).schedule(captor.capture(), eq(TimeUnit.HOURS.toMillis(1L)));
        verify(logger).info("Sending Optimizely instance to listener");
        verify(startListener).onStart(any(OptimizelyClient.class));
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void injectOptimizelyNullListener() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("com.optly");
        AndroidUserExperimentRecord userExperimentRecord = mock(AndroidUserExperimentRecord.class);
        ServiceScheduler serviceScheduler = mock(ServiceScheduler.class);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        optimizelyManager.setOptimizelyStartListener(null);
        optimizelyManager.injectOptimizely(context, userExperimentRecord, serviceScheduler, minDataFile);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(userExperimentRecord).start();
        verify(serviceScheduler).schedule(captor.capture(), eq(TimeUnit.HOURS.toMillis(1L)));
        verify(logger).info("No listener to send Optimizely to");

        Intent intent = captor.getValue();
        assertTrue(intent.getComponent().getShortClassName().contains("DataFileService"));
        assertEquals(optimizelyManager.getProjectId(), intent.getStringExtra(DataFileService.EXTRA_PROJECT_ID));
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void injectOptimizelyHandlesInvalidDataFile() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("com.optly");
        AndroidUserExperimentRecord userExperimentRecord = mock(AndroidUserExperimentRecord.class);
        ServiceScheduler serviceScheduler = mock(ServiceScheduler.class);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        optimizelyManager.setOptimizelyStartListener(null);
        optimizelyManager.injectOptimizely(context, userExperimentRecord, serviceScheduler, "{}");
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(userExperimentRecord).start();
        verify(serviceScheduler).schedule(captor.capture(), eq(TimeUnit.HOURS.toMillis(1L)));
        verify(logger).error(eq("Unable to build optimizely instance"), any(Exception.class));

        Intent intent = captor.getValue();
        assertTrue(intent.getComponent().getShortClassName().contains("DataFileService"));
        assertEquals(optimizelyManager.getProjectId(), intent.getStringExtra(DataFileService.EXTRA_PROJECT_ID));
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void injectOptimizelyDoesNotDuplicateCallback() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("com.optly");
        AndroidUserExperimentRecord userExperimentRecord = mock(AndroidUserExperimentRecord.class);
        ServiceScheduler serviceScheduler = mock(ServiceScheduler.class);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        OptimizelyStartListener startListener = mock(OptimizelyStartListener.class);
        optimizelyManager.setOptimizelyStartListener(startListener);
        optimizelyManager.injectOptimizely(context, userExperimentRecord, serviceScheduler, minDataFile);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(userExperimentRecord).start();
        verify(serviceScheduler).schedule(captor.capture(), eq(TimeUnit.HOURS.toMillis(1L)));

        verify(logger).info("Sending Optimizely instance to listener");
        verify(startListener).onStart(any(OptimizelyClient.class));

        optimizelyManager.injectOptimizely(context, userExperimentRecord, serviceScheduler, minDataFile);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }
    }
}
