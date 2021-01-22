/****************************************************************************
 * Copyright 2017-2018, Optimizely, Inc. and contributors                   *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                            *
 * *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.android.sdk

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.RawRes
import androidx.annotation.RequiresApi
import com.optimizely.ab.Optimizely
import com.optimizely.ab.android.datafile_handler.DatafileHandler
import com.optimizely.ab.android.datafile_handler.DatafileLoadedListener
import com.optimizely.ab.android.datafile_handler.DefaultDatafileHandler
import com.optimizely.ab.android.event_handler.DefaultEventHandler
import com.optimizely.ab.android.shared.DatafileConfig
import com.optimizely.ab.android.user_profile.DefaultUserProfileService
import com.optimizely.ab.annotations.VisibleForTesting
import com.optimizely.ab.bucketing.UserProfileService
import com.optimizely.ab.config.parser.ConfigParseException
import com.optimizely.ab.error.ErrorHandler
import com.optimizely.ab.event.BatchEventProcessor
import com.optimizely.ab.event.EventHandler
import com.optimizely.ab.event.EventProcessor
import com.optimizely.ab.notification.NotificationCenter
import com.optimizely.ab.notification.UpdateConfigNotification
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Handles loading the Optimizely data file
 */
open class OptimizelyManager internal constructor(projectId: String?,
                                             sdkKey: String?,
                                             datafileConfig: DatafileConfig?,
                                             logger: Logger,
                                             datafileDownloadInterval: Long,
                                             datafileHandler: DatafileHandler,
                                             errorHandler: ErrorHandler?,
                                             eventDispatchRetryInterval: Long,
                                             eventHandler: EventHandler,
                                             eventProcessor: EventProcessor?,
                                             userProfileService: UserProfileService,
                                             notificationCenter: NotificationCenter) {
    private var optimizelyClient = OptimizelyClient(null,
            LoggerFactory.getLogger(OptimizelyClient::class.java))
    val datafileHandler: DatafileHandler

    @get:VisibleForTesting
    val datafileDownloadInterval: Long
    private val eventDispatchRetryInterval: Long
    private var eventHandler: EventHandler? = null
    private var eventProcessor: EventProcessor? = null
    private var notificationCenter: NotificationCenter? = null
    private val errorHandler: ErrorHandler?
    private val logger: Logger
    private val projectId: String?
    private val sdkKey: String?
    val datafileConfig: DatafileConfig

    @get:VisibleForTesting
    val userProfileService: UserProfileService
    var optimizelyStartListener: OptimizelyStartListener? = null
    private fun notifyStartListener() {
        if (optimizelyStartListener != null) {
            optimizelyStartListener!!.onStart(optimizely)
            optimizelyStartListener = null
        }
    }

    /**
     * Initialize Optimizely Synchronously using the datafile passed in while downloading the latest datafile in the background from the CDN to cache.
     * It should be noted that even though it initiates a download of the datafile to cache, this method does not use that cached datafile.
     * You can always test if a datafile exists in cache with [.isDatafileCached].
     *
     *
     * Instantiates and returns an [OptimizelyClient] instance. It will also cache the instance
     * for future lookups via getClient
     *
     * @param context  any [Context] instance
     * @param datafile the datafile used to initialize the OptimizelyClient.
     * @return an [OptimizelyClient] instance
     */
    fun initialize(context: Context, datafile: String): OptimizelyClient {
        initialize(context, datafile, true)
        return optimizelyClient
    }
    /**
     * Initialize Optimizely Synchronously using the datafile passed in.
     * It should be noted that even though it initiates a download of the datafile to cache, this method does not use that cached datafile.
     * You can always test if a datafile exists in cache with [.isDatafileCached].
     *
     *
     * Instantiates and returns an [OptimizelyClient] instance. It will also cache the instance
     * for future lookups via getClient
     *
     * @param context  any [Context] instance
     * @param datafile the datafile used to initialize the OptimizelyClient.
     * @param downloadToCache to check if datafile should get updated in cache after initialization.
     * @param updateConfigOnNewDatafile When a new datafile is fetched from the server in the background thread, the SDK will be updated with the new datafile immediately if this value is set to true. When it's set to false (default), the new datafile is cached and will be used when the SDK is started again.
     * @return an [OptimizelyClient] instance
     */
    /**
     * Initialize Optimizely Synchronously using the datafile passed in.
     * It should be noted that even though it initiates a download of the datafile to cache, this method does not use that cached datafile.
     * You can always test if a datafile exists in cache with [.isDatafileCached].
     *
     *
     * Instantiates and returns an [OptimizelyClient] instance. It will also cache the instance
     * for future lookups via getClient
     *
     * @param context  any [Context] instance
     * @param datafile the datafile used to initialize the OptimizelyClient.
     * @param downloadToCache to check if datafile should get updated in cache after initialization.
     * @return an [OptimizelyClient] instance
     */
    @JvmOverloads
    fun initialize(context: Context, datafile: String?, downloadToCache: Boolean, updateConfigOnNewDatafile: Boolean = false): OptimizelyClient {
        if (!isAndroidVersionSupported) {
            return optimizelyClient
        }
        try {
            if (datafile != null) {
                if (userProfileService is DefaultUserProfileService) {
                    val defaultUserProfileService = userProfileService as DefaultUserProfileService
                    defaultUserProfileService.start()
                }
                optimizelyClient = buildOptimizely(context, datafile)
                startDatafileHandler(context)
            } else {
                logger.error("Invalid datafile")
            }
        } catch (e: ConfigParseException) {
            logger.error("Unable to parse compiled data file", e)
        } catch (e: Exception) {
            logger.error("Unable to build OptimizelyClient instance", e)
        } catch (e: Error) {
            logger.error("Unable to build OptimizelyClient instance", e)
        }
        if (downloadToCache) {
            datafileHandler.downloadDatafileToCache(context, datafileConfig, updateConfigOnNewDatafile)
        }
        return optimizelyClient
    }
    /**
     * Initialize Optimizely Synchronously by loading the resource, use it to initialize Optimizely,
     * and downloading the latest datafile from the CDN in the background to cache.
     *
     *
     * Instantiates and returns an [OptimizelyClient]  instance using the datafile cached on disk
     * if not available then it will expect that raw data file should exist on given id.
     * and initialize using raw file. Will also cache the instance
     * for future lookups via getClient. The datafile should be stored in res/raw.
     *
     * @param context     any [Context] instance
     * @param datafileRes the R id that the data file is located under.
     * @param downloadToCache to check if datafile should get updated in cache after initialization.
     * @param updateConfigOnNewDatafile When a new datafile is fetched from the server in the background thread, the SDK will be updated with the new datafile immediately if this value is set to true. When it's set to false (default), the new datafile is cached and will be used when the SDK is started again.
     * @return an [OptimizelyClient] instance
     */
    /**
     * Initialize Optimizely Synchronously by loading the resource, use it to initialize Optimizely,
     * and downloading the latest datafile from the CDN in the background to cache.
     *
     *
     * Instantiates and returns an [OptimizelyClient]  instance using the datafile cached on disk
     * if not available then it will expect that raw data file should exist on given id.
     * and initialize using raw file. Will also cache the instance
     * for future lookups via getClient. The datafile should be stored in res/raw.
     *
     * @param context     any [Context] instance
     * @param datafileRes the R id that the data file is located under.
     * @return an [OptimizelyClient] instance
     */
    @JvmOverloads
    fun initialize(context: Context, @RawRes datafileRes: Int?, downloadToCache: Boolean = true, updateConfigOnNewDatafile: Boolean = false): OptimizelyClient {
        try {
            val datafile: String?
            val datafileInCache = isDatafileCached(context)
            datafile = getDatafile(context, datafileRes)
            optimizelyClient = initialize(context, datafile, downloadToCache, updateConfigOnNewDatafile)
            if (datafileInCache) {
                cleanupUserProfileCache(userProfileService)
            }
        } catch (e: NullPointerException) {
            logger.error("Unable to find compiled data file in raw resource", e)
        }

        // return dummy client if not able to initialize a valid one
        return optimizelyClient
    }

    private fun cleanupUserProfileCache(userProfileService: UserProfileService) {
        val defaultUserProfileService: DefaultUserProfileService
        defaultUserProfileService = if (userProfileService is DefaultUserProfileService) {
            userProfileService
        } else {
            return
        }
        val config = optimizelyClient.projectConfig ?: return
        Thread {
            try {
                val experimentIds: Set<String> = config.experimentIdMapping.keys
                defaultUserProfileService.removeInvalidExperiments(experimentIds)
            } catch (e: Exception) {
                logger.error("Error removing invalid experiments from default user profile service.", e)
            }
        }.start()
    }

    /** This function will first try to get datafile from Cache, if file is not cached yet
     * than it will read from Raw file
     * @param context
     * @param datafileRes
     * @return datafile
     */
    fun getDatafile(context: Context, @RawRes datafileRes: Int?): String? {
        try {
            if (isDatafileCached(context)) {
                val datafile = datafileHandler.loadSavedDatafile(context, datafileConfig)
                if (datafile != null) {
                    return datafile
                }
            }
            return safeLoadResource(context, datafileRes)
        } catch (e: NullPointerException) {
            logger.error("Unable to find compiled data file in raw resource", e)
        }
        return null
    }

    /**
     * Starts Optimizely asynchronously
     *
     *
     * See [.initialize]
     * @param context                 any type of context instance
     * @param optimizelyStartListener callback that [OptimizelyClient] instances are sent to.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Deprecated("Consider using {@link #initialize(Context, Integer, OptimizelyStartListener)}")
    fun initialize(context: Context, optimizelyStartListener: OptimizelyStartListener) {
        initialize(context, null, optimizelyStartListener)
    }

    /**
     * Starts Optimizely asynchronously
     *
     *
     * * Attempts to fetch the most recent remote datafile and construct an [OptimizelyClient].
     * If the datafile has not changed since the SDK last fetched it or if there is an error
     * fetching, the SDK will attempt to construct an [OptimizelyClient] using a cached datafile.
     * If there is no cached datafile, then the SDK will return a dummy, uninitialized [OptimizelyClient].
     * Passing in a datafileRes will guarantee the SDK returns an initialized [OptimizelyClient].
     * @param context                 any type of context instance
     * @param datafileRes             Null is allowed here if user don't want to put datafile in res. Null handling is done in [.getDatafile]
     * @param optimizelyStartListener callback that [OptimizelyClient] instances are sent to.
     * @see .initialize
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun initialize(context: Context, @RawRes datafileRes: Int?, optimizelyStartListener: OptimizelyStartListener) {
        if (!isAndroidVersionSupported) {
            return
        }
        this.optimizelyStartListener = optimizelyStartListener
        datafileHandler.downloadDatafile(context, datafileConfig, getDatafileLoadedListener(context, datafileRes))
    }

    private fun safeLoadResource(context: Context, @RawRes datafileRes: Int?): String? {
        var resource: String? = null
        try {
            if (datafileRes != null) {
                resource = loadRawResource(context, datafileRes)
            } else {
                logger.error("Invalid datafile resource ID.")
            }
        } catch (exception: IOException) {
            logger.error("Error parsing resource", exception)
        }
        return resource
    }

    fun getDatafileLoadedListener(context: Context, @RawRes datafileRes: Int?): DatafileLoadedListener {
        return object : DatafileLoadedListener {

            override fun onDatafileLoaded(dataFile: String?) {
                if (dataFile != null && !dataFile.isEmpty()) {
                    injectOptimizely(context, userProfileService, dataFile)
                } else {
                    injectOptimizely(context, userProfileService, safeLoadResource(context, datafileRes)!!)
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun stop(activity: Activity, optlyActivityLifecycleCallbacks: OptlyActivityLifecycleCallbacks) {
        stop(activity)
        activity.application.unregisterActivityLifecycleCallbacks(optlyActivityLifecycleCallbacks)
    }

    /**
     * Called after the [DatafileService] is unbound.
     *
     *
     * Here we just cancel the start listener.
     *
     * @param context any [Context] instance
     */
    fun stop(context: Context) {
        if (!isAndroidVersionSupported) {
            return
        }
        optimizelyStartListener = null
    }// Check version and log warning if version is less than what is required.

    /**
     * Gets a cached Optimizely instance
     *
     *
     * If [.initialize] or [.initialize]
     * has not been called yet the returned [OptimizelyClient] instance will be a dummy instance
     * that logs warnings in order to prevent [NullPointerException].
     *
     *
     * Using [.initialize] or [.initialize]
     * will update the cached instance with a new [OptimizelyClient] built from a cached local
     * datafile on disk or a remote datafile on the CDN.
     *
     * @return the cached instance of [OptimizelyClient]
     */
    val optimizely: OptimizelyClient
        get() {
            // Check version and log warning if version is less than what is required.
            isAndroidVersionSupported
            return optimizelyClient
        }

    /**
     * Check if the datafile is cached on the disk
     *
     * @param context any [Context] instance
     * @return True if the datafile is cached on the disk
     */
    fun isDatafileCached(context: Context?): Boolean {
        return datafileHandler.isDatafileSaved(context, datafileConfig)
    }

    /**
     * Returns the URL of the versioned datafile that this SDK expects to use
     * @return the CDN location of the datafile
     */
    val datafileUrl: String?
        get() = datafileConfig.url

    fun getProjectId(): String {
        return projectId!!
    }

    private fun datafileDownloadEnabled(): Boolean {
        return datafileDownloadInterval > 0
    }

    private fun startDatafileHandler(context: Context) {
        if (!datafileDownloadEnabled()) {
            logger.debug("Invalid download interval, ignoring background updates.")
            return
        }
        val listener = object : DatafileLoadedListener {
            override fun onDatafileLoaded(dataFile: String?) {
                val notificationCenter = optimizely.notificationCenter
                if (notificationCenter == null) {
                    logger.debug("NotificationCenter null, not sending notification")
                }
                else {
                    notificationCenter.send(UpdateConfigNotification())
                }
            }
        }
        datafileHandler.startBackgroundUpdates(context, datafileConfig, datafileDownloadInterval, listener)
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    fun injectOptimizely(context: Context, userProfileService: UserProfileService,
                         datafile: String) {
        try {
            optimizelyClient = buildOptimizely(context, datafile)
            optimizelyClient.defaultAttributes = OptimizelyDefaultAttributes.buildDefaultAttributesMap(context, logger)
            startDatafileHandler(context)
            if (userProfileService is DefaultUserProfileService) {
                val startCallback = object : DefaultUserProfileService.StartCallback {
                    override fun onStartComplete(userProfileService: UserProfileService?) {
                        userProfileService?.let {
                            cleanupUserProfileCache(userProfileService)
                        }
                        if (optimizelyStartListener != null) {
                            logger.info("Sending Optimizely instance to listener")
                            notifyStartListener()
                        } else {
                            logger.info("No listener to send Optimizely to")
                        }
                    }

                }
                userProfileService.startInBackground(startCallback)
            } else {
                if (optimizelyStartListener != null) {
                    logger.info("Sending Optimizely instance to listener")
                    notifyStartListener()
                } else {
                    logger.info("No listener to send Optimizely to")
                }
            }
        } catch (e: Exception) {
            logger.error("Unable to build OptimizelyClient instance", e)
            if (optimizelyStartListener != null) {
                logger.info("Sending Optimizely instance to listener may be null on failure")
                notifyStartListener()
            }
        } catch (e: Error) {
            logger.error("Unable to build OptimizelyClient instance", e)
        }
    }

    @Throws(ConfigParseException::class)
    private fun buildOptimizely(context: Context, datafile: String): OptimizelyClient {
        val eventHandler = getEventHandler(context)
        val clientEngine = OptimizelyClientEngine.getClientEngineFromContext(context)
        val builder = Optimizely.builder()
        builder.withEventHandler(eventHandler)
        builder.withEventProcessor(eventProcessor)
        if (datafileHandler is DefaultDatafileHandler) {
            val handler = datafileHandler
            handler.setDatafile(datafile)
            builder.withConfigManager(handler)
        } else {
            builder.withDatafile(datafile)
        }
        builder.withClientEngine(clientEngine)
                .withClientVersion(BuildConfig.CLIENT_VERSION)
        if (errorHandler != null) {
            builder.withErrorHandler(errorHandler)
        }
        builder.withUserProfileService(userProfileService)
        builder.withNotificationCenter(notificationCenter)
        val optimizely = builder.build()
        return OptimizelyClient(optimizely, LoggerFactory.getLogger(OptimizelyClient::class.java))
    }

    fun getEventHandler(context: Context?): EventHandler? {
        if (eventHandler == null) {
            val eventHandler = DefaultEventHandler.getInstance(context!!)
            eventHandler.setDispatchInterval(eventDispatchRetryInterval)
            this.eventHandler = eventHandler
        }
        return eventHandler
    }

    fun getErrorHandler(context: Context?): ErrorHandler? {
        return errorHandler
    }

    private val isAndroidVersionSupported: Boolean
        private get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            true
        } else {
            logger.warn("Optimizely will not work on this phone.  It's Android version {} is less the minimum " +
                    "supported version {}", Build.VERSION.SDK_INT, Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            false
        }

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    class OptlyActivityLifecycleCallbacks(private val optimizelyManager: OptimizelyManager) : ActivityLifecycleCallbacks {
        /**
         * @hide
         * @see android.app.Application.ActivityLifecycleCallbacks.onActivityCreated
         */
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle) {
            // NO-OP
        }

        /**
         * @hide
         * @see android.app.Application.ActivityLifecycleCallbacks.onActivityStarted
         */
        override fun onActivityStarted(activity: Activity) {
            // NO-OP
        }

        /**
         * @hide
         * @see android.app.Application.ActivityLifecycleCallbacks.onActivityResumed
         */
        override fun onActivityResumed(activity: Activity) {
            // NO-OP
        }

        /**
         * @hide
         * @see android.app.Application.ActivityLifecycleCallbacks.onActivityPaused
         */
        override fun onActivityPaused(activity: Activity) {
            // NO-OP
        }

        /**
         * @hide
         * @see android.app.Application.ActivityLifecycleCallbacks.onActivityStopped
         */
        override fun onActivityStopped(activity: Activity) {
            optimizelyManager.stop(activity, this)
        }

        /**
         * @hide
         * @see android.app.Application.ActivityLifecycleCallbacks.onActivitySaveInstanceState
         */
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            // NO-OP
        }

        /**
         * @hide
         * @see android.app.Application.ActivityLifecycleCallbacks.onActivityDestroyed
         */
        override fun onActivityDestroyed(activity: Activity) {
            // NO-OP
        }
    }

    /**
     * Builds instances of [OptimizelyManager]
     */
    class Builder {
        private val projectId: String?

        // -1 will cause the background download to not be initiated.
        private var datafileDownloadInterval = -1L

        // -1 will disable event batching.
        private var eventFlushInterval = -1L

        // -l will disable periodic retries on event dispatch failures (but queued and retried on next event dispatch request)
        private var eventDispatchRetryInterval = -1L
        private var datafileHandler: DatafileHandler? = null
        private var logger: Logger? = null
        private var eventHandler: EventHandler? = null
        private var errorHandler: ErrorHandler? = null
        private var eventProcessor: EventProcessor? = null
        private var notificationCenter: NotificationCenter? = null
        private var userProfileService: UserProfileService? = null
        private var sdkKey: String? = null
        private var datafileConfig: DatafileConfig? = null

        @Deprecated("")
        internal constructor(projectId: String?) {
            this.projectId = projectId
        }

        internal constructor() {
            projectId = null
        }

        /**
         * Override the default [DatafileHandler].
         * @param overrideHandler datafile handler to replace default handler
         * @return this [Builder] instance
         */
        fun withDatafileHandler(overrideHandler: DatafileHandler?): Builder {
            datafileHandler = overrideHandler
            return this
        }

        fun withSDKKey(sdkKey: String?): Builder {
            this.sdkKey = sdkKey
            return this
        }

        /**
         * Override the default [Logger].
         * @param overrideHandler logger to override OptimizedlyManager and OptimizelyClient logger
         * @return this [Builder] instance
         */
        fun withLogger(overrideHandler: Logger?): Builder {
            logger = overrideHandler
            return this
        }

        /**
         * Override the default [ErrorHandler].
         * @param errorHandler  handler to override the java core error handler.
         * @return this [Builder] instance
         */
        fun withErrorHandler(errorHandler: ErrorHandler?): Builder {
            this.errorHandler = errorHandler
            return this
        }

        /**
         * Sets the interval which [DatafileService] through the [DatafileHandler] will attempt to update the
         * cached datafile.  If you set this to -1, you disable background updates.  If you don't set
         * a download interval (or set to less than 0), then no background updates will be scheduled or occur.
         * The minimum interval is 15 minutes (enforced by the Android JobScheduler API. See [android.app.job.JobInfo])
         *
         * @param interval the interval
         * @param timeUnit the time unit of the timeout argument
         * @return this [Builder] instance
         */
        fun withDatafileDownloadInterval(interval: Long, timeUnit: TimeUnit): Builder {
            datafileDownloadInterval = if (interval > 0) timeUnit.toSeconds(interval) else interval
            return this
        }

        /**
         * Sets the interval which [DatafileService] through the [DatafileHandler] will attempt to update the
         * cached datafile.  If you set this to -1, you disable background updates.  If you don't set
         * a download interval (or set to less than 0), then no background updates will be scheduled or occur.
         * The minimum interval is 900 secs (15 minutes) (enforced by the Android JobScheduler API. See [android.app.job.JobInfo])
         *
         * @param interval the interval in seconds
         * @return this [Builder] instance
         */
        @Deprecated("")
        fun withDatafileDownloadInterval(interval: Long): Builder {
            datafileDownloadInterval = interval
            return this
        }

        /**
         * Sets the interval which queued events will be flushed periodically.
         * If you don't set this value or set this to -1, the default interval will be used (30 seconds).
         *
         * @param interval the interval
         * @param timeUnit the time unit of the timeout argument
         * @return this [Builder] instance
         */
        fun withEventDispatchInterval(interval: Long, timeUnit: TimeUnit): Builder {
            eventFlushInterval = if (interval > 0) timeUnit.toMillis(interval) else interval
            return this
        }

        /**
         * Sets the interval which [EventIntentService] will retry event dispatch periodically.
         * If you don't set this value or set this to -1, periodic retries on event dispatch failures will be disabled (but still queued and retried on next event dispatch request)
         *
         * @param interval the interval
         * @param timeUnit the time unit of the timeout argument
         * @return this [Builder] instance
         */
        fun withEventDispatchRetryInterval(interval: Long, timeUnit: TimeUnit): Builder {
            eventDispatchRetryInterval = if (interval > 0) timeUnit.toMillis(interval) else interval
            return this
        }

        /**
         * Sets the interval which [EventIntentService] will retry event dispatch periodically.
         * If you don't set this value or set this to -1, periodic retries on event dispatch failures will be disabled (but still queued and retried on next event dispatch request)
         *
         * @param interval the interval in milliseconds
         * @return this [Builder] instance
         */
        @Deprecated("")
        fun withEventDispatchInterval(interval: Long): Builder {
            eventFlushInterval = interval
            eventDispatchRetryInterval = interval
            return this
        }

        /**
         * Override the default [EventHandler].
         *
         * @param eventHandler event handler to replace the default event handler
         * @return this [Builder] instance
         */
        fun withEventHandler(eventHandler: EventHandler?): Builder {
            this.eventHandler = eventHandler
            return this
        }

        /**
         * Override the default [UserProfileService].
         * @param userProfileService the user profile service to replace the default user profile service
         * @return this [Builder] instance
         */
        fun withUserProfileService(userProfileService: UserProfileService?): Builder {
            this.userProfileService = userProfileService
            return this
        }

        fun withDatafileConfig(datafileConfig: DatafileConfig?): Builder {
            this.datafileConfig = datafileConfig
            return this
        }

        fun withEventProcessor(eventProcessor: EventProcessor?): Builder {
            this.eventProcessor = eventProcessor
            return this
        }

        fun withNotificationCenter(notificationCenter: NotificationCenter?): Builder {
            this.notificationCenter = notificationCenter
            return this
        }

        /**
         * Get a new [Builder] instance to create [OptimizelyManager] with.
         * @param  context the application context used to create default service if not provided.
         * @return a [Builder] instance
         */
        fun build(context: Context?): OptimizelyManager? {
            if (logger == null) {
                try {
                    logger = LoggerFactory.getLogger("com.optimizely.ab.android.sdk.OptimizelyManager")
                } catch (e: Exception) {
                    logger = OptimizelyLiteLogger("com.optimizely.ab.android.sdk.OptimizelyManager")
                    (logger as OptimizelyLiteLogger).error("Unable to generate logger from class.", e)
                } catch (e: Error) {
                    logger = OptimizelyLiteLogger("com.optimizely.ab.android.sdk.OptimizelyManager")
                    (logger as OptimizelyLiteLogger).error("Unable to generate logger from class.", e)
                }
            }
            if (datafileDownloadInterval > 0) {
                // JobScheduler API doesn't allow intervals less than 15 minutes
//                if (datafileDownloadInterval < 900) {
//                    datafileDownloadInterval = 900;
//                    logger.warn("Minimum datafile polling interval is 15 minutes. Defaulting to 15 minutes.");
//                }
            }
            if (datafileConfig == null) {
                datafileConfig = DatafileConfig(projectId, sdkKey)
            }
            if (datafileHandler == null) {
                datafileHandler = DefaultDatafileHandler()
            }
            if (userProfileService == null) {
                val config = DatafileConfig(projectId, sdkKey)
                userProfileService = DefaultUserProfileService.newInstance(config.key, context!!)
            }
            if (eventHandler == null) {
                eventHandler = DefaultEventHandler.getInstance(context!!)
            }
            if (notificationCenter == null) {
                notificationCenter = NotificationCenter()
            }
            if (eventProcessor == null) {
                eventProcessor = BatchEventProcessor.builder()
                        .withNotificationCenter(notificationCenter)
                        .withEventHandler(eventHandler)
                        .withFlushInterval(eventFlushInterval)
                        .build()
            }
            if (projectId == null && sdkKey == null) {
                logger!!.error("ProjectId and SDKKey cannot both be null")
                return null
            }
            return OptimizelyManager(projectId, sdkKey,
                    datafileConfig,
                    logger!!,
                    datafileDownloadInterval,
                    datafileHandler!!,
                    errorHandler,
                    eventDispatchRetryInterval,
                    eventHandler!!,
                    eventProcessor,
                    userProfileService!!,
                    notificationCenter!!)
        }
    }

    companion object {
        /**
         * Returns the [OptimizelyManager] builder
         *
         * @param projectId your project's id
         * @return a [OptimizelyManager.Builder]
         */
        @Deprecated("")
        fun builder(projectId: String?): Builder {
            return Builder(projectId)
        }

        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }

        @JvmStatic
        @Throws(IOException::class)
        fun loadRawResource(context: Context, @RawRes rawRes: Int): String {
            val res = context.resources
            val `in` = res.openRawResource(rawRes)
            val b = ByteArray(`in`.available())
            val read = `in`.read(b)
            return if (read > -1) {
                String(b)
            } else {
                throw IOException("Couldn't parse raw res fixture, no bytes")
            }
        }
    }

    init {
        if (projectId == null && sdkKey == null) {
            logger.error("projectId and sdkKey are both null!")
        }
        this.projectId = projectId
        this.sdkKey = sdkKey
        if (datafileConfig == null) {
            this.datafileConfig = DatafileConfig(this.projectId, this.sdkKey)
        } else {
            this.datafileConfig = datafileConfig
        }
        this.logger = logger
        this.datafileDownloadInterval = datafileDownloadInterval
        this.datafileHandler = datafileHandler
        this.eventDispatchRetryInterval = eventDispatchRetryInterval
        this.eventHandler = eventHandler
        this.eventProcessor = eventProcessor
        this.errorHandler = errorHandler
        this.userProfileService = userProfileService
        this.notificationCenter = notificationCenter
    }
}