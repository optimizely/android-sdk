/****************************************************************************
 * Copyright 2016-2017, Optimizely, Inc. and contributors                   *
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Abstracts the actual datafile to a cached file containing the JSONObject as a string.
 */
public class DatafileCache {

    private static final String FILENAME = "optly-data-file-%s.json";

    @NonNull private final Cache cache;
    @NonNull private final String filename;
    @NonNull private final Logger logger;

    /**
     * Create a DatafileCache Object
     * @param cacheKey key used to cache
     * @param cache shared generic file based {link Cache}
     * @param logger logger to use
     */
    public DatafileCache(@NonNull String cacheKey, @NonNull Cache cache, @NonNull Logger logger) {
        this.cache = cache;
        this.filename = String.format(FILENAME, cacheKey);
        this.logger = logger;
    }

    /**
     * Delete the datafile cache
     * @return true if successful
     */
    public boolean delete() {
        return cache.delete(filename);
    }

    /**
     * Check to see if the datafile cache exists
     * @return true if it exists
     */
    public boolean exists() {
        return cache.exists(filename);
    }

    /**
     * Return the filename to the datafile cache
     * @return filename for datafile cache
     */
    @VisibleForTesting
    public String getFileName() {
        return filename;
    }

    /**
     * Loads the datafile from cache into a JSONObject
     * @return JSONObject if exists or null if it doesn't or there was a problem
     */
    @Nullable
    public JSONObject load() {
        String datafile = cache.load(filename);

        if (datafile == null) {
            return null;
        }
        try {
            return new JSONObject(datafile);
        } catch (JSONException e) {
            logger.error("Unable to parse data file", e);
            return null;
        }
    }

    /**
     * Save a datafile to cache.
     * @param dataFile to write to cache
     * @return true if successful.
     */
    public boolean save(String dataFile) {
        return cache.save(filename, dataFile);
    }
}
