package com.optimizely.persistent_bucketer;

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
public class PersistentBucketerCache {

    @NonNull private final Cache cache;
    @NonNull private final Logger logger;

    static final String FILE_NAME = "optly-persistent-bucketer.json";

    public PersistentBucketerCache(@NonNull Cache cache, @NonNull Logger logger) {
        this.cache = cache;
        this.logger = logger;
    }

    @NonNull
    public JSONObject load() throws JSONException {
        String persistentBucketerCache = null;
        try {
            persistentBucketerCache = cache.load(FILE_NAME);
        } catch (FileNotFoundException e) {
            logger.info("No persistent bucketer cache found");
        } catch (IOException e) {
            logger.error("Unable to load persistent bucketer cache", e);
        }

        if (persistentBucketerCache == null) {
            return new JSONObject();
        } else {
            return new JSONObject(persistentBucketerCache);
        }
    }

    public boolean save(@NonNull String userId, @NonNull String experimentId, @NonNull String variationId) {
        try {
            JSONObject persistentBucketerCache = load();
            persistentBucketerCache.put(userId, null);
            JSONObject expIdToVarId = new JSONObject();
            expIdToVarId.put(experimentId, variationId);
            persistentBucketerCache.put(userId, expIdToVarId);
            return cache.save(FILE_NAME, persistentBucketerCache.toString());
        } catch (IOException e) {
            logger.error("Unable to save persistent bucketer cache", e);
            return false;
        } catch (JSONException e) {
            logger.error("Unable to parse persistent bucketer cache", e);
            return false;
        }
    }
}
