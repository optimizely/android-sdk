/****************************************************************************
 * Copyright 2016-2018, Optimizely, Inc. and contributors                   *
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

import com.optimizely.ab.android.shared.DatafileConfig
import org.json.JSONObject
import org.json.JSONException
import kotlin.Throws
import com.optimizely.ab.android.datafile_handler.BackgroundWatchersCache
import com.optimizely.ab.android.shared.Cache
import org.slf4j.Logger
import java.util.ArrayList

/**
 * Caches a json dict that saves state about which project IDs have background watching enabled.
 * This is used by the rescheduler to determine if backgrounding was on for a project id.  If backgrounding is on,
 * then when the device is restarted or the app is reinstalled, the rescheduler will kick in and reschedule the datafile background
 * download.  In order to use this the rescheduler needs to be included in the application manifest.
 * Calling [DatafileHandler.stopBackgroundUpdates] sets this background cache to false.
 */
internal class BackgroundWatchersCache
/**
 * Create BackgroundWatchersCache Object.
 *
 * @param cache object for caching project id and whether watched or not.
 * @param logger the logger to log errors and warnings.
 */(private val cache: Cache, private val logger: Logger) {
    /**
     * Set the watching flag for the project id.
     * @param datafileConfig project id to set watching.
     * @param watching flag to signify if the project is running in the background.
     * @return boolean indicating whether the set succeed or not
     */
    fun setIsWatching(datafileConfig: DatafileConfig, watching: Boolean): Boolean {
        if (datafileConfig.key.isEmpty()) {
            logger.error("Passed in an empty string for projectId")
            return false
        }
        try {
            val backgroundWatchers = load()
            if (backgroundWatchers != null) {
                backgroundWatchers.put(datafileConfig.key, watching)
                return save(backgroundWatchers.toString())
            }
        } catch (e: JSONException) {
            logger.error("Unable to update watching state for project id", e)
        }
        return false
    }

    /**
     * Return if the project is set to be watched in the background or not.
     * @param datafileConfig project id to test
     * @return true if it has backgrounding, false if not.
     */
    fun isWatching(datafileConfig: DatafileConfig): Boolean {
        if (datafileConfig.key.isEmpty()) {
            logger.error("Passed in an empty string for projectId")
            return false
        }
        try {
            val backgroundWatchers = load()
            if (backgroundWatchers != null) {
                return if (backgroundWatchers.has(datafileConfig.key)) {
                    backgroundWatchers.getBoolean(datafileConfig.key)
                } else {
                    false
                }
            }
        } catch (e: JSONException) {
            logger.error("Unable check if project id is being watched", e)
        }
        return false
    }//TODO: This should be changed to store a jsonized datafile config.

    /**
     * Get a list of all project ids that are being watched for backgrounding.
     * @return a list of DatafileConfig
     */
    val watchingDatafileConfigs: List<DatafileConfig>
        get() {
            val datafileConfigs: MutableList<DatafileConfig> = ArrayList()
            try {
                val backgroundWatchers = load()
                if (backgroundWatchers != null) {
                    val iterator = backgroundWatchers.keys()
                    while (iterator.hasNext()) {
                        val projectKey = iterator.next()
                        if (backgroundWatchers.getBoolean(projectKey)) {
                            var datafileConfig: DatafileConfig? = null
                            val sdkKey = projectKey.matches(".*[A-Za-z].*".toRegex())
                            //TODO: This should be changed to store a jsonized datafile config.
                            datafileConfig = if (sdkKey) {
                                DatafileConfig(null, projectKey)
                            } else {
                                DatafileConfig(projectKey, null)
                            }
                            datafileConfigs.add(datafileConfig)
                        }
                    }
                }
            } catch (e: JSONException) {
                logger.error("Unable to get watching project ids", e)
            }
            return datafileConfigs
        }

    /**
     * Load the JSONObject from cache
     * @return JSONObject if successful. JSONObject can be empty
     * @throws JSONException if there was a problem parsing the JSON
     */
    @Throws(JSONException::class)
    private fun load(): JSONObject? {
        var backGroundWatchersFile = cache.load(BACKGROUND_WATCHERS_FILE_NAME)
        if (backGroundWatchersFile == null) {
            backGroundWatchersFile = "{}"
            logger.info("Creating background watchers file {}.", BACKGROUND_WATCHERS_FILE_NAME)
        }
        return JSONObject(backGroundWatchersFile)
    }

    /**
     * Delete the background watchers cache file.
     * @return true if successful and false if it failed.
     */
    protected fun delete(): Boolean {
        return cache.delete(BACKGROUND_WATCHERS_FILE_NAME)
    }

    /**
     * Save the JSON string to the background cache file.
     * @param backgroundWatchersJson JSON string containing projectid and whether watched or not.
     * @return true if successful.
     */
    private fun save(backgroundWatchersJson: String): Boolean {
        logger.info("Saving background watchers file {}.", BACKGROUND_WATCHERS_FILE_NAME)
        val saved = cache.save(BACKGROUND_WATCHERS_FILE_NAME, backgroundWatchersJson)
        if (saved) {
            logger.info("Saved background watchers file {}.", BACKGROUND_WATCHERS_FILE_NAME)
        } else {
            logger.warn("Unable to save background watchers file {}.", BACKGROUND_WATCHERS_FILE_NAME)
        }
        return saved
    }

    companion object {
        const val BACKGROUND_WATCHERS_FILE_NAME = "optly-background-watchers.json"
        const val WATCHING = "watching"
    }
}