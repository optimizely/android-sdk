/****************************************************************************
 * Copyright 2017-2023, Optimizely, Inc. and contributors                   *
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
import android.app.job.JobInfo;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.android.datafile_handler.DatafileHandler;
import com.optimizely.ab.android.datafile_handler.DatafileWorker;
import com.optimizely.ab.android.datafile_handler.DatafileLoadedListener;
import com.optimizely.ab.android.datafile_handler.DefaultDatafileHandler;
import com.optimizely.ab.android.event_handler.DefaultEventHandler;
import com.optimizely.ab.android.event_handler.EventDispatcher;
import com.optimizely.ab.android.odp.DefaultODPApiManager;
import com.optimizely.ab.android.odp.VuidManager;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.android.user_profile.DefaultUserProfileService;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.event.BatchEventProcessor;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.EventProcessor;
import com.optimizely.ab.event.internal.BuildVersionInfo;
import com.optimizely.ab.event.internal.ClientEngineInfo;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.notification.UpdateConfigNotification;
import com.optimizely.ab.odp.ODPApiManager;
import com.optimizely.ab.odp.ODPEventManager;
import com.optimizely.ab.odp.ODPManager;
import com.optimizely.ab.odp.ODPSegmentManager;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * Handles loading the Optimizely data file
 */
public class OptimizelyManager {

    @NonNull private OptimizelyClient optimizelyClient = new OptimizelyClient(null,
            LoggerFactory.getLogger(OptimizelyClient.class));

    @NonNull private DatafileHandler datafileHandler;
    private final long datafileDownloadInterval;
    private final long eventDispatchRetryInterval;
    @Nullable private EventHandler eventHandler = null;
    @Nullable private EventProcessor eventProcessor = null;
    @Nullable private NotificationCenter notificationCenter = null;
    @Nullable private ErrorHandler errorHandler;
    @NonNull private Logger logger;
    @Nullable private final String projectId;
    @Nullable private final String sdkKey;
    @NonNull private final DatafileConfig datafileConfig;

    @NonNull private UserProfileService userProfileService;
    @Nullable private ODPManager odpManager;
    @Nullable private final String vuid;

    @Nullable private OptimizelyStartListener optimizelyStartListener;
    private boolean returnInMainThreadFromAsyncInit = true;

    @Nullable private final List<OptimizelyDecideOption> defaultDecideOptions;
    private String sdkVersion = null;

