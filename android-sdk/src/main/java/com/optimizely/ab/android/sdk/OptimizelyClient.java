/****************************************************************************
 * Copyright 2017-2018, Optimizely, Inc. and contributors                   *
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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.UnknownEventTypeException;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.notification.NotificationCenter;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps {@link Optimizely} instances
 *
 * This proxy ensures that the Android SDK will not crash if the inner Optimizely SDK
 * failed to start.  When Optimizely fails to start via {@link OptimizelyManager#initialize(Context,Integer, OptimizelyStartListener)}
 * there will be no cached instance returned from {@link OptimizelyManager#getOptimizely()}.  By accessing
 * Optimizely through this interface checking for null is not required.  If Optimizely is null warnings
 * will be logged.
 */
public class OptimizelyClient {

    private final Logger logger;

    @Nullable private Optimizely optimizely;
    @NonNull private Map<String, String> defaultAttributes = new HashMap<>();

    OptimizelyClient(@Nullable Optimizely optimizely, @NonNull Logger logger) {
        this.optimizely = optimizely;
        this.logger = logger;
        /*
        OptimizelyManager is initialized with an OptimizelyClient with a null optimizely property:
        https://github.com/optimizely/android-sdk/blob/master/android-sdk/src/main/java/com/optimizely/ab/android/sdk/OptimizelyManager.java#L63
        optimizely will remain null until OptimizelyManager#initialize has been called, so isValid checks for that. Otherwise apps would crash if
        the public methods here were called before initialize.
        So, we start with an empty map of default attributes until the manager is initialized.
        */
    }

    /**
     * Set default attributes to a non null attribute map.
     * This is set by the Optimizely manager and includes things like os version and sdk version.
     * @param attrs a map of default attributes.
     */
    protected void setDefaultAttributes(@NonNull Map<String, String> attrs) {
        this.defaultAttributes = attrs;
    }

    /**
     * Return the default attributes map
     * @return the map of default attributes
     */
    public @NonNull Map<String, String> getDefaultAttributes() {
        return this.defaultAttributes;
    }

