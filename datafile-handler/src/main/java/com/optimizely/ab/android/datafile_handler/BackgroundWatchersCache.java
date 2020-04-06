/****************************************************************************
 * Copyright 2016-2018, Optimizely, Inc. and contributors                   *
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.DatafileConfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Caches a json dict that saves state about which project IDs have background watching enabled.
 * This is used by the rescheduler to determine if backgrounding was on for a project id.  If backgrounding is on,
 * then when the device is restarted or the app is reinstalled, the rescheduler will kick in and reschedule the datafile background
 * download.  In order to use this the rescheduler needs to be included in the application manifest.
 * Calling {@link DatafileHandler#stopBackgroundUpdates(Context, DatafileConfig)} sets this background cache to false.
 */
class BackgroundWatchersCache {
    static final String BACKGROUND_WATCHERS_FILE_NAME = "optly-background-watchers.json";
    @NonNull private final Cache cache;
    @NonNull private final Logger logger;

    static final String WATCHING = "watching";

    /**
     * Create BackgroundWatchersCache Object.
     *
     * @param cache object for caching project id and whether watched or not.
     * @param logger the logger to log errors and warnings.
     */
    BackgroundWatchersCache(@NonNull Cache cache, @NonNull Logger logger) {
        this.cache = cache;
        this.logger = logger;
    }

    /**
     * Set the watching flag for the project id.
     * @param datafileConfig project id to set watching.
     * @param watching flag to signify if the project is running in the background.
     * @return boolean indicating whether the set succeed or not
     */
    boolean setIsWatching(@NonNull DatafileConfig datafileConfig, boolean watching) {
        if (datafileConfig.getKey().isEmpty()) {
            logger.error("Passed in an empty string for projectId");
            return false;
        }

        try {
            JSONObject backgroundWatchers = load();
            if (backgroundWatchers != null) {

                backgroundWatchers.put(datafileConfig.getKey(), watching);

                return save(backgroundWatchers.toString());
            }
        } catch (JSONException e) {
            logger.error("Unable to update watching state for project id", e);
        }

        return false;
    }

    /**
     * Return if the project is set to be watched in the background or not.
     * @param datafileConfig project id to test
     * @return true if it has backgrounding, false if not.
     */
    boolean isWatching(@NonNull DatafileConfig datafileConfig) {
        if (datafileConfig.getKey().isEmpty()) {
            logger.error("Passed in an empty string for projectId");
            return false;
        }

        try {
            JSONObject backgroundWatchers = load();

            if (backgroundWatchers != null) {
                if (backgroundWatchers.has(datafileConfig.getKey())) {
                    return backgroundWatchers.getBoolean(datafileConfig.getKey());
                }
                else {
                    return false;
                }
            }
        } catch (JSONException e) {
            logger.error("Unable check if project id is being watched", e);
        }

        return false;
    }

    /**
     * Get a list of all project ids that are being watched for backgrounding.
     * @return a list of DatafileConfig
     */
    List<DatafileConfig> getWatchingDatafileConfigs() {
        List<DatafileConfig> datafileConfigs = new ArrayList<>();
        try {
            JSONObject backgroundWatchers = load();
            if (backgroundWatchers != null) {
                Iterator<String> iterator = backgroundWatchers.keys();
                while (iterator.hasNext()) {
                    final String projectKey = iterator.next();
                    if (backgroundWatchers.getBoolean(projectKey)) {
                        DatafileConfig datafileConfig = null;
                        boolean sdkKey = projectKey.matches(".*[A-Za-z].*");
                        //TODO: This should be changed to store a jsonized datafile config.
                        if (sdkKey) {
                            datafileConfig = new DatafileConfig(null, projectKey);
                        }
                        else {
                            datafileConfig = new DatafileConfig(projectKey, null);
                        }
                        datafileConfigs.add(datafileConfig);
                    }
                }
            }
        } catch (JSONException e) {
            logger.error("Unable to get watching project ids", e);
        }

        return datafileConfigs;
    }

    /**
     * Load the JSONObject from cache
     * @return JSONObject if successful. JSONObject can be empty
     * @throws JSONException if there was a problem parsing the JSON
     */
    @Nullable
    private JSONObject load() throws JSONException {
        String backGroundWatchersFile = cache.load(BACKGROUND_WATCHERS_FILE_NAME);
        if (backGroundWatchersFile == null) {
            backGroundWatchersFile = "{}";
            logger.info("Creating background watchers file {}.", BACKGROUND_WATCHERS_FILE_NAME);
        }

        return new JSONObject(backGroundWatchersFile);
    }

    /**
     * Delete the background watchers cache file.
     * @return true if successful and false if it failed.
     */
    protected boolean delete() {
        return cache.delete(BACKGROUND_WATCHERS_FILE_NAME);
    }

    /**
     * Save the JSON string to the background cache file.
     * @param backgroundWatchersJson JSON string containing projectid and whether watched or not.
     * @return true if successful.
     */
    private boolean save(String backgroundWatchersJson) {
        logger.info("Saving background watchers file {}.", BACKGROUND_WATCHERS_FILE_NAME);
        boolean saved = cache.save(BACKGROUND_WATCHERS_FILE_NAME, backgroundWatchersJson);
        if (saved) {
            logger.info("Saved background watchers file {}.", BACKGROUND_WATCHERS_FILE_NAME);
        } else {
            logger.warn("Unable to save background watchers file {}.", BACKGROUND_WATCHERS_FILE_NAME);
        }
        return saved;
    }
}
