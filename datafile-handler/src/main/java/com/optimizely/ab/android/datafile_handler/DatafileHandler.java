/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
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

package com.optimizely.ab.android.datafile_handler;

import android.content.Context;

/**
 * DatafileHandler
 * class that is used to interact with the datafile_handler module. This interface can be
 * overridden so that the sdk user can provide a override for the default DatafileHandler.
 */
public interface DatafileHandler {
    /**
     * Synchronous call to get download the datafile.
     *
     * @param context   application context for download
     * @param projectId project id of the project for the datafile
     * @return a valid datafile or null
     */
    String downloadDatafile(Context context, String projectId);

    /**
     * Asynchronous download data file.
     *
     * @param context   application context for download
     * @param projectId project id of the datafile to get
     * @param listener  listener to call when datafile download complete
     */
    void downloadDatafile(Context context, String projectId, DatafileLoadedListener listener);

    /**
     * Start background updates to the project datafile .
     *
     * @param context        application context for download
     * @param updateInterval frequency of updates in seconds
     */
    void startBackgroundUpdates(Context context, String projectId, Long updateInterval);

    /**
     * Stop the background updates.
     *
     * @param context   application context for download
     * @param projectId project id of the datafile uploading
     */
    void stopBackgroundUpdates(Context context, String projectId);

    /**
     * Save the datafile to cache.
     *
     * @param context   application context for datafile cache
     * @param projectId project id of the datafile
     * @param dataFile  the datafile to save
     */
    void saveDatafile(Context context, String projectId, String dataFile);

    /**
     * Load a cached datafile if it exists
     *
     * @param context   application context for datafile cache
     * @param projectId project id of the datafile to try and get from cache
     * @return the datafile cached or null if it was not available
     */
    String loadSavedDatafile(Context context, String projectId);

    /**
     * Has the file already been cached locally?
     *
     * @param context   application context for datafile cache
     * @param projectId
     * @return
     */
    Boolean isDatafileSaved(Context context, String projectId);

    /**
     * Remove the datafile in cache.
     *
     * @param context   application context for datafile cache
     * @param projectId project id of the datafile
     */
    void removeSavedDatafile(Context context, String projectId);
}