    /**
     * Get the default attributes and combine them with the attributes passed in.
     * The attributes passed in take precedence over the default attributes. So, you can override default attributes.
     * @param attrs attributes that will be combined with default attributes.
     * @return a new map of both the default attributes and attributes passed in.
     */
    private Map<String, String> getAllAttributes(@NonNull Map<String, String> attrs) {
        Map<String,String> combinedMap = new HashMap<>(defaultAttributes);

        // this essentially overrides defaultAttributes if the attrs passed in have the same key.
        combinedMap.putAll(attrs);

        return combinedMap;
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
            return optimizely.activate(experimentKey, userId, getDefaultAttributes());
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
            return optimizely.activate(experimentKey, userId, getAllAttributes(attributes));
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
                optimizely.track(eventName, userId, getDefaultAttributes());
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
            optimizely.track(eventName, userId, getAllAttributes(attributes));

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
            optimizely.track(eventName, userId, getAllAttributes(attributes), eventTags);

        } else {
            logger.warn("Optimizely is not initialized, could not track event {} for user {}" +
                    " with attributes and event tags", eventName, userId);
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
            return optimizely.getVariation(experimentKey, userId, getDefaultAttributes());
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
            return optimizely.getVariation(experimentKey, userId, getAllAttributes(attributes));
        } else {
            logger.warn("Optimizely is not initialized, could not get variation for experiment {} " +
                    "for user {} with attributes", experimentKey, userId);
            return null;
        }
    }

    /**
     * Force a user into a variation for a given experiment.
     * The forced variation value does not persist across application launches.
     * If the experiment key is not in the project file, this call fails and returns false.
     * If the variationKey is not in the experiment, this call fails.
     * @param experimentKey The key for the experiment.
     * @param userId The user ID to be used for bucketing.
     * @param variationKey The variation key to force the user into.  If the variation key is null
     *                     then the forcedVariation for that experiment is removed.
     *
     * @return boolean A boolean value that indicates if the set completed successfully.
     */
    public boolean setForcedVariation(@NonNull String experimentKey,
                                      @NonNull String userId,
                                      @Nullable String variationKey) {

        if (isValid()) {
            return optimizely.setForcedVariation(experimentKey, userId, variationKey);
        } else {
            logger.warn("Optimizely is not initialized, could not set forced variation");
        }

        return false;
    }

    /**
     * Gets the forced variation for a given user and experiment.
     * The forced variation value does not persist across application launches.
     * It is runtime only.
     * @param experimentKey The key for the experiment.
     * @param userId The user ID to be used for bucketing.
     *
     * @return The variation the user will be bucketed into. This value can be null if the
     * forced variation fails.
     */
    public @Nullable Variation getForcedVariation(@NonNull String experimentKey,
                                                  @NonNull String userId) {
        if (isValid()) {
            return optimizely.getForcedVariation(experimentKey, userId);
        } else {
            logger.warn("Optimizely is not initialized, could not get forced variation");
        }

        return null;
    }

    //======== FeatureFlag APIs ========//

    /**
     * Get the list of features that are enabled for the user.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return List of the feature keys that are enabled for the user if the userId is empty it will
     * return Empty List.
     */
    public List<String> getEnabledFeatures(@NonNull String userId, @NonNull Map<String, String> attributes) {
        if (isValid()) {
            return optimizely.getEnabledFeatures(userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, could not get enabled feature for user {}",
                    userId);
            return null;
        }
    }

    /**
     * Determine whether a feature is enabled for a user.
     * Send an impression event if the user is bucketed into an experiment using the feature.
     *
     * @param featureKey The unique key of the feature.
     * @param userId The ID of the user.
     * @return True if the feature is enabled.
     *         False if the feature is disabled.
     *         False if the feature is not found.
     */
    public @NonNull
    Boolean isFeatureEnabled(@NonNull String featureKey,
                             @NonNull String userId) {
        if (isValid()) {
            return optimizely.isFeatureEnabled(featureKey, userId);
        } else {
            logger.warn("Optimizely is not initialized, could not enable feature {} for user {}",
                    featureKey, userId);
            return false;
        }
    }

    /**
     * Determine whether a feature is enabled for a user.
     * Send an impression event if the user is bucketed into an experiment using the feature.
     *
     * @param featureKey The unique key of the feature.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return True if the feature is enabled.
     *         False if the feature is disabled.
     *         False if the feature is not found.
     */
    public @NonNull Boolean isFeatureEnabled(@NonNull String featureKey,
                                             @NonNull String userId,
                                             @NonNull Map<String, String> attributes) {
        if (isValid()) {
            return optimizely.isFeatureEnabled(featureKey, userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, could not enable feature {} for user {} with attributes",
                    featureKey, userId);
            return false;
        }
    }

    //======== Feature Variables APIs ========//

    /**
     * Get the Boolean value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @return The Boolean value of the boolean single variable feature.
     *         Null if the feature could not be found.
     */
    public @Nullable
    Boolean getFeatureVariableBoolean(@NonNull String featureKey,
                                      @NonNull String variableKey,
                                      @NonNull String userId) {
        if (isValid()) {
            return optimizely.getFeatureVariableBoolean(featureKey, variableKey, userId);
        } else {
            logger.warn("Optimizely is not initialized, could not get feature {} variable {} boolean for user {}",
                    featureKey,variableKey, userId);
            return null;
        }
    }

    /**
     * Get the Boolean value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return The Boolean value of the boolean single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable
    Boolean getFeatureVariableBoolean(@NonNull String featureKey,
                                      @NonNull String variableKey,
                                      @NonNull String userId,
                                      @NonNull Map<String, String> attributes) {
        if (isValid()) {
            return optimizely.getFeatureVariableBoolean(featureKey, variableKey, userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, could not get feature {} variable {} boolean for user {} with attributes",
                    featureKey,variableKey, userId);
            return null;
        }
    }

    /**
     * Get the Double value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @return The Double value of the double single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable
    Double getFeatureVariableDouble(@NonNull String featureKey,
                                    @NonNull String variableKey,
                                    @NonNull String userId) {
        if (isValid()) {
            return optimizely.getFeatureVariableDouble(featureKey, variableKey, userId);
        } else {
            logger.warn("Optimizely is not initialized, could not get feature {} variable {} double for user {}",
                    featureKey,variableKey, userId);
            return null;
        }
    }

    /**
     * Get the Double value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return The Double value of the double single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable
    Double getFeatureVariableDouble(@NonNull String featureKey,
                                    @NonNull String variableKey,
                                    @NonNull String userId,
                                    @NonNull Map<String, String> attributes) {
        if (isValid()) {
            return optimizely.getFeatureVariableDouble(featureKey, variableKey, userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, could not get feature {} variable {} double for user {} with attributes",
                    featureKey,variableKey, userId);
            return null;
        }
    }

    /**
     * Get the Integer value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @return The Integer value of the integer single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable
    Integer getFeatureVariableInteger(@NonNull String featureKey,
                                      @NonNull String variableKey,
                                      @NonNull String userId) {
        if (isValid()) {
            return optimizely.getFeatureVariableInteger(featureKey, variableKey, userId);
        } else {
            logger.warn("Optimizely is not initialized, could not get feature {} variable {} integer for user {}",
                    featureKey,variableKey, userId);
            return null;
        }
    }

    /**
     * Get the Integer value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return The Integer value of the integer single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable
    Integer getFeatureVariableInteger(@NonNull String featureKey,
                                      @NonNull String variableKey,
                                      @NonNull String userId,
                                      @NonNull Map<String, String> attributes) {
        if (isValid()) {
            return optimizely.getFeatureVariableInteger(featureKey, variableKey, userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, could not get feature {} variable {} integer for user {} with attributes",
                    featureKey,variableKey, userId);
            return null;
        }
    }

    /**
     * Get the String value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @return The String value of the string single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable
    String getFeatureVariableString(@NonNull String featureKey,
                                    @NonNull String variableKey,
                                    @NonNull String userId) {
        if (isValid()) {
            return optimizely.getFeatureVariableString(featureKey, variableKey, userId);
        } else {
            logger.warn("Optimizely is not initialized, could not get feature {} variable {} string for user {}",
                    featureKey,variableKey, userId);
            return null;
        }
    }

    /**
     * Get the String value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return The String value of the string single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable
    String getFeatureVariableString(@NonNull String featureKey,
                                    @NonNull String variableKey,
                                    @NonNull String userId,
                                    @NonNull Map<String, String> attributes) {
        if (isValid()) {
            return optimizely.getFeatureVariableString(featureKey, variableKey, userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, could not get feature {} variable {} string for user {} with attributes",
                    featureKey, variableKey, userId);
            return null;
        }
    }

    /**
     * Return the notification center {@link NotificationCenter} used to add notifications for events
     * such as Activate and track.
     * @return
     */
    public NotificationCenter getNotificationCenter() {
        if (isValid()) {
            return optimizely.notificationCenter;
        } else {
            logger.warn("Optimizely is not initialized, could not get the notification listener");
        }

        return null;
    }
}
