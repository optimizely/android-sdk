/****************************************************************************
 * Copyright 2016-2017, Optimizely, Inc. and contributors                   *
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

package com.optimizely.ab.android.datafile_handler;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.OptlyStorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

/**
 * Service that handles loading the datafile from cache or downloads it from the CDN
 * These services will only be used if you are using our {@link DefaultDatafileHandler}.
 * You can chose to implement your own handler and use all or part of this package.
 */
public class DatafileService extends Service {
    /**
     * Extra containing the project id this instance of Optimizely was built with
     */
    public static final String EXTRA_PROJECT_ID = "com.optimizely.ab.android.EXTRA_PROJECT_ID";
    public static final String FORMAT_VERSIONED_CDN_URL = "https://cdn.optimizely.com/public/%s/datafile_v%s.json";
    static final String DATAFILE_VERSION = "3";
    public static final Integer JOB_ID = 2113;

    @NonNull private final IBinder binder = new LocalBinder();
    Logger logger = LoggerFactory.getLogger(DatafileService.class);
    private boolean isBound;

    /**
     * @hide
     * @see Service#onStartCommand(Intent, int, int)
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.hasExtra(EXTRA_PROJECT_ID)) {
                String projectId = intent.getStringExtra(EXTRA_PROJECT_ID);
                DatafileClient datafileClient = new DatafileClient(
                        new Client(new OptlyStorage(this.getApplicationContext()), LoggerFactory.getLogger(OptlyStorage.class)),
                        LoggerFactory.getLogger(DatafileClient.class));
                DatafileCache datafileCache = new DatafileCache(
                        projectId,
                        new Cache(this.getApplicationContext(), LoggerFactory.getLogger(Cache.class)),
                        LoggerFactory.getLogger(DatafileCache.class));

                String datafileUrl = getDatafileUrl(projectId);
                DatafileLoader datafileLoader = new DatafileLoader(this, datafileClient, datafileCache, Executors.newSingleThreadExecutor(), LoggerFactory.getLogger(DatafileLoader.class));
                datafileLoader.getDatafile(datafileUrl, null);
            } else {
                logger.warn("Data file service received an intent with no project id extra");
            }
        } else {
            logger.warn("Data file service received a null intent");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * @hide
     * @see Service#onBind(Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
        isBound = true;
        return binder;
    }

    /**
     * @hide
     * @see Service#onUnbind(Intent)
     */
    @Override
    public boolean onUnbind(Intent intent) {
        isBound = false;
        logger.info("All clients are unbound from data file service");
        return false;
    }

    public boolean isBound() {
        return isBound;
    }

    public static @NonNull String getDatafileUrl(@NonNull String projectId) {
        return String.format(FORMAT_VERSIONED_CDN_URL, projectId, DATAFILE_VERSION);
    }

    public void stop() {
        stopSelf();
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public void getDatafile(String projectId, DatafileLoader datafileLoader, DatafileLoadedListener loadedListener) {
        String datafileUrl = getDatafileUrl(projectId);
        datafileLoader.getDatafile(datafileUrl, loadedListener);
    }

    public class LocalBinder extends Binder {
        public DatafileService getService() {
            // Return this instance of LocalService so clients can call public methods
            return DatafileService.this;
        }
    }
}
