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

    @NonNull private final Cache cache;
    @NonNull private final Logger logger;

    static final String FILE_NAME = "optly-user-experiment-record.json";

    public UserExperimentRecordCache(@NonNull Cache cache, @NonNull Logger logger) {
        this.cache = cache;
        this.logger = logger;
    }

    @NonNull
    public JSONObject load() throws JSONException {
        String userExperimentRecord = null;
        try {
            userExperimentRecord = cache.load(FILE_NAME);
        } catch (FileNotFoundException e) {
            logger.info("No persistent bucketer cache found");
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
            return cache.save(FILE_NAME, userExperimentRecord.toString());
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
            return cache.save(FILE_NAME, userExperimentRecord.toString());
        } catch (IOException e) {
            logger.error("Unable to save user experiment record cache", e);
            return false;
        } catch (JSONException e) {
            logger.error("Unable to parse user experiment record cache", e);
            return false;
        }
    }
}
