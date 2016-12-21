/*
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

package com.optimizely.ab.android.user_profile;

import android.support.annotation.NonNull;

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;

/*
 * Stores a map of userIds to a map of expIds to variationIds in a file.
 */
class UserProfileCache {

    private static final String FILE_NAME = "optly-user-profile-%s.json";
    @NonNull private final String projectId;
    @NonNull private final Cache cache;
    @NonNull private final Logger logger;

    UserProfileCache(@NonNull String projectId, @NonNull Cache cache, @NonNull Logger logger) {
        this.projectId = projectId;
        this.cache = cache;
        this.logger = logger;
    }

    @NonNull
    JSONObject load() throws JSONException {
        String userProfile = cache.load(getFileName());
        if (userProfile == null) {
            return new JSONObject();
        } else {
            return new JSONObject(userProfile);
        }
    }

    boolean remove(@NonNull String userId, @NonNull String experimentId) {
        try {
            JSONObject userProfile = load();
            JSONObject expIdToVarId = userProfile.getJSONObject(userId);
            expIdToVarId.remove(experimentId);
            return cache.save(getFileName(), userProfile.toString());
        } catch (JSONException e) {
            logger.error("Unable to remove experiment for user from user profile cache", e);
            return false;
        }
    }

    boolean save(@NonNull String userId, @NonNull String experimentId, @NonNull String variationId) {
        try {
            JSONObject userProfile = load();
            JSONObject expIdToVarId = userProfile.optJSONObject(userId);
            if (expIdToVarId == null) {
                expIdToVarId = new JSONObject();
            }
            expIdToVarId.put(experimentId, variationId);
            userProfile.put(userId, expIdToVarId);
            return cache.save(getFileName(), userProfile.toString());
        } catch (JSONException e) {
            logger.error("Unable to parse user profile cache", e);
            return false;
        }
    }

    String getFileName() {
        return String.format(FILE_NAME, projectId);
    }
}
