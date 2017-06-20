package com.optimizely.ab.android.datafile_handler;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.DataFileCache;
import com.optimizely.ab.android.shared.DataFileLoadedListener;
import com.optimizely.ab.android.shared.OptlyStorage;

import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

public class DataFileServiceConnection implements ServiceConnection {

    @NonNull private final Context context;
    @NonNull private final String projectId;
    @NonNull private final DataFileLoadedListener listener;

    private boolean bound = false;

    public DataFileServiceConnection(@NonNull String projectId, @NonNull Context context, @NonNull DataFileLoadedListener listener) {
        this.projectId = projectId;
        this.context = context;
        this.listener = listener;
    }

    /**
     * @hide
     * @see ServiceConnection#onServiceConnected(ComponentName, IBinder)
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onServiceConnected(ComponentName className,
                                   IBinder service) {
        if (!(service instanceof DataFileService.LocalBinder)) {
            return;
        }

        // We've bound to DataFileService, cast the IBinder and get DataFileService instance
        DataFileService.LocalBinder binder = (DataFileService.LocalBinder) service;
        final DataFileService dataFileService = binder.getService();
        if (dataFileService != null) {
            DataFileClient dataFileClient = new DataFileClient(
                    new Client(new OptlyStorage(dataFileService.getApplicationContext()),
                            LoggerFactory.getLogger(OptlyStorage.class)),
                    LoggerFactory.getLogger(DataFileClient.class));

            DataFileCache dataFileCache = new DataFileCache(
                    projectId,
                    new Cache(dataFileService.getApplicationContext(), LoggerFactory.getLogger(Cache.class)),
                    LoggerFactory.getLogger(DataFileCache.class));

            DataFileLoader dataFileLoader = new DataFileLoader(dataFileService,
                    dataFileClient,
                    dataFileCache,
                    Executors.newSingleThreadExecutor(),
                    LoggerFactory.getLogger(DataFileLoader.class));

            dataFileService.getDataFile(projectId, dataFileLoader, listener);
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
       listener.onStop(context);
    }

    public boolean isBound() {
        return bound;
    }

    public void setBound(Boolean bound) {
        this.bound = bound;
    }
}
