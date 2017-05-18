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

package com.optimizely.ab.android.user_profile;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.bucketing.Decision;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static com.optimizely.ab.bucketing.UserProfileService.experimentBucketMapKey;
import static com.optimizely.ab.bucketing.UserProfileService.userIdKey;
import static com.optimizely.ab.bucketing.UserProfileService.variationIdKey;

/**
 * Stores a map of user IDs to {@link com.optimizely.ab.bucketing.UserProfile} with write-back to a file.
 */
class UserProfileCache {

    @NonNull @VisibleForTesting protected final DiskCache diskCache;
    @NonNull private final Logger logger;
    @NonNull private final Map<String, Map<String, Object>> memoryCache;

    UserProfileCache(@NonNull DiskCache diskCache, @NonNull Logger logger,
                     @NonNull Map<String, Map<String, Object>> memoryCache) {
        this.logger = logger;
        this.diskCache = diskCache;
        this.memoryCache = memoryCache;
    }

    /**
     * Clear the in-memory and disk caches of all entries.
     */
    void clear() {
        memoryCache.clear();
        diskCache.save();
        logger.info("User profile cache cleared.");
    }

    /**
     * Lookup a user profile map in the cache by user ID.
     *
     * @param userId the user ID of the user profile
     * @return user profile from the cache if found
     */
    @Nullable
    Map<String, Object> lookup(String userId) {
        if (userId == null) {
            logger.error("Unable to lookup user profile because user ID was null.");
            return null;
        } else if (userId.isEmpty()) {
            logger.error("Unable to lookup user profile because user ID was empty.");
            return null;
        }
        return memoryCache.get(userId);
    }

    /**
     * Remove a user profile.
     *
     * @param userId the user ID of the user profile to remove
     */
    void remove(String userId) {
        if (userId == null) {
            logger.error("Unable to remove user profile because user ID was null.");
        } else if (userId.isEmpty()) {
            logger.error("Unable to remove user profile because user ID was empty.");
        } else {
            if (memoryCache.containsKey(userId)) {
                memoryCache.remove(userId);
                diskCache.save();
                logger.info("Removed user profile for {}.", userId);
            }
        }
    }

    /**
     * Remove a decision from a user profile.
     *
     * @param userId the user ID of the decision to remove
     * @param experimentId the experiment ID of the decision to remove
     */
    void removeDecision(String userId, String experimentId) {
        if (userId == null) {
            logger.error("Unable to remove decision because user ID was null.");
        } else if (userId.isEmpty()) {
            logger.error("Unable to remove decision because user ID was empty.");
        } else if (experimentId == null) {
            logger.error("Unable to remove decision because experiment ID was null.");
        } else if (experimentId.isEmpty()) {
            logger.error("Unable to remove decision because experiment ID was empty.");
        } else {
            Map<String, Object> userProfileMap = memoryCache.get(userId);
            if (userProfileMap != null) {
                Map<String, Decision> experimentBucketMap =
                        (ConcurrentHashMap<String, Decision>) userProfileMap.get(experimentBucketMapKey);
                if (experimentBucketMap.containsKey(experimentId)) {
                    experimentBucketMap.remove(experimentId);
                    diskCache.save();
                    logger.info("Removed decision for experiment {} from user profile for {}.", experimentId, userId);
                }
            }
        }
    }

    /**
     * Add a decision to a user profile.
     *
     * @param userProfileMap map representation of user profile
     */
    void save(Map<String, Object> userProfileMap) {
        String userId = (String) userProfileMap.get(userIdKey);
        if (userId == null) {
            logger.error("Unable to save user profile because user ID was null.");
        } else if (userId.isEmpty()) {
            logger.error("Unable to save user profile because user ID was empty.");
        } else {
            memoryCache.put(userId, userProfileMap);
            diskCache.save();
            logger.info("Saved user profile for {}.", userId);
        }
    }

