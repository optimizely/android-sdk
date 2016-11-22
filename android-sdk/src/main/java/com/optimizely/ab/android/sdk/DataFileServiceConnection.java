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

import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.OptlyStorage;
import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.ab.android.user_experiment_record.AndroidUserExperimentRecord;

import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

class DataFileServiceConnection implements ServiceConnection {

    @NonNull
    private final OptimizelyManager optimizelyManager;
    private boolean bound = false;

    DataFileServiceConnection(@NonNull OptimizelyManager optimizelyManager) {
        this.optimizelyManager = optimizelyManager;
    }

    /**
     * @hide
     * @see ServiceConnection#onServiceConnected(ComponentName, IBinder)
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onServiceConnected(ComponentName className,
                                   IBinder service) {
        // We've bound to DataFileService, cast the IBinder and get DataFileService instance
        DataFileService.LocalBinder binder = (DataFileService.LocalBinder) service;
        final DataFileService dataFileService = binder.getService();
        if (dataFileService != null) {
            DataFileClient dataFileClient = new DataFileClient(
                    new Client(new OptlyStorage(dataFileService.getApplicationContext()), LoggerFactory.getLogger(OptlyStorage.class)),
                    LoggerFactory.getLogger(DataFileClient.class));

            DataFileCache dataFileCache = new DataFileCache(
                    optimizelyManager.getProjectId(),
                    new Cache(dataFileService.getApplicationContext(), LoggerFactory.getLogger(Cache.class)),
                    LoggerFactory.getLogger(DataFileCache.class));

            DataFileLoader dataFileLoader = new DataFileLoader(dataFileService,
                    dataFileClient,
                    dataFileCache,
                    Executors.newSingleThreadExecutor(),
                    LoggerFactory.getLogger(DataFileLoader.class));

            dataFileService.getDataFile(optimizelyManager.getProjectId(), dataFileLoader, new DataFileLoadedListener() {
                @Override
                public void onDataFileLoaded(@Nullable String dataFile) {
                    // App is being used, i.e. in the foreground
                    AlarmManager alarmManager = (AlarmManager) dataFileService.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                    ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler.PendingIntentFactory(dataFileService.getApplicationContext());
                    ServiceScheduler serviceScheduler = new ServiceScheduler(alarmManager, pendingIntentFactory, LoggerFactory.getLogger(ServiceScheduler.class));

                    // If the user provided the datafile upon initialization then we don't want to re-initialize it
                    // instead we just re-use the cached version and the client can be re-instantiate with the fetched
                    // datafile the next time app starts
                    OptimizelyClient optimizelyClient = optimizelyManager.getOptimizely();
                    if (dataFile != null && (optimizelyClient == null || !optimizelyClient.isValid())) {
                        AndroidUserExperimentRecord userExperimentRecord =
                                (AndroidUserExperimentRecord) AndroidUserExperimentRecord.newInstance(optimizelyManager.getProjectId(), dataFileService.getApplicationContext());
                        optimizelyManager.injectOptimizely(dataFileService.getApplicationContext(), userExperimentRecord, serviceScheduler, dataFile);
                    } else {
                        // We should always call the callback even with the dummy
                        // instances.  Devs might gate the rest of their app
                        // based on the loading of Optimizely
                        OptimizelyStartListener optimizelyStartListener = optimizelyManager.getOptimizelyStartListener();
                        if (optimizelyStartListener != null) {
                            optimizelyStartListener.onStart(optimizelyManager.getOptimizely());
                        }
                    }
                }
            });
        }
        bound = true;
    }

    /**
     * @hide
     * @see ServiceConnection#onServiceDisconnected(ComponentName)
     */
    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        bound = false;
    }

    boolean isBound() {
        return bound;
    }

    void setBound(boolean bound) {
        this.bound = bound;
    }
}
