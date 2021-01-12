/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
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
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import com.optimizely.ab.android.datafile_handler.DatafileService.LocalBinder
import com.optimizely.ab.android.shared.*
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

/**
 * The DatafileServiceConnection is used to bind to a DatafileService.  The DatafileService does that actual download.
 * The Service Connection kicks off the service after being connected.  The connection is unbound after a successful download.
 */
class DatafileServiceConnection
/**
 * Create a datafile service connection object.
 * @param datafileConfig for this datafile.
 * @param context current application context.
 * @param listener listener to call after service download has completed.
 */(private val datafileConfig: DatafileConfig, private val context: Context, private val listener: DatafileLoadedListener) : ServiceConnection {
    /**
     * Is the service bound?
     * @return true if it is bound.
     */
    /**
     * Set whether the service is bound or not.
     * @param bound boolean flag.
     */
    var isBound = false

    /**
     * Get the bound [DatafileService] and set it up for download.
     *
     * @see ServiceConnection.onServiceConnected
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    override fun onServiceConnected(className: ComponentName,
                                    service: IBinder) {
        if (service !is LocalBinder) {
            return
        }

        // We've bound to DatafileService, cast the IBinder and get DatafileService instance
        val datafileService = service.service
        if (datafileService != null) {
            val datafileClient = DatafileClient(
                    Client(OptlyStorage(context.applicationContext),
                            LoggerFactory.getLogger(OptlyStorage::class.java)),
                    LoggerFactory.getLogger(DatafileClient::class.java))
            val datafileCache = DatafileCache(
                    datafileConfig.key,
                    Cache(context.applicationContext, LoggerFactory.getLogger(Cache::class.java)),
                    LoggerFactory.getLogger(DatafileCache::class.java))
            val datafileLoader = DatafileLoader(datafileService,
                    datafileClient,
                    datafileCache,
                    Executors.newSingleThreadExecutor(),
                    LoggerFactory.getLogger(DatafileLoader::class.java))
            val url = datafileConfig.url

            if (datafileClient == null || url == null || datafileLoader == null) {
                return
            }
            datafileService.getDatafile(url, datafileLoader, listener)
        }
        isBound = true
    }

    /**
     * Call stop on the listener after the service has been disconnected.
     * @see ServiceConnection.onServiceDisconnected
     */
    override fun onServiceDisconnected(arg0: ComponentName) {
        isBound = false
    }
}