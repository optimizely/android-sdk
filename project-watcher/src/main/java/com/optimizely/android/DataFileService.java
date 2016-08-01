package com.optimizely.android;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataFileService extends Service {
    @NonNull private final IBinder binder = new LocalBinder();

    Logger logger = LoggerFactory.getLogger(getClass());
    public static String EXTRA_PROJECT_ID = "com.optimizely.android.EXTRA_PROJECT_ID";

    private boolean isBound;

    @Override
    public IBinder onBind(Intent intent) {
        isBound = true;
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        isBound = false;
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.hasExtra(EXTRA_PROJECT_ID)) {
                String projectId = intent.getStringExtra(EXTRA_PROJECT_ID);
                getDataFile(projectId, null);
                BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(this, LoggerFactory.getLogger(BackgroundWatchersCache.class));
                backgroundWatchersCache.setIsWatching(projectId, true);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    boolean isBound() {
        return  isBound;
    }

    public void getDataFile(String projectId, @Nullable OnDataFileLoadedListener onDataFileLoadedListener) {
        DataFileClient dataFileClient = new DataFileClient(projectId, new OptlyStorage(this), LoggerFactory.getLogger(DataFileClient.class));
        DataFileCache dataFileCache = new DataFileCache(this, projectId, LoggerFactory.getLogger(DataFileCache.class));
        RequestDataFileFromClientTask requestDataFileFromClientTask =
                new RequestDataFileFromClientTask(this, dataFileCache, dataFileClient, onDataFileLoadedListener);
        LoadDataFileFromCacheTask loadDataFileFromCacheTask =
                new LoadDataFileFromCacheTask(dataFileCache, requestDataFileFromClientTask, onDataFileLoadedListener);
        loadDataFileFromCacheTask.execute();

        logger.info("Refreshing data file");
    }

    static class LoadDataFileFromCacheTask extends AsyncTask<Void, Void, String> {

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
        protected String doInBackground(Void... params) {
            return dataFileCache.load();
        }

        @Override
        protected void onPostExecute(String dataFile) {
            if (dataFile != null) {
                if (onDataFileLoadedListener != null) {
                    onDataFileLoadedListener.onDataFileLoaded(dataFile);
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

    public class LocalBinder extends Binder {
        DataFileService getService() {
            // Return this instance of LocalService so clients can call public methods
            return DataFileService.this;
        }
    }
}
