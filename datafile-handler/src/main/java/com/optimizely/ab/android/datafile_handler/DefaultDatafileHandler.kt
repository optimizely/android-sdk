/****************************************************************************
 * Copyright 2017-2018, Optimizely, Inc. and contributors                        *
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
package com.optimizely.ab.android.datafile_handler

import android.content.*
import android.os.FileObserver
import com.optimizely.ab.android.shared.*
import com.optimizely.ab.android.shared.ServiceScheduler.PendingIntentFactory
import com.optimizely.ab.config.DatafileProjectConfig
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.ProjectConfigManager
import com.optimizely.ab.config.parser.ConfigParseException
import org.slf4j.LoggerFactory

/**
 * The default implementation of [DatafileHandler] and the main
 * interaction point to the datafile-handler module.
 */
open class DefaultDatafileHandler : DatafileHandler, ProjectConfigManager {
    private var currentProjectConfig: ProjectConfig? = null
    private var datafileServiceConnection: DatafileServiceConnection? = null
    private var fileObserver: FileObserver? = null

    /**
     * Synchronous call to download the datafile.
     * Gets the file on the current thread from the Optimizely CDN.
     *
     * @param context   application context
     * @param datafileConfig DatafileConfig for the datafile
     * @return a valid datafile or null
     */
    override fun downloadDatafile(context: Context?, datafileConfig: DatafileConfig): String? {
        val datafileClient = DatafileClient(
                Client(OptlyStorage(context!!), LoggerFactory.getLogger(OptlyStorage::class.java)),
                LoggerFactory.getLogger(DatafileClient::class.java))
        val datafileUrl = datafileConfig.url
        return datafileClient.request(datafileUrl)
    }

    /**
     * Asynchronous download data file.
     *
     *
     * We create a DatafileService intent, create a DataService connection, and bind it to the application context.
     * After we receive the datafile, we unbind the service and cleanup the service connection.
     * This gets the project file from the Optimizely CDN.
     *
     * @param context   application context
     * @param datafileConfig DatafileConfig for the datafile to get
     * @param listener  listener to call when datafile download complete
     */
    override fun downloadDatafile(context: Context, datafileConfig: DatafileConfig, listener: DatafileLoadedListener?) {
        val intent = Intent(context.applicationContext, DatafileService::class.java)
        if (datafileServiceConnection == null) {
            val dfListener = object : DatafileLoadedListener {
                override fun onDatafileLoaded(dataFile: String?) {
                    listener?.onDatafileLoaded(dataFile)
                    if (datafileServiceConnection != null && (datafileServiceConnection as DatafileServiceConnection).isBound) {
                        context.applicationContext.unbindService((datafileServiceConnection as DatafileServiceConnection))
                        datafileServiceConnection = null
                    }
                }
            }
            datafileServiceConnection = DatafileServiceConnection(datafileConfig, context.applicationContext, dfListener)

            context.applicationContext.bindService(intent, datafileServiceConnection!!, Context.BIND_AUTO_CREATE)
        }
    }

    override fun downloadDatafileToCache(context: Context, datafileConfig: DatafileConfig, updateConfigOnNewDatafile: Boolean) {
        if (updateConfigOnNewDatafile) {
            enableUpdateConfigOnNewDatafile(context, datafileConfig, null)
        }
        downloadDatafile(context, datafileConfig, null)
    }

    /**
     * Start background checks if the the project datafile jas been updated.  This starts an alarm service that checks to see if there is a
     * new datafile to download at interval provided.  If there is a update, the new datafile is cached.
     *
     * @param context        application context
     * @param datafileConfig DatafileConfig for the datafile
     * @param updateInterval frequency of updates in seconds
     */
    override fun startBackgroundUpdates(context: Context, datafileConfig: DatafileConfig, updateInterval: Long, listener: DatafileLoadedListener?) {
        // if already running, stop it
        stopBackgroundUpdates(context, datafileConfig)

        // save the project id background start is set.  If we get a reboot or a replace, we can restart via the
        // DatafileRescheduler
        enableBackgroundCache(context, datafileConfig)
        val pendingIntentFactory = PendingIntentFactory(context.applicationContext)
        val serviceScheduler = ServiceScheduler(context, pendingIntentFactory,
                LoggerFactory.getLogger(ServiceScheduler::class.java))
        val intent = Intent(context.applicationContext, DatafileService::class.java)
        intent.putExtra(DatafileService.Companion.EXTRA_DATAFILE_CONFIG, datafileConfig.toJSONString())
        serviceScheduler.schedule(intent, updateInterval * 1000)
        storeInterval(context, updateInterval * 1000)
        enableUpdateConfigOnNewDatafile(context, datafileConfig, listener)
    }

    @Synchronized
    fun enableUpdateConfigOnNewDatafile(context: Context, datafileConfig: DatafileConfig, listener: DatafileLoadedListener?) {
        // do not restart observer if already set
        if (fileObserver != null) {
            return
        }
        val datafileCache = DatafileCache(
                datafileConfig.key,
                Cache(context, LoggerFactory.getLogger(Cache::class.java)),
                LoggerFactory.getLogger(DatafileCache::class.java)
        )
        val filesFolder = context.filesDir
        fileObserver = object : FileObserver(filesFolder.path) {
            override fun onEvent(event: Int, path: String?) {
                logger.debug("EVENT: " + event.toString() + path + datafileCache.fileName)
                if (event == MODIFY && path == datafileCache.fileName) {
                    val newConfig = datafileCache.load()
                    if (newConfig == null) {
                        logger.error("Cached datafile is empty or corrupt")
                        return
                    }
                    val config = newConfig.toString()
                    setDatafile(config)
                    listener?.onDatafileLoaded(config)
                }
            }
        }
        (fileObserver as FileObserver).startWatching()
    }

