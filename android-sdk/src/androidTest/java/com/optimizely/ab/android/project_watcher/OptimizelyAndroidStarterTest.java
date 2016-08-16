package com.optimizely.ab.android.project_watcher;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.android.shared.ServiceScheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 8/3/16 for Optimizely.
 *
 * Tests for {@link OptimizelyAndroid}
 */
@RunWith(AndroidJUnit4.class)
public class OptimizelyAndroidStarterTest {

    ServiceScheduler serviceScheduler;
    BackgroundWatchersCache backgroundWatchersCache;
    Application application;
    Logger logger;
    OptimizelyAndroid optimizelyAndroid;
    OptimizelyStartedListener optimizelyStartedListener;

    @Before
    public void setup() {
        application = mock(Application.class);
        serviceScheduler = mock(ServiceScheduler.class);
        logger = mock(Logger.class);
        optimizelyStartedListener = mock(OptimizelyStartedListener.class);
        backgroundWatchersCache = mock(BackgroundWatchersCache.class);
        optimizelyAndroid = new OptimizelyAndroid("1", application, serviceScheduler, backgroundWatchersCache, optimizelyStartedListener, logger);
    }

    @Test
    public void loadDataFile() {
        optimizelyAndroid.start();
        assertNotNull(optimizelyAndroid.getOptimizelyStartedListener());
        verify(application).bindService(new Intent(application, DataFileService.class), optimizelyAndroid.getDataFileServiceConnection(), Context.BIND_AUTO_CREATE);
    }

    @Test
    public void cancelDataFileLoadWhenBound() {
        optimizelyAndroid.setBound(true);
        assertTrue(optimizelyAndroid.isBound());
        assertFalse(optimizelyAndroid.isBound());
        assertNull(optimizelyAndroid.getOptimizelyStartedListener());
        verify(application).unbindService(optimizelyAndroid.getDataFileServiceConnection());
    }

    @Test
    public void startWatching() {
        optimizelyAndroid.syncDataFile(TimeUnit.DAYS, 1);
        verify(optimizelyAndroid.getServiceScheduler()).schedule(any(Intent.class), anyLong());
    }

    @Test
    public void stopWatching() {
        optimizelyAndroid.stopSyncingDataFile();
        verify(serviceScheduler).unschedule(any(Intent.class));
        verify(backgroundWatchersCache).setIsWatching("1", false);
    }

    @Test
    public void notifyNullListeners() {
        optimizelyAndroid.notifyListener("");
        verify(logger).error("No listener to send Optimizely to");
    }

    @Test
    public void notifyListeners() {
        optimizelyAndroid.notifyListener("");
        verify(optimizelyStartedListener).onOptimizelyStarted(any(Optimizely.class));
        verify(logger).info("Sending Optimizely instance to listener");
    }

    @Test
    public void onServiceDisconnected() {
        OptimizelyAndroid.DataFileServiceConnection conn = new OptimizelyAndroid.DataFileServiceConnection(optimizelyAndroid);
        conn.onServiceDisconnected(null);
        assertFalse(optimizelyAndroid.isBound());
    }

    @Test
    public void onServiceConnected() {
        OptimizelyAndroid.DataFileServiceConnection conn = new OptimizelyAndroid.DataFileServiceConnection(optimizelyAndroid);
        DataFileService.LocalBinder binder = mock(DataFileService.LocalBinder.class);
        DataFileService service = mock(DataFileService.class);
        when(binder.getService()).thenReturn(service);
        conn.onServiceConnected(null, binder);
        verify(service).getDataFile(eq("1"), any(DataFileLoader.class), any(DataFileLoadedListener.class));
        assertTrue(optimizelyAndroid.isBound());
    }

    @Test
    public void onlyNotifiesWhenBound() {
        optimizelyAndroid.setBound(true);
        OptimizelyStartedListener optimizelyStartedListener = mock(OptimizelyStartedListener.class);
        optimizelyAndroid.setOptimizelyStartedListener(optimizelyStartedListener);
        OptimizelyAndroid.DataFileServiceConnection conn = new OptimizelyAndroid.DataFileServiceConnection(optimizelyAndroid);
        DataFileService.LocalBinder binder = mock(DataFileService.LocalBinder.class);
        DataFileService service = mock(DataFileService.class);
        when(binder.getService()).thenReturn(service);
        conn.onServiceConnected(null, binder);
        verify(logger).info("Notifying listener of new data file");
    }
}
