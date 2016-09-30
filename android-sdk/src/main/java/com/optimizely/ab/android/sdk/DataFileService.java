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

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;

import com.optimizely.ab.android.shared.Cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataFileService extends Service {
    public static final String EXTRA_PROJECT_ID = "com.optimizely.ab.android.EXTRA_PROJECT_ID";
    @NonNull private final IBinder binder = new LocalBinder();
    Logger logger = LoggerFactory.getLogger(getClass());
    private boolean isBound;

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
