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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Abstracts the actual data "file" {@link java.io.File}.
 */
public class DatafileCache {

    private static final String FILENAME = "optly-data-file-%s.json";

    @NonNull private final Cache cache;
    @NonNull private final String filename;
    @NonNull private final Logger logger;

    public DatafileCache(@NonNull String projectId, @NonNull Cache cache, @NonNull Logger logger) {
        this.cache = cache;
        this.filename = String.format(FILENAME, projectId);
        this.logger = logger;
    }

    public boolean delete() {
        return cache.delete(filename);
    }

    public boolean exists() {
        return cache.exists(filename);
    }

    @VisibleForTesting
    public String getFileName() {
        return filename;
    }

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

    public boolean save(String dataFile) {
        return cache.save(filename, dataFile);
    }
}
