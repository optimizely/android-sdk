package com.optimizely.ab.android.project_watcher;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.OptlyStorage;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jdeffibaugh on 8/1/16 for Optimizely.
 *
 * Handles intents and bindings in {@link DataFileService}
 */
public class DataFileLoader {

    @NonNull private final DataFileService dataFileService;
    @NonNull private final Logger logger;

    public DataFileLoader(@NonNull DataFileService dataFileService, @NonNull Logger logger) {
        this.dataFileService = dataFileService;
        this.logger = logger;
    }

    public void getDataFile(String projectId, @Nullable OnDataFileLoadedListener onDataFileLoadedListener) {
        DataFileClient dataFileClient = new DataFileClient(
                new Client(new OptlyStorage(dataFileService), LoggerFactory.getLogger(OptlyStorage.class)),
                projectId,
                LoggerFactory.getLogger(DataFileClient.class));
        DataFileCache dataFileCache = new DataFileCache(
                new Cache(dataFileService, LoggerFactory.getLogger(Cache.class)),
                projectId,
                LoggerFactory.getLogger(DataFileCache.class));
        RequestDataFileFromClientTask requestDataFileFromClientTask =
                new RequestDataFileFromClientTask(dataFileService, dataFileCache, dataFileClient, onDataFileLoadedListener);
        LoadDataFileFromCacheTask loadDataFileFromCacheTask =
                new LoadDataFileFromCacheTask(dataFileCache, requestDataFileFromClientTask, onDataFileLoadedListener);
        loadDataFileFromCacheTask.execute();

        logger.info("Refreshing data file");
    }

    static class LoadDataFileFromCacheTask extends AsyncTask<Void, Void, JSONObject> {

        @NonNull private final DataFileCache dataFileCache;
        @NonNull private final RequestDataFileFromClientTask requestDataFileFromClientTask;
        @Nullable private final OnDataFileLoadedListener onDataFileLoadedListener;

        LoadDataFileFromCacheTask(@NonNull DataFileCache dataFileCache,
                                  @NonNull RequestDataFileFromClientTask requestDataFileFromClientTask,
                                  @Nullable OnDataFileLoadedListener onDataFileLoadedListener) {
            this.dataFileCache = dataFileCache;
            this.requestDataFileFromClientTask = requestDataFileFromClientTask;
            this.onDataFileLoadedListener = onDataFileLoadedListener;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            return dataFileCache.load();
        }

        @Override
        protected void onPostExecute(JSONObject dataFile) {
            if (dataFile != null) {
                if (onDataFileLoadedListener != null) {
                    onDataFileLoadedListener.onDataFileLoaded(dataFile.toString());
                }
            }

            requestDataFileFromClientTask.execute();
        }
    }

    static class RequestDataFileFromClientTask extends AsyncTask<Void, Void, String> {

        @NonNull private final DataFileService dataFileService;
        @NonNull private final DataFileCache dataFileCache;
        @NonNull private final DataFileClient dataFileClient;
        @Nullable private final OnDataFileLoadedListener onDataFileLoadedListener;

        RequestDataFileFromClientTask(@NonNull DataFileService dataFileService,
                                      @NonNull DataFileCache dataFileCache,
                                      @NonNull DataFileClient dataFileClient,
                                      @Nullable OnDataFileLoadedListener onDataFileLoadedListener) {
            this.dataFileService = dataFileService;
            this.dataFileCache = dataFileCache;
            this.dataFileClient = dataFileClient;
            this.onDataFileLoadedListener = onDataFileLoadedListener;
        }

        @Override
        protected String doInBackground(Void... params) {
            String dataFile = dataFileClient.request();
            if (dataFile != null) {
                dataFileCache.delete(); // Delete the old file first
                dataFileCache.save(dataFile); // save the new file from the CDN
            }

            return dataFile;
        }

        @Override
        protected void onPostExecute(String dataFile) {
            // If dataFile isn't null the dataFile has been modified on the CDN because we are
            // using last-modified and since-last-modified headers.
            if (dataFile != null) {
                if (onDataFileLoadedListener != null) {
                    onDataFileLoadedListener.onDataFileLoaded(dataFile);
                }
            }

            if (!dataFileService.isBound()) {
                // We are running in the background so stop the service
                dataFileService.stopSelf();
            }
        }
    }
}
