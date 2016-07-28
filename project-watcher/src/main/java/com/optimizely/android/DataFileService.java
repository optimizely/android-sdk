package com.optimizely.android;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class DataFileService extends Service {
    @NonNull private final IBinder binder = new LocalBinder();
    @Nullable private DataFileClient dataFileClient;
    @Nullable private DataFileCache dataFileCache;

    @Override
    public void onCreate() {
        dataFileClient = new DataFileClient();
        dataFileCache = new DataFileCache(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void getDataFile(OnDataFileLoadedListener onDataFileLoadedListener) {
        if (dataFileCache != null && dataFileClient != null) {
            RequestDataFileFromClientTask requestDataFileFromClientTask =
                    new RequestDataFileFromClientTask(dataFileCache, dataFileClient, onDataFileLoadedListener);
            LoadDataFileFromCacheTask loadDataFileFromCacheTask =
                    new LoadDataFileFromCacheTask(dataFileCache, requestDataFileFromClientTask, onDataFileLoadedListener);
            loadDataFileFromCacheTask.execute();
        }
    }

    static class LoadDataFileFromCacheTask extends AsyncTask<Void, Void, String> {

        @NonNull private final DataFileCache dataFileCache;
        @NonNull private final RequestDataFileFromClientTask requestDataFileFromClientTask;
        private OnDataFileLoadedListener onDataFileLoadedListener;

        LoadDataFileFromCacheTask(@NonNull DataFileCache dataFileCache,
                                  @NonNull RequestDataFileFromClientTask requestDataFileFromClientTask,
                                  @NonNull OnDataFileLoadedListener onDataFileLoadedListener) {
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
                onDataFileLoadedListener.onDataFileLoaded(dataFile);
            }

            requestDataFileFromClientTask.execute();
        }
    }

    static class RequestDataFileFromClientTask extends AsyncTask<Void, Void, String> {

        @NonNull private final DataFileCache dataFileCache;
        @NonNull private final DataFileClient dataFileClient;
        private OnDataFileLoadedListener onDataFileLoadedListener;

        RequestDataFileFromClientTask(@NonNull DataFileCache dataFileCache,
                                      @NonNull DataFileClient dataFileClient,
                                      @NonNull OnDataFileLoadedListener onDataFileLoadedListener) {
            this.dataFileCache = dataFileCache;
            this.dataFileClient = dataFileClient;
            this.onDataFileLoadedListener = onDataFileLoadedListener;
        }

        @Override
        protected String doInBackground(Void... params) {
            String dataFile = dataFileClient.request();
            if (dataFile != null) {
                dataFileCache.save(dataFile);
            }

            return dataFile;
        }

        @Override
        protected void onPostExecute(String dataFile) {
            // If dataFile isn't null the dataFile has been modified on the CDN because we are
            // using last-modified and since-last-modified headers.
            if (dataFile != null) {
                onDataFileLoadedListener.onDataFileLoaded(dataFile);
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
