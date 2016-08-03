package com.optimizely.ab.android.project_watcher;

import android.content.Context;
import android.content.Intent;
import android.support.test.runner.AndroidJUnit4;

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
 * Tests for {@link OptlyProjectWatcher}
 */
@RunWith(AndroidJUnit4.class)
public class OptlyProjectWatcherTest {

    ServiceScheduler serviceScheduler;
    BackgroundWatchersCache backgroundWatchersCache;
    Context context;
    Logger logger;
    OptlyProjectWatcher optlyProjectWatcher;

    @Before
    public void setup() {
        context = mock(Context.class);
        serviceScheduler = mock(ServiceScheduler.class);
        logger = mock(Logger.class);
        backgroundWatchersCache = mock(BackgroundWatchersCache.class);
        optlyProjectWatcher = new OptlyProjectWatcher("1", context, serviceScheduler, backgroundWatchersCache, logger);
    }

    @Test
    public void loadDataFile() {
        OnDataFileLoadedListener onDataFileLoadedListener = mock(OnDataFileLoadedListener.class);
        optlyProjectWatcher.loadDataFile(onDataFileLoadedListener);
        assertNotNull(optlyProjectWatcher.getOnDataFileLoadedListener());
        verify(context).bindService(new Intent(context, DataFileService.class), optlyProjectWatcher.getDataFileServiceConnection(), Context.BIND_AUTO_CREATE);
    }

    @Test
    public void cancelDataFileLoadWhenBound() {
        optlyProjectWatcher.setBound(true);
        assertTrue(optlyProjectWatcher.isBound());
        optlyProjectWatcher.cancelDataFileLoad();
        assertFalse(optlyProjectWatcher.isBound());
        assertNull(optlyProjectWatcher.getOnDataFileLoadedListener());
        verify(context).unbindService(optlyProjectWatcher.getDataFileServiceConnection());
    }

    @Test
    public void startWatching() {
        optlyProjectWatcher.startWatching(TimeUnit.DAYS, 1);
        verify(optlyProjectWatcher.getServiceScheduler()).schedule(any(Intent.class), anyLong());
    }

    @Test
    public void stopWatching() {
        optlyProjectWatcher.stopWatching();
        verify(serviceScheduler).unschedule(any(Intent.class));
        verify(backgroundWatchersCache).setIsWatching("1", false);
    }

    @Test
    public void notifyNullListeners() {
        optlyProjectWatcher.notifyListener("");
        verify(logger).error("Tried to notify null listener");
    }

    @Test
    public void notifyListeners() {
        OnDataFileLoadedListener loadedListener = mock(OnDataFileLoadedListener.class);
        optlyProjectWatcher.loadDataFile(loadedListener);
        optlyProjectWatcher.notifyListener("");
        verify(loadedListener).onDataFileLoaded("");
        verify(logger).info("Notifying listener of new data file");
    }

    @Test
    public void onServiceDisconnected() {
        OptlyProjectWatcher.DataFileServiceConnection conn = new OptlyProjectWatcher.DataFileServiceConnection(optlyProjectWatcher);
        conn.onServiceDisconnected(null);
        assertFalse(optlyProjectWatcher.isBound());
    }

    @Test
    public void onServiceConnected() {
        OptlyProjectWatcher.DataFileServiceConnection conn = new OptlyProjectWatcher.DataFileServiceConnection(optlyProjectWatcher);
        DataFileService.LocalBinder binder = mock(DataFileService.LocalBinder.class);
        DataFileService service = mock(DataFileService.class);
        when(binder.getService()).thenReturn(service);
        conn.onServiceConnected(null, binder);
        verify(service).getDataFile(eq("1"), any(DataFileLoader.class), any(OnDataFileLoadedListener.class));
        assertTrue(optlyProjectWatcher.isBound());
    }

    @Test
    public void onlyNotifiesWhenBound() {
        optlyProjectWatcher.setBound(true);
        OnDataFileLoadedListener onDataFileLoadedListener = mock(OnDataFileLoadedListener.class);
        optlyProjectWatcher.setOnDataFileLoadedListener(onDataFileLoadedListener);
        OptlyProjectWatcher.DataFileServiceConnection conn = new OptlyProjectWatcher.DataFileServiceConnection(optlyProjectWatcher);
        conn.notifyProjectWatcher("");
        verify(onDataFileLoadedListener).onDataFileLoaded("");
        verify(logger).info("Notifying listener of new data file");
    }
}
