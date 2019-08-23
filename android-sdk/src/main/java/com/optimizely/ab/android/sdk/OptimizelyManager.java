/****************************************************************************
 * Copyright 2017-2018, Optimizely, Inc. and contributors                   *
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
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.annotation.RequiresApi;
import android.support.annotation.VisibleForTesting;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.android.datafile_handler.DatafileHandler;
import com.optimizely.ab.android.datafile_handler.DatafileLoadedListener;
import com.optimizely.ab.android.datafile_handler.DatafileService;
import com.optimizely.ab.android.datafile_handler.DefaultDatafileHandler;
import com.optimizely.ab.android.event_handler.DefaultEventHandler;
import com.optimizely.ab.android.event_handler.EventIntentService;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.android.user_profile.DefaultUserProfileService;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.notification.UpdateConfigNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Handles loading the Optimizely data file
 */
public class OptimizelyManager {

    @NonNull private OptimizelyClient optimizelyClient = new OptimizelyClient(null,
            LoggerFactory.getLogger(OptimizelyClient.class));

    @NonNull private DatafileHandler datafileHandler;
    private final long datafileDownloadInterval;
    private final long eventDispatchInterval;
    @Nullable private EventHandler eventHandler = null;
    @Nullable private ErrorHandler errorHandler;
    @NonNull private Logger logger;
    @Nullable private final String projectId;
    @Nullable private final String sdkKey;
    @NonNull private final DatafileConfig datafileConfig;

    @NonNull private UserProfileService userProfileService;

    @Nullable private OptimizelyStartListener optimizelyStartListener;

    OptimizelyManager(@Nullable String projectId,
                      @Nullable String sdkKey,
                      @Nullable DatafileConfig datafileConfig,
                      @NonNull Logger logger,
                      long datafileDownloadInterval,
                      @NonNull DatafileHandler datafileHandler,
                      @Nullable ErrorHandler errorHandler,
                      long eventDispatchInterval,
                      @NonNull EventHandler eventHandler,
                      @NonNull UserProfileService userProfileService) {

        if (projectId == null && sdkKey == null) {
            logger.error("projectId and sdkKey are both null!");
        }
        this.projectId = projectId;
        this.sdkKey = sdkKey;
        if (datafileConfig == null) {
            this.datafileConfig = new DatafileConfig(this.projectId, this.sdkKey);
        }
        else {
            this.datafileConfig = datafileConfig;
        }
        this.logger = logger;
        this.datafileDownloadInterval = datafileDownloadInterval;
        this.datafileHandler = datafileHandler;
        this.eventDispatchInterval = eventDispatchInterval;
        this.eventHandler = eventHandler;
        this.errorHandler = errorHandler;
        this.userProfileService = userProfileService;
    }

    @VisibleForTesting
    public Long getDatafileDownloadInterval() {
        return datafileDownloadInterval;
    }

