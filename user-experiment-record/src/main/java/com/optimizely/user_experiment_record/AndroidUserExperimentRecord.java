package com.optimizely.user_experiment_record;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
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
 * Created by jdeffibaugh on 8/8/16 for Optimizely.
 * <p/>
 * Android implemenation of {@link UserExperimentRecord}
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

    public static UserExperimentRecord newInstance(@NonNull Context context) {
        HashMap<String, Map<String, String>> memoryUserExperimentRecordCache = new HashMap<>();
        UserExperimentRecordCache userExperimentRecordCache =
                new UserExperimentRecordCache(new Cache(context,
                        LoggerFactory.getLogger(Cache.class)),
                        LoggerFactory.getLogger(UserExperimentRecordCache.class));
        return new AndroidUserExperimentRecord(userExperimentRecordCache,
                new WriteThroughCacheTaskFactory(userExperimentRecordCache,
                        memoryUserExperimentRecordCache,
                        Executors.newSingleThreadExecutor(),
                        LoggerFactory.getLogger(WriteThroughCacheTaskFactory.class)),
                LoggerFactory.getLogger(AndroidUserExperimentRecord.class));
    }

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
                    Map<String, String> expIdToVarIdMap = new HashMap<>();
                    expIdToVarIdMap.put(expId, varId);
                    writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache().put(userId, expIdToVarIdMap);
                }
            }
        } catch (JSONException e) {
            logger.error("Unable to parse persistent data file cache", e);
        }
    }

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

        if (variationKey == null) {
            logger.error("Project config did not contain matching experiment and variation ids");
        }

        return variationKey;
    }

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
        if (expKeyToVarKeyMap.containsKey(experimentKey)) { // Don't do anything if the mapping doesn't exist
            writeThroughCacheTaskFactory.startRemoveCacheTask(userId, experimentKey, expKeyToVarKeyMap.get(experimentKey));
        }

        return true;
    }

    @Override
    public Map<String, Map<String, String>> records() {
        return writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache();
    }

    public static class WriteThroughCacheTaskFactory {
        @NonNull private final UserExperimentRecordCache diskUserExperimentRecordCache;

        @NonNull
        public Map<String, Map<String, String>> getMemoryUserExperimentRecordCache() {
            return memoryUserExperimentRecordCache;
        }

        @NonNull private final Map<String, Map<String, String>> memoryUserExperimentRecordCache;
        @NonNull private final Executor executor;
        @NonNull private final Logger logger;

        public WriteThroughCacheTaskFactory(@NonNull UserExperimentRecordCache diskUserExperimentRecordCache, @NonNull Map<String, Map<String, String>> memoryUserExperimentRecordCache, @NonNull Executor executor, @NonNull Logger logger) {
            this.diskUserExperimentRecordCache = diskUserExperimentRecordCache;
            this.memoryUserExperimentRecordCache = memoryUserExperimentRecordCache;
            this.executor = executor;
            this.logger = logger;
        }

        public void startWriteCacheTask(final String userId, final String experimentKey, final String variationKey) {
            AsyncTask<Void,Void,Boolean> task = new AsyncTask<Void, Void, Boolean>() {
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
                protected Boolean doInBackground(Void[] params) {
                    return diskUserExperimentRecordCache.save(userId, experimentKey, variationKey);
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

        public void startRemoveCacheTask(final String userId, final String experimentKey, final String variationKey) {
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
