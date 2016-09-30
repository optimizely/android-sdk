/**
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.test.espresso.core.deps.guava.util.concurrent.ListeningExecutorService;
import android.support.test.espresso.core.deps.guava.util.concurrent.MoreExecutors;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.user_experiment_record.AndroidUserExperimentRecord;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 8/3/16 for Optimizely.
 *
 * Tests for {@link OptimizelyManager}
 *
 * *NOTE*
 * Some tests are ignored here because Activity#getApplication() is final and can't be mocked
 * Also, mockito fails when making {@link OptimizelyManager#stop(Activity, OptimizelyManager.OptlyActivityLifecycleCallbacks)}
 * private or package private.
 * // TODO Get these tests working via PowerMock https://github.com/jayway/powermock
 */
@RunWith(AndroidJUnit4.class)
public class OptimizelyManagerTest {

    BackgroundWatchersCache backgroundWatchersCache;
    ListeningExecutorService executor;
    Logger logger;
    OptimizelyManager optimizelyManager;

    @Before
    public void setup() {
        logger = mock(Logger.class);
        backgroundWatchersCache = mock(BackgroundWatchersCache.class);
        executor = MoreExecutors.newDirectExecutorService();

        optimizelyManager = new OptimizelyManager("1", 1L, TimeUnit.HOURS, 1L, TimeUnit.HOURS, executor, logger);
    }

    @SuppressWarnings("WrongConstant")
    @Test
    @Ignore
    public void start() {
        OptimizelyStartListener startListener = mock(OptimizelyStartListener.class);
        Activity activity = mock(Activity.class);
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("com.optly");
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        optimizelyManager.start(activity, startListener);

        assertNotNull(optimizelyManager.getOptimizelyStartListener());
        assertNotNull(optimizelyManager.getDataFileServiceConnection());

        verify(context).bindService(captor.capture(), any(OptimizelyManager.DataFileServiceConnection.class), eq(Context.BIND_AUTO_CREATE));

        Intent intent = captor.getValue();
        assertTrue(intent.getComponent().getShortClassName().contains("DataFileService"));
    }

    @Test
    @Ignore
    public void stop() {
        Context context = mock(Context.class);
        Activity activity = mock(Activity.class);
        OptimizelyManager.OptlyActivityLifecycleCallbacks activityLifecycleCallbacks = mock(OptimizelyManager.OptlyActivityLifecycleCallbacks.class);

        OptimizelyManager.DataFileServiceConnection dataFileServiceConnection = mock(OptimizelyManager.DataFileServiceConnection.class);
        optimizelyManager.setDataFileServiceConnection(dataFileServiceConnection);
        when(dataFileServiceConnection.isBound()).thenReturn(true);

        optimizelyManager.stop(activity, activityLifecycleCallbacks);

        assertNull(optimizelyManager.getOptimizelyStartListener());
        verify(context).unbindService(dataFileServiceConnection);
    }

    // TODO add a data file fixture so parsing doesn't fail in SST core
    @Test
    @Ignore
    public void injectOptimizely() {
        Context context = mock(Context.class);
        AndroidUserExperimentRecord userExperimentRecord = mock(AndroidUserExperimentRecord.class);
        ServiceScheduler serviceScheduler = mock(ServiceScheduler.class);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        OptimizelyStartListener startListener = mock(OptimizelyStartListener.class);
        optimizelyManager.setOptimizelyStartListener(startListener);
        optimizelyManager.injectOptimizely(context, userExperimentRecord, serviceScheduler, "");
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(userExperimentRecord).start();
        verify(serviceScheduler).schedule(captor.capture(), TimeUnit.HOURS.toMillis(1L));
        verify(logger).info("Sending Optimizely instance to listener");
        verify(startListener).onStart(any(AndroidOptimizely.class));
    }

    @Test
    public void injectOptimizelyNullListener() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("com.optly");
        AndroidUserExperimentRecord userExperimentRecord = mock(AndroidUserExperimentRecord.class);
        ServiceScheduler serviceScheduler = mock(ServiceScheduler.class);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        OptimizelyStartListener startListener = mock(OptimizelyStartListener.class);
        optimizelyManager.setOptimizelyStartListener(null);
        optimizelyManager.injectOptimizely(context, userExperimentRecord, serviceScheduler, "");
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(userExperimentRecord).start();
        verify(serviceScheduler).schedule(captor.capture(), eq(TimeUnit.HOURS.toMillis(1L)));
        verify(logger).info("No listener to send Optimizely to");
        verify(startListener, never()).onStart(any(AndroidOptimizely.class));

        Intent intent = captor.getValue();
        assertTrue(intent.getComponent().getShortClassName().contains("DataFileService"));
        assertEquals(optimizelyManager.getProjectId(), intent.getStringExtra(DataFileService.EXTRA_PROJECT_ID));
    }
}
