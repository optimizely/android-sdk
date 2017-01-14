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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Pair;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.bucketing.UserProfile;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Android implemenation of {@link UserProfile}
 *
 * Makes bucketing sticky.  This module is what allows the core
 * to know if a user has already been bucketed for an experiment.
 * Once a user is bucketed they will stay bucketed unless the device's
 * storage is cleared. Bucketing information is stored in a simple file.
 */
public class AndroidUserProfile implements UserProfile {

    @NonNull private final UserProfileCache diskUserProfileCache;
    @NonNull private final Logger logger;
    @NonNull private final WriteThroughCacheTaskFactory writeThroughCacheTaskFactory;

    AndroidUserProfile(@NonNull UserProfileCache diskUserProfileCache,
                       @NonNull WriteThroughCacheTaskFactory writeThroughCacheTaskFactory,
                       @NonNull Logger logger) {
        this.diskUserProfileCache = diskUserProfileCache;
        this.writeThroughCacheTaskFactory = writeThroughCacheTaskFactory;
        this.logger = logger;
    }

    /**
     * Gets a new instance of {@link AndroidUserProfile}
     *
     * @param projectId your project's id
     * @param context   an instance of {@link Context}
     * @return the instance as {@link UserProfile}
     */
    public static UserProfile newInstance(@NonNull String projectId, @NonNull Context context) {
        Map<String, Map<String, String>> memoryUserProfileCache = new ConcurrentHashMap<>();
        UserProfileCache userProfileCache =
                new UserProfileCache(projectId,
                        new Cache(context,
                        LoggerFactory.getLogger(Cache.class)),
                        LoggerFactory.getLogger(UserProfileCache.class));
        return new AndroidUserProfile(userProfileCache,
                new WriteThroughCacheTaskFactory(userProfileCache,
                        memoryUserProfileCache,
                        Executors.newSingleThreadExecutor(),
                        LoggerFactory.getLogger(WriteThroughCacheTaskFactory.class)),
                LoggerFactory.getLogger(AndroidUserProfile.class));
    }

    /**
     * Creates the file that backs {@link AndroidUserProfile}
     */
    public void start() {
        try {
            JSONObject userIdToActivationJson = diskUserProfileCache.load();
            Iterator<String> iterator1 = userIdToActivationJson.keys();
            while (iterator1.hasNext()) {
                String userId = iterator1.next();
                JSONObject expIdToVarIdJson = userIdToActivationJson.getJSONObject(userId);
                Iterator<String> iterator2 = expIdToVarIdJson.keys();
                while (iterator2.hasNext()) {
                    String expId = iterator2.next();
                    String varId = expIdToVarIdJson.getString(expId);
                    Map<String, String> expIdToVarIdMap = writeThroughCacheTaskFactory.getMemoryUserProfileCache().get(userId);
                    if (expIdToVarIdMap == null) {
                        expIdToVarIdMap = new ConcurrentHashMap<>();
                    }
                    expIdToVarIdMap.put(expId, varId);
                    writeThroughCacheTaskFactory.getMemoryUserProfileCache().put(userId, expIdToVarIdMap);
                }
            }
        } catch (JSONException e) {
            logger.error("Unable to parse user profile cache", e);
        }
    }

    /**
     * @see UserProfile#save(String, String, String)
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean save(final String userId, final String experimentId, final String variationId) {
        if (userId == null) {
            logger.error("Received null userId, unable to save activation");
            return false;
        } else if (experimentId == null) {
            logger.error("Received null experiment ID, unable to save activation");
            return false;
        } else if (variationId == null) {
            logger.error("Received null variation ID, unable to save activation");
            return false;
        } else if (userId.isEmpty()) {
            logger.error("Received empty user ID, unable to save activation");
            return false;
        } else if (experimentId.isEmpty()) {
            logger.error("Received empty experiment ID, unable to save activation");
            return false;
        } else if (variationId.isEmpty()) {
            logger.error("Received empty variation ID, unable to save activation");
            return false;
        }

        writeThroughCacheTaskFactory.startWriteCacheTask(userId, experimentId, variationId);

        return true;
    }

    /**
     * @see UserProfile#lookup(String, String)
     */
    @Override
    @Nullable
    public String lookup(String userId, String experimentId) {
        if (userId == null) {
            logger.error("Received null user ID, unable to lookup activation");
            return null;
        } else if (experimentId == null) {
            logger.error("Received null experiment ID, unable to lookup activation");
            return null;
        } else if (userId.isEmpty()) {
            logger.error("Received empty user ID, unable to lookup activation");
            return null;
        } else if (experimentId.isEmpty()) {
            logger.error("Received empty experiment ID, unable to lookup activation");
            return null;
        }

        Map<String, String> expIdToVarIdMap = writeThroughCacheTaskFactory.getMemoryUserProfileCache().get(userId);
        String variationId = null;
        if (expIdToVarIdMap != null) {
            variationId = expIdToVarIdMap.get(experimentId);
        }

        return variationId;
    }

