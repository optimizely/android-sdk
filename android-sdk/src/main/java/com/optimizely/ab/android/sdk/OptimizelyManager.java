/****************************************************************************
 * Copyright 2016-2017, Optimizely, Inc. and contributors                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.android.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.annotation.RequiresApi;
import android.support.annotation.VisibleForTesting;

import com.optimizely.ab.Optimizely;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.DataFileLoadedListener;
import com.optimizely.ab.android.shared.ReflectionUtils;
import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.ab.android.shared.DataFileCache;

import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.internal.payload.Event;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Handles loading the Optimizely data file
 */
public class OptimizelyManager {

    @NonNull private OptimizelyClient optimizelyClient = new OptimizelyClient(null,
            LoggerFactory.getLogger(OptimizelyClient.class));
    @NonNull private final String projectId;
    @NonNull private final Long eventHandlerDispatchInterval;
    @NonNull private final TimeUnit eventHandlerDispatchIntervalTimeUnit;
    @NonNull private final Long dataFileDownloadInterval;
    @NonNull private final TimeUnit dataFileDownloadIntervalTimeUnit;
    @NonNull private final Executor executor;
    @NonNull private final Logger logger;
    @Nullable private ServiceConnection dataFileServiceConnection;
    @NonNull public Boolean useDataFileService = true;
    @Nullable private OptimizelyStartListener optimizelyStartListener;
    @Nullable private UserProfileService userProfileService;

