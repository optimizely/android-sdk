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

import android.app.Service
import android.content.Intent
import android.os.*
import androidx.annotation.RequiresApi
import com.optimizely.ab.android.datafile_handler.DatafileService
import com.optimizely.ab.android.shared.*
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

/**
 * Service that handles loading the datafile from cache or downloads it from the CDN
 * These services will only be used if you are using our [DefaultDatafileHandler].
 * You can chose to implement your own handler and use all or part of this package.
 */
class DatafileService : Service() {
    private val binder: IBinder = LocalBinder()
    @JvmField
    var logger = LoggerFactory.getLogger(DatafileService::class.java)
    var isBound = false
        private set

    /**
     * @hide
     * @see Service.onStartCommand
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.hasExtra(EXTRA_DATAFILE_CONFIG)) {
                val extraDatafileConfig = intent.getStringExtra(EXTRA_DATAFILE_CONFIG)
                val datafileConfig = DatafileConfig.fromJSONString(extraDatafileConfig)
                val datafileClient = DatafileClient(
                        Client(OptlyStorage(this.applicationContext), LoggerFactory.getLogger(OptlyStorage::class.java)),
                        LoggerFactory.getLogger(DatafileClient::class.java))
                var key = ""
                datafileConfig?.key?.let {
                    key = datafileConfig.key
                }
                val datafileCache = DatafileCache(
                        key,
                        Cache(this.applicationContext, LoggerFactory.getLogger(Cache::class.java)),
                        LoggerFactory.getLogger(DatafileCache::class.java))
                val datafileUrl = datafileConfig?.url
                val datafileLoader = DatafileLoader(this, datafileClient, datafileCache, Executors.newSingleThreadExecutor(), LoggerFactory.getLogger(DatafileLoader::class.java))
                if (datafileUrl != null) {
                    datafileLoader.getDatafile(datafileUrl, null)
                }
            } else {
                logger.warn("Data file service received an intent with no project id extra")
            }
        } else {
            logger.warn("Data file service received a null intent")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * @hide
     * @see Service.onBind
     */
    override fun onBind(intent: Intent): IBinder? {
        isBound = true
        return binder
    }

    /**
     * @hide
     * @see Service.onUnbind
     */
    override fun onUnbind(intent: Intent): Boolean {
        isBound = false
        logger.info("All clients are unbound from data file service")
        return false
    }

    fun stop() {
        stopSelf()
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    fun getDatafile(datafileUrl: String, datafileLoader: DatafileLoader, loadedListener: DatafileLoadedListener?) {
        datafileLoader.getDatafile(datafileUrl, loadedListener)
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        val service: DatafileService
            get() =// Return this instance of LocalService so clients can call public methods
                this@DatafileService
    }

    companion object {
        /**
         * Extra containing the project id this instance of Optimizely was built with
         */
        const val EXTRA_DATAFILE_CONFIG = "com.optimizely.ab.android.EXTRA_DATAFILE_CONFIG"
        const val JOB_ID = 2113
    }
}