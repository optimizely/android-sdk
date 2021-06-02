/****************************************************************************
 * Copyright 2016-2017,2021, Optimizely, Inc. and contributors              *
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
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.android.shared.OptlyStorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that handles loading the datafile from cache or downloads it from the CDN
 * These services will only be used if you are using our {@link DefaultDatafileHandler}.
 * You can chose to implement your own handler and use all or part of this package.
 */
@Deprecated
public class DatafileService extends Service {
    /**
     * Extra containing the project id this instance of Optimizely was built with
     */
    public static final String EXTRA_DATAFILE_CONFIG = "com.optimizely.ab.android.EXTRA_DATAFILE_CONFIG";
    public static final Integer JOB_ID = 2113;

    @NonNull private final IBinder binder = new LocalBinder();
    Logger logger = LoggerFactory.getLogger(DatafileService.class);
    private boolean isBound;

    /**
     * @hide
     * @see Service#onStartCommand(Intent, int, int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.hasExtra(EXTRA_DATAFILE_CONFIG)) {
                String extraDatafileConfig = intent.getStringExtra(EXTRA_DATAFILE_CONFIG);
                DatafileConfig datafileConfig = DatafileConfig.fromJSONString(extraDatafileConfig);
                DatafileClient datafileClient = new DatafileClient(
                        new Client(new OptlyStorage(this.getApplicationContext()), LoggerFactory.getLogger(OptlyStorage.class)),
                        LoggerFactory.getLogger(DatafileClient.class));
                DatafileCache datafileCache = new DatafileCache(
                        datafileConfig.getKey(),
                        new Cache(this.getApplicationContext(), LoggerFactory.getLogger(Cache.class)),
                        LoggerFactory.getLogger(DatafileCache.class));

                String datafileUrl = datafileConfig.getUrl();
                DatafileLoader datafileLoader = new DatafileLoader(this, datafileClient, datafileCache, LoggerFactory.getLogger(DatafileLoader.class));
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

    public void stop() {
        stopSelf();
    }

    public void getDatafile(String datafileUrl, DatafileLoader datafileLoader, DatafileLoadedListener loadedListener) {
        datafileLoader.getDatafile(datafileUrl, loadedListener);
    }

    public class LocalBinder extends Binder {
        public DatafileService getService() {
            // Return this instance of LocalService so clients can call public methods
            return DatafileService.this;
        }
    }
}
