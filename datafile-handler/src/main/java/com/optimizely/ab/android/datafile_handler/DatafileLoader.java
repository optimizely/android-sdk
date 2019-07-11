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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.android.shared.OptlyStorage;

import org.slf4j.Logger;

import java.util.concurrent.Executor;

/**
 * Handles intents and bindings in {@link DatafileService}
 */
public class DatafileLoader {

    @NonNull private final DatafileCache datafileCache;
    @NonNull private final DatafileClient datafileClient;
    @NonNull private final DatafileService datafileService;
    @NonNull private final Executor executor;
    @NonNull private final Logger logger;

    private boolean hasNotifiedListener = false;

    public DatafileLoader(@NonNull DatafileService datafileService,
                          @NonNull DatafileClient datafileClient,
                          @NonNull DatafileCache datafileCache,
                          @NonNull Executor executor,
                          @NonNull Logger logger) {
        this.logger = logger;
        this.datafileService = datafileService;
        this.datafileClient = datafileClient;
        this.datafileCache = datafileCache;
        this.executor = executor;

        new DatafileServiceConnection(new DatafileConfig("projectId", (String)null), datafileService.getApplicationContext(), new DatafileLoadedListener() {
            public void onDatafileLoaded(@Nullable String dataFile) {}
            public void onStop(Context context) {}
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public void getDatafile(@NonNull String datafileUrl, @Nullable DatafileLoadedListener datafileLoadedListener) {
        RequestDatafileFromClientTask requestDatafileFromClientTask =
                new RequestDatafileFromClientTask(datafileUrl,
                        datafileService,
                        datafileCache,
                        datafileClient,
                        this,
                        datafileLoadedListener,
                        logger);
        // Execute tasks in order
        requestDatafileFromClientTask.executeOnExecutor(executor);
        logger.info("Refreshing data file");
    }

    private void notify(@Nullable DatafileLoadedListener datafileLoadedListener, @Nullable String dataFile) {
        // The listener should be notified ONCE and ONLY ONCE with a valid datafile or null
        // If there are no activities bound there is no need to notify
        if (datafileLoadedListener != null && datafileService.isBound() && !hasNotifiedListener) {
            datafileLoadedListener.onDatafileLoaded(dataFile);
            this.hasNotifiedListener = true;
        }
    }
    private static class RequestDatafileFromClientTask extends AsyncTask<Void, Void, String> {

        @NonNull private final String datafileUrl;
        @NonNull private final DatafileService datafileService;
        @NonNull private final DatafileCache datafileCache;
        @NonNull private final DatafileClient datafileClient;
        @NonNull private final DatafileLoader datafileLoader;
        @NonNull private final Logger logger;
        @Nullable private final DatafileLoadedListener datafileLoadedListener;

        RequestDatafileFromClientTask(@NonNull String datafileUrl,
                                      @NonNull DatafileService datafileService,
                                      @NonNull DatafileCache datafileCache,
                                      @NonNull DatafileClient datafileClient,
                                      @NonNull DatafileLoader datafileLoader,
                                      @Nullable DatafileLoadedListener datafileLoadedListener,
                                      @NonNull Logger logger) {
            this.datafileUrl = datafileUrl;
            this.datafileService = datafileService;
            this.datafileCache = datafileCache;
            this.datafileClient = datafileClient;
            this.datafileLoader = datafileLoader;
            this.datafileLoadedListener = datafileLoadedListener;
            this.logger = logger;
        }

        @Override
        protected String doInBackground(Void... params) {

            // if there is a problem with the cached datafile, set last modified to 1970
            if (!datafileCache.exists() || (datafileCache.exists() && datafileCache.load() == null)) {
                // create a wrapper for application context default storage.
                OptlyStorage storage = new OptlyStorage(this.datafileService.getApplicationContext());
                // set the last modified for this url to 1 millisecond past Jan 1, 1970.
                storage.saveLong(datafileUrl, 1);
            }
            String dataFile = datafileClient.request(datafileUrl);
            if (dataFile != null && !dataFile.isEmpty()) {
                if (datafileCache.exists()) {
                    if (!datafileCache.delete()) {
                        logger.warn("Unable to delete old datafile");
                    }
                }
                if (!datafileCache.save(dataFile)) {
                    logger.warn("Unable to save new datafile");
                }
            }

            return dataFile;
        }

        @Override
        protected void onPostExecute(@Nullable String dataFile) {
            // Only send null or a real datafile
            // If the datafile is empty it means we got a 304
            // We are notifying datafileLoader in either case
            // if empty or null than it should be handled in datafileLoader listener to get from cache or Raw resource
            datafileLoader.notify(datafileLoadedListener, dataFile);
            datafileService.stop();
        }
    }
}
