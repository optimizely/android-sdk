/****************************************************************************
 * Copyright 2017,2021, Optimizely, Inc. and contributors                   *
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

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.android.shared.OptlyStorage;

import org.slf4j.LoggerFactory;

/**
 * The DatafileServiceConnection is used to bind to a DatafileService.  The DatafileService does that actual download.
 * The Service Connection kicks off the service after being connected.  The connection is unbound after a successful download.
 */
@Deprecated
public class DatafileServiceConnection implements ServiceConnection {

    @NonNull private final Context context;
    @NonNull private final DatafileConfig datafileConfig;
    @NonNull private final DatafileLoadedListener listener;

    private boolean bound = false;

    /**
     * Create a datafile service connection object.
     * @param datafileConfig for this datafile.
     * @param context current application context.
     * @param listener listener to call after service download has completed.
     */
    public DatafileServiceConnection(@NonNull DatafileConfig datafileConfig, @NonNull Context context, @NonNull DatafileLoadedListener listener) {
        this.datafileConfig = datafileConfig;
        this.context = context;
        this.listener = listener;
    }

    /**
     * Get the bound {@link DatafileService} and set it up for download.
     *
     * @see ServiceConnection#onServiceConnected(ComponentName, IBinder)
     */
    @Override
    public void onServiceConnected(ComponentName className,
                                   IBinder service) {
        if (!(service instanceof DatafileService.LocalBinder)) {
            return;
        }

        // We've bound to DatafileService, cast the IBinder and get DatafileService instance
        DatafileService.LocalBinder binder = (DatafileService.LocalBinder) service;
        final DatafileService datafileService = binder.getService();
        if (datafileService != null) {
            DatafileClient datafileClient = new DatafileClient(
                    new Client(new OptlyStorage(context.getApplicationContext()),
                            LoggerFactory.getLogger(OptlyStorage.class)),
                    LoggerFactory.getLogger(DatafileClient.class));

            DatafileCache datafileCache = new DatafileCache(
                    datafileConfig.getKey(),
                    new Cache(context.getApplicationContext(), LoggerFactory.getLogger(Cache.class)),
                    LoggerFactory.getLogger(DatafileCache.class));

            DatafileLoader datafileLoader = new DatafileLoader(
                    context,
                    datafileClient,
                    datafileCache,
                    LoggerFactory.getLogger(DatafileLoader.class));

            datafileService.getDatafile(datafileConfig.getUrl(), datafileLoader, listener);
        }
        bound = true;
    }

    /**
     * Call stop on the listener after the service has been disconnected.
     * @see ServiceConnection#onServiceDisconnected(ComponentName)
     */
    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        bound = false;
    }

    /**
     * Is the service bound?
     * @return true if it is bound.
     */
    public boolean isBound() {
        return bound;
    }

    /**
     * Set whether the service is bound or not.
     * @param bound boolean flag.
     */
    public void setBound(Boolean bound) {
        this.bound = bound;
    }
}
