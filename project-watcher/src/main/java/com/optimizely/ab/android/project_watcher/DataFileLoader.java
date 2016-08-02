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

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by jdeffibaugh on 8/1/16 for Optimizely.
 * <p/>
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
                LoggerFactory.getLogger(DataFileClient.class));
        DataFileCache dataFileCache = new DataFileCache(
                projectId,
                new Cache(dataFileService, LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(DataFileCache.class));
        RequestDataFileFromClientTask requestDataFileFromClientTask =
                new RequestDataFileFromClientTask(projectId,
                        dataFileService, dataFileCache,
                        dataFileClient,
                        onDataFileLoadedListener,
                        LoggerFactory.getLogger(RequestDataFileFromClientTask.class));
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

        @NonNull private final String projectId;
        @NonNull private final DataFileService dataFileService;
        @NonNull private final DataFileCache dataFileCache;
        @NonNull private final DataFileClient dataFileClient;
        @NonNull private final Logger logger;
        @Nullable private final OnDataFileLoadedListener onDataFileLoadedListener;

        static final String FORMAT_CDN_URL = "https://cdn.optimizely.com/json/%s.json";

        RequestDataFileFromClientTask(@NonNull String projectId,
                                      @NonNull DataFileService dataFileService,
                                      @NonNull DataFileCache dataFileCache,
                                      @NonNull DataFileClient dataFileClient,
                                      @Nullable OnDataFileLoadedListener onDataFileLoadedListener,
                                      @NonNull Logger logger) {
            this.projectId = projectId;
            this.dataFileService = dataFileService;
            this.dataFileCache = dataFileCache;
            this.dataFileClient = dataFileClient;
            this.onDataFileLoadedListener = onDataFileLoadedListener;
            this.logger = logger;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                String dataFile = dataFileClient.request(new URL(String.format(FORMAT_CDN_URL, projectId)));
                if (dataFile != null) {
                    dataFileCache.delete(); // Delete the old file first
                    dataFileCache.save(dataFile); // save the new file from the CDN
                }

                return dataFile;
            } catch (MalformedURLException e) {
                logger.error("Unable to make data file cdn URL", e);
                return null;
            }
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
