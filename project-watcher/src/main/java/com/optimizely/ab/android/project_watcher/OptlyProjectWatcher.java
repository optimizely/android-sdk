package com.optimizely.ab.android.project_watcher;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.ServiceScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by jdeffibaugh on 7/28/16 for Optimizely.
 * <p/>
 * Handles loading the Optimizely data file
 */
public class OptlyProjectWatcher implements ProjectWatcher {
    @NonNull private final DataFileServiceConnection dataFileServiceConnection;
    @NonNull private final String projectId;
    @NonNull private final BackgroundWatchersCache backgroundWatchersCache;
    @NonNull private final Context applicationContext;
    @NonNull private final ServiceScheduler serviceScheduler;
    @NonNull private final Logger logger;
    boolean bound = false;
    @Nullable private OnDataFileLoadedListener onDataFileLoadedListener;

    OptlyProjectWatcher(@NonNull String projectId,
                        @NonNull Context applicationContext,
                        @NonNull ServiceScheduler serviceScheduler,
                        @NonNull BackgroundWatchersCache backgroundWatchersCache,
                        @NonNull Logger logger) {
        this.projectId = projectId;
        this.backgroundWatchersCache = backgroundWatchersCache;
        this.dataFileServiceConnection = new DataFileServiceConnection(this);
        this.applicationContext = applicationContext;
        this.serviceScheduler = serviceScheduler;
        this.logger = logger;
    }

    public static ProjectWatcher getInstance(@NonNull String projectId, @NonNull Context applicationContext) {
        Logger logger = LoggerFactory.getLogger(OptlyProjectWatcher.class);
        if (applicationContext instanceof Activity || applicationContext instanceof Service) {
            logger.warn("Project watcher expects application context not service or activity context");
        }
        AlarmManager alarmManager = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);
        ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler.PendingIntentFactory(applicationContext);
        ServiceScheduler serviceScheduler = new ServiceScheduler(alarmManager, pendingIntentFactory, LoggerFactory.getLogger(ServiceScheduler.class));

        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(
                new Cache(applicationContext, LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(BackgroundWatchersCache.class));

        return new OptlyProjectWatcher(projectId, applicationContext, serviceScheduler, backgroundWatchersCache, logger);
    }

    @NonNull
    ServiceScheduler getServiceScheduler() {
        return serviceScheduler;
    }

    @Override
    public void loadDataFile(OnDataFileLoadedListener onDataFileLoadedListener) {
        this.onDataFileLoadedListener = onDataFileLoadedListener;
        final Intent intent = new Intent(applicationContext, DataFileService.class);
        applicationContext.bindService(intent, dataFileServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void cancelDataFileLoad() {
        if (bound) {
            applicationContext.unbindService(dataFileServiceConnection);
            bound = false;
        }
        onDataFileLoadedListener = null;
    }

    @Override
    public void startWatching(TimeUnit timeUnit, long interval) {
        serviceScheduler.schedule(getWatchInBackgroundIntent(applicationContext), timeUnit.toMillis(interval));
    }

    @Override
    public void stopWatching() {
        serviceScheduler.unschedule(getWatchInBackgroundIntent(applicationContext));

        backgroundWatchersCache.setIsWatching(projectId, false);
    }

    Intent getWatchInBackgroundIntent(Context context) {
        Intent intent = new Intent(context, DataFileService.class);
        intent.putExtra(DataFileService.EXTRA_PROJECT_ID, projectId);
        return intent;
    }

    public boolean isBound() {
        return bound;
    }

    void setBound(boolean bound) {
        this.bound = bound;
    }

    @NonNull
    public String getProjectId() {
        return projectId;
    }

    void notifyListener(String dataFile) {
        if (onDataFileLoadedListener != null) {
            onDataFileLoadedListener.onDataFileLoaded(dataFile);
            logger.info("Notifying listener of new data file");
        } else {
            logger.error("Tried to notify null listener");
        }
    }

    @Nullable
    OnDataFileLoadedListener getOnDataFileLoadedListener() {
        return onDataFileLoadedListener;
    }

    void setOnDataFileLoadedListener(OnDataFileLoadedListener onDataFileLoadedListener) {
        this.onDataFileLoadedListener = onDataFileLoadedListener;
    }

    @NonNull
    DataFileServiceConnection getDataFileServiceConnection() {
        return dataFileServiceConnection;
    }

    public static class DataFileServiceConnection implements ServiceConnection {

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
                DataFileLoader dataFileLoader = new DataFileLoader(new DataFileLoader.TaskChain(dataFileService), LoggerFactory.getLogger(DataFileLoader.class));
                dataFileService.getDataFile(optlyProjectWatcher.getProjectId(), dataFileLoader, new OnDataFileLoadedListener() {
                    @Override
                    public void onDataFileLoaded(String dataFile) {
                        notifyProjectWatcher(dataFile);
                    }
                });
            }
            optlyProjectWatcher.setBound(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            optlyProjectWatcher.setBound(false);
        }

        public void notifyProjectWatcher(String dataFile) {
            if (optlyProjectWatcher.isBound()) {
                optlyProjectWatcher.notifyListener(dataFile);
            }
        }
    }
}
