package com.optimizely.ab.android.sdk;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Service;
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
import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.ab.bucketing.UserExperimentRecord;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.user_experiment_record.AndroidUserExperimentRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by jdeffibaugh on 7/28/16 for Optimizely.
 * <p/>
 * Handles loading the Optimizely data file
 */
public class OptimizelySDK {
    @NonNull private final String projectId;
    @NonNull private final Long eventHandlerDispatchInterval;
    @NonNull private final TimeUnit eventHandlerDispatchIntervalTimeUnit;
    @NonNull private final Long dataFileDownloadInterval;
    @NonNull private final TimeUnit dataFileDownloadIntervalTimeUnit;
    @NonNull private final Executor executor;
    @NonNull private final Logger logger;
    @Nullable private DataFileServiceConnection dataFileServiceConnection;
    @Nullable private OptimizelyStartListener optimizelyStartListener;
    @Nullable private OptlyEventHandler eventHandler;
    @Nullable private AndroidUserExperimentRecord androidUserExperimentRecord;

    @Nullable private String dataFile;

    OptimizelySDK(@NonNull String projectId,
                  @NonNull Long eventHandlerDispatchInterval,
                  @NonNull TimeUnit eventHandlerDispatchIntervalTimeUnit,
                  @NonNull Long dataFileDownloadInterval,
                  @NonNull TimeUnit dataFileDownloadIntervalTimeUnit,
                  @NonNull Executor executor,
                  @NonNull Logger logger) {
        this.projectId = projectId;
        this.eventHandlerDispatchInterval = eventHandlerDispatchInterval;
        this.eventHandlerDispatchIntervalTimeUnit = eventHandlerDispatchIntervalTimeUnit;
        this.dataFileDownloadInterval = dataFileDownloadInterval;
        this.dataFileDownloadIntervalTimeUnit = dataFileDownloadIntervalTimeUnit;
        this.executor = executor;
        this.logger = logger;
    }

    @NonNull
    public static Builder builder(@NonNull String projectId) {
        return new Builder(projectId);
    }

    @Nullable
    public DataFileServiceConnection getDataFileServiceConnection() {
        return dataFileServiceConnection;
    }

    public void setDataFileServiceConnection(@Nullable DataFileServiceConnection dataFileServiceConnection) {
        this.dataFileServiceConnection = dataFileServiceConnection;
    }

    @Nullable
    public OptimizelyStartListener getOptimizelyStartListener() {
        return optimizelyStartListener;
    }

    public void setOptimizelyStartListener(@Nullable OptimizelyStartListener optimizelyStartListener) {
        this.optimizelyStartListener = optimizelyStartListener;
    }

    public void start(@NonNull Activity activity, @NonNull OptimizelyStartListener optimizelyStartListener) {
        start(activity.getApplication(), new OptlyActivityLifecycleCallbacks(this), optimizelyStartListener);
    }

    public void start(@NonNull Service service, @NonNull OptimizelyStartListener optimizelyStartListener) {
        start(service.getApplication(), null, optimizelyStartListener);
    }

    public void start(@NonNull Application application, @Nullable OptlyActivityLifecycleCallbacks lifeCycleCallbacks, @NonNull OptimizelyStartListener optimizelyStartListener) {
        this.optimizelyStartListener = optimizelyStartListener;
        this.dataFileServiceConnection = new DataFileServiceConnection(this);

        final Intent intent = new Intent(application.getApplicationContext(), DataFileService.class);
        application.getApplicationContext().bindService(intent, dataFileServiceConnection, Context.BIND_AUTO_CREATE);

        if (lifeCycleCallbacks != null) {
            application.registerActivityLifecycleCallbacks(lifeCycleCallbacks);
        }
    }

    public void stop(@NonNull Service service) {
        stop(service.getApplication(), null);
    }

    public void stop(@NonNull Activity activity, @NonNull OptlyActivityLifecycleCallbacks optlyActivityLifecycleCallbacks) {
        stop(activity.getApplication(), optlyActivityLifecycleCallbacks);
    }

    public void stop(@NonNull Application application, @Nullable OptlyActivityLifecycleCallbacks optlyActivityLifecycleCallbacks) {
        this.optimizelyStartListener = null;

        if (dataFileServiceConnection != null && dataFileServiceConnection.isBound()) {
            application.getApplicationContext().unbindService(dataFileServiceConnection);
        }

        if (optlyActivityLifecycleCallbacks != null) {
            application.unregisterActivityLifecycleCallbacks(optlyActivityLifecycleCallbacks);
        }
    }

    @Nullable
    public ParcelableOptimizely getParcelableOptimizely() {
        if (dataFile != null) {
            return new ParcelableOptimizely(dataFile);
        } else {
            logger.warn("Tried to get parcelable Optimizely before the datafile has been loaded");
            return null;
        }
    }

    public Optimizely getOptimizely(ParcelableOptimizely parcelableOptimizely) {
        return parcelableOptimizely.unParcel(this);
    }

    @NonNull
    public String getProjectId() {
        return projectId;
    }

    @Nullable
    public EventHandler getEventHandler() {
        return eventHandler;
    }

    @Nullable
    public UserExperimentRecord getUserExperimentRecord() {
        return androidUserExperimentRecord;
    }

    public void setUserExperimentRecord(AndroidUserExperimentRecord androidUserExperimentRecord) {
        this.androidUserExperimentRecord = androidUserExperimentRecord;
    }