    OptimizelyManager(@NonNull String projectId,
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
    public Long getDataFileDownloadInterval() {
        return dataFileDownloadInterval;
    }

    @NonNull
    public TimeUnit getDataFileDownloadIntervalTimeUnit() {
        return dataFileDownloadIntervalTimeUnit;
    }

    /**
     * Returns the {@link OptimizelyManager} builder
     *
     * @param projectId your project's id
     * @return a {@link OptimizelyManager.Builder}
     */
    @NonNull
    public static Builder builder(@NonNull String projectId) {
        return new Builder(projectId);
    }

    @Nullable
    ServiceConnection getDataFileServiceConnection() {
        return dataFileServiceConnection;
    }

    void setDataFileServiceConnection(@Nullable ServiceConnection dataFileServiceConnection) {
        this.dataFileServiceConnection = dataFileServiceConnection;
    }

    @Nullable
    OptimizelyStartListener getOptimizelyStartListener() {
        return optimizelyStartListener;
    }

    void setOptimizelyStartListener(@Nullable OptimizelyStartListener optimizelyStartListener) {
        this.optimizelyStartListener = optimizelyStartListener;
    }

    /**
     * Initialize Optimizely Synchronously
     * <p>
     * Instantiates and returns an {@link OptimizelyClient} instance. Will also cache the instance
     * for future lookups via getClient
     *
     * @param context  any {@link Context} instance
     * @param datafile the datafile
     * @return an {@link OptimizelyClient} instance
     */
    public OptimizelyClient initialize(@NonNull Context context, @NonNull String datafile) {
        if (!isAndroidVersionSupported()) {
            return optimizelyClient;
        }

        UserProfileService userProfileService = getAndroidUserProfileServiceAndStart(context);
        try {
            optimizelyClient = buildOptimizely(context, datafile, userProfileService);
        } catch (ConfigParseException e) {
            logger.error("Unable to parse compiled data file", e);
        } catch (Exception e) {
            logger.error("Unable to build OptimizelyClient instance", e);
        }

        initializeServiceConnection(context);

        return optimizelyClient;
    }

    private Boolean initializeServiceConnection(Context context) {
        if (useDataFileService == false) {
            logger.info("DataFileServiceConnection called but use data file service is set to false");
            return false;
        }
        // After instantiating the OptimizelyClient, we will begin the datafile sync so that next time
        // the user can instantiate with the latest datafile
        Class clazz = getDataFileServiceClass();
        if (clazz == null) {
            // the data file service may have been excluded from the package.
            // So, we are going to all inject so that optimizely can continue to run.
            logger.info("DataFileServiceConnect was attempted to be found in the classPath but does not seem to exist." +
                    "  The developer must have excluded the datafile-handler module.  DataFileServiceConnection cannot be started.");
            return false;
        }

        final Intent intent = new Intent(context.getApplicationContext(), clazz);
        if (dataFileServiceConnection == null) {
            this.dataFileServiceConnection = getDataFileServiceConnection(projectId, context, getDataFileLoadedListener(context));
            context.getApplicationContext().bindService(intent, dataFileServiceConnection, Context.BIND_AUTO_CREATE);
        }

        return true;

    }
    /**
     * Initialize Optimizely Synchronously
     * <p>
     * Instantiates and returns an {@link OptimizelyClient} instance. Will also cache the instance
     * for future lookups via getClient. The datafile should be stored in res/raw.
     *
     * @param context     any {@link Context} instance
     * @param dataFileRes the R id that the data file is located under.
     * @return an {@link OptimizelyClient} instance
     */
    @NonNull
    public OptimizelyClient initialize(@NonNull Context context, @RawRes int dataFileRes) {
        try {
            String datafile = loadRawResource(context, dataFileRes);
            return initialize(context, datafile);
        } catch (IOException e) {
            logger.error("Unable to load compiled data file", e);
        }

        // return dummy client if not able to initialize a valid one
        return optimizelyClient;
    }

    /**
     * Initialize Optimizely Synchronously
     * <p>
     * Instantiates and returns an {@link OptimizelyClient} instance using the datafile cached on disk
     * if not available then it will return a dummy instance.
     *
     * @param context any {@link Context} instance
     * @return an {@link OptimizelyClient} instance
     */
    public OptimizelyClient initialize(@NonNull Context context) {
        DataFileCache dataFileCache = new DataFileCache(
                projectId,
                new Cache(context, LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(DataFileCache.class)
        );

        JSONObject datafile = dataFileCache.load();
        if (datafile != null) {
            return initialize(context, datafile.toString());
        }

        // return dummy client if not able to initialize a valid one
        return optimizelyClient;
    }

    /**
     * Starts Optimizely asynchronously
     * <p>
     * An {@link OptimizelyClient} instance will be delivered to
     * {@link OptimizelyStartListener#onStart(OptimizelyClient)}. The callback will only be hit
     * once.  If there is a cached datafile the returned instance will be built from it.  The cached
     * datafile will be updated from network if it is different from the cache.  If there is no
     * cached datafile the returned instance will always be built from the remote datafile.
     *
     * @param activity                an Activity, used to automatically unbind com.optimizely.ab.android.datafile_handler.DataFileService
     * @param optimizelyStartListener callback that {@link OptimizelyClient} instances are sent to.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void initialize(@NonNull Activity activity, @NonNull OptimizelyStartListener optimizelyStartListener) {
        if (!isAndroidVersionSupported()) {
            return;
        }
        activity.getApplication().registerActivityLifecycleCallbacks(new OptlyActivityLifecycleCallbacks(this));
        initialize(activity.getApplicationContext(), optimizelyStartListener);
    }

    /**
     * @param context                 any type of context instance
     * @param optimizelyStartListener callback that {@link OptimizelyClient} instances are sent to.
     * @see #initialize(Activity, OptimizelyStartListener)
     * <p>
     * This method does the same thing except it can be used with a generic {@link Context}.
     * When using this method be sure to call {@link #stop(Context)} to unbind com.optimizely.ab.android.datafile_handler.DataFileService.
     */
    public void initialize(@NonNull Context context, @NonNull OptimizelyStartListener optimizelyStartListener) {
        if (!isAndroidVersionSupported()) {
            return;
        }
        this.optimizelyStartListener = optimizelyStartListener;
        initializeServiceConnection(context);
    }

    DataFileLoadedListener getDataFileLoadedListener(final Context context) {
        return
                new DataFileLoadedListener() {
                         @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
                         @Override
                            public void onDataFileLoaded(@Nullable String dataFile) {
                             // App is being used, i.e. in the foreground
                            AlarmManager alarmManager = (AlarmManager) context
                                    .getSystemService(Context.ALARM_SERVICE);
                             ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler
                                     .PendingIntentFactory(context);
                             ServiceScheduler serviceScheduler = new ServiceScheduler(alarmManager, pendingIntentFactory,
                                     LoggerFactory.getLogger(ServiceScheduler.class));
                             if (dataFile != null) {
                                 UserProfileService userProfileService = getAndroidUserProfileService(context);
                                 injectOptimizely(context, userProfileService, serviceScheduler, dataFile);
                             } else {
                                 // We should always call the callback even with the dummy
                                 // instances.  Devs might gate the rest of their app
                                 // based on the loading of Optimizely
                                 OptimizelyStartListener optimizelyStartListener = getOptimizelyStartListener();
                                 if (optimizelyStartListener != null) {
                                     optimizelyStartListener.onStart(getOptimizely());
                                 }
                             }
                         }

                    @Override
                    public void onStop(Context context) {
                        stop(context);
                    }
                };

    }
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    void stop(@NonNull Activity activity, @NonNull OptlyActivityLifecycleCallbacks optlyActivityLifecycleCallbacks) {
        stop(activity);
        activity.getApplication().unregisterActivityLifecycleCallbacks(optlyActivityLifecycleCallbacks);
    }

    /**
     * Unbinds com.optimizely.ab.android.datafile_handler.DataFileService
     * <p>
     * Calling this is not necessary if using {@link #initialize(Activity, OptimizelyStartListener)} which
     * handles unbinding implicitly.
     *
     * @param context any {@link Context} instance
     */
    @SuppressWarnings("WeakerAccess")
    public void stop(@NonNull Context context) {
        if (!isAndroidVersionSupported()) {
            return;
        }
        if (dataFileServiceConnection != null && ReflectionUtils.callMethod(dataFileServiceConnection,"isBound", ReflectionUtils.emptyArgTypes, ReflectionUtils.emptyArgs) == Boolean.TRUE) {
            context.getApplicationContext().unbindService(dataFileServiceConnection);
            dataFileServiceConnection = null;
        }

        this.optimizelyStartListener = null;
    }

    /**
     * Gets a cached Optimizely instance
     * <p>
     * If {@link #initialize(Activity, OptimizelyStartListener)} or {@link #initialize(Context, OptimizelyStartListener)}
     * has not been called yet the returned {@link OptimizelyClient} instance will be a dummy instance
     * that logs warnings in order to prevent {@link NullPointerException}.
     * <p>
     * Using {@link #initialize(Activity, OptimizelyStartListener)} or {@link #initialize(Context, OptimizelyStartListener)}
     * will update the cached instance with a new {@link OptimizelyClient} built from a cached local
     * datafile on disk or a remote datafile on the CDN.
     *
     * @return the cached instance of {@link OptimizelyClient}
     */
    @NonNull
    public OptimizelyClient getOptimizely() {
        // Check version and log warning if version is less than what is required.
        isAndroidVersionSupported();
        return optimizelyClient;
    }

    private String loadRawResource(Context context, @RawRes int rawRes) throws IOException {
        Resources res = context.getResources();
        InputStream in = res.openRawResource(rawRes);
        byte[] b = new byte[in.available()];
        int read = in.read(b);
        if (read > -1) {
            return new String(b);
        } else {
            throw new IOException("Couldn't parse raw res fixture, no bytes");
        }
    }

    /**
     * Check if the datafile is cached on the disk
     *
     * @param context any {@link Context} instance
     * @return True if the datafile is cached on the disk
     */
    public boolean isDatafileCached(Context context) {
        DataFileCache dataFileCache = new DataFileCache(
                projectId,
                new Cache(context, LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(DataFileCache.class)
        );

        return dataFileCache.exists();
    }

    /**
     * Returns the URL of the versioned datafile that this SDK expects to use
     * @param projectId The id of the project for which we are getting the datafile
     * @return the CDN location of the datafile
     */
    public static @NonNull String getDatafileUrl(String projectId) {
        Class fileService = ReflectionUtils.getClass("com.optimizely.ab.android.datafile_handler.DataFileService", ClassLoader.getSystemClassLoader());
        Class[] argTypes = {projectId.getClass() };
        return (String) ReflectionUtils.callStaticMethod(fileService, "getDatafileUrl", argTypes, projectId);
        //return DataFileService.getDatafileUrl(projectId);
    }

    @NonNull
    String getProjectId() {
        return projectId;
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    void injectOptimizely(@NonNull final Context context, final @NonNull UserProfileService userProfileService,
                          @Nullable final ServiceScheduler serviceScheduler, @NonNull final String dataFile) {
        AsyncTask<Void, Void, UserProfileService> initUserProfileTask = new AsyncTask<Void, Void, UserProfileService>() {
            @Override
            protected UserProfileService doInBackground(Void[] params) {
                ReflectionUtils.callMethod(userProfileService, "start", ReflectionUtils.emptyArgTypes, ReflectionUtils.emptyArgs);
                //userProfileService.start();
                return userProfileService;
            }

            @Override
            protected void onPostExecute(UserProfileService userProfileService) {

                Class clazz = getDataFileServiceClass();
                if (clazz != null && useDataFileService == true) {
                    Intent intent = new Intent(context, clazz);
                    intent.putExtra("com.optimizely.ab.android.EXTRA_PROJECT_ID", projectId);
                    serviceScheduler.schedule(intent, dataFileDownloadIntervalTimeUnit.toMillis(dataFileDownloadInterval));
                }

                try {
                    OptimizelyManager.this.optimizelyClient = buildOptimizely(context, dataFile, userProfileService);
                    OptimizelyManager.this.userProfileService = userProfileService;
                    optimizelyClient.setDefaultAttributes(OptimizelyDefaultAttributes.buildDefaultAttributesMap(context, logger));
                    logger.info("Sending Optimizely instance to listener");

                    if (optimizelyStartListener != null) {
                        optimizelyStartListener.onStart(optimizelyClient);
                    } else {
                        logger.info("No listener to send Optimizely to");
                    }
                } catch (Exception e) {
                    logger.error("Unable to build optimizely instance", e);
                }
            }
        };

        try {
            initUserProfileTask.executeOnExecutor(executor);
        } catch (Exception e) {
            logger.error("Unable to initialize the user profile while injecting Optimizely", e);
        }
    }

    private OptimizelyClient buildOptimizely(@NonNull Context context, @NonNull String dataFile, @NonNull
            UserProfileService userProfileService) throws ConfigParseException {
        EventHandler eventHandler = getEventHandler(context);

        Event.ClientEngine clientEngine = OptimizelyClientEngine.getClientEngineFromContext(context);

        Optimizely optimizely = Optimizely.builder(dataFile, eventHandler)
                .withUserProfileService(userProfileService)
                .withClientEngine(clientEngine)
                .withClientVersion(BuildConfig.CLIENT_VERSION)
                .build();
        return new OptimizelyClient(optimizely, LoggerFactory.getLogger(OptimizelyClient.class));
    }

    @VisibleForTesting
    public UserProfileService getUserProfileService() {
        return userProfileService;
    }

    protected Class getDataFileServiceClass() {
        return ReflectionUtils.getClass("com.optimizely.ab.android.datafile_handler.DataFileService", this.getClass().getClassLoader());
    }

    protected UserProfileService getAndroidUserProfileServiceAndStart(Context context) {
        Class[] classes = { String.class, Context.class };
        Object obj = ReflectionUtils.getObject("com.optimizely.ab.android.user_profile.AndroidUserProfileServiceDefault", this.getClass().getClassLoader(),
                "newInstance", classes, getProjectId(), context);

        ReflectionUtils.callMethod(obj, "start",ReflectionUtils.emptyArgTypes, ReflectionUtils.emptyArgs);

        return (UserProfileService)obj;
    }

    protected UserProfileService getAndroidUserProfileService(Context context) {
        Class[] classes = { String.class, Context.class };
        Object obj = ReflectionUtils.getObject("com.optimizely.ab.android.user_profile.AndroidUserProfileServiceDefault", this.getClass().getClassLoader(),
                "newInstance", classes, getProjectId(), context);

        return (UserProfileService)obj;
    }

    protected EventHandler getEventHandler(Context context) {
        Class[] classes = { Context.class };
        Object obj = ReflectionUtils.getObject("com.optimizely.ab.android.event_handler.OptlyEventHandler", this.getClass().getClassLoader(),
                "getInstance", classes, context);
        Class[] argTypes = { Long.class, TimeUnit.class };
        ReflectionUtils.callMethod(obj, "setDispatchInterval", argTypes, eventHandlerDispatchInterval, eventHandlerDispatchIntervalTimeUnit);

        return (EventHandler)obj;
    }

    protected ServiceConnection getDataFileServiceConnection(String projectId, Context context, DataFileLoadedListener listener) {
        Class[] classes = { String.class, Context.class, DataFileLoadedListener.class };
        Object obj = ReflectionUtils.getObject("com.optimizely.ab.android.datafile_handler.DataFileServiceConnection", this.getClass().getClassLoader(),
                null, classes, projectId, context, listener);

        return (ServiceConnection)obj;
    }

    private boolean isAndroidVersionSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return true;
        } else {
            logger.warn("Optimizely will not work on this phone.  It's Android version {} is less the minimum " +
                    "supported version {}", Build.VERSION.SDK_INT, Build.VERSION_CODES.ICE_CREAM_SANDWICH);
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    static class OptlyActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

        @NonNull private OptimizelyManager optimizelyManager;

        OptlyActivityLifecycleCallbacks(@NonNull OptimizelyManager optimizelyManager) {
            this.optimizelyManager = optimizelyManager;
        }

        /**
         * @hide
         * @see android.app.Application.ActivityLifecycleCallbacks#onActivityCreated(Activity, Bundle)
         */
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            // NO-OP
        }

        /**
         * @hide
         * @see android.app.Application.ActivityLifecycleCallbacks#onActivityStarted(Activity)
         */
        @Override
        public void onActivityStarted(Activity activity) {
            // NO-OP
        }

        /**
         * @hide
         * @see android.app.Application.ActivityLifecycleCallbacks#onActivityResumed(Activity)
         */
        @Override
        public void onActivityResumed(Activity activity) {
            // NO-OP
        }

        /**
         * @hide
         * @see android.app.Application.ActivityLifecycleCallbacks#onActivityPaused(Activity)
         */
        @Override
        public void onActivityPaused(Activity activity) {
            // NO-OP
        }

        /**
         * @hide
         * @see android.app.Application.ActivityLifecycleCallbacks#onActivityStopped(Activity)
         */
        @Override
        public void onActivityStopped(Activity activity) {
            optimizelyManager.stop(activity, this);
        }

        /**
         * @hide
         * @see android.app.Application.ActivityLifecycleCallbacks#onActivitySaveInstanceState(Activity, Bundle)
         */
        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            // NO-OP
        }

        /**
         * @hide
         * @see android.app.Application.ActivityLifecycleCallbacks#onActivityDestroyed(Activity)
         */
        @Override
        public void onActivityDestroyed(Activity activity) {
            // NO-OP
        }
    }

    /**
     * Builds instances of {@link OptimizelyManager}
     */
    @SuppressWarnings("WeakerAccess")
    public static class Builder {

        @NonNull private final String projectId;

        @NonNull private Long dataFileDownloadInterval = 1L;
        @NonNull private TimeUnit dataFileDownloadIntervalTimeUnit = TimeUnit.DAYS;
        @NonNull private Long eventHandlerDispatchInterval = 1L;
        @NonNull private TimeUnit eventHandlerDispatchIntervalTimeUnit = TimeUnit.DAYS;

        Builder(@NonNull String projectId) {
            this.projectId = projectId;
        }

        /**
         * Sets the interval which com.optimizely.ab.android.event_handler.EventIntentService
         * will flush events.
         *
         * @param interval the interval
         * @param timeUnit the unit of the interval
         * @return this {@link Builder} instance
         */
        public Builder withEventHandlerDispatchInterval(long interval, @NonNull TimeUnit timeUnit) {
            this.eventHandlerDispatchInterval = interval;
            this.eventHandlerDispatchIntervalTimeUnit = timeUnit;
            return this;
        }

        /**
         * Sets the interval which com.optimizely.ab.android.datafile_handler.DataFileService will attempt to update the
         * cached datafile.
         *
         * @param interval the interval
         * @param timeUnit the unit of the interval
         * @return this {@link Builder} instance
         */
        public Builder withDataFileDownloadInterval(long interval, @NonNull TimeUnit timeUnit) {
            this.dataFileDownloadInterval = interval;
            this.dataFileDownloadIntervalTimeUnit = timeUnit;
            return this;
        }

        /**
         * Get a new {@link Builder} instance to create {@link OptimizelyManager} with.
         *
         * @return a {@link Builder} instance
         */
        public OptimizelyManager build() {
            Logger logger;
            try {
                logger = LoggerFactory.getLogger(OptimizelyManager.class);
            } catch (Exception e) {
                logger = LoggerFactory.getLogger("Optly.androidSdk");
                logger.error("Unable to generate logger from class");
            }

            // AlarmManager doesn't allow intervals less than 60 seconds
            if (dataFileDownloadIntervalTimeUnit.toMillis(dataFileDownloadInterval) < (60 * 1000)) {
                dataFileDownloadIntervalTimeUnit = TimeUnit.SECONDS;
                dataFileDownloadInterval = 60L;
                logger.warn("Minimum datafile polling interval is 60 seconds. " +
                        "Defaulting to 60 seconds.");
            }

            return new OptimizelyManager(projectId,
                    eventHandlerDispatchInterval,
                    eventHandlerDispatchIntervalTimeUnit,
                    dataFileDownloadInterval,
                    dataFileDownloadIntervalTimeUnit,
                    Executors.newSingleThreadExecutor(),
                    logger);
        }
    }
}
