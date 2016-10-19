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

package com.optimizely.user_experiment_record;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.bucketing.UserExperimentRecord;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Android implemenation of {@link UserExperimentRecord}
 *
 * Makes bucketing sticky.  This module is what allows the core
 * to know if a user has already been bucketed for an experiment.
 * Once a user is bucketed they will stay bucketed unless the device's
 * storage is cleared. Bucketting information is stored in a simple file.
 */
public class AndroidUserExperimentRecord implements UserExperimentRecord {

    @NonNull private final UserExperimentRecordCache diskUserExperimentRecordCache;
    @NonNull private final Logger logger;
    @NonNull private final WriteThroughCacheTaskFactory writeThroughCacheTaskFactory;

    AndroidUserExperimentRecord(@NonNull UserExperimentRecordCache diskUserExperimentRecordCache,
                                @NonNull WriteThroughCacheTaskFactory writeThroughCacheTaskFactory,
                                @NonNull Logger logger) {
        this.diskUserExperimentRecordCache = diskUserExperimentRecordCache;
        this.writeThroughCacheTaskFactory = writeThroughCacheTaskFactory;
        this.logger = logger;
    }

    /**
     * Gets a new instance of {@link AndroidUserExperimentRecord}
     *
     * @param projectId your project's id
     * @param context   an instance of {@link Context}
     * @return the instance as {@link UserExperimentRecord}
     */
    public static UserExperimentRecord newInstance(@NonNull String projectId, @NonNull Context context) {
        HashMap<String, Map<String, String>> memoryUserExperimentRecordCache = new HashMap<>();
        UserExperimentRecordCache userExperimentRecordCache =
                new UserExperimentRecordCache(projectId,
                        new Cache(context,
                        LoggerFactory.getLogger(Cache.class)),
                        LoggerFactory.getLogger(UserExperimentRecordCache.class));
        return new AndroidUserExperimentRecord(userExperimentRecordCache,
                new WriteThroughCacheTaskFactory(userExperimentRecordCache,
                        memoryUserExperimentRecordCache,
                        Executors.newSingleThreadExecutor(),
                        LoggerFactory.getLogger(WriteThroughCacheTaskFactory.class)),
                LoggerFactory.getLogger(AndroidUserExperimentRecord.class));
    }

    /**
     * Creates the file that backs {@link AndroidUserExperimentRecord}
     */
    public void start() {
        try {
            JSONObject userIdToActivationJson = diskUserExperimentRecordCache.load();
            Iterator<String> iterator1 = userIdToActivationJson.keys();
            while (iterator1.hasNext()) {
                String userId = iterator1.next();
                JSONObject expIdToVarIdJson = userIdToActivationJson.getJSONObject(userId);
                Iterator<String> iterator2 = expIdToVarIdJson.keys();
                while (iterator2.hasNext()) {
                    String expId = iterator2.next();
                    String varId = expIdToVarIdJson.getString(expId);
                    Map<String, String> expIdToVarIdMap = writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache().get(userId);
                    if (expIdToVarIdMap == null) {
                        expIdToVarIdMap = new HashMap<>();
                    }
                    expIdToVarIdMap.put(expId, varId);
                    writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache().put(userId, expIdToVarIdMap);
                }
            }
        } catch (JSONException e) {
            logger.error("Unable to parse user experiment record cache", e);
        }
    }

    /**
     * @see UserExperimentRecord#save(String, String, String)
     */
    @Override
    public boolean save(final String userId, final String experimentKey, final String variationKey) {
        if (userId == null) {
            logger.error("Received null userId, unable to save activation");
            return false;
        } else if (experimentKey == null) {
            logger.error("Received null experiment key, unable to save activation");
            return false;
        } else if (variationKey == null) {
            logger.error("Received null variation key, unable to save activation");
            return false;
        } else if (userId.isEmpty()) {
            logger.error("Received empty user id, unable to save activation");
            return false;
        } else if (experimentKey.isEmpty()) {
            logger.error("Received empty experiment key, unable to save activation");
            return false;
        } else if (variationKey.isEmpty()) {
            logger.error("Received empty variation key, unable to save activation");
            return false;
        }

        writeThroughCacheTaskFactory.startWriteCacheTask(userId, experimentKey, variationKey);

        return true;
    }

    /**
     * @see UserExperimentRecord#lookup(String, String)
     */
    @Override
    @Nullable
    public String lookup(String userId, String experimentKey) {
        if (userId == null) {
            logger.error("Received null user id, unable to lookup activation");
            return null;
        } else if (experimentKey == null) {
            logger.error("Received null experiment key, unable to lookup activation");
            return null;
        } else if (userId.isEmpty()) {
            logger.error("Received empty user id, unable to lookup activation");
            return null;
        } else if (experimentKey.isEmpty()) {
            logger.error("Received empty experiment key, unable to lookup activation");
            return null;
        }

        Map<String, String> expIdToVarIdMap = writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache().get(userId);
        String variationKey = null;
        if (expIdToVarIdMap != null) {
            variationKey = expIdToVarIdMap.get(experimentKey);
        }

        return variationKey;
    }