    /**
     * Returns the {@link OptimizelyManager} builder
     *
     * @param projectId your project's id
     * @return a {@link OptimizelyManager.Builder}
     */
    @Deprecated
    @NonNull
    public static Builder builder(@Nullable String projectId) {
        return new Builder(projectId);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    OptimizelyStartListener getOptimizelyStartListener() {
        return optimizelyStartListener;
    }

    void setOptimizelyStartListener(@Nullable OptimizelyStartListener optimizelyStartListener) {
        this.optimizelyStartListener = optimizelyStartListener;
    }

    private void notifyStartListener() {
        if (optimizelyStartListener != null) {
            optimizelyStartListener.onStart(getOptimizely());
            optimizelyStartListener = null;
        }

    }

    /**
     * Initialize Optimizely Synchronously using the datafile passed in while downloading the latest datafile in the background from the CDN to cache.
     * It should be noted that even though it initiates a download of the datafile to cache, this method does not use that cached datafile.
     * You can always test if a datafile exists in cache with {@link #isDatafileCached(Context)}.
     * <p>
     * Instantiates and returns an {@link OptimizelyClient} instance. It will also cache the instance
     * for future lookups via getClient
     *
     * @param context  any {@link Context} instance
     * @param datafile the datafile used to initialize the OptimizelyClient.
     * @return an {@link OptimizelyClient} instance
     */
    public OptimizelyClient initialize(@NonNull Context context, @NonNull String datafile) {
        initialize(context, datafile,true);
        return optimizelyClient;
    }

    /**
     * Initialize Optimizely Synchronously using the datafile passed in while downloading the latest datafile in the background from the CDN to cache.
     * It should be noted that even though it initiates a download of the datafile to cache, this method does not use that cached datafile.
     * You can always test if a datafile exists in cache with {@link #isDatafileCached(Context)}.
     * <p>
     * Instantiates and returns an {@link OptimizelyClient} instance. It will also cache the instance
     * for future lookups via getClient
     *
     * @param context  any {@link Context} instance
     * @param datafile the datafile used to initialize the OptimizelyClient.
     * @param downloadToCache to check if datafile should get updated in cache after initialization.
     * @return an {@link OptimizelyClient} instance
     */
    protected OptimizelyClient initialize(@NonNull Context context, @Nullable String datafile, boolean downloadToCache) {
        if (!isAndroidVersionSupported()) {
            return optimizelyClient;
        }
        try {
            if(datafile!=null) {
                if (getUserProfileService() instanceof DefaultUserProfileService) {
                    DefaultUserProfileService defaultUserProfileService = (DefaultUserProfileService) getUserProfileService();
                    defaultUserProfileService.start();
                }
                optimizelyClient = buildOptimizely(context, datafile);
                startDatafileHandler(context);
            }
            else {
                logger.error("Invalid datafile");
            }
        } catch (ConfigParseException e) {
            logger.error("Unable to parse compiled data file", e);
        } catch (Exception e) {
            logger.error("Unable to build OptimizelyClient instance", e);
        } catch (Error e) {
            logger.error("Unable to build OptimizelyClient instance", e);
        }
        if(downloadToCache){
            datafileHandler.downloadDatafile(context, datafileConfig, null);
        }

        return optimizelyClient;
    }

    /**
     * Initialize Optimizely Synchronously by loading the resource, use it to initialize Optimizely,
     * and downloading the latest datafile from the CDN in the background to cache.
     * <p>
     * Instantiates and returns an {@link OptimizelyClient}  instance using the datafile cached on disk
     * if not available then it will expect that raw data file should exist on given id.
     * and initialize using raw file. Will also cache the instance
     * for future lookups via getClient. The datafile should be stored in res/raw.
     *
     * @param context     any {@link Context} instance
     * @param datafileRes the R id that the data file is located under.
     * @return an {@link OptimizelyClient} instance
     */
    @NonNull
    public OptimizelyClient initialize(@NonNull Context context, @RawRes Integer datafileRes) {
        try {

            String datafile;
            Boolean datafileInCache = isDatafileCached(context);
            datafile = getDatafile(context, datafileRes);

            optimizelyClient = initialize(context, datafile, true);
            if (datafileInCache) {
                cleanupUserProfileCache(getUserProfileService());
            }
        }catch (NullPointerException e){
            logger.error("Unable to find compiled data file in raw resource",e);
        }

        // return dummy client if not able to initialize a valid one
        return optimizelyClient;
    }

    private void cleanupUserProfileCache(UserProfileService userProfileService) {
        final DefaultUserProfileService defaultUserProfileService;
        if (userProfileService instanceof DefaultUserProfileService) {
            defaultUserProfileService = (DefaultUserProfileService)userProfileService;
        }
        else {
            return;
        }

        final ProjectConfig config = optimizelyClient.getProjectConfig();
        if (config == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Set<String> experimentIds = config.getExperimentIdMapping().keySet();

                    defaultUserProfileService.removeInvalidExperiments(experimentIds);
                }
                catch (Exception e) {
                    logger.error("Error removing invalid experiments from default user profile service.", e);
                }
            }
        }).start();

    }

    /** This function will first try to get datafile from Cache, if file is not cached yet
     * than it will read from Raw file
     * @param context
     * @param datafileRes
     * @return datafile
     */
    public String getDatafile(Context context,@RawRes Integer datafileRes){
        try {
            if (isDatafileCached(context)) {
                String datafile = datafileHandler.loadSavedDatafile(context, datafileConfig);
                if (datafile != null) {
                    return datafile;
                }
            }

            if (datafileRes != null) {
                return loadRawResource(context, datafileRes);
            }else{
                logger.error("Invalid datafile resource ID.");
                return null;
            }
        } catch (IOException e) {
            logger.error("Unable to load compiled data file", e);
        } catch (NullPointerException e){
            logger.error("Unable to find compiled data file in raw resource",e);
        }
        return null;
    }

    /**
     * Starts Optimizely asynchronously
     * <p>
     * See {@link #initialize(Context, Integer, OptimizelyStartListener)}
     * @param context                 any type of context instance
     * @param optimizelyStartListener callback that {@link OptimizelyClient} instances are sent to.
     * @deprecated Consider using {@link #initialize(Context, Integer, OptimizelyStartListener)}
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void initialize(@NonNull final Context context, @NonNull OptimizelyStartListener optimizelyStartListener) {
        initialize(context, null, optimizelyStartListener);
    }

    /**
     * Starts Optimizely asynchronously
     * <p>
     * * Attempts to fetch the most recent remote datafile and construct an {@link OptimizelyClient}.
     * If the datafile has not changed since the SDK last fetched it or if there is an error
     * fetching, the SDK will attempt to construct an {@link OptimizelyClient} using a cached datafile.
     * If there is no cached datafile, then the SDK will return a dummy, uninitialized {@link OptimizelyClient}.
     * Passing in a datafileRes will guarantee the SDK returns an initialized {@link OptimizelyClient}.
     * @param context                 any type of context instance
     * @param datafileRes             Null is allowed here if user don't want to put datafile in res. Null handling is done in {@link #getDatafile(Context,Integer)}
     * @param optimizelyStartListener callback that {@link OptimizelyClient} instances are sent to.
     * @see #initialize(Context, Integer, OptimizelyStartListener)
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void initialize(@NonNull final Context context, @RawRes final Integer datafileRes, @NonNull OptimizelyStartListener optimizelyStartListener) {
        if (!isAndroidVersionSupported()) {
            return;
        }
        setOptimizelyStartListener(optimizelyStartListener);
        datafileHandler.downloadDatafile(context, datafileConfig, getDatafileLoadedListener(context,datafileRes));
    }

    DatafileLoadedListener getDatafileLoadedListener(final Context context, @RawRes final Integer datafileRes) {
        return new DatafileLoadedListener() {
            @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void onDatafileLoaded(@Nullable String datafile) {
                // App is being used, i.e. in the foreground
                if (datafile != null && !datafile.isEmpty()) {
                    injectOptimizely(context, userProfileService, datafile);
                } else {
                    //if datafile is null than it should be able to take from cache and if not present
                    //in Cache than should be able to get from raw data file
                    injectOptimizely(context, userProfileService, getDatafile(context,datafileRes));
                }
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    void stop(@NonNull Activity activity, @NonNull OptlyActivityLifecycleCallbacks optlyActivityLifecycleCallbacks) {
        stop(activity);
        activity.getApplication().unregisterActivityLifecycleCallbacks(optlyActivityLifecycleCallbacks);
    }

    /**
     * Called after the {@link DatafileService} is unbound.
     * <p>
     * Here we just cancel the start listener.
     *
     * @param context any {@link Context} instance
     */
    public void stop(@NonNull Context context) {
        if (!isAndroidVersionSupported()) {
            return;
        }

        this.optimizelyStartListener = null;
    }

    /**
     * Gets a cached Optimizely instance
     * <p>
     * If {@link #initialize(Context,Integer, OptimizelyStartListener)} or {@link #initialize(Context, Integer)}
     * has not been called yet the returned {@link OptimizelyClient} instance will be a dummy instance
     * that logs warnings in order to prevent {@link NullPointerException}.
     * <p>
     * Using {@link #initialize(Context,Integer, OptimizelyStartListener)} or {@link #initialize(Context, Integer)}
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

    public static String loadRawResource(Context context, @RawRes int rawRes) throws IOException {
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
        return datafileHandler.isDatafileSaved(context, datafileConfig);
    }

    /**
     * Returns the URL of the versioned datafile that this SDK expects to use
     * @return the CDN location of the datafile
     */
    public @NonNull String getDatafileUrl() {
        return datafileConfig.getUrl();
    }
    @NonNull
    public String getProjectId() {
        return projectId;
    }

    @NonNull
    public DatafileConfig getDatafileConfig() {
        return datafileConfig;
    }

    @NonNull
    public DatafileHandler getDatafileHandler() {
        return datafileHandler;
    }

    private void startDatafileHandler(Context context) {
        if (datafileDownloadInterval <= 0) {
            logger.debug("Invalid download interval, ignoring background updates.");
            return;
        }

        datafileHandler.startBackgroundUpdates(context, datafileConfig, datafileDownloadInterval, datafile1 -> {
            NotificationCenter notificationCenter = getOptimizely().getNotificationCenter();
            if (notificationCenter == null) {
                logger.debug("NotificationCenter null, not sending notification");
                return;
            }
            notificationCenter.send(new UpdateConfigNotification());
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    void injectOptimizely(@NonNull final Context context, final @NonNull UserProfileService userProfileService,
                          @NonNull final String datafile) {

        try {
            optimizelyClient = buildOptimizely(context, datafile);
            optimizelyClient.setDefaultAttributes(OptimizelyDefaultAttributes.buildDefaultAttributesMap(context, logger));

            startDatafileHandler(context);

            if (userProfileService instanceof DefaultUserProfileService) {
                ((DefaultUserProfileService) userProfileService).startInBackground(new DefaultUserProfileService.StartCallback() {
                    @Override
                    public void onStartComplete(UserProfileService userProfileService) {
                        cleanupUserProfileCache(userProfileService);
                        if (optimizelyStartListener != null) {
                            logger.info("Sending Optimizely instance to listener");
                            notifyStartListener();
                        } else {
                            logger.info("No listener to send Optimizely to");
                        }
                    }
                });
            }
            else {
                if (optimizelyStartListener != null) {
                    logger.info("Sending Optimizely instance to listener");
                    notifyStartListener();
                } else {
                    logger.info("No listener to send Optimizely to");
                }
            }
        } catch (Exception e) {
            logger.error("Unable to build OptimizelyClient instance", e);
            if (optimizelyStartListener != null) {
                logger.info("Sending Optimizely instance to listener may be null on failure");
                notifyStartListener();
            }
        } catch (Error e) {
            logger.error("Unable to build OptimizelyClient instance", e);
        }
    }

    private OptimizelyClient buildOptimizely(@NonNull Context context, @NonNull String datafile) throws ConfigParseException {
        EventHandler eventHandler = getEventHandler(context);

        EventBatch.ClientEngine clientEngine = OptimizelyClientEngine.getClientEngineFromContext(context);

        Optimizely.Builder builder = Optimizely.builder();

        builder.withEventHandler(eventHandler);

        if (datafileHandler instanceof DefaultDatafileHandler) {
            DefaultDatafileHandler handler = (DefaultDatafileHandler)datafileHandler;
            handler.setDatafile(datafile);
            builder.withConfigManager(handler);
        }
        else {
            builder.withDatafile(datafile);
        }

        builder.withClientEngine(clientEngine)
                .withClientVersion(BuildConfig.CLIENT_VERSION);

        if (errorHandler != null) {
            builder.withErrorHandler(errorHandler);
        }

        builder.withUserProfileService(userProfileService);

        Optimizely optimizely = builder.build();
        return new OptimizelyClient(optimizely, LoggerFactory.getLogger(OptimizelyClient.class));
    }

    @NonNull
    @VisibleForTesting
    public UserProfileService getUserProfileService() {
        return userProfileService;
    }


    protected EventHandler getEventHandler(Context context) {
        if (eventHandler == null) {
            DefaultEventHandler eventHandler = DefaultEventHandler.getInstance(context);
            eventHandler.setDispatchInterval(eventDispatchInterval);
            this.eventHandler = eventHandler;
        }

        return eventHandler;
    }

    protected ErrorHandler getErrorHandler(Context context) {
        return errorHandler;
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

        @Nullable private final String projectId;

        // -1 will cause the background download to not be initiated.
        private long datafileDownloadInterval = -1L;
        // -1 will cause the background download to not be initiated.
        private long eventDispatchInterval = -1L;
        @Nullable private DatafileHandler datafileHandler = null;
        @Nullable private Logger logger = null;
        @Nullable private EventHandler eventHandler = null;
        @Nullable private ErrorHandler errorHandler = null;
        @Nullable private UserProfileService userProfileService = null;
        @Nullable private String sdkKey = null;
        @Nullable private DatafileConfig datafileConfig = null;

        @Deprecated
        /**
         * @deprecated use {@link #Builder()} instead and pass in an SDK Key with {@link #withSDKKey(String)}
         */
        Builder(@Nullable String projectId) {
            this.projectId = projectId;
        }

        Builder() {
            this.projectId = null;
        }


        /**
         * Sets the interval which {@link DatafileService} through the {@link DatafileHandler} will attempt to update the
         * cached datafile.  If you set this to -1, you disable background updates.  If you don't set
         * a download interval (or set to less than 0), then no background updates will be scheduled or occur.
         *
         * @param interval the interval in seconds
         * @return this {@link Builder} instance
         */
        public Builder withDatafileDownloadInterval(long interval) {
            this.datafileDownloadInterval = interval;
            return this;
        }

        /**
         * Override the default {@link DatafileHandler}.
         * @param overrideHandler datafile handler to replace default handler
         * @return this {@link Builder} instance
         */
        public Builder withDatafileHandler(DatafileHandler overrideHandler) {
            this.datafileHandler = overrideHandler;
            return this;
        }

        public Builder withSDKKey(String sdkKey) {
            this.sdkKey = sdkKey;
            return this;
        }

        /**
         * Override the default {@link Logger}.
         * @param overrideHandler logger to override OptimizedlyManager and OptimizelyClient logger
         * @return this {@link Builder} instance
         */
        public Builder withLogger(Logger overrideHandler) {
            this.logger = overrideHandler;
            return this;
        }

        /**
         * Override the default {@link ErrorHandler}.
         * @param errorHandler  handler to override the java core error handler.
         * @return this {@link Builder} instance
         */
        public Builder withErrorHandler(ErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        /**
         * Sets the interval which {@link EventIntentService} will flush events.
         * If you set this to -1, you disable background updates.  If you don't set
         * a event dispatch interval, then no background updates will be scheduled or occur.
         *
         * @param interval the interval in seconds
         * @return this {@link Builder} instance
         */
        public Builder withEventDispatchInterval(long interval) {
            this.eventDispatchInterval = interval;
            return this;
        }

        /**
         * Override the default {@link EventHandler}.
         *
         * @param eventHandler event handler to replace the default event handler
         * @return this {@link Builder} instance
         */
        public Builder withEventHandler(EventHandler eventHandler) {
            this.eventHandler = eventHandler;
            return this;
        }

        /**
         * Override the default {@link UserProfileService}.
         * @param userProfileService the user profile service to replace the default user profile service
         * @return this {@link Builder} instance
         */
        public Builder withUserProfileService(UserProfileService userProfileService) {
            this.userProfileService = userProfileService;
            return this;
        }

        public Builder withDatafileConfig(DatafileConfig datafileConfig) {
            this.datafileConfig = datafileConfig;
            return this;
        }

        /**
         * Get a new {@link Builder} instance to create {@link OptimizelyManager} with.
         * @param  context the application context used to create default service if not provided.
         * @return a {@link Builder} instance
         */
        public OptimizelyManager build(Context context) {
            if (logger == null) {
                try {
                    logger = LoggerFactory.getLogger("com.optimizely.ab.android.sdk.OptimizelyManager");
                } catch (Exception e) {
                    logger = new OptimizelyLiteLogger("com.optimizely.ab.android.sdk.OptimizelyManager");
                    logger.error("Unable to generate logger from class.", e);
                } catch (Error e) {
                    logger = new OptimizelyLiteLogger("com.optimizely.ab.android.sdk.OptimizelyManager");
                    logger.error("Unable to generate logger from class.", e);
                }
            }

            if (datafileDownloadInterval > 0) {
                // AlarmManager doesn't allow intervals less than 60 seconds
                if (datafileDownloadInterval < 60) {
                    datafileDownloadInterval = 60;
                    logger.warn("Minimum datafile polling interval is 60 seconds. Defaulting to 60 seconds.");
                }
            }

            if (datafileConfig == null) {
                datafileConfig = new DatafileConfig(projectId, sdkKey);
            }

            if (datafileHandler == null) {
                datafileHandler = new DefaultDatafileHandler();
            }

            if (userProfileService == null) {
                DatafileConfig config = new DatafileConfig(projectId, sdkKey);
                userProfileService = DefaultUserProfileService.newInstance(config.getKey(), context);
            }

            if (eventHandler == null) {
                eventHandler = DefaultEventHandler.getInstance(context);
            }

            if (projectId == null && sdkKey == null) {
                logger.error("ProjectId and SDKKey cannot both be null");
                return null;
            }

            return new OptimizelyManager(projectId, sdkKey,
                    datafileConfig,
                    logger,
                    datafileDownloadInterval,
                    datafileHandler,
                    errorHandler,
                    eventDispatchInterval,
                    eventHandler,
                    userProfileService);
        }
    }
}