    /**
     * Load the cache from disk to memory.
     */
    void start() {
        try {
            JSONObject userProfilesJson = diskCache.load();

            Iterator<String> userIdIterator = userProfilesJson.keys();
            while (userIdIterator.hasNext()) {
                // Convert JSONObject to map.
                String userId = userIdIterator.next();
                JSONObject userProfileJson = userProfilesJson.getJSONObject(userId);

                Map<String, Map<String, String>> experimentBucketMap =
                        new ConcurrentHashMap<String, Map<String, String>>();
                JSONObject experimentBucketMapJson = userProfileJson.getJSONObject(
                        experimentBucketMapKey);
                Iterator<String> experimentIdIterator = experimentBucketMapJson.keys();
                while (experimentIdIterator.hasNext()) {
                    String experimentId = experimentIdIterator.next();
                    JSONObject experimentBucketMapEntryJson = experimentBucketMapJson.getJSONObject(experimentId);
                    String variationId = experimentBucketMapEntryJson.getString(variationIdKey);

                    Map<String, String> decisionMap = new ConcurrentHashMap<String, String>();
                    decisionMap.put(variationIdKey, variationId);
                    experimentBucketMap.put(experimentId, decisionMap);
                }

                Map<String, Object> userProfileMap = new ConcurrentHashMap<String, Object>();
                userProfileMap.put(userIdKey, userId);
                userProfileMap.put(experimentBucketMapKey, experimentBucketMap);

                // Add map to in-memory cache.
                memoryCache.put(userId, userProfileMap);
            }
            logger.info("Loaded user profile cache from disk.");
        } catch (Exception e) {
            clear();
            logger.error("Unable to parse user profile cache from disk.", e);
        }
    }

    /**
     * Write-through cache persisted on disk.
     */
    static class DiskCache {

        private static final String FILE_NAME = "optly-user-profile-%s.json";
        @NonNull private final Cache cache;
        @NonNull private final Executor executor;
        @NonNull private final Logger logger;
        @NonNull private final Map<String, Map<String, Object>> memoryCache;
        @NonNull private final String projectId;


        public DiskCache(@NonNull Cache cache, @NonNull Executor executor, @NonNull Logger logger,
                         @NonNull Map<String, Map<String, Object>> memoryCache, @NonNull String projectId) {
            this.cache = cache;
            this.executor = executor;
            this.logger = logger;
            this.memoryCache = memoryCache;
            this.projectId = projectId;
        }

        String getFileName() {
            return String.format(FILE_NAME, projectId);
        }

        @NonNull
        JSONObject load() throws JSONException {
            String cacheString = cache.load(getFileName());
            if (cacheString == null) {
                logger.warn("Unable to load user profile cache from disk.");
                return new JSONObject();
            }
            return new JSONObject(cacheString);
        }

        /**
         * Save the in-memory cache to disk in a background thread.
         */
        void save() {
            AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void[] params) {
                    JSONObject userProfilesJson = new JSONObject();

                    try {
                        Iterator userProfileIterator = memoryCache.entrySet().iterator();
                        while (userProfileIterator.hasNext()) {
                            // Convert UserProfile to map.
                            Map.Entry userProfileEntry = (Map.Entry) userProfileIterator.next();
                            Map<String, Object> userProfileMap = (ConcurrentHashMap<String, Object>) userProfileEntry
                                    .getValue();

                            // Convert map to JSONObject.
                            String userId = (String) userProfileMap.get(userIdKey);
                            Map<String, Map<String, String>> experimentBucketMap = (Map<String, Map<String, String>>)
                                    userProfileMap.get(experimentBucketMapKey);

                            JSONObject experimentBucketMapJson = new JSONObject();
                            Iterator experimentBucketMapIterator = experimentBucketMap.entrySet().iterator();
                            while (experimentBucketMapIterator.hasNext()) {
                                Map.Entry experimentBucketMapEntry = (Map.Entry) experimentBucketMapIterator.next();
                                String experimentId = (String) experimentBucketMapEntry.getKey();
                                Map<String, String> decisionsMap = (Map<String, String>) experimentBucketMapEntry
                                        .getValue();
                                JSONObject decisionJson = new JSONObject();
                                decisionJson.put(variationIdKey, decisionsMap.get
                                        (variationIdKey));
                                experimentBucketMapJson.put(experimentId, decisionJson);
                            }

                            JSONObject userProfileJson = new JSONObject();
                            userProfileJson.put(userIdKey, userId);
                            userProfileJson.put(experimentBucketMapKey, experimentBucketMapJson);

                            // Add user profile to JSONObject of all user profiles.
                            userProfilesJson.put(userId, userProfileJson);
                        }
                    } catch (Exception e) {
                        logger.error("Unable to serialize user profiles to save to disk.", e);
                        return false;
                    }

                    // Write to disk.
                    boolean saved = cache.save(getFileName(), userProfilesJson.toString());
                    if (saved) {
                        logger.info("Saved user profiles to disk.");
                    } else {
                        logger.warn("Unable to save user profiles to disk.");
                    }
                    return saved;
                }
            };
            task.executeOnExecutor(executor);
        }
    }
}