    /**
     * @see UserExperimentRecord#remove(String, String)
     */
    @Override
    public boolean remove(final String userId, final String experimentKey) {
        if (userId == null) {
            logger.error("Received null user id, unable to remove activation");
            return false;
        } else if (experimentKey == null) {
            logger.error("Received null experiment key, unable to remove activation");
            return false;
        } else if (userId.isEmpty()) {
            logger.error("Received empty user id, unable to remove activation");
            return false;
        } else if (experimentKey.isEmpty()) {
            logger.error("Received empty experiment key, unable to remove activation");
            return false;
        }

        Map<String, String> expKeyToVarKeyMap = writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache().get(userId);
        if (expKeyToVarKeyMap == null) {
            return false;
        }
        if (expKeyToVarKeyMap.containsKey(experimentKey)) { // Don't do anything if the mapping doesn't exist
            writeThroughCacheTaskFactory.startRemoveCacheTask(userId, experimentKey, expKeyToVarKeyMap.get(experimentKey));
        }

        return true;
    }

    /**
     * @see UserExperimentRecord#getAllRecords()
     */
    @Override
    public Map<String, Map<String, String>> getAllRecords() {
        return writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache();
    }

    static class WriteThroughCacheTaskFactory {
        @NonNull private final UserExperimentRecordCache diskUserExperimentRecordCache;
        @NonNull private final Map<String, Map<String, String>> memoryUserExperimentRecordCache;
        @NonNull private final Executor executor;
        @NonNull private final Logger logger;

        WriteThroughCacheTaskFactory(@NonNull UserExperimentRecordCache diskUserExperimentRecordCache, @NonNull Map<String, Map<String, String>> memoryUserExperimentRecordCache, @NonNull Executor executor, @NonNull Logger logger) {
            this.diskUserExperimentRecordCache = diskUserExperimentRecordCache;
            this.memoryUserExperimentRecordCache = memoryUserExperimentRecordCache;
            this.executor = executor;
            this.logger = logger;
        }

        @NonNull
        Map<String, Map<String, String>> getMemoryUserExperimentRecordCache() {
            return memoryUserExperimentRecordCache;
        }

        void startWriteCacheTask(final String userId, final String experimentKey, final String variationKey) {
            AsyncTask<Void,Void,Boolean> task = new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void[] params) {
                    return diskUserExperimentRecordCache.save(userId, experimentKey, variationKey);
                }

                @Override
                protected void onPreExecute() {
                    Map<String, String> expIdToVarIdMap = memoryUserExperimentRecordCache.get(userId);
                    if (expIdToVarIdMap == null) {
                        expIdToVarIdMap = new HashMap<>();
                    }
                    expIdToVarIdMap.put(experimentKey, variationKey);
                    memoryUserExperimentRecordCache.put(userId, expIdToVarIdMap);
                    logger.info("Updated in memory user experiment record");
                }


                @Override
                protected void onPostExecute(Boolean success) {
                    if (success) {
                        logger.info("Persisted user in variation {} for experiment {}.", variationKey, experimentKey);
                    } else {
                        // Remove the activation from the cache since saving failed
                        memoryUserExperimentRecordCache.get(userId).remove(experimentKey);
                        logger.error("Failed to persist user in variation {} for experiment {}.", variationKey, experimentKey);
                    }
                }
            };
            task.executeOnExecutor(executor);
        }

        void startRemoveCacheTask(final String userId, final String experimentKey, final String variationKey) {
            AsyncTask<String, Void, Pair<String, Boolean>> task =  new AsyncTask<String, Void, Pair<String, Boolean>>() {

                @Override
                protected void onPreExecute() {
                    Map<String, String> expIdToVarIdMap = memoryUserExperimentRecordCache.get(userId);
                    if (expIdToVarIdMap != null) {
                        expIdToVarIdMap.remove(experimentKey);
                        logger.info("Removed experimentKey: {} variationKey: {} record for user: {} from memory", experimentKey, variationKey, userId);
                    }
                }

                @Override
                protected Pair<String, Boolean> doInBackground(String... params) {
                    boolean success = diskUserExperimentRecordCache.remove(userId, experimentKey);
                    if (success) {
                        return new Pair<>(params[0], true);
                    } else {
                        // This is the variationKey
                        return new Pair<>(params[0], false);
                    }
                }

                @Override
                protected void onPostExecute(Pair<String, Boolean> result) {
                    // Put the mapping back in the write through cache if removing failed
                    if (!result.second) {
                        Map<String, String> expIdToVarIdMap = new HashMap<>();
                        expIdToVarIdMap.put(experimentKey, result.first);
                        memoryUserExperimentRecordCache.put(userId, expIdToVarIdMap);
                        logger.error("Restored experimentKey: {} variationKey: {} record for user: {} to memory", experimentKey, result.first, userId);
                    } else {
                        logger.info("Removed experimentKey: {} variationKey: {} record for user: {} from disk", experimentKey, result.first, userId);
                    }
                }
            };
            task.executeOnExecutor(executor, variationKey);
        }
    }


}
