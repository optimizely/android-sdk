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

package com.optimizely.ab.android.sdk;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.UnknownEventTypeException;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.notification.NotificationListener;

import org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;

/**
 * Wraps {@link Optimizely} instances
 *
 * This proxy ensures that the Android SDK will not crash if the inner Optimizely SDK
 * failed to start.  When Optimizely fails to start via {@link OptimizelyManager#initialize(Activity, OptimizelyStartListener)}
 * there will be no cached instance returned from {@link OptimizelyManager#getOptimizely()}.  By accessing
 * Optimizely through this interface checking for null is not required.  If Optimizely is null warnings
 * will be logged.
 */
public class OptimizelyClient {

    private final Logger logger;

    @Nullable private Optimizely optimizely;
    @Nullable private Map<String, String> defaultAttributes;

    OptimizelyClient(@Nullable Optimizely optimizely, @NonNull Logger logger) {
        this.optimizely = optimizely;
        this.logger = logger;
    }

    protected void setDefaultAttributes(Map<String, String> attrs) {
        this.defaultAttributes = attrs;
    }

    public Map<String, String> getDefaultAttributes() {
        return this.defaultAttributes;
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
        if (isValid()) {
            return optimizely.activate(experimentKey, userId);
        } else {
            logger.warn("Optimizely is not initialized, could not activate experiment {} for user {}",
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
        if (isValid()) {
            attributes.putAll(defaultAttributes);
            return optimizely.activate(experimentKey, userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, could not activate experiment {} for user {} " +
                    "with attributes", experimentKey, userId);
            return null;
        }
    }

    /**
     * Get the {@link ProjectConfig} instance
     * @return the current {@link ProjectConfig} instance
     */
    public @Nullable ProjectConfig getProjectConfig() {
        if (isValid()) {
            return optimizely.getProjectConfig();
        } else {
            logger.warn("Optimizely is not initialized, could not get project config");
            return null;
        }
    }

    /**
     * Check that this is a valid instance
     * @return True if the OptimizelyClient instance was instantiated correctly
     */
    public boolean isValid() {
        return optimizely != null;
    }

    /**
     * Track an event for a user
     * @param eventName the name of the event
     * @param userId the user id
     */
    public void track(@NonNull String eventName,
                      @NonNull String userId) {
        if (isValid()) {
            try {
                optimizely.track(eventName, userId);
            } catch (Exception e) {
                logger.error("Unable to track event", e);
            }
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
        if (isValid()) {
            attributes.putAll(defaultAttributes);
            optimizely.track(eventName, userId, attributes);

        } else {
            logger.warn("Optimizely is not initialized, could not track event {} for user {} with attributes",
                    eventName, userId);
        }
    }

    /**
     * Track an event for a user
     * @param eventName the name of the event
     * @param userId the user id
     * @param attributes a map of attributes about the user
     * @param eventTags a map of metadata associated with the event
     */
    public void track(@NonNull String eventName,
                      @NonNull String userId,
                      @NonNull Map<String, String> attributes,
                      @NonNull Map<String, ?> eventTags) throws UnknownEventTypeException {
        if (isValid()) {
            attributes.putAll(defaultAttributes);
            optimizely.track(eventName, userId, attributes, eventTags);

        } else {
            logger.warn("Optimizely is not initialized, could not track event {} for user {}" +
                    " with attributes and event tags", eventName, userId);
        }
    }

    /**
     * Track an event for a user
     * @deprecated see {@link Optimizely#track(String, String, Map, Map)} and pass in revenue values as event tags instead.
     * @param eventName the name of the event
     * @param userId the user id
     * @param eventValue a value to tie to the event
     */
    public void track(@NonNull String eventName,
                      @NonNull String userId,
                      long eventValue) throws UnknownEventTypeException {
        if (isValid()) {
            optimizely.track(eventName, userId, eventValue);
        } else {
            logger.warn("Optimizely is not initialized, could not track event {} for user {}" +
                    " with value {}", eventName, userId, eventValue);
        }
    }

    /**
     * Track an event for a user with attributes and a value
     * @see Optimizely#track(String, String, Map, long)
     * @deprecated see {@link Optimizely#track(String, String, Map, Map)} and pass in revenue values as event tags instead.
     * @param eventName the String name of the event
     * @param userId the String user id
     * @param attributes the attributes of the event
     * @param eventValue the value of the event
     */
    public void track(@NonNull String eventName,
                      @NonNull String userId,
                      @NonNull Map<String, String> attributes,
                      long eventValue) {
        if (isValid()) {
            attributes.putAll(defaultAttributes);
            optimizely.track(eventName, userId, attributes, eventValue);
        } else {
            logger.warn("Optimizely is not initialized, could not track event {} for user {}" +
                    " with value {} and attributes", eventName, userId, eventValue);
        }
    }

    /**
     * Get the value of a String live variable
     * @param variableKey the String key for the variable
     * @param userId the user ID
     * @param activateExperiment the flag denoting whether to activate an experiment or not
     * @return String value of the live variable
     */
    public @Nullable String getVariableString(@NonNull String variableKey,
                                              @NonNull String userId,
                                              boolean activateExperiment) {
        return getVariableString(variableKey, userId, Collections.<String, String>emptyMap(),
                                 activateExperiment);
    }

    /**
     * Get the value of a String live variable
     * @param variableKey the String key for the variable
     * @param userId the user ID
     * @param attributes a map of attributes about the user
     * @param activateExperiment the flag denoting whether to activate an experiment or not
     * @return String value of the live variable
     */
    public @Nullable String getVariableString(@NonNull String variableKey,
                                              @NonNull String userId,
                                              @NonNull Map<String, String> attributes,
                                              boolean activateExperiment) {
        if (isValid()) {
            attributes.putAll(defaultAttributes);
            return optimizely.getVariableString(variableKey, userId, attributes,
                                                activateExperiment);
        } else {
            logger.warn("Optimizely is not initialized, could not get live variable {} " +
                    "for user {}", variableKey, userId);
            return null;
        }
    }

    /**
     * Get the value of a Boolean live variable
     * @param variableKey the String key for the variable
     * @param userId the user ID
     * @param activateExperiment the flag denoting whether to activate an experiment or not
     * @return Boolean value of the live variable
     */
    public @Nullable Boolean getVariableBoolean(@NonNull String variableKey,
                                                @NonNull String userId,
                                                boolean activateExperiment) {
        return getVariableBoolean(variableKey, userId, Collections.<String, String>emptyMap(),
                                  activateExperiment);
    }

    /**
     * Get the value of a Boolean live variable
     * @param variableKey the String key for the variable
     * @param userId the user ID
     * @param attributes a map of attributes about the user
     * @param activateExperiment the flag denoting whether to activate an experiment or not
     * @return Boolean value of the live variable
     */
    public @Nullable Boolean getVariableBoolean(@NonNull String variableKey,
                                                @NonNull String userId,
                                                @NonNull Map<String, String> attributes,
                                                boolean activateExperiment) {
        if (isValid()) {
            return optimizely.getVariableBoolean(variableKey, userId, attributes,
                                                 activateExperiment);
        } else {
            logger.warn("Optimizely is not initialized, could not get live variable {} " +
                    "for user {}", variableKey, userId);
            return null;
        }
    }

    /**
     * Get the value of a Integer live variable
     * @param variableKey the String key for the variable
     * @param userId the user ID
     * @param activateExperiment the flag denoting whether to activate an experiment or not
     * @return Integer value of the live variable
     */
    public @Nullable Integer getVariableInteger(@NonNull String variableKey,
                                                @NonNull String userId,
                                                boolean activateExperiment) {
        return getVariableInteger(variableKey, userId, Collections.<String, String>emptyMap(),
                                  activateExperiment);
    }

    /**
     * Get the value of a Integer live variable
     * @param variableKey the String key for the variable
     * @param userId the user ID
     * @param attributes a map of attributes about the user
     * @param activateExperiment the flag denoting whether to activate an experiment or not
     * @return Integer value of the live variable
     */
    public @Nullable Integer getVariableInteger(@NonNull String variableKey,
                                                @NonNull String userId,
                                                @NonNull Map<String, String> attributes,
                                                boolean activateExperiment) {
        if (isValid()) {
            return optimizely.getVariableInteger(variableKey, userId, attributes,
                                                 activateExperiment);
        } else {
            logger.warn("Optimizely is not initialized, could not get live variable {} " +
                    "for user {}", variableKey, userId);
            return null;
        }
    }

    /**
     * Get the value of a Double live variable
     * @param variableKey the String key for the variable
     * @param userId the user ID
     * @param activateExperiment the flag denoting whether to activate an experiment or not
     * @return Double value of the live variable
     */
    public @Nullable Double getVariableDouble(@NonNull String variableKey,
                                              @NonNull String userId,
                                              boolean activateExperiment) {
        return getVariableDouble(variableKey, userId, Collections.<String, String>emptyMap(),
                                 activateExperiment);
    }

    /**
     * Get the value of a Double live variable
     * @param variableKey the String key for the variable
     * @param userId the user ID
     * @param attributes a map of attributes about the user
     * @param activateExperiment the flag denoting whether to activate an experiment or not
     * @return Double value of the live variable
     */
    public @Nullable Double getVariableDouble(@NonNull String variableKey,
                                              @NonNull String userId,
                                              @NonNull Map<String, String> attributes,
                                              boolean activateExperiment) {
        if (isValid()) {
            return optimizely.getVariableDouble(variableKey, userId, attributes,
                                                activateExperiment);
        } else {
            logger.warn("Optimizely is not initialized, could not get live variable {} " +
                    "for user {}", variableKey, userId);
            return null;
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
        if (isValid()) {
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
        if (isValid()) {
            attributes.putAll(defaultAttributes);
            return optimizely.getVariation(experimentKey, userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, could not get variation for experiment {} " +
                    "for user {} with attributes", experimentKey, userId);
            return null;
        }
    }

    //======== Notification listeners ========//

    /**
     * Add a {@link NotificationListener} if it does not exist already.
     * <p>
     * Listeners are held by weak reference and may automatically be garbage collected. You may
     * need to re-register them, for example if your Activity subclass implements the listener
     * interface, you will need to re-register the listener on each onCreate.
     *
     * @param listener listener to add
     */
    public void addNotificationListener(@NonNull NotificationListener listener) {
        if (isValid()) {
            optimizely.addNotificationListener(listener);
        } else {
            logger.warn("Optimizely is not initialized, could not add notification listener");
        }
    }

    /**
     * Remove a {@link NotificationListener} if it exists.
     *
     * @param listener listener to remove
     */
    public void removeNotificationListener(@NonNull NotificationListener listener) {
        if (isValid()) {
            optimizely.removeNotificationListener(listener);
        } else {
            logger.warn("Optimizely is not initialized, could not remove notification listener");
        }
    }

    /**
     * Remove all {@link NotificationListener} instances.
     */
    public void clearNotificationListeners() {
        if (isValid()) {
            optimizely.clearNotificationListeners();
        } else {
            logger.warn("Optimizely is not initialized, could not clear notification listeners");
        }
    }
}
