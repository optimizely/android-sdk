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
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;

import org.slf4j.Logger;

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

    private final Logger logger;

    @Nullable private Optimizely optimizely;

    AndroidOptimizely(@Nullable Optimizely optimizely, @NonNull Logger logger) {
        this.optimizely = optimizely;
        this.logger = logger;
    }

    /**
     * Activate an experiment for a user
     * @see Optimizely#activate(String, String)
     * @param experimentKey the experiment key
     * @param userId the user id
     * @return the {@link Variation} the user bucketed into
     */
    public @Nullable Variation activate(@NonNull String experimentKey,
                                        @NonNull String userId) {
        if (optimizely != null) {
            return optimizely.activate(experimentKey, userId);
        } else {
            logger.warn("Optimizely is not initialized, can't activate experiment {} for user {}",
                    experimentKey, userId);
            return null;
        }
    }

    /**
     * Activate an experiment for a user
     * @see Optimizely#activate(String, String)
     * @param experimentKey the experiment key
     * @param userId the user id
     * @param attributes a map of attributes about the user
     * @return the {@link Variation} the user bucketed into
     */
    @SuppressWarnings("WeakerAccess")
    public @Nullable Variation activate(@NonNull String experimentKey,
                                        @NonNull String userId,
                                        @NonNull Map<String, String> attributes) {
        if (optimizely != null) {
            return optimizely.activate(experimentKey, userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, can't activate experiment {} for user {} " +
                    "with attributes", experimentKey, userId);
            return null;
        }
    }

    /**
     * Track an event for a user
     * @param eventName the name of the event
     * @param userId the user id
     */
    public void track(@NonNull String eventName,
                      @NonNull String userId) {
        if (optimizely != null) {
            optimizely.track(eventName, userId);
        } else {
            logger.warn("Optimizely is not initialized, could not track event {} for user {}", eventName, userId);
        }
    }

    /**
     * Track an event for a user
     * @param eventName the name of the event
     * @param userId the user id
     * @param attributes a map of attributes about the user
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
     * Track an event for a user
     * @param eventName the name of the event
     * @param userId the user id
     * @param eventValue a value to tie to the event
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
     * Track an event for a user with attributes and a value
     * @see Optimizely#track(String, String, Map, Long)
     * @param eventName the String name of the event
     * @param userId the String user id
     * @param attributes the attributes of the event
     * @param eventValue the value of the event
     */
    public void track(@NonNull String eventName,
                      @NonNull String userId,
                      @NonNull Map<String, String> attributes,
                      long eventValue) {
        if (optimizely != null) {
            optimizely.track(eventName, userId, attributes, eventValue);
        } else {
            logger.warn("Optimizely is not initialized, could not track event {} for user {}" +
                    " with value {} and attributes", eventName, userId, eventValue);
        }
    }

    /**
     * Get the variation the user is bucketed into
     * @see Optimizely#getVariation(Experiment, String)
     * @param experimentKey a String experiment key
     * @param userId a String user id
     * @return a variation for the provided experiment key and user id
     */
    @SuppressWarnings("WeakerAccess")
    public @Nullable Variation getVariation(@NonNull String experimentKey,
                                            @NonNull String userId) {
        if (optimizely != null) {
            return optimizely.getVariation(experimentKey, userId);
        } else {
            logger.warn("Optimizely is not initialized, could not get variation for experiment {} " +
                    "for user {}", experimentKey, userId);
            return null;
        }
    }

    /**
     * Get the variation the user is bucketed into
     * @see Optimizely#getVariation(Experiment, String)
     * @param experimentKey a String experiment key
     * @param userId a String userId
     * @param attributes a map of attributes
     * @return the variation for the provided experiment key, user id, and attributes
     */
    @SuppressWarnings("WeakerAccess")
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
}
