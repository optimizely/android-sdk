/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
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

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Abstracts the actual data "file" {@link java.io.File}
 */
public class DataFileCache {

    private static final String OPTLY_DATA_FILE_NAME = "optly-data-file-%s.json";

    @NonNull private final Cache cache;
    @NonNull private final String projectId;
    @NonNull private final Logger logger;

    public DataFileCache(@NonNull String projectId, @NonNull Cache cache, @NonNull Logger logger) {
        this.cache = cache;
        this.projectId = projectId;
        this.logger = logger;
    }

    @Nullable
    public JSONObject load() {
        String optlyDataFile = cache.load(getFileName());

        if (optlyDataFile == null) {
            return null;
        }
        try {
            return new JSONObject(optlyDataFile);
        } catch (JSONException e) {
            logger.error("Unable to parse data file", e);
            return null;
        }

    }

    public boolean delete() {
        return cache.delete(getFileName());
    }

    public boolean exists() {
        return cache.exists(getFileName());
    }

    public boolean save(String dataFile) {
        return cache.save(getFileName(), dataFile);
    }

    public String getFileName() {
        return String.format(OPTLY_DATA_FILE_NAME, projectId);
    }
}
