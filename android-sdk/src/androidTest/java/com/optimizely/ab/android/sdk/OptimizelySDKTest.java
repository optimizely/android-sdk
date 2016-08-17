package com.optimizely.ab.android.sdk;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.test.espresso.core.deps.guava.util.concurrent.ListeningExecutorService;
import android.support.test.espresso.core.deps.guava.util.concurrent.MoreExecutors;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.Optimizely;
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
 * Tests for {@link OptimizelySDK}
 */
@RunWith(AndroidJUnit4.class)
public class OptimizelySDKTest {

    BackgroundWatchersCache backgroundWatchersCache;
    ListeningExecutorService executor;
    Logger logger;
    OptimizelySDK optimizelySDK;

    @Before
    public void setup() {
        logger = mock(Logger.class);
        backgroundWatchersCache = mock(BackgroundWatchersCache.class);
        executor = MoreExecutors.newDirectExecutorService();

        optimizelySDK = new OptimizelySDK("1", 1L, TimeUnit.HOURS, 1L, TimeUnit.HOURS, executor, logger);
    }

    @SuppressWarnings("WrongConstant")
    @Test
    public void start() {
        OptimizelyStartListener startListener = mock(OptimizelyStartListener.class);
        Application application = mock(Application.class);
        Context context = mock(Context.class);
        when(application.getApplicationContext()).thenReturn(context);
        when(context.getPackageName()).thenReturn("com.optly");
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        OptimizelySDK.OptlyActivityLifecycleCallbacks optlyActivityLifecycleCallbacks = mock(OptimizelySDK.OptlyActivityLifecycleCallbacks.class);

        optimizelySDK.start(application, optlyActivityLifecycleCallbacks, startListener);

        assertNotNull(optimizelySDK.getOptimizelyStartListener());
        assertNotNull(optimizelySDK.getDataFileServiceConnection());

        verify(context).bindService(captor.capture(), any(OptimizelySDK.DataFileServiceConnection.class), eq(Context.BIND_AUTO_CREATE));
        verify(application).registerActivityLifecycleCallbacks(optlyActivityLifecycleCallbacks);

        Intent intent = captor.getValue();
        assertTrue(intent.getComponent().getShortClassName().contains("DataFileService"));
    }

    @Test
    public void startNullCallbacks() {
        OptimizelyStartListener startListener = mock(OptimizelyStartListener.class);
        Application application = mock(Application.class);
        Context context = mock(Context.class);
        when(application.getApplicationContext()).thenReturn(context);
        OptimizelySDK.OptlyActivityLifecycleCallbacks optlyActivityLifecycleCallbacks = mock(OptimizelySDK.OptlyActivityLifecycleCallbacks.class);
        optimizelySDK.start(application, null, startListener);

        verify(application, never()).registerActivityLifecycleCallbacks(optlyActivityLifecycleCallbacks);
    }

    @Test
    public void stop() {
        Context context = mock(Context.class);
        Application application = mock(Application.class);
        when(application.getApplicationContext()).thenReturn(context);
        OptimizelySDK.OptlyActivityLifecycleCallbacks activityLifecycleCallbacks = mock(OptimizelySDK.OptlyActivityLifecycleCallbacks.class);

        OptimizelySDK.DataFileServiceConnection dataFileServiceConnection = mock(OptimizelySDK.DataFileServiceConnection.class);
        optimizelySDK.setDataFileServiceConnection(dataFileServiceConnection);
        when(dataFileServiceConnection.isBound()).thenReturn(true);

        optimizelySDK.stop(application, activityLifecycleCallbacks);

        assertNull(optimizelySDK.getOptimizelyStartListener());
        verify(context).unbindService(dataFileServiceConnection);
        verify(application).unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    @Test
    public void stopNotBoundNullCallbacks() {
        Context context = mock(Context.class);
        Application application = mock(Application.class);
        when(application.getApplicationContext()).thenReturn(context);
        OptimizelySDK.OptlyActivityLifecycleCallbacks activityLifecycleCallbacks = mock(OptimizelySDK.OptlyActivityLifecycleCallbacks.class);

        OptimizelySDK.DataFileServiceConnection dataFileServiceConnection = mock(OptimizelySDK.DataFileServiceConnection.class);
        optimizelySDK.setDataFileServiceConnection(dataFileServiceConnection);
        when(dataFileServiceConnection.isBound()).thenReturn(false);

        optimizelySDK.stop(application, null);

        assertNull(optimizelySDK.getOptimizelyStartListener());
        verify(context, never()).unbindService(dataFileServiceConnection);
        verify(application, never()).unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);
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
        optimizelySDK.setOptimizelyStartListener(startListener);
        optimizelySDK.injectOptimizely(context, userExperimentRecord, serviceScheduler, "");
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(userExperimentRecord).start();
        verify(serviceScheduler).schedule(captor.capture(), TimeUnit.HOURS.toMillis(1L));
        verify(logger).info("Sending Optimizely instance to listener");
        verify(startListener).onStart(any(Optimizely.class));
    }

    @Test
    public void injectOptimizelyNullListener() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("com.optly");
        AndroidUserExperimentRecord userExperimentRecord = mock(AndroidUserExperimentRecord.class);
        ServiceScheduler serviceScheduler = mock(ServiceScheduler.class);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        OptimizelyStartListener startListener = mock(OptimizelyStartListener.class);
        optimizelySDK.setOptimizelyStartListener(null);
        optimizelySDK.injectOptimizely(context, userExperimentRecord, serviceScheduler, "");
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(userExperimentRecord).start();
        verify(serviceScheduler).schedule(captor.capture(), eq(TimeUnit.HOURS.toMillis(1L)));
        verify(logger).info("No listener to send Optimizely to");
        verify(startListener, never()).onStart(any(Optimizely.class));

        Intent intent = captor.getValue();
        assertTrue(intent.getComponent().getShortClassName().contains("DataFileService"));
        assertEquals(optimizelySDK.getProjectId(), intent.getStringExtra(DataFileService.EXTRA_PROJECT_ID));
    }
}
