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

import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.OptlyStorage;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/*
 * Handles intents and bindings in {@link DataFileService}
 */
class DataFileLoader {

    @NonNull private final TaskChain taskChain;
    @NonNull private final Logger logger;

    DataFileLoader(@NonNull TaskChain taskChain, @NonNull Logger logger) {
        this.logger = logger;
        this.taskChain = taskChain;
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    boolean getDataFile(@NonNull String projectId, @Nullable DataFileLoadedListener dataFileLoadedListener) {
        taskChain.start(projectId, dataFileLoadedListener);

        logger.info("Refreshing data file");

        return true;
    }

    static class TaskChain {

        @NonNull private final DataFileService dataFileService;
        @NonNull private final Executor executor;

        TaskChain(@NonNull DataFileService dataFileService) {
            this.dataFileService = dataFileService;
            this.executor = Executors.newSingleThreadExecutor();
        }

        @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
        void start(@NonNull String projectId, @Nullable DataFileLoadedListener dataFileLoadedListener) {
            DataFileClient dataFileClient = new DataFileClient(
                    new Client(new OptlyStorage(dataFileService), LoggerFactory.getLogger(OptlyStorage.class)),
                    LoggerFactory.getLogger(DataFileClient.class));
            DataFileCache dataFileCache = new DataFileCache(
                    projectId,
                    new Cache(dataFileService, LoggerFactory.getLogger(Cache.class)),
                    LoggerFactory.getLogger(DataFileCache.class));
            RequestDataFileFromClientTask requestDataFileFromClientTask =
                    new RequestDataFileFromClientTask(projectId,
                            dataFileService, dataFileCache,
                            dataFileClient,
                            dataFileLoadedListener,
                            LoggerFactory.getLogger(RequestDataFileFromClientTask.class));
            LoadDataFileFromCacheTask loadDataFileFromCacheTask = new LoadDataFileFromCacheTask(dataFileCache, dataFileLoadedListener);

            // Execute tasks in order
            loadDataFileFromCacheTask.executeOnExecutor(executor);
            requestDataFileFromClientTask.executeOnExecutor(executor);
        }
    }

    static class LoadDataFileFromCacheTask extends AsyncTask<Void, Void, JSONObject> {

        @NonNull private final DataFileCache dataFileCache;
        @Nullable private final DataFileLoadedListener dataFileLoadedListener;

        LoadDataFileFromCacheTask(@NonNull DataFileCache dataFileCache,
                                  @Nullable DataFileLoadedListener dataFileLoadedListener) {
            this.dataFileCache = dataFileCache;
            this.dataFileLoadedListener = dataFileLoadedListener;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            return dataFileCache.load();
        }

        @Override
        protected void onPostExecute(JSONObject dataFile) {
            if (dataFile != null) {
                if (dataFileLoadedListener != null) {
                    dataFileLoadedListener.onDataFileLoaded(dataFile.toString());
                }
            }
        }
    }

    static class RequestDataFileFromClientTask extends AsyncTask<Void, Void, String> {

        static final String FORMAT_CDN_URL = "https://cdn.optimizely.com/json/%s.json";
        @NonNull private final String projectId;
        @NonNull private final DataFileService dataFileService;
        @NonNull private final DataFileCache dataFileCache;
        @NonNull private final DataFileClient dataFileClient;
        @NonNull private final Logger logger;
        @Nullable private final DataFileLoadedListener optimizelyStartedListener;

        RequestDataFileFromClientTask(@NonNull String projectId,
                                      @NonNull DataFileService dataFileService,
                                      @NonNull DataFileCache dataFileCache,
                                      @NonNull DataFileClient dataFileClient,
                                      @Nullable DataFileLoadedListener dataFileLoadedListener,
                                      @NonNull Logger logger) {
            this.projectId = projectId;
            this.dataFileService = dataFileService;
            this.dataFileCache = dataFileCache;
            this.dataFileClient = dataFileClient;
            this.optimizelyStartedListener = dataFileLoadedListener;
            this.logger = logger;
        }

        @Override
        protected String doInBackground(Void... params) {
            String dataFile = dataFileClient.request(String.format(FORMAT_CDN_URL, projectId));
            if (dataFile != null) {
                if (dataFileCache.exists()) {
                    if (!dataFileCache.delete()) {
                        logger.warn("Unable to delete old data file");
                        return null; // Unable to delete
                    }
                }
                if (!dataFileCache.save(dataFile)) {
                    logger.warn("Unable to save new data file");
                    return null;
                }
            }

            return dataFile;
        }

        @Override
        protected void onPostExecute(String dataFile) {
            // If dataFile isn't null the dataFile has been modified on the CDN because we are
            // using last-modified and since-last-modified headers.
            if (dataFile != null) {
                if (optimizelyStartedListener != null) {
                    optimizelyStartedListener.onDataFileLoaded(dataFile);
                }
            }

            // We are running in the background so stop the service
            dataFileService.stop();
        }
    }
}
