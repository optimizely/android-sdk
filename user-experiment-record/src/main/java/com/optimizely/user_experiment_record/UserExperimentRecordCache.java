/**
 * Copyright 2016, Optimizely
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.user_experiment_record;

import android.support.annotation.NonNull;

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by jdeffibaugh on 8/8/16 for Optimizely.
 *
 * Stores a map of userIds to a map of expIds to variationIds in a file.
 */
public class UserExperimentRecordCache {

    static final String FILE_NAME = "optly-user-experiment-record-%s.json";
    @NonNull private final String projectId;
    @NonNull private final Cache cache;
    @NonNull private final Logger logger;

    public UserExperimentRecordCache(@NonNull String projectId, @NonNull Cache cache, @NonNull Logger logger) {
        this.projectId = projectId;
        this.cache = cache;
        this.logger = logger;
    }

    @NonNull
    public JSONObject load() throws JSONException {
        String userExperimentRecord = null;
        try {
            userExperimentRecord = cache.load(getFileName());
        } catch (FileNotFoundException e) {
            logger.info("No user experiment record cache found");
        } catch (IOException e) {
            logger.error("Unable to load user experiment record cache", e);
        }

        if (userExperimentRecord == null) {
            return new JSONObject();
        } else {
            return new JSONObject(userExperimentRecord);
        }
    }

    public boolean remove(@NonNull String userId, @NonNull String experimentId) {
        try {
            JSONObject userExperimentRecord = load();
            JSONObject expIdToVarId = userExperimentRecord.getJSONObject(userId);
            expIdToVarId.remove(experimentId);
            return cache.save(getFileName(), userExperimentRecord.toString());
        } catch (IOException e) {
            logger.error("Unable to remove experiment for user from user experiment record cache", e);
            return false;
        } catch (JSONException e) {
            logger.error("Unable to remove experiment for user from user experiment record cache", e);
            return false;
        }
    }

    public boolean save(@NonNull String userId, @NonNull String experimentId, @NonNull String variationId) {
        try {
            JSONObject userExperimentRecord = load();
            userExperimentRecord.put(userId, null);
            JSONObject expIdToVarId = new JSONObject();
            expIdToVarId.put(experimentId, variationId);
            userExperimentRecord.put(userId, expIdToVarId);
            return cache.save(getFileName(), userExperimentRecord.toString());
        } catch (IOException e) {
            logger.error("Unable to save user experiment record cache", e);
            return false;
        } catch (JSONException e) {
            logger.error("Unable to parse user experiment record cache", e);
            return false;
        }
    }

    String getFileName() {
        return String.format(FILE_NAME, projectId);
    }
}
