/****************************************************************************
 * Copyright 2016-2017, Optimizely, Inc. and contributors                   *
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
import android.os.AsyncTask
import android.os.Build
import androidx.annotation.RequiresApi
import com.optimizely.ab.android.shared.DatafileConfig
import com.optimizely.ab.android.shared.OptlyStorage
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.Executor

/**
 * Handles intents and bindings in [DatafileService]
 */
class DatafileLoader(private val datafileService: DatafileService,
                     private val datafileClient: DatafileClient,
                     private val datafileCache: DatafileCache,
                     private val executor: Executor,
                     private val logger: Logger) {
    private val storage: OptlyStorage
    private var hasNotifiedListener = false
    private fun allowDownload(url: String, datafileLoadedListener: DatafileLoadedListener?): Boolean {
        val time = storage.getLong(url + datafileDownloadTime, 1)
        val last = Date(time)
        val now = Date()
        if (now.time - last.time < minTimeBetweenDownloadsMilli && datafileCache.exists()) {
            logger.debug("Last download happened under 1 minute ago. Throttled to be at least 1 minute apart.")
            datafileLoadedListener?.let { notify(it, cachedDatafile) }
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
        val requestDatafileFromClientTask = RequestDatafileFromClientTask(datafileUrl,
                datafileService,
                datafileCache,
                datafileClient,
                this,
                datafileLoadedListener,
                logger)

        // Execute tasks in order
        requestDatafileFromClientTask.executeOnExecutor(executor)
        // set the download time and don't allow downloads to overlap less than a minute
        saveDownloadTime(datafileUrl)
        logger.info("Refreshing data file")
    }

    private fun notify(datafileLoadedListener: DatafileLoadedListener?, dataFile: String?) {
        // The listener should be notified ONCE and ONLY ONCE with a valid datafile or null
        // If there are no activities bound there is no need to notify
        if (datafileLoadedListener != null && datafileService.isBound && !hasNotifiedListener) {
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

    private class RequestDatafileFromClientTask internal constructor(private val datafileUrl: String,
                                                                     private val datafileService: DatafileService,
                                                                     private val datafileCache: DatafileCache,
                                                                     private val datafileClient: DatafileClient,
                                                                     private val datafileLoader: DatafileLoader,
                                                                     private val datafileLoadedListener: DatafileLoadedListener?,
                                                                     private val logger: Logger) : AsyncTask<Void?, Void?, String?>() {
        protected override fun doInBackground(vararg params: Void?): String? {

            // if there is a problem with the cached datafile, set last modified to 1970
            if (!datafileCache.exists() || datafileCache.exists() && datafileCache.load() == null) {
                // create a wrapper for application context default storage.
                val storage = OptlyStorage(datafileService.applicationContext)
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
                val cachedDatafile = datafileLoader.cachedDatafile
                if (cachedDatafile != null) {
                    dataFile = cachedDatafile
                }
            }
            return dataFile
        }

        override fun onPostExecute(dataFile: String?) {
            // Only send null or a real datafile
            // If the datafile is empty it means we got a 304
            // We are notifying datafileLoader in either case
            // if empty or null than it should be handled in datafileLoader listener to get from cache or Raw resource
            datafileLoader.notify(datafileLoadedListener, dataFile)
            datafileService.stop()
        }
    }

    companion object {
        private const val datafileDownloadTime = "optlyDatafileDownloadTime"
        private const val minTimeBetweenDownloadsMilli = 60 * 1000L
    }

    init {
        storage = OptlyStorage(datafileService.applicationContext)
        DatafileServiceConnection(DatafileConfig("projectId", null as String?), datafileService.applicationContext, object : DatafileLoadedListener {
            override fun onDatafileLoaded(dataFile: String?) {}
            fun onStop(context: Context?) {}
        })
    }
}