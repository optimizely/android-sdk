package com.optimizely.android;

import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.optimizely.ab.config.ProjectConfig;

import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by jdeffibaugh on 7/28/16 for Optimizely.
 *
 * Handles loading the Optimizely data file
 */
public class OptlyProjectWatcher implements ProjectWatcher {

    @Nullable private OnDataFileLoadedListener onDataFileLoadedListener;
    @NonNull private final DataFileServiceConnection dataFileServiceConnection;
    boolean bound = false;
    @NonNull private final String projectId;

    public static ProjectWatcher getInstance(@NonNull String projectId) {
        return new OptlyProjectWatcher(projectId);
    }

    private OptlyProjectWatcher(@NonNull String projectId) {
        this.projectId = projectId;
        this.dataFileServiceConnection = new DataFileServiceConnection(this);
    }

    @Override
    public void startWatching(Context context, OnDataFileLoadedListener onDataFileLoadedListener) {
        this.onDataFileLoadedListener = onDataFileLoadedListener;
        final Intent intent = new Intent(context, DataFileService.class);
        context.bindService(intent, dataFileServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void stopWatching(Context context) {
        if (bound) {
            context.unbindService(dataFileServiceConnection);
            bound = false;
        }
        onDataFileLoadedListener = null;
    }

    @Override
    public void startWatchingInBackground(Context context, TimeUnit timeUnit, long interval) {
        getServiceScheduler(context).schedule(getWatchInBackgroundIntent(context), timeUnit.toMillis(interval));
    }

    @Override
    public void stopWatchingInBackground(Context context) {
        getServiceScheduler(context).unschedule(getWatchInBackgroundIntent(context));
        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(context, LoggerFactory.getLogger(BackgroundWatchersCache.class));
        backgroundWatchersCache.setIsWatching(projectId, false);
    }

    Intent getWatchInBackgroundIntent(Context context) {
        Intent intent = new Intent(context, DataFileService.class);
        intent.putExtra(DataFileService.EXTRA_PROJECT_ID, projectId);
        return intent;
    }

    ServiceScheduler getServiceScheduler(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler.PendingIntentFactory(context);
        return new ServiceScheduler(alarmManager, pendingIntentFactory, LoggerFactory.getLogger(ServiceScheduler.class));
    }

    void setBound(boolean bound) {
        this.bound = bound;
    }

    boolean isBound() {
        return bound;
    }

    @NonNull
    String getProjectId() {
        return projectId;
    }

    void notifyListener(String dataFile) {
        if (onDataFileLoadedListener != null) {
            onDataFileLoadedListener.onDataFileLoaded(dataFile);
        }
    }

    static class DataFileServiceConnection implements ServiceConnection {

        @NonNull private final OptlyProjectWatcher optlyProjectWatcher;

        DataFileServiceConnection(@NonNull OptlyProjectWatcher optlyProjectWatcher) {
            this.optlyProjectWatcher = optlyProjectWatcher;
        }

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to DataFileService, cast the IBinder and get DataFileService instance
            DataFileService.LocalBinder binder = (DataFileService.LocalBinder) service;
            DataFileService dataFileService = binder.getService();
            if (dataFileService != null) {
                dataFileService.getDataFile(optlyProjectWatcher.getProjectId(), new OnDataFileLoadedListener() {
                    @Override
                    public void onDataFileLoaded(String dataFile) {
                        if (optlyProjectWatcher.isBound()) {
                            optlyProjectWatcher.notifyListener(dataFile);
                        }
                    }
                });
            }
            optlyProjectWatcher.setBound(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            optlyProjectWatcher.setBound(false);
        }
    }
}
