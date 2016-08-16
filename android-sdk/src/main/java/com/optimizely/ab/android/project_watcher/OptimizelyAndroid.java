package com.optimizely.ab.android.project_watcher;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.android.event_handler.OptlyEventHandler;
import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.ab.bucketing.UserExperimentRecord;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.user_experiment_record.AndroidUserExperimentRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by jdeffibaugh on 7/28/16 for Optimizely.
 * <p/>
 * Handles loading the Optimizely data file
 */
public class OptimizelyAndroid {
    @NonNull private final DataFileServiceConnection dataFileServiceConnection;
    @NonNull private final String projectId;
    @NonNull private final BackgroundWatchersCache backgroundWatchersCache;
    @NonNull private final Application application;
    @NonNull private final ServiceScheduler serviceScheduler;
    @NonNull private final Logger logger;
    @Nullable private OptimizelyStartedListener optimizelyStartedListener;
    @Nullable private AsyncTask<Void,Void,UserExperimentRecord> initUserExperimentRecordTask;
    boolean bound = false;

    OptimizelyAndroid(@NonNull String projectId,
                      @NonNull Application application,
                      @NonNull ServiceScheduler serviceScheduler,
                      @NonNull BackgroundWatchersCache backgroundWatchersCache,
                      @NonNull OptimizelyStartedListener optimizelyStartedListener,
                      @NonNull Logger logger) {
        this.projectId = projectId;
        this.backgroundWatchersCache = backgroundWatchersCache;
        this.optimizelyStartedListener = optimizelyStartedListener;
        this.dataFileServiceConnection = new DataFileServiceConnection(this);
        this.application = application;
        this.serviceScheduler = serviceScheduler;
        this.logger = logger;
    }

    public static OptimizelyAndroid start(@NonNull String projectId, @NonNull Application application, @NonNull OptimizelyStartedListener optimizelyStartedListener) {
        final Logger logger = LoggerFactory.getLogger(OptimizelyAndroid.class);
        Context applicationContext = application.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);
        ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler.PendingIntentFactory(applicationContext);
        ServiceScheduler serviceScheduler = new ServiceScheduler(alarmManager, pendingIntentFactory, LoggerFactory.getLogger(ServiceScheduler.class));

        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(
                new Cache(applicationContext, LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(BackgroundWatchersCache.class));


        OptimizelyAndroid optimizelyAndroid = new OptimizelyAndroid(projectId, application, serviceScheduler, backgroundWatchersCache, optimizelyStartedListener, logger);
        optimizelyAndroid.init();

        return optimizelyAndroid;
    }

    void init() {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
                start();

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {
                stop();

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    @NonNull
    ServiceScheduler getServiceScheduler() {
        return serviceScheduler;
    }

    void start() {
        final Intent intent = new Intent(application.getApplicationContext(), DataFileService.class);
        application.getApplicationContext().bindService(intent, dataFileServiceConnection, Context.BIND_AUTO_CREATE);
    }

    void stop() {
        if (bound) {
            application.getApplicationContext().unbindService(dataFileServiceConnection);
            bound = false;
        }
        if (initUserExperimentRecordTask != null) {
            initUserExperimentRecordTask.cancel(false);
        }
        optimizelyStartedListener = null;
    }

    public void syncDataFile(TimeUnit timeUnit, long interval) {
        serviceScheduler.schedule(getWatchInBackgroundIntent(application.getApplicationContext()), timeUnit.toMillis(interval));
    }

    public void stopSyncingDataFile() {
        serviceScheduler.unschedule(getWatchInBackgroundIntent(application.getApplicationContext()));

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
        if (optimizelyStartedListener != null) {
            injectOptimizely(dataFile);
            logger.info("Sending Optimizely instance to listener");
        } else {
            logger.error("No listener to send Optimizely to");
        }
    }

    @Nullable
    OptimizelyStartedListener getOptimizelyStartedListener() {
        return optimizelyStartedListener;
    }

    void setOptimizelyStartedListener(@Nullable OptimizelyStartedListener optimizelyStartedListener) {
        this.optimizelyStartedListener = optimizelyStartedListener;
    }

    @NonNull
    DataFileServiceConnection getDataFileServiceConnection() {
        return dataFileServiceConnection;
    }

    private void injectOptimizely(final String dataFile) {
        initUserExperimentRecordTask = new AsyncTask<Void, Void, UserExperimentRecord>() {

            @Override
            protected UserExperimentRecord doInBackground(Void[] params) {
                AndroidUserExperimentRecord userExperimentRecord =
                        (AndroidUserExperimentRecord) AndroidUserExperimentRecord.newInstance(application.getApplicationContext());
                userExperimentRecord.start();
                return userExperimentRecord;

            }

            @Override
            protected void onPostExecute(UserExperimentRecord userExperimentRecord) {
                EventHandler eventHandler = OptlyEventHandler.getInstance(application.getApplicationContext());
                Optimizely optimizely = Optimizely.builder(dataFile, eventHandler)
                        .withUserExperimentRecord(userExperimentRecord)
                        .build();
                if (optimizelyStartedListener != null) {
                    optimizelyStartedListener.onOptimizelyStarted(optimizely);
                }
            }
        };
        initUserExperimentRecordTask.execute();
    }

    public static class DataFileServiceConnection implements ServiceConnection {

        @NonNull private final OptimizelyAndroid optlyProjectWatcher;

        DataFileServiceConnection(@NonNull OptimizelyAndroid optlyProjectWatcher) {
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
                dataFileService.getDataFile(optlyProjectWatcher.getProjectId(), dataFileLoader, new DataFileLoadedListener() {
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
