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

import com.optimizely.ab.android.shared.Cache
import com.optimizely.ab.annotations.VisibleForTesting
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.Logger

/**
 * Abstracts the actual datafile to a cached file containing the JSONObject as a string.
 */
class DatafileCache(cacheKey: String, private val cache: Cache, logger: Logger) {
    /**
     * Return the filename to the datafile cache
     * @return filename for datafile cache
     */
    @get:VisibleForTesting
    val fileName: String
    private val logger: Logger

    /**
     * Delete the datafile cache
     * @return true if successful
     */
    fun delete(): Boolean {
        return cache.delete(fileName)
    }

    /**
     * Check to see if the datafile cache exists
     * @return true if it exists
     */
    fun exists(): Boolean {
        return cache.exists(fileName)
    }

    /**
     * Loads the datafile from cache into a JSONObject
     * @return JSONObject if exists or null if it doesn't or there was a problem
     */
    fun load(): JSONObject? {
        val datafile = cache.load(fileName) ?: return null
        return try {
            JSONObject(datafile)
        } catch (e: JSONException) {
            logger.error("Unable to parse data file", e)
            null
        }
    }

    /**
     * Save a datafile to cache.
     * @param dataFile to write to cache
     * @return true if successful.
     */
    fun save(dataFile: String?): Boolean {
        return cache.save(fileName, dataFile!!)
    }

    companion object {
        private const val FILENAME = "optly-data-file-%s.json"
    }

    /**
     * Create a DatafileCache Object
     * @param cacheKey key used to cache
     * @param cache shared generic file based {link Cache}
     * @param logger logger to use
     */
    init {
        fileName = String.format(FILENAME, cacheKey)
        this.logger = logger
    }
}