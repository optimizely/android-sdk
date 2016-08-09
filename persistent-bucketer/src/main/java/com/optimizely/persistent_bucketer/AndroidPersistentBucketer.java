package com.optimizely.persistent_bucketer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.bucketing.PersistentBucketer;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jdeffibaugh on 8/8/16 for Optimizely.
 *
 * Android implemenation of {@link PersistentBucketer}
 */
public class AndroidPersistentBucketer implements PersistentBucketer {

    public static PersistentBucketer newInstance(@NonNull Context context) {
        PersistentBucketerCache persistentBucketerCache =
                new PersistentBucketerCache(new Cache(context, LoggerFactory.getLogger(Cache.class)),
                        LoggerFactory.getLogger(PersistentBucketerCache.class));
        return new AndroidPersistentBucketer(persistentBucketerCache, LoggerFactory.getLogger(AndroidPersistentBucketer.class));
    }

    @NonNull private final PersistentBucketerCache persistentBucketerCache;
    @NonNull private final Logger logger;

    AndroidPersistentBucketer(@NonNull PersistentBucketerCache persistentBucketerCache, @NonNull Logger logger) {
        this.persistentBucketerCache = persistentBucketerCache;
        this.logger = logger;
    }

    @Override
    public boolean saveActivation(ProjectConfig projectConfig, String userId, Experiment experiment, Variation variation) {
        if (projectConfig == null) {
            logger.error("Received null projectConfig, unable to save activation");
            return false;
        } else if (userId == null) {
            logger.error("Received null userId, unable to save activation");
            return false;
        } else if (experiment == null) {
            logger.error("Received null experiment, unable to save activation");
            return false;
        } else if (variation == null) {
            logger.error("Received null variation, unable to save activation");
            return false;
        }
        return persistentBucketerCache.save(userId, experiment.getId(), variation.getId());
    }

    @Override
    @Nullable
    public Variation restoreActivation(ProjectConfig projectConfig, String userId, Experiment experiment) {
        if (projectConfig == null) {
            logger.error("Received null projectConfig, unable to restore activation");
            return null;
        } else if (userId == null) {
            logger.error("Received null userId, unable to restore activation");
            return null;
        } else if (experiment == null) {
            logger.error("Received null experiment, unable to restore activation");
            return null;
        }

        try {
            JSONObject persistentBucketerCache = this.persistentBucketerCache.load();
            JSONObject activationForUser = persistentBucketerCache.getJSONObject(userId);
            String variationId = activationForUser.getString(experiment.getId());
            if (projectConfig.getExperimentIdMapping().get(experiment.getId()) != null
                    && projectConfig.getExperimentIdMapping().get(experiment.getId()).getVariationIdToVariationMap().get(variationId) != null) {
                return projectConfig.getExperimentIdMapping().get(experiment.getId()).getVariationIdToVariationMap().get(variationId);
            } else {
                logger.error("Project config did not contain matching experiment and variation ids");
            }
        } catch (JSONException e) {
            logger.error("Unable to parse persistent data file cache", e);
        }

        return null;
    }
}
