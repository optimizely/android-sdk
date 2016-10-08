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
package com.optimizely.ab.android.sdk;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.UnknownEventTypeException;
import com.optimizely.ab.UnknownExperimentException;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Wraps {@link Optimizely} instances
 *
 * This proxy ensures that the Android SDK will not crash if the inner Optimizely SDK
 * failed to start.  When Optimizely fails to start via {@link OptimizelyManager#start(Activity, OptimizelyStartListener)}
 * there will be no cached instance returned from {@link OptimizelyManager#getOptimizely()}.  By accessing
 * Optimizely through this interface checking for null is not required.  If Optimizely is null warnings
 * will be logged.
 */
public class AndroidOptimizely {

    Logger logger = LoggerFactory.getLogger(AndroidOptimizely.class);

    @Nullable private Optimizely optimizely;

    AndroidOptimizely(@Nullable Optimizely optimizely) {
        this.optimizely = optimizely;
    }

    /**
     * @see Optimizely#activate(String, String)
     */
    public @Nullable Variation activate(@NonNull String experimentKey,
                       @NonNull String userId) throws UnknownExperimentException {
        if (optimizely != null) {
            return optimizely.activate(experimentKey, userId);
        } else {
            logger.warn("Optimizely is not initialized, can't activate experiment {} for {}",
                    experimentKey, userId);
            return null;
        }
    }

    /**
     * @see Optimizely#activate(String, String, Map)
     */
    public @Nullable Variation activate(@NonNull String experimentKey,
                                        @NonNull String userId,
                                        @NonNull Map<String, String> attributes) throws UnknownExperimentException {
        if (optimizely != null) {
            return optimizely.activate(experimentKey, userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, can't activate experiment {} for user {} " +
                    "with attributes", experimentKey, userId);
            return null;
        }
    }

    /**
     * @see Optimizely#activate(Experiment, String)
     */
    public @Nullable Variation activate(@NonNull Experiment experiment,
                                        @NonNull String userId) {
        if (optimizely != null) {
            return optimizely.activate(experiment, userId);
        } else {
            logger.warn("Optimizely is not initialized, can't activate experiment {} for user {}",
                    experiment.getKey(), userId);
            return null;
        }
    }

    public @Nullable Variation activate(@NonNull Experiment experiment,
                                        @NonNull String userId,
                                        @NonNull Map<String, String> attributes) {
        if (optimizely != null) {
            return optimizely.activate(experiment, userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, can't activate experiment {} for user {}, " +
                    "with attributes", experiment.getKey(), userId);
            return null;
        }
    }


    /**
     * @see Optimizely#track(String, String)
     */
    public void track(@NonNull String eventName,
                      @NonNull String userId) throws UnknownEventTypeException {
        if (optimizely != null) {
            optimizely.track(eventName, userId);
        } else {
            logger.warn("Optimizely is not initialized, could not track event {} for user {}", eventName, userId);
        }
    }

    /**
     * @see Optimizely#track(String, String, Map)
     */
    public void track(@NonNull String eventName,
                      @NonNull String userId,
                      @NonNull Map<String, String> attributes) throws UnknownEventTypeException {
        if (optimizely != null) {
            optimizely.track(eventName, userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, could not track event {} for user {}" +
                    " with attributes", eventName, userId);
        }
    }

    /**
     * @see Optimizely#track(String, String, long)
     */
    public void track(@NonNull String eventName,
                      @NonNull String userId,
                      long eventValue) throws UnknownEventTypeException {
        if (optimizely != null) {
            optimizely.track(eventName, userId, eventValue);
        } else {
            logger.warn("Optimizely is not initialized, could not track event {} for user {}" +
                    " with value {}", eventName, userId, eventValue);
        }
    }

    /**
     * @see Optimizely#track(String, String, Map, long)
     */
    public void track(@NonNull String eventName,
                      @NonNull String userId,
                      @NonNull Map<String, String> attributes,
                      long eventValue) throws UnknownEventTypeException {
        if (optimizely != null) {
            optimizely.track(eventName, userId, attributes, eventValue);
        } else {
            logger.warn("Optimizely is not initialized, could not track event {} for user {}" +
                    " with value {} and attributes", eventName, userId, eventValue);
        }
    }


    /**
     * @see Optimizely#getVariation(Experiment, String)
     */
    public @Nullable Variation getVariation(@NonNull Experiment experiment,
                                            @NonNull String userId) throws UnknownExperimentException {
        if (optimizely != null) {
            return optimizely.getVariation(experiment, userId);
        } else {
            logger.warn("Optimizely is not initialized, could not get variation for experiment {} " +
                    "for user {}", experiment.getKey(), userId);
            return null;
        }
    }

    /**
     * @see Optimizely#getVariation(String, String)
     */
    public @Nullable Variation getVariation(@NonNull String experimentKey,
                                            @NonNull String userId) throws UnknownExperimentException{
        if (optimizely != null) {
            return getVariation(experimentKey, userId);
        } else {
            logger.warn("Optimizely is not initialized, could not get variation for experiment {} " +
                    "for user {}", experimentKey, userId);
            return null;
        }
    }

    /**
     * @see Optimizely#getVariation(String, String, Map)
     */
    public @Nullable Variation getVariation(@NonNull String experimentKey,
                                            @NonNull String userId,
                                            @NonNull Map<String, String> attributes) {
        if (optimizely != null) {
            return optimizely.getVariation(experimentKey, userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, could not get variation for experiment {} " +
                    "for user {} with attributes", experimentKey, userId);
            return null;
        }
    }

    /**
     * @see Optimizely#getVariation(ProjectConfig, Experiment, Map, String)
     */
    public @Nullable Variation getVariation(@NonNull ProjectConfig projectConfig,
                                            @NonNull Experiment experiment,
                                            @NonNull Map<String, String> attributes,
                                            @NonNull String userId) {
        if (optimizely != null) {
            return optimizely.getVariation(projectConfig, experiment, attributes, userId);
        } else {
            logger.warn("Optimizely is not initialized, could not get variation for experiment {} " +
                    "for user {} with attributes and project config", experiment.getKey(), userId);
            return null;
        }
    }
}