    @Synchronized
    private fun disableUploadConfig() {
        if (fileObserver != null) {
            fileObserver!!.stopWatching()
            fileObserver = null
        }
    }

    /**
     * Stop the background updates.
     *
     * @param context   application context
     * @param datafileConfig DatafileConfig for the datafile
     */
    override fun stopBackgroundUpdates(context: Context, datafileConfig: DatafileConfig) {
        val pendingIntentFactory = PendingIntentFactory(context.applicationContext)
        val serviceScheduler = ServiceScheduler(context.applicationContext, pendingIntentFactory,
                LoggerFactory.getLogger(ServiceScheduler::class.java))
        val intent = Intent(context.applicationContext, DatafileService::class.java)
        serviceScheduler.unschedule(intent)
        clearBackgroundCache(context, datafileConfig)
        storeInterval(context, -1)
        disableUploadConfig()
    }

    private fun enableBackgroundCache(context: Context, datafileConfig: DatafileConfig) {
        val backgroundWatchersCache = BackgroundWatchersCache(
                Cache(context, LoggerFactory.getLogger(Cache::class.java)),
                LoggerFactory.getLogger(BackgroundWatchersCache::class.java))
        backgroundWatchersCache.setIsWatching(datafileConfig, true)
    }

    private fun clearBackgroundCache(context: Context, projectId: DatafileConfig) {
        val backgroundWatchersCache = BackgroundWatchersCache(
                Cache(context, LoggerFactory.getLogger(Cache::class.java)),
                LoggerFactory.getLogger(BackgroundWatchersCache::class.java))
        backgroundWatchersCache.setIsWatching(projectId, false)
    }

    /**
     * Save the datafile to cache.
     *
     * @param context   application context
     * @param datafileConfig DatafileConfig for the datafile
     * @param dataFile  the datafile to save
     */
    override fun saveDatafile(context: Context?, datafileConfig: DatafileConfig, dataFile: String?) {
        val datafileCache = DatafileCache(
                datafileConfig.key,
                Cache(context!!, LoggerFactory.getLogger(Cache::class.java)),
                LoggerFactory.getLogger(DatafileCache::class.java)
        )
        datafileCache.delete()
        datafileCache.save(dataFile)
    }

    /**
     * Load a cached datafile if it exists.
     *
     * @param context   application context
     * @param datafileConfig DatafileConfig for the datafile
     * @return the datafile cached or null if it was not available
     */
    override fun loadSavedDatafile(context: Context?, datafileConfig: DatafileConfig): String? {
        val datafileCache = DatafileCache(
                datafileConfig.key,
                Cache(context!!, LoggerFactory.getLogger(Cache::class.java)),
                LoggerFactory.getLogger(DatafileCache::class.java)
        )
        val datafile = datafileCache.load()
        return datafile?.toString()
    }

    /**
     * Is the datafile cached locally?
     *
     * @param context   application context
     * @param datafileConfig DatafileConfig for the datafile
     * @return true if cached false if not
     */
    override fun isDatafileSaved(context: Context?, datafileConfig: DatafileConfig): Boolean {
        val datafileCache = DatafileCache(
                datafileConfig.key,
                Cache(context!!, LoggerFactory.getLogger(Cache::class.java)),
                LoggerFactory.getLogger(DatafileCache::class.java)
        )
        return datafileCache.exists()
    }

    /**
     * Remove the datafile in cache.
     *
     * @param context   application context
     * @param datafileConfig DatafileConfig of the current datafile.
     */
    override fun removeSavedDatafile(context: Context?, datafileConfig: DatafileConfig) {
        val datafileCache = DatafileCache(
                datafileConfig.key,
                Cache(context!!, LoggerFactory.getLogger(Cache::class.java)),
                LoggerFactory.getLogger(DatafileCache::class.java)
        )
        if (datafileCache.exists()) {
            datafileCache.delete()
        }
    }

    fun setDatafile(datafile: String?) {
        if (datafile == null) {
            logger.info("datafile is null, ignoring update")
            return
        }
        if (datafile.isEmpty()) {
            logger.info("datafile is empty, ignoring update")
            return
        }
        try {
            currentProjectConfig = DatafileProjectConfig.Builder().withDatafile(datafile).build()
            logger.info("Datafile successfully loaded with revision: {}", (currentProjectConfig as ProjectConfig).getRevision())
        } catch (ex: ConfigParseException) {
            logger.error("Unable to parse the datafile", ex)
            logger.info("Datafile is invalid")
        }
    }

    override fun getConfig(): ProjectConfig {
        return currentProjectConfig!!
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultDatafileHandler::class.java)
        private fun storeInterval(context: Context, interval: Long) {
            val storage = OptlyStorage(context)
            storage.saveLong("DATAFILE_INTERVAL", interval)
        }

        fun getUpdateInterval(context: Context?): Long {
            val storage = OptlyStorage(context!!)
            return storage.getLong("DATAFILE_INTERVAL", -1)
        }
    }
}