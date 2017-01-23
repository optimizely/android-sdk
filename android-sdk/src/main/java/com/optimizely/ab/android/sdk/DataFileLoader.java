/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
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

package com.optimizely.ab.android.sdk;

import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.concurrent.Executor;

/**
 * Handles intents and bindings in {@link DataFileService}
 */
class DataFileLoader {

    @NonNull private final DataFileService dataFileService;
    @NonNull private final Executor executor;
    @NonNull private final Logger logger;
    @NonNull private final DataFileCache dataFileCache;
    @NonNull private final DataFileClient dataFileClient;

    private boolean hasNotifiedListener = false;

    DataFileLoader(@NonNull DataFileService dataFileService,
                   @NonNull DataFileClient dataFileClient,
                   @NonNull DataFileCache dataFileCache,
                   @NonNull Executor executor,
                   @NonNull Logger logger) {
        this.logger = logger;
        this.dataFileService = dataFileService;
        this.dataFileClient = dataFileClient;
        this.dataFileCache = dataFileCache;
        this.executor = executor;
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    void getDataFile(@NonNull String datafileUrl, @Nullable DataFileLoadedListener dataFileLoadedListener) {
        RequestDataFileFromClientTask requestDataFileFromClientTask =
                new RequestDataFileFromClientTask(datafileUrl,
                        dataFileService,
                        dataFileCache,
                        dataFileClient,
                        this,
                        dataFileLoadedListener,
                        logger);
        LoadDataFileFromCacheTask loadDataFileFromCacheTask =
                new LoadDataFileFromCacheTask(dataFileCache,
                        this,
                        dataFileLoadedListener);

        // Execute tasks in order
        loadDataFileFromCacheTask.executeOnExecutor(executor);
        requestDataFileFromClientTask.executeOnExecutor(executor);
        logger.info("Refreshing data file");
    }

    private void notify(@Nullable DataFileLoadedListener dataFileLoadedListener, @Nullable String dataFile) {
        // The listener should be notified ONCE and ONLY ONCE with a valid datafile or null
        // If there are no activities bound there is no need to notify
        if (dataFileLoadedListener != null && dataFileService.isBound() && !hasNotifiedListener) {
            dataFileLoadedListener.onDataFileLoaded(dataFile);
            this.hasNotifiedListener = true;
        }
    }

    private static class LoadDataFileFromCacheTask extends AsyncTask<Void, Void, JSONObject> {

        @NonNull private final DataFileCache dataFileCache;
        @NonNull private final DataFileLoader dataFileLoader;
        @Nullable private final DataFileLoadedListener dataFileLoadedListener;

        LoadDataFileFromCacheTask(@NonNull DataFileCache dataFileCache,
                                  @NonNull DataFileLoader dataFileLoader,
                                  @Nullable DataFileLoadedListener dataFileLoadedListner) {
            this.dataFileCache = dataFileCache;
            this.dataFileLoader = dataFileLoader;
            this.dataFileLoadedListener = dataFileLoadedListner;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            return dataFileCache.load();
        }

        @Override
        protected void onPostExecute(JSONObject dataFile) {
            if (dataFile != null) {
                dataFileLoader.notify(dataFileLoadedListener, dataFile.toString());
            }
        }
    }

    private static class RequestDataFileFromClientTask extends AsyncTask<Void, Void, String> {

        @NonNull private final String datafileUrl;
        @NonNull private final DataFileService dataFileService;
        @NonNull private final DataFileCache dataFileCache;
        @NonNull private final DataFileClient dataFileClient;
        @NonNull private final DataFileLoader dataFileLoader;
        @NonNull private final Logger logger;
        @Nullable private final DataFileLoadedListener dataFileLoadedListener;

        RequestDataFileFromClientTask(@NonNull String datafileUrl,
                                      @NonNull DataFileService dataFileService,
                                      @NonNull DataFileCache dataFileCache,
                                      @NonNull DataFileClient dataFileClient,
                                      @NonNull DataFileLoader dataFileLoader,
                                      @Nullable DataFileLoadedListener dataFileLoadedListener,
                                      @NonNull Logger logger) {
            this.datafileUrl = datafileUrl;
            this.dataFileService = dataFileService;
            this.dataFileCache = dataFileCache;
            this.dataFileClient = dataFileClient;
            this.dataFileLoader = dataFileLoader;
            this.dataFileLoadedListener = dataFileLoadedListener;
            this.logger = logger;
        }

        @Override
        protected String doInBackground(Void... params) {
            String dataFile = dataFileClient.request(datafileUrl);
            if (dataFile != null && !dataFile.isEmpty()) {
                if (dataFileCache.exists()) {
                    if (!dataFileCache.delete()) {
                        logger.warn("Unable to delete old datafile");
                    }
                }
                if (!dataFileCache.save(dataFile)) {
                    logger.warn("Unable to save new datafile");
                }
            }

            return dataFile;
        }

        @Override
        protected void onPostExecute(@Nullable String dataFile) {
            // Only send null or a real datafile
            // If the datafile is empty it means we got a 304
            // We should have already sent the local datafile in this case
            if (dataFile == null || !dataFile.isEmpty()) {
                dataFileLoader.notify(dataFileLoadedListener, dataFile);
            }
            dataFileService.stop();
        }
    }
}