    public void injectOptimizely(@NonNull final Context context, final @NonNull AndroidUserExperimentRecord userExperimentRecord, @NonNull final ServiceScheduler serviceScheduler, @NonNull final String dataFile) {
        this.dataFile = dataFile;
        AsyncTask<Void, Void, UserExperimentRecord> initUserExperimentRecordTask = new AsyncTask<Void, Void, UserExperimentRecord>() {
            @Override
            protected UserExperimentRecord doInBackground(Void[] params) {
                userExperimentRecord.start();
                return userExperimentRecord;
            }

            @Override
            protected void onPostExecute(UserExperimentRecord userExperimentRecord) {
                Intent intent = new Intent(context, DataFileService.class);
                intent.putExtra(DataFileService.EXTRA_PROJECT_ID, projectId);
                serviceScheduler.schedule(intent, dataFileDownloadIntervalTimeUnit.toMillis(dataFileDownloadInterval));

                if (optimizelyStartListener != null) {
                    eventHandler = OptlyEventHandler.getInstance(context);
                    eventHandler.setDispatchInterval(eventHandlerDispatchInterval, eventHandlerDispatchIntervalTimeUnit);
                    Optimizely optimizely = Optimizely.builder(dataFile, eventHandler)
                            .withUserExperimentRecord(userExperimentRecord)
                            .build();
                    logger.info("Sending Optimizely instance to listener");
                    optimizelyStartListener.onStart(optimizely);
                } else {
                    logger.info("No listener to send Optimizely to");
                }
            }
        };
        initUserExperimentRecordTask.executeOnExecutor(executor);
    }

    public static class OptlyActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

        @NonNull private OptimizelySDK optimizelySDK;

        public OptlyActivityLifecycleCallbacks(@NonNull OptimizelySDK optimizelySDK) {
            this.optimizelySDK = optimizelySDK;
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            // NO-OP
        }

        @Override
        public void onActivityStarted(Activity activity) {
            // NO-OP
        }

        @Override
        public void onActivityResumed(Activity activity) {
            // NO-OP
        }

        @Override
        public void onActivityPaused(Activity activity) {
            // NO-OP
        }

        @Override
        public void onActivityStopped(Activity activity) {
            optimizelySDK.stop(activity, this);
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            // NO-OP
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            // NO-OP
        }
    }

    public static class DataFileServiceConnection implements ServiceConnection {

        @NonNull private final OptimizelySDK optimizelySDK;
        private boolean bound = false;

        DataFileServiceConnection(@NonNull OptimizelySDK optimizelySDK) {
            this.optimizelySDK = optimizelySDK;
        }

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to DataFileService, cast the IBinder and get DataFileService instance
            DataFileService.LocalBinder binder = (DataFileService.LocalBinder) service;
            final DataFileService dataFileService = binder.getService();
            if (dataFileService != null) {
                DataFileLoader dataFileLoader = new DataFileLoader(new DataFileLoader.TaskChain(dataFileService),
                        LoggerFactory.getLogger(DataFileLoader.class));
                dataFileService.getDataFile(optimizelySDK.getProjectId(), dataFileLoader, new DataFileLoadedListener() {
                    @Override
                    public void onDataFileLoaded(String dataFile) {
                        if (bound) {
                            AlarmManager alarmManager = (AlarmManager) dataFileService.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                            ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler.PendingIntentFactory(dataFileService.getApplicationContext());
                            ServiceScheduler serviceScheduler = new ServiceScheduler(alarmManager, pendingIntentFactory, LoggerFactory.getLogger(ServiceScheduler.class));
                            AndroidUserExperimentRecord userExperimentRecord =
                                    (AndroidUserExperimentRecord) AndroidUserExperimentRecord.newInstance(optimizelySDK.getProjectId(), dataFileService.getApplicationContext());
                            optimizelySDK.setUserExperimentRecord(userExperimentRecord);
                            optimizelySDK.injectOptimizely(dataFileService.getApplicationContext(), userExperimentRecord, serviceScheduler, dataFile);
                        }
                    }
                });
            }
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }

        public boolean isBound() {
            return bound;
        }
    }

    public static class Builder {

        @NonNull private final String projectId;

        @NonNull private Long dataFileDownloadInterval = 1L;
        @NonNull private TimeUnit dataFileDownloadIntervalTimeUnit = TimeUnit.DAYS;
        @NonNull private Long eventHandlerDispatchInterval = 1L;
        @NonNull private TimeUnit eventHandlerDispatchIntervalTimeUnit = TimeUnit.DAYS;

        public Builder(@NonNull String projectId) {
            this.projectId = projectId;
        }

        public Builder withEventHandlerDispatchInterval(long interval, @NonNull TimeUnit timeUnit) {
            this.eventHandlerDispatchInterval = interval;
            this.eventHandlerDispatchIntervalTimeUnit = timeUnit;
            return this;
        }

        public Builder withDataFileDownloadInterval(long interval, @NonNull TimeUnit timeUnit) {
            this.dataFileDownloadInterval = interval;
            this.dataFileDownloadIntervalTimeUnit = timeUnit;
            return this;
        }

        public OptimizelySDK build() {
            final Logger logger = LoggerFactory.getLogger(OptimizelySDK.class);

            return new OptimizelySDK(projectId,
                    eventHandlerDispatchInterval,
                    eventHandlerDispatchIntervalTimeUnit,
                    dataFileDownloadInterval,
                    dataFileDownloadIntervalTimeUnit,
                    Executors.newSingleThreadExecutor(),
                    logger);

        }
    }
}
