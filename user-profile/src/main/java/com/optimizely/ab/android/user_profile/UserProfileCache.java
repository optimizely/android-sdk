/****************************************************************************
 * Copyright 2017,2021, Optimizely, Inc. and contributors                   *
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

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
    @NonNull private final LegacyDiskCache legacyDiskCache;

    UserProfileCache(@NonNull DiskCache diskCache, @NonNull Logger logger,
                     @NonNull Map<String, Map<String, Object>> memoryCache,
                     @NonNull LegacyDiskCache legacyDiskCache) {
        this.logger = logger;
        this.diskCache = diskCache;
        this.memoryCache = memoryCache;
        this.legacyDiskCache = legacyDiskCache;
    }

    /**
     * Clear the in-memory and disk caches of all entries.
     */
    void clear() {
        memoryCache.clear();
        diskCache.save(memoryCache);
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
     * Migrate legacy user profiles if found.
     * <p>
     * Note: this will overwrite a newer `UserProfile` cache in the unlikely event that a legacy cache and new cache
     * both exist on disk.
     */
    @VisibleForTesting
    void migrateLegacyUserProfiles() {
        JSONObject legacyUserProfilesJson = legacyDiskCache.load();

        if (legacyUserProfilesJson == null) {
            logger.info("No legacy user profiles to migrate.");
            return;
        }

        try {
            Iterator<String> userIdIterator = legacyUserProfilesJson.keys();
            while (userIdIterator.hasNext()) {
                String userId = userIdIterator.next();
                JSONObject legacyUserProfileJson = legacyUserProfilesJson.getJSONObject(userId);

                Map<String, Map<String, String>> experimentBucketMap = new ConcurrentHashMap<>();
                Iterator<String> experimentIdIterator = legacyUserProfileJson.keys();
                while (experimentIdIterator.hasNext()) {
                    String experimentId = experimentIdIterator.next();
                    String variationId = legacyUserProfileJson.getString(experimentId);
                    Map<String, String> decisionMap = new ConcurrentHashMap<>();
                    decisionMap.put(variationIdKey, variationId);
                    experimentBucketMap.put(experimentId, decisionMap);
                }

                Map<String, Object> userProfileMap = new ConcurrentHashMap<>();
                userProfileMap.put(userIdKey, userId);
                userProfileMap.put(experimentBucketMapKey, experimentBucketMap);
                save(userProfileMap);
            }
        } catch (JSONException e) {
            logger.warn("Unable to deserialize legacy user profiles. Will delete legacy user profile cache file.", e);
        } finally {
            legacyDiskCache.delete();
        }
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
                diskCache.save(memoryCache);
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
    void remove(String userId, String experimentId) {
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
                Map<String, Map<String, String>> experimentBucketMap =
                        (ConcurrentHashMap<String, Map<String, String>>) userProfileMap.get(experimentBucketMapKey);
                if (experimentBucketMap.containsKey(experimentId)) {
                    experimentBucketMap.remove(experimentId);
                    diskCache.save(memoryCache);
                    logger.info("Removed decision for experiment {} from user profile for {}.", experimentId, userId);
                }
            }
        }
    }

    /**
     * Remove experiments that are no longer valid
     * @param validExperimentIds list of valid experiment ids.
     */
    public void removeInvalidExperiments(Set<String> validExperimentIds) {
        for (String userId : memoryCache.keySet()) {
            Map<String, Object> maps = memoryCache.get(userId);
            Map<String, Map<String, String>> experimentBucketMap =
                    (ConcurrentHashMap<String, Map<String, String>>) maps.get(experimentBucketMapKey);
            if (experimentBucketMap != null && experimentBucketMap.keySet().size() > 100) {
                for (String experimentId : experimentBucketMap.keySet()) {
                    if (!validExperimentIds.contains(experimentId)) {
                        experimentBucketMap.remove(experimentId);
                    }
                }
            }

        }
        diskCache.save(memoryCache);
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
            diskCache.save(memoryCache);
            logger.info("Saved user profile for {}.", userId);
        }
    }

    /**
     * Load the cache from disk to memory.
     */
    void start() {
        // Migrate legacy user profiles if found.
        migrateLegacyUserProfiles();

        try {
            JSONObject userProfilesJson = diskCache.load();
            Map<String, Map<String, Object>> userProfilesMap = UserProfileCacheUtils.convertJSONObjectToMap
                    (userProfilesJson);
            memoryCache.clear();
            memoryCache.putAll(userProfilesMap);
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

        private static final String FILE_NAME = "optly-user-profile-service-%s.json";
        @NonNull private final Cache cache;
        @NonNull private final Executor executor;
        @NonNull private final Logger logger;
        @NonNull private final String projectId;

        public DiskCache(@NonNull Cache cache, @NonNull Executor executor, @NonNull Logger logger,
                         @NonNull String projectId) {
            this.cache = cache;
            this.executor = executor;
            this.logger = logger;
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
        void save(final Map<String, Map<String, Object>> userProfilesMap) {
            AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void[] params) {
                    JSONObject userProfilesJson;
                    try {
                        userProfilesJson = UserProfileCacheUtils.convertMapToJSONObject(userProfilesMap);
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

    /**
     * Stores a map of userIds to a map of expIds to variationIds in a file.
     *
     * @deprecated This class is only used to migrate legacy user profiles to the new {@link UserProfileCache}.
     */
    static class LegacyDiskCache {

        private static final String FILE_NAME = "optly-user-profile-%s.json";
        @NonNull private final Cache cache;
        @NonNull private final Executor executor;
        @NonNull private final Logger logger;
        @NonNull private final String projectId;

        LegacyDiskCache(@NonNull Cache cache, @NonNull Executor executor, @NonNull Logger logger,
                        @NonNull String projectId) {
            this.cache = cache;
            this.executor = executor;
            this.logger = logger;
            this.projectId = projectId;
        }

        @VisibleForTesting
        String getFileName() {
            return String.format(FILE_NAME, projectId);
        }

        /**
         * Load legacy user profiles from disk if found.
         */
        @Nullable
        JSONObject load() {
            String cacheString = cache.load(getFileName());

            if (cacheString == null) {
                logger.info("Legacy user profile cache not found.");
                return null;
            }

            try {
                return new JSONObject(cacheString);
            } catch (JSONException e) {
                logger.warn("Unable to parse legacy user profiles. Will delete legacy user profile cache file.", e);
                delete();
                return null;
            }
        }

        /**
         * Delete the legacy user profile cache from disk in a background thread.
         */
        void delete() {
            AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void[] params) {
                    Boolean deleted = cache.delete(getFileName());
                    if (deleted) {
                        logger.info("Deleted legacy user profile from disk.");
                    } else {
                        logger.warn("Unable to delete legacy user profile from disk.");
                    }
                    return deleted;
                }
            };
            task.executeOnExecutor(executor);
        }
    }
}
