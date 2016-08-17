package com.optimizely.ab.android.sdk;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;

import com.optimizely.ab.android.shared.Cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataFileService extends Service {
    @NonNull private final IBinder binder = new LocalBinder();

    Logger logger = LoggerFactory.getLogger(getClass());
    public static String EXTRA_PROJECT_ID = "com.optimizely.ab.android.EXTRA_PROJECT_ID";

    private boolean isBound;

    @Override
    public IBinder onBind(Intent intent) {
        isBound = true;
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        isBound = false;
        logger.info("All clients are unbound from data file service");
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.hasExtra(EXTRA_PROJECT_ID)) {
                String projectId = intent.getStringExtra(EXTRA_PROJECT_ID);
                DataFileLoader dataFileLoader = new DataFileLoader(new DataFileLoader.TaskChain(this), LoggerFactory.getLogger(DataFileLoader.class));
                dataFileLoader.getDataFile(projectId, null);
                BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(
                        new Cache(this, LoggerFactory.getLogger(Cache.class)),
                        LoggerFactory.getLogger(BackgroundWatchersCache.class));
                backgroundWatchersCache.setIsWatching(projectId, true);

                logger.info("Started watching project {} in the background", projectId);
            } else {
                logger.warn("Data file service received an intent with no project id extra");
            }
        } else {
            logger.warn("Data file service received a null intent");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public boolean isBound() {
        return  isBound;
    }

    public void stop() {
        stopSelf();
    }

    public void getDataFile(String projectId, DataFileLoader dataFileLoader, DataFileLoadedListener loadedListener) {
        dataFileLoader.getDataFile(projectId, loadedListener);
    }

    public class LocalBinder extends Binder {
        public DataFileService getService() {
            // Return this instance of LocalService so clients can call public methods
            return DataFileService.this;
        }
    }
}