    OptimizelyManager(@Nullable String projectId,
                      @Nullable String sdkKey,
                      @Nullable DatafileConfig datafileConfig,
                      @NonNull Logger logger,
                      long datafileDownloadInterval,
                      @NonNull DatafileHandler datafileHandler,
                      @Nullable ErrorHandler errorHandler,
                      long eventDispatchRetryInterval,
                      @NonNull EventHandler eventHandler,
                      @Nullable EventProcessor eventProcessor,
                      @NonNull UserProfileService userProfileService,
                      @NonNull NotificationCenter notificationCenter,
                      @Nullable List<OptimizelyDecideOption> defaultDecideOptions,
                      @Nullable ODPManager odpManager,
                      @Nullable String vuid) {

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
        this.eventDispatchRetryInterval = eventDispatchRetryInterval;
        this.eventHandler = eventHandler;
        this.eventProcessor = eventProcessor;
        this.errorHandler = errorHandler;
        this.userProfileService = userProfileService;
        this.vuid = vuid;
        this.odpManager = odpManager;
        this.notificationCenter = notificationCenter;
        this.defaultDecideOptions = defaultDecideOptions;

        try {
            sdkVersion = BuildConfig.CLIENT_VERSION;
            logger.info("SDK Version: {}", sdkVersion);
        } catch (Exception e) {
            logger.warn("Error getting BuildConfig version");
        }
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

    void setOptimizelyStartListener(@Nullable OptimizelyStartListener optimizelyStartListener, boolean returnInMainThread) {
        this.optimizelyStartListener = optimizelyStartListener;
        this.returnInMainThreadFromAsyncInit = returnInMainThread;
    }

    void setOptimizelyStartListener(@Nullable OptimizelyStartListener optimizelyStartListener) {
        boolean returnInMainThread = true;
        setOptimizelyStartListener(optimizelyStartListener, returnInMainThread);
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
     * Initialize Optimizely Synchronously using the datafile passed in.
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
    public OptimizelyClient initialize(@NonNull Context context, @Nullable String datafile, boolean downloadToCache) {
        return initialize(context, datafile, downloadToCache, false);
    }

    /**
     * Initialize Optimizely Synchronously using the datafile passed in.
     * It should be noted that even though it initiates a download of the datafile to cache, this method does not use that cached datafile.
     * You can always test if a datafile exists in cache with {@link #isDatafileCached(Context)}.
     * <p>
     * Instantiates and returns an {@link OptimizelyClient} instance. It will also cache the instance
     * for future lookups via getClient
     *
     * @param context  any {@link Context} instance
     * @param datafile the datafile used to initialize the OptimizelyClient.
     * @param downloadToCache to check if datafile should get updated in cache after initialization.
     * @param updateConfigOnNewDatafile When a new datafile is fetched from the server in the background thread, the SDK will be updated with the new datafile immediately if this value is set to true. When it's set to false (default), the new datafile is cached and will be used when the SDK is started again.
     * @return an {@link OptimizelyClient} instance
     */
    public OptimizelyClient initialize(@NonNull Context context, @Nullable String datafile, boolean downloadToCache, boolean updateConfigOnNewDatafile) {
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

        if (downloadToCache) {
            datafileHandler.downloadDatafileToCache(context, datafileConfig, updateConfigOnNewDatafile);
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
     * @param downloadToCache to check if datafile should get updated in cache after initialization.
     * @param updateConfigOnNewDatafile When a new datafile is fetched from the server in the background thread, the SDK will be updated with the new datafile immediately if this value is set to true. When it's set to false (default), the new datafile is cached and will be used when the SDK is started again.
     * @return an {@link OptimizelyClient} instance
     */
    @NonNull
    public OptimizelyClient initialize(@NonNull Context context, @RawRes Integer datafileRes, boolean downloadToCache, boolean updateConfigOnNewDatafile) {
        try {

            String datafile;
            Boolean datafileInCache = isDatafileCached(context);
            datafile = getDatafile(context, datafileRes);

            optimizelyClient = initialize(context, datafile, downloadToCache, updateConfigOnNewDatafile);
            if (datafileInCache) {
                cleanupUserProfileCache(getUserProfileService());
            }
        }catch (NullPointerException e){
            logger.error("Unable to find compiled data file in raw resource",e);
        }

        // return dummy client if not able to initialize a valid one
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
        return initialize(context, datafileRes, true, false);
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
     * @param context     any {@link Context} instance
     * @param datafileRes the R id that the data file is located under.
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
            return safeLoadResource(context, datafileRes);
        } catch (NullPointerException | Resources.NotFoundException e){
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
    public void initialize(
        @NonNull final Context context,
        @RawRes final Integer datafileRes,
        @NonNull OptimizelyStartListener optimizelyStartListener)
    {
        // return in main thread after async completed (backward compatible)
        boolean returnInMainThread = true;
        initialize(context, datafileRes, returnInMainThread, optimizelyStartListener);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void initialize(
        @NonNull final Context context,
        @RawRes final Integer datafileRes,
        final boolean returnInMainThread,
        @NonNull OptimizelyStartListener optimizelyStartListener)
    {
        if (!isAndroidVersionSupported()) {
            return;
        }
        setOptimizelyStartListener(optimizelyStartListener, returnInMainThread);
        datafileHandler.downloadDatafile(context, datafileConfig, getDatafileLoadedListener(context,datafileRes));
    }

    private String safeLoadResource(Context context, @RawRes final Integer datafileRes) {
        String resource = null;
        try {
            if (datafileRes != null) {
                resource = loadRawResource(context, datafileRes);
            }
            else {
                logger.error("Invalid datafile resource ID.");
            }
        }
        catch (IOException exception) {
            logger.error("Error parsing resource", exception);
        }
        return resource;
    }

    DatafileLoadedListener getDatafileLoadedListener(final Context context, @RawRes final Integer datafileRes) {
        return new DatafileLoadedListener() {
            @Override
            public void onDatafileLoaded(@Nullable String datafile) {
                if (datafile != null && !datafile.isEmpty()) {
                    injectOptimizely(context, userProfileService, datafile);
                } else {

                    injectOptimizely(context, userProfileService, safeLoadResource(context, datafileRes));
                }
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    void stop(@NonNull Activity activity, @NonNull OptlyActivityLifecycleCallbacks optlyActivityLifecycleCallbacks) {
        stop(activity);
        activity.getApplication().unregisterActivityLifecycleCallbacks(optlyActivityLifecycleCallbacks);
    }

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

    private boolean datafileDownloadEnabled() {
        return datafileDownloadInterval > 0;
    }

    private void startDatafileHandler(Context context) {
        // if already running, stop it first.
        datafileHandler.stopBackgroundUpdates(context, datafileConfig);

        if (!datafileDownloadEnabled()) {
            logger.debug("Invalid download interval, ignoring background updates.");
            return;
        }

        datafileHandler.startBackgroundUpdates(context, datafileConfig, datafileDownloadInterval, datafile1 -> {
            getOptimizely().sendUpdateConfigNotification();
        });
    }

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
                }, returnInMainThreadFromAsyncInit);
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
        builder.withEventProcessor(eventProcessor);

        if (datafileHandler instanceof DefaultDatafileHandler) {
            DefaultDatafileHandler handler = (DefaultDatafileHandler)datafileHandler;
            handler.setDatafile(datafile);
            builder.withConfigManager(handler);
        }
        else {
            builder.withDatafile(datafile);
        }

        // override client sdk name/version to be included in events
        builder.withClientInfo(clientEngine, sdkVersion);

        if (errorHandler != null) {
            builder.withErrorHandler(errorHandler);
        }

        builder.withUserProfileService(userProfileService);
        builder.withNotificationCenter(notificationCenter);
        builder.withDefaultDecideOptions(defaultDecideOptions);
        builder.withODPManager(odpManager);
        Optimizely optimizely = builder.build();

        return new OptimizelyClient(optimizely, LoggerFactory.getLogger(OptimizelyClient.class), vuid);
    }

    @NonNull
    @VisibleForTesting
    public UserProfileService getUserProfileService() {
        return userProfileService;
    }


    protected EventHandler getEventHandler(Context context) {
        if (eventHandler == null) {
            DefaultEventHandler eventHandler = DefaultEventHandler.getInstance(context);
            eventHandler.setDispatchInterval(eventDispatchRetryInterval);
            this.eventHandler = eventHandler;
        }

        return eventHandler;
    }

    protected ErrorHandler getErrorHandler(Context context) {
        return errorHandler;
    }

    protected boolean isAndroidVersionSupported() {
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
        // -1 will disable event batching.
        private long eventFlushInterval = -1L;
        // -l will disable periodic retries on event dispatch failures (but queued and retried on next event dispatch request)
        private long eventDispatchRetryInterval = -1L;
        @Nullable private DatafileHandler datafileHandler = null;
        @Nullable private Logger logger = null;
        @Nullable private EventHandler eventHandler = null;
        @Nullable private ErrorHandler errorHandler = null;
        @Nullable private EventProcessor eventProcessor = null;
        @Nullable private NotificationCenter notificationCenter = null;
        @Nullable private UserProfileService userProfileService = null;
        @Nullable private String sdkKey = null;
        @Nullable private DatafileConfig datafileConfig = null;
        @Nullable private List<OptimizelyDecideOption> defaultDecideOptions = null;
        @Nullable private ODPEventManager odpEventManager;
        @Nullable private ODPSegmentManager odpSegmentManager;

        private int odpSegmentCacheSize = 100;
        private int odpSegmentCacheTimeoutInSecs = 600;
        private int timeoutForODPSegmentFetchInSecs = 10;
        private int timeoutForODPEventDispatchInSecs = 10;
        private boolean odpEnabled = true;
        private String vuid = null;

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
         * @param overrideHandler logger to override OptimizelyManager and OptimizelyClient logger
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
         * Sets the interval which {@link DatafileWorker} will attempt to update the
         * cached datafile.  If you set this to -1, you disable background updates.  If you don't set
         * a download interval (or set to less than 0), then no background updates will be scheduled or occur.
         * The minimum interval is 15 minutes (enforced by the Android JobScheduler API. See {@link android.app.job.JobInfo})
         *
         * @param interval the interval
         * @param timeUnit the time unit of the timeout argument
         * @return this {@link Builder} instance
         */
        public Builder withDatafileDownloadInterval(long interval, TimeUnit timeUnit) {
            this.datafileDownloadInterval = interval > 0 ? timeUnit.toSeconds(interval) : interval;
            return this;
        }

        /**
         * Sets the interval which {@link DatafileWorker} will attempt to update the
         * cached datafile.  If you set this to -1, you disable background updates.  If you don't set
         * a download interval (or set to less than 0), then no background updates will be scheduled or occur.
         * The minimum interval is 900 secs (15 minutes) (enforced by the Android JobScheduler API. See {@link android.app.job.JobInfo})
         *
         * @param interval the interval in seconds
         * @return this {@link Builder} instance
         */
        @Deprecated
        public Builder withDatafileDownloadInterval(long interval) {
            this.datafileDownloadInterval = interval;
            return this;
        }

        /**
         * Sets the interval which queued events will be flushed periodically.
         * If you don't set this value or set this to -1, the default interval will be used (30 seconds).
         *
         * @param interval the interval
         * @param timeUnit the time unit of the timeout argument
         * @return this {@link Builder} instance
         */
        public Builder withEventDispatchInterval(long interval, TimeUnit timeUnit) {
            this.eventFlushInterval = interval > 0 ? timeUnit.toMillis(interval) : interval;
            return this;
        }

        /**
         * Sets the interval which {@link EventDispatcher} will retry event dispatch periodically.
         * If you don't set this value or set this to -1, periodic retries on event dispatch failures will be disabled (but still queued and retried on next event dispatch request)
         *
         * @param interval the interval
         * @param timeUnit the time unit of the timeout argument
         * @return this {@link Builder} instance
         */
        public Builder withEventDispatchRetryInterval(long interval, TimeUnit timeUnit) {
            this.eventDispatchRetryInterval = interval > 0 ? timeUnit.toMillis(interval) : interval;
            return this;
        }

        /**
         * Sets the interval which {@link EventDispatcher} will retry event dispatch periodically.
         * If you don't set this value or set this to -1, periodic retries on event dispatch failures will be disabled (but still queued and retried on next event dispatch request)
         *
         * @param interval the interval in milliseconds
         * @return this {@link Builder} instance
         */
        @Deprecated
        public Builder withEventDispatchInterval(long interval) {
            this.eventFlushInterval = interval;
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

        public Builder withEventProcessor(EventProcessor eventProcessor) {
            this.eventProcessor = eventProcessor;
            return this;
        }

        public Builder withNotificationCenter(NotificationCenter notificationCenter) {
            this.notificationCenter = notificationCenter;
            return this;
        }

        public Builder withDefaultDecideOptions(List<OptimizelyDecideOption> defaultDecideOtions) {
            this.defaultDecideOptions = defaultDecideOtions;
            return this;
        }

        /**
         * Override the default ODP segment cache size (100).
         * @param size the size
         * @return this {@link Builder} instance
         */
        public Builder withODPSegmentCacheSize(int size) {
            this.odpSegmentCacheSize = size;
            return this;
        }

        /**
         * Override the default ODP segment cache timeout (10 minutes).
         * @param interval the interval
         * @param timeUnit the time unit of the timeout argument
         * @return this {@link Builder} instance
         */
        public Builder withODPSegmentCacheTimeout(int interval, TimeUnit timeUnit) {
            this.odpSegmentCacheTimeoutInSecs = (int) timeUnit.toSeconds(interval);
            return this;
        }

        /**
         * Override the default timeout of odp segment fetch (10 seconds).
         * @param interval the interval in secs
         * @return this {@link Builder} instance
         */
        public Builder withTimeoutForODPSegmentFetch(int interval) {
            this.timeoutForODPSegmentFetchInSecs = interval;
            return this;
        }

        /**
         * Override the default timeout of odp event dispatch (10 seconds).
         * @param interval the interval in secs
         * @return this {@link Builder} instance
         */
        public Builder withTimeoutForODPEventDispatch(int interval) {
            this.timeoutForODPEventDispatchInSecs = interval;
            return this;
        }

        /**
         * Provide an optional custom {@link ODPEventManager} instance.
         *
         * A Default {@link ODPEventManager} implementation is automatically used if none provided.
         *
         * @param odpEventManager The implementation of {@link ODPEventManager}
         * @return ODPManager builder
         */
        public Builder withODPEventManager(ODPEventManager odpEventManager) {
            this.odpEventManager = odpEventManager;
            return this;
        }

        /**
         * Provide an optional custom {@link ODPSegmentManager} instance.
         *
         * A Default {@link ODPSegmentManager} implementation is automatically used if none provided.
         *
         * @param odpSegmentManager The implementation of {@link ODPSegmentManager}
         * @return this {@link Builder} instance
         */
        public Builder withODPSegmentManager(ODPSegmentManager odpSegmentManager) {
            this.odpSegmentManager = odpSegmentManager;
            return this;
        }

        /**
         * Disable ODP integration.
         * @return this {@link Builder} instance
         */
        public Builder withODPDisabled() {
            this.odpEnabled = false;
            return this;
        }

        /**
         * Override the default (SDK-generated and persistent) vuid.
         * @param vuid a user-defined vuid value
         * @return this {@link Builder} instance
         */
        public Builder withVuid(String vuid) {
            this.vuid = vuid;
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
                // JobScheduler API doesn't allow intervals less than 15 minutes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    long minIntervalSecs = TimeUnit.MILLISECONDS.toSeconds(JobInfo.getMinPeriodMillis());
                    long minIntervalMins = TimeUnit.SECONDS.toMinutes(minIntervalSecs);

                    if (datafileDownloadInterval < minIntervalSecs) {
                        datafileDownloadInterval = minIntervalSecs;
                        logger.warn("Minimum datafile polling interval is {} minutes. Defaulting to the minimum.", minIntervalMins);
                    }
                }
            }

            if (datafileConfig == null) {
                if (projectId == null && sdkKey == null) {
                    logger.error("ProjectId and SDKKey cannot both be null");
                    return null;
                }

                datafileConfig = new DatafileConfig(projectId, sdkKey);
            }

            if (datafileHandler == null) {
                datafileHandler = new DefaultDatafileHandler();
            }

            if (userProfileService == null) {
                userProfileService = DefaultUserProfileService.newInstance(datafileConfig.getKey(), context);
            }

            if (eventHandler == null) {
                DefaultEventHandler defaultHandler = DefaultEventHandler.getInstance(context);
                defaultHandler.setDispatchInterval(eventDispatchRetryInterval);
                eventHandler = defaultHandler;
            }

            if(notificationCenter == null) {
                notificationCenter = new NotificationCenter();
            }

            if(eventProcessor == null) {
                eventProcessor = BatchEventProcessor.builder()
                    .withNotificationCenter(notificationCenter)
                    .withEventHandler(eventHandler)
                    .withFlushInterval(eventFlushInterval)
                    .build();

            }

            if (vuid == null) {
                vuid = VuidManager.Companion.getShared(context).getVuid();
            }

            ODPManager odpManager = null;
            if (odpEnabled) {
                // Pass common data for android-sdk only to java-core sdk. All ODP events will include these data.
                Map<String, Object> commonData = OptimizelyDefaultAttributes.buildODPCommonData(context, logger);

                // Pass common identifiers for android-sdk only to java-core sdk. All ODP events will include these identifiers.
                Map<String, String> commonIdentifiers = (vuid != null) ? Collections.singletonMap("vuid", vuid) : Collections.emptyMap();

                ODPApiManager odpApiManager = new DefaultODPApiManager(
                    context,
                    timeoutForODPSegmentFetchInSecs,
                    timeoutForODPEventDispatchInSecs);

                // NOTE: ODPManager get updated with ODP configuration from datafile.
                //       1) ODPConfig is updated when Optimizely.class is instantiated.
                //       2) ODPConfig update is also added to the UpdateConfigNotification handler.

                odpManager = ODPManager.builder()
                        .withApiManager(odpApiManager)
                        .withSegmentCacheSize(odpSegmentCacheSize)
                        .withSegmentCacheTimeout(odpSegmentCacheTimeoutInSecs)
                        .withSegmentManager(odpSegmentManager)
                        .withEventManager(odpEventManager)
                        .withUserCommonData(commonData)
                        .withUserCommonIdentifiers(commonIdentifiers)
                        .build();
            }

            return new OptimizelyManager(projectId, sdkKey,
                    datafileConfig,
                    logger,
                    datafileDownloadInterval,
                    datafileHandler,
                    errorHandler,
                    eventDispatchRetryInterval,
                    eventHandler,
                    eventProcessor,
                    userProfileService,
                    notificationCenter,
                    defaultDecideOptions,
                    odpManager,
                    vuid);
        }
    }
}