    /**
     * @see UserProfile#remove(String, String)
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean remove(final String userId, final String experimentId) {
        if (userId == null) {
            logger.error("Received null user id, unable to remove activation");
            return false;
        } else if (experimentId == null) {
            logger.error("Received null experiment ID, unable to remove activation");
            return false;
        } else if (userId.isEmpty()) {
            logger.error("Received empty user ID, unable to remove activation");
            return false;
        } else if (experimentId.isEmpty()) {
            logger.error("Received empty experiment ID, unable to remove activation");
            return false;
        }

        Map<String, String> expKeyToVarKeyMap = writeThroughCacheTaskFactory.getMemoryUserProfileCache().get(userId);
        if (expKeyToVarKeyMap == null) {
            return false;
        }
        if (expKeyToVarKeyMap.containsKey(experimentId)) { // Don't do anything if the mapping doesn't exist
            writeThroughCacheTaskFactory.startRemoveCacheTask(userId, experimentId, expKeyToVarKeyMap.get(experimentId));
        }

        return true;
    }

    /**
     * @see UserProfile#getAllRecords()
     */
    @Override
    public Map<String, Map<String, String>> getAllRecords() {
        return writeThroughCacheTaskFactory.getMemoryUserProfileCache();
    }

    static class WriteThroughCacheTaskFactory {
        @NonNull private final UserProfileCache diskUserProfileCache;
        @NonNull private final Map<String, Map<String, String>> memoryUserProfileCache;
        @NonNull private final Executor executor;
        @NonNull private final Logger logger;

        WriteThroughCacheTaskFactory(@NonNull UserProfileCache diskUserProfileCache, @NonNull Map<String, Map<String, String>> memoryUserProfileCache, @NonNull Executor executor, @NonNull Logger logger) {
            this.diskUserProfileCache = diskUserProfileCache;
            this.memoryUserProfileCache = memoryUserProfileCache;
            this.executor = executor;
            this.logger = logger;
        }

        @NonNull
        Map<String, Map<String, String>> getMemoryUserProfileCache() {
            return memoryUserProfileCache;
        }

        @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
        void startWriteCacheTask(final String userId, final String experimentId, final String variationId) {
            AsyncTask<Void,Void,Boolean> task = new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void[] params) {
                    return diskUserProfileCache.save(userId, experimentId, variationId);
                }

                @Override
                protected void onPreExecute() {
                    Map<String, String> expIdToVarIdMap = memoryUserProfileCache.get(userId);
                    if (expIdToVarIdMap == null) {
                        expIdToVarIdMap = new ConcurrentHashMap<>();
                    }
                    expIdToVarIdMap.put(experimentId, variationId);
                    memoryUserProfileCache.put(userId, expIdToVarIdMap);
                    logger.info("Updated in memory user profile");
                }


                @Override
                protected void onPostExecute(Boolean success) {
                    if (success) {
                        logger.info("Persisted user in variation {} for experiment {}.", variationId, experimentId);
                    } else {
                        // Remove the activation from the cache since saving failed
                        memoryUserProfileCache.get(userId).remove(experimentId);
                        logger.error("Failed to persist user in variation {} for experiment {}.", variationId, experimentId);
                    }
                }
            };
            task.executeOnExecutor(executor);
        }

        @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
        void startRemoveCacheTask(final String userId, final String experimentId, final String variationId) {
            AsyncTask<String, Void, Pair<String, Boolean>> task =  new AsyncTask<String, Void, Pair<String, Boolean>>() {

                @Override
                protected void onPreExecute() {
                    Map<String, String> expIdToVarIdMap = memoryUserProfileCache.get(userId);
                    if (expIdToVarIdMap != null) {
                        expIdToVarIdMap.remove(experimentId);
                        logger.info("Removed experimentId: {} variationId: {} record for user: {} from memory", experimentId, variationId, userId);
                    }
                }

                @Override
                protected Pair<String, Boolean> doInBackground(String... params) {
                    boolean success = diskUserProfileCache.remove(userId, experimentId);
                    if (success) {
                        return new Pair<>(params[0], true);
                    } else {
                        // This is the variationId
                        return new Pair<>(params[0], false);
                    }
                }

                @Override
                protected void onPostExecute(Pair<String, Boolean> result) {
                    // Put the mapping back in the write through cache if removing failed
                    if (!result.second) {
                        Map<String, String> expIdToVarIdMap = new ConcurrentHashMap<>();
                        expIdToVarIdMap.put(experimentId, result.first);
                        memoryUserProfileCache.put(userId, expIdToVarIdMap);
                        logger.error("Restored experimentId: {} variationId: {} record for user: {} to memory", experimentId, result.first, userId);
                    } else {
                        logger.info("Removed experimentId: {} variationId: {} record for user: {} from disk", experimentId, result.first, userId);
                    }
                }
            };
            task.executeOnExecutor(executor, variationId);
        }
    }


}
