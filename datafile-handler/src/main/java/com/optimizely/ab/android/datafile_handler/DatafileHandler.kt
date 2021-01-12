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

import android.content.Context
import com.optimizely.ab.android.shared.DatafileConfig

/**
 * DatafileHandler
 */
@Deprecated(""" This interface will be replaced by the ProjectConfigManager.  If you are implementing this interface moving forward,
  you will also need to implement the {@link ProjectConfigManager} .
  class that is used to interact with the datafile_handler module. This interface can be
  overridden so that the sdk user can provide a override for the default DatafileHandler.""")
interface DatafileHandler {
    /**
     * Synchronous call to download the datafile.
     *
     * @param context   application context for download
     * @param datafileConfig DatafileConfig for the datafile
     * @return a valid datafile or null
     */
    fun downloadDatafile(context: Context?, datafileConfig: DatafileConfig): String?

    /**
     * Asynchronous download data file.
     *
     * @param context   application context for download
     * @param datafileConfig DatafileConfig for the datafile to get
     * @param listener  listener to call when datafile download complete
     */
    fun downloadDatafile(context: Context, datafileConfig: DatafileConfig, listener: DatafileLoadedListener?)
    fun downloadDatafileToCache(context: Context, datafileConfig: DatafileConfig, updateConfigOnNewDatafile: Boolean) {
        downloadDatafile(context, datafileConfig, null)
    }

    /**
     * Start background updates to the project datafile .
     *
     * @param context application context for download
     * @param datafileConfig DatafileConfig for the datafile
     * @param updateInterval frequency of updates in seconds
     * @param listener function to call when a new datafile has been detected.
     */
    fun startBackgroundUpdates(context: Context, datafileConfig: DatafileConfig, updateInterval: Long, listener: DatafileLoadedListener?)

    /**
     * Stop the background updates.
     *
     * @param context   application context for download
     * @param datafileConfig DatafileConfig for the datafile
     */
    fun stopBackgroundUpdates(context: Context, datafileConfig: DatafileConfig)

    /**
     * Save the datafile to cache.
     *
     * @param context   application context for datafile cache
     * @param datafileConfig DatafileConfig for the datafile
     * @param dataFile  the datafile to save
     */
    fun saveDatafile(context: Context?, datafileConfig: DatafileConfig, dataFile: String?)

    /**
     * Load a cached datafile if it exists
     *
     * @param context   application context for datafile cache
     * @param projectId project id of the datafile to try and get from cache
     * @return the datafile cached or null if it was not available
     */
    fun loadSavedDatafile(context: Context?, projectId: DatafileConfig): String?

    /**
     * Has the file already been cached locally?
     *
     * @param context   application context for datafile cache
     * @param datafileConfig DatafileConfig for the datafile
     * @return true if the datafile is cached or false if not.
     */
    fun isDatafileSaved(context: Context?, datafileConfig: DatafileConfig): Boolean

    /**
     * Remove the datafile in cache.
     *
     * @param context   application context for datafile cache
     * @param datafileConfig DatafileConfig for the datafile
     */
    fun removeSavedDatafile(context: Context?, datafileConfig: DatafileConfig)
}