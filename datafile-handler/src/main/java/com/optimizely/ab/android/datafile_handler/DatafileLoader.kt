/****************************************************************************
 * Copyright 2016-2021, Optimizely, Inc. and contributors                   *
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

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.optimizely.ab.android.shared.OptlyStorage
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.Executors

/**
 * Handles intents and bindings in [DatafileService]
 */
class DatafileLoader(private val context: Context,
                     private val datafileClient: DatafileClient,
                     private val datafileCache: DatafileCache,
                     private val logger: Logger) {
    private val storage: OptlyStorage
    private var hasNotifiedListener = false
    private fun allowDownload(url: String, datafileLoadedListener: DatafileLoadedListener?): Boolean {
        val time = storage.getLong(url + datafileDownloadTime, 1)
        val last = Date(time)
        val now = Date()
        if (now.time - last.time < minTimeBetweenDownloadsMilli && datafileCache.exists()) {
            logger.debug("Last download happened under 1 minute ago. Throttled to be at least 1 minute apart.")
            datafileLoadedListener?.let { notifyListener(it, cachedDatafile) }
            return false
        }
        return true
    }

    private fun saveDownloadTime(url: String) {
        val time = Date().time
        storage.saveLong(url + datafileDownloadTime, time)
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    fun getDatafile(datafileUrl: String, datafileLoadedListener: DatafileLoadedListener?) {
        if (!allowDownload(datafileUrl, datafileLoadedListener)) {
            return
        }
        val executorService = Executors.newSingleThreadExecutor()
        executorService.execute { // if there is a problem with the cached datafile, set last modified to 1970
            if (!datafileCache.exists() || datafileCache.exists() && datafileCache.load() == null) {
                // create a wrapper for application context default storage.
                val storage = OptlyStorage(context)
                // set the last modified for this url to 1 millisecond past Jan 1, 1970.
                storage.saveLong(datafileUrl, 1)
            }
            var dataFile = datafileClient.request(datafileUrl)
            if (dataFile != null && !dataFile.isEmpty()) {
                if (datafileCache.exists()) {
                    if (!datafileCache.delete()) {
                        logger.warn("Unable to delete old datafile")
                    }
                }
                if (!datafileCache.save(dataFile)) {
                    logger.warn("Unable to save new datafile")
                }
            } else {
                val cachedDatafile = cachedDatafile
                if (cachedDatafile != null) {
                    dataFile = cachedDatafile
                }
            }


            // notify the listener asap and don't wait for the storage update of the time.
            notifyListener(datafileLoadedListener, dataFile)

            // set the download time and don't allow downloads to overlap less than a minute
            saveDownloadTime(datafileUrl)
            logger.info("Refreshing data file")
        }
    }

    private fun notifyListener(datafileLoadedListener: DatafileLoadedListener?, dataFile: String?) {
        // The listener should be notified ONCE and ONLY ONCE with a valid datafile or null
        // If there are no activities bound there is no need to notify
        if (datafileLoadedListener != null) {
            datafileLoadedListener.onDatafileLoaded(dataFile)
            hasNotifiedListener = true
        }
    }

    private val cachedDatafile: String?
        private get() {
            var dataFile: String? = null
            val jsonFile = datafileCache.load()
            if (jsonFile != null) {
                dataFile = jsonFile.toString()
            }
            return dataFile
        }

    companion object {
        private const val datafileDownloadTime = "optlyDatafileDownloadTime"
        private const val minTimeBetweenDownloadsMilli = 60 * 1000L
    }

    init {
        storage = OptlyStorage(context)
    }
}