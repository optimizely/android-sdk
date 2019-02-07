/****************************************************************************
 * Copyright 2017-2019, Optimizely, Inc. and contributors                   *
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
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.notification.NotificationCenter;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The top-level container class that wraps an {@link Optimizely} instance.
 *
 * This proxy ensures that the Android SDK will not crash if the inner Optimizely SDK
 * fails to start. When Optimizely fails to start via {@link OptimizelyManager#initialize(Context,Integer, OptimizelyStartListener)}
 * there will be no cached instance returned from {@link OptimizelyManager#getOptimizely()}.
 *
 * Accessing Optimizely through this interface eliminates the need to check for null on the reference to the Optimizely client object.
 * If the internal reference to Optimizely is null, the methods in this class will log warnings.
 */
public class OptimizelyClient {

    private final Logger logger;

    @Nullable private Optimizely optimizely;
    @NonNull private Map<String, ?> defaultAttributes = new HashMap<>();

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
     * Set default attributes to a non-null attribute map.
     * This is set by the Optimizely manager and includes things like os version and sdk version.
     *
     * @param attrs      A map of default attributes.
     */
    protected void setDefaultAttributes(@NonNull Map<String, ?> attrs) {
        this.defaultAttributes = attrs;
    }

    /**
     * Returns a map of the default attributes.
     *
     * @return      The map of default attributes.
     */
    public @NonNull Map<String, ?> getDefaultAttributes() {
        return this.defaultAttributes;
    }

    /**
     * Get the default attributes and combine them with the attributes passed in.
     * The attributes passed in take precedence over the default attributes. So, you can override default attributes.
     *
     * @param attrs      Attributes that will be combined with default attributes.
     *
     * @return           A new map of both the default attributes and attributes passed in.
     */
    private Map<String, ?> getAllAttributes(@NonNull Map<String, ?> attrs) {
        Map<String, Object> combinedMap = new HashMap<>(defaultAttributes);

        // this essentially overrides defaultAttributes if the attrs passed in have the same key.
        if (attrs != null) {
            combinedMap.putAll(attrs);
        } else if (combinedMap.isEmpty()) {
            combinedMap = null;
        }

        return combinedMap;
    }

    /**
     * Activates an A/B test for a user, determines whether they qualify for the experiment, buckets a qualified
     * user into a variation, and sends an impression event to Optimizely.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/activate.
     *
     * @param experimentKey The key of the variation's experiment to activate.
     * @param userId        The user ID.
     *
     * @return              The key of the variation where the user is bucketed, or `null` if the
     *                      user doesn't qualify for the experiment.
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
     * Activates an A/B test for a user, determines whether they qualify for the experiment, buckets a qualified
     * user into a variation, and sends an impression event to Optimizely.
     *
     * This method takes into account the user `attributes` passed in, to determine if the user
     * is part of the audience that qualifies for the experiment.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/activate.
     *
     * @param experimentKey The key of the variation's experiment to activate.
     * @param userId        The user ID.
     * @param attributes    A map of custom key-value string pairs specifying attributes for the user.
     *
     * @return              The key of the variation where the user is bucketed, or `null` if the
     *                      user doesn't qualify for the experiment.
     */
    @SuppressWarnings("WeakerAccess")
    public @Nullable Variation activate(@NonNull String experimentKey,
                                        @NonNull String userId,
                                        @NonNull Map<String, ?> attributes) {
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
     *
     * @return               The current {@link ProjectConfig} instance.
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
     *
     * @return               True if the OptimizelyClient instance was instantiated correctly.
     */
    public boolean isValid() {
        if (optimizely != null)
            return optimizely.isValid();
        else
            return false;
    }

    /**
     * Tracks a conversion event for a user who meets the default audience conditions for an experiment.
     * When the user does not meet those conditions, events are not tracked.
     *
     * This method sends conversion data to Optimizely but doesn't return any values.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/track.
     *
     * @param eventName     The key of the event to be tracked. This key must match the event key provided when the event was created in the Optimizely app.
     * @param userId        The ID of the user associated with the event being tracked.
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
     * Tracks a conversion event for a user whose attributes meet the audience conditions for an experiment.
     * When the user does not meet those conditions, events are not tracked.
     *
     * This method takes into account the user `attributes` passed in, to determine if the user is part of the audience that qualifies for the experiment.
     *
     * This method sends conversion data to Optimizely but doesn't return any values.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/track.
     *
     * @param eventName     The key of the event to be tracked. This key must match the event key provided when the event was created in the Optimizely app.
     * @param userId        The ID of the user associated with the event being tracked. This ID must match the user ID provided to `activate` or `isFeatureEnabled`.
     * @param attributes    A map of custom key-value string pairs specifying attributes for the user.
     */
    public void track(@NonNull String eventName,
                      @NonNull String userId,
                      @NonNull Map<String, ?> attributes) throws UnknownEventTypeException {
        if (isValid()) {
            optimizely.track(eventName, userId, getAllAttributes(attributes));

        } else {
            logger.warn("Optimizely is not initialized, could not track event {} for user {} with attributes",
                    eventName, userId);
        }
    }

    /**
     * Tracks a conversion event for a user whose attributes meet the audience conditions for the experiment.
     * When the user does not meet those conditions, events are not tracked.
     *
     * This method takes into account the user `attributes` passed in, to determine if the user is part of the audience that qualifies for the experiment.
     *
     * This method sends conversion data to Optimizely but doesn't return any values.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/track.
     *
     * @param eventName     The key of the event to be tracked. This key must match the event key provided when the event was created in the Optimizely app.
     * @param userId        The ID of the user associated with the event being tracked. This ID must match the user ID provided to `activate` or `isFeatureEnabled`.
     * @param attributes    A map of custom key-value string pairs specifying attributes for the user.
     * @param eventTags     A map of key-value string pairs specifying event names and their corresponding event values associated with the event.
     */
    public void track(@NonNull String eventName,
                      @NonNull String userId,
                      @NonNull Map<String, ?> attributes,
                      @NonNull Map<String, ?> eventTags) throws UnknownEventTypeException {
        if (isValid()) {
            optimizely.track(eventName, userId, getAllAttributes(attributes), eventTags);

        } else {
            logger.warn("Optimizely is not initialized, could not track event {} for user {}" +
                    " with attributes and event tags", eventName, userId);
        }
    }

    /**
     * Buckets a qualified user into an A/B test. Takes the same arguments and returns the same values as `activate`,
     * but without sending an impression network request. The behavior of the two methods is identical otherwise.
     * Use `getVariation` if `activate` has been called and the current variation assignment is needed for a given
     * experiment and user.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/get-variation.
     *
     * @param experimentKey The key of the experiment for which to retrieve the forced variation.
     * @param userId        The ID of the user for whom to retrieve the forced variation.
     *
     * @return              The key of the variation where the user is bucketed, or `null` if the user
     *                      doesn't qualify for the experiment.
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
     * Buckets a qualified user into an A/B test. Takes the same arguments and returns the same values as `activate`,
     * but without sending an impression network request. The behavior of the two methods is identical otherwise.
     * Use `getVariation` if `activate` has been called and the current variation assignment is needed for a given
     * experiment and user.
     *
     * This method takes into account the user `attributes` passed in, to determine if the user is part of the
     * audience that qualifies for the experiment.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/get-variation.
     *
     * @param experimentKey The key of the experiment for which to retrieve the variation.
     * @param userId        The ID of the user for whom to retrieve the variation.
     * @param attributes    A map of custom key-value string pairs specifying attributes for the user.
     *
     * @return              The key of the variation where the user is bucketed, or `null` if the user
     *                      doesn't qualify for the experiment.
     */
    @SuppressWarnings("WeakerAccess")
    public @Nullable Variation getVariation(@NonNull String experimentKey,
                                            @NonNull String userId,
                                            @NonNull Map<String, ?> attributes) {
        if (isValid()) {
            return optimizely.getVariation(experimentKey, userId, getAllAttributes(attributes));
        } else {
            logger.warn("Optimizely is not initialized, could not get variation for experiment {} " +
                    "for user {} with attributes", experimentKey, userId);
            return null;
        }
    }

    /**
     * Forces a user into a variation for a given experiment for the lifetime of the Optimizely client.
     * The forced variation value doesn't persist across application launches.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/set-forced-variation.
     *
     * @param experimentKey  The key of the experiment to set with the forced variation.
     * @param userId         The ID of the user to force into the variation.
     * @param variationKey   The key of the forced variation.
     *                       Set the value to `null` to clear the existing experiment-to-variation mapping.
     *
     * @return boolean       `true` if the user was successfully forced into a variation, `false` if the `experimentKey`
     *                       isn't in the project file or the `variationKey` isn't in the experiment.
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
     * Returns the forced variation set by `setForcedVariation`, or `null` if no variation was forced.
     * A user can be forced into a variation for a given experiment for the lifetime of the Optimizely client.
     * The forced variation value is runtime only and doesn't persist across application launches.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/set-forced-variation.
     *
     * @param experimentKey   The key of the experiment for which to retrieve the forced variation.
     * @param userId          The ID of the user in the forced variation.
     *
     * @return                The variation the user was bucketed into, or `null` if `setForcedVariation` failed
     *                        to force the user into the variation.
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
     * Retrieves a list of features that are enabled for the user.
     * Invoking this method is equivalent to running `isFeatureEnabled` for each feature in the datafile sequentially.
     *
     * This method takes into account the user `attributes` passed in, to determine if the user
     * is part of the audience that qualifies for the experiment.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/get-enabled-features.
     *
     * @param userId      The ID of the user who may have features enabled in one or more experiments.
     * @param attributes  A map of custom key-value string pairs specifying attributes for the user.
     *
     * @return            A list of keys corresponding to the features that are enabled for the user, or an empty list if no features could be found for the specified user.
     */
    public List<String> getEnabledFeatures(@NonNull String userId, @NonNull Map<String, ?> attributes) {
        if (isValid()) {
            return optimizely.getEnabledFeatures(userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, could not get enabled feature for user {}",
                    userId);
            return null;
        }
    }

    /**
     * Determines whether a feature test or rollout is enabled for a given user, and
     * sends an impression event if the user is bucketed into an experiment using the feature.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/is-feature-enabled.
     *
     * @param featureKey The key of the feature to check.
     * @param userId     The ID of the user to check.
     *
     * @return           `true` if the feature is enabled, or `false` if the feature is disabled or couldn't be found.
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
     * Determines whether a feature test or rollout is enabled for a given user, and
     * sends an impression event if the user is bucketed into an experiment using the feature.
     *
     * This method takes into account the user `attributes` passed in, to determine if the user
     * is part of the audience that qualifies for the experiment.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/is-feature-enabled.
     *
     * @param featureKey   The key of the feature on which to perform the check.
     * @param userId       The ID of the user on which to perform the check.
     * @param attributes   A map of custom key-value string pairs specifying attributes for the user.
     *
     * @return             `true` if the feature is enabled, or `false` if the feature is disabled or couldn't be found.
     */
    public @NonNull Boolean isFeatureEnabled(@NonNull String featureKey,
                                             @NonNull String userId,
                                             @NonNull Map<String, ?> attributes) {
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
     * Evaluates the specified boolean feature variable and returns its value.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/get-feature-variable.
     *
     * @param featureKey   The key of the feature whose variable's value is being accessed.
     *                     The feature key is defined from the Features dashboard, as described in
     *                     https://help.optimizely.com/Build_Campaigns_and_Experiments/Feature_tests%3A_Experiment_on_features.
     * @param variableKey  The key of the variable whose value is being accessed.
     * @param userId       The ID of the participant in the experiment.
     *
     * @return             The value of the boolean feature variable, or `null` if the feature could not be found.
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
     * Evaluates the specified boolean feature variable and returns its value.
     *
     * This method takes into account the user `attributes` passed in, to determine if the user
     * is part of the audience that qualifies for the experiment.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/get-feature-variable.
     *
     * @param featureKey   The key of the feature whose variable's value is being accessed.
     * @param variableKey  The key of the variable whose value is being accessed.
     * @param userId       The ID of the participant in the experiment.
     * @param attributes   A map of custom key-value string pairs specifying attributes for the user.
     *
     * @return             The value of the boolean feature variable, or `null` if the feature could not be found.
     */
    public @Nullable
    Boolean getFeatureVariableBoolean(@NonNull String featureKey,
                                      @NonNull String variableKey,
                                      @NonNull String userId,
                                      @NonNull Map<String, ?> attributes) {
        if (isValid()) {
            return optimizely.getFeatureVariableBoolean(featureKey, variableKey, userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, could not get feature {} variable {} boolean for user {} with attributes",
                    featureKey,variableKey, userId);
            return null;
        }
    }

    /**
     * Evaluates the specified double feature variable and returns its value.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/get-feature-variable.
     *
     * @param featureKey   The key of the feature whose variable's value is being accessed.
     * @param variableKey  The key of the variable whose value is being accessed.
     * @param userId       The ID of the participant in the experiment.
     *
     * @return             The value of the double feature variable, or `null` if the feature could not be found.
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
     * Evaluates the specified double feature variable and returns its value.
     *
     * This method takes into account the user `attributes` passed in, to determine if the user
     * is part of the audience that qualifies for the experiment.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/get-feature-variable.
     *
     * @param featureKey   The key of the feature whose variable's value is being accessed.
     * @param variableKey  The key of the variable whose value is being accessed.
     * @param userId       The ID of the participant in the experiment.
     * @param attributes   A map of custom key-value string pairs specifying attributes for the user.
     *
     * @return             The value of the double feature variable, or `null` if the feature could not be found.
     */
    public @Nullable
    Double getFeatureVariableDouble(@NonNull String featureKey,
                                    @NonNull String variableKey,
                                    @NonNull String userId,
                                    @NonNull Map<String, ?> attributes) {
        if (isValid()) {
            return optimizely.getFeatureVariableDouble(featureKey, variableKey, userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, could not get feature {} variable {} double for user {} with attributes",
                    featureKey,variableKey, userId);
            return null;
        }
    }

    /**
     * Evaluates the specified integer feature variable and returns its value.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/get-feature-variable.
     *
     * @param featureKey   The key of the feature whose variable's value is being accessed.
     * @param variableKey  The key of the variable whose value is being accessed.
     * @param userId       The ID of the participant in the experiment.
     *
     * @return             The value of the integer feature variable, or `null` if the feature could not be found.
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
     * Evaluates the specified integer feature variable and returns its value.
     *
     * This method takes into account the user `attributes` passed in, to determine if the user
     * is part of the audience that qualifies for the experiment.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/get-feature-variable.
     *
     * @param featureKey   The key of the feature whose variable's value is being accessed.
     * @param variableKey  The key of the variable whose value is being accessed.
     * @param userId       The ID of the participant in the experiment.
     * @param attributes   A map of custom key-value string pairs specifying attributes for the user.
     *
     * @return             The value of the integer feature variable, or `null` if the feature could not be found.
     */
    public @Nullable
    Integer getFeatureVariableInteger(@NonNull String featureKey,
                                      @NonNull String variableKey,
                                      @NonNull String userId,
                                      @NonNull Map<String, ?> attributes) {
        if (isValid()) {
            return optimizely.getFeatureVariableInteger(featureKey, variableKey, userId, attributes);
        } else {
            logger.warn("Optimizely is not initialized, could not get feature {} variable {} integer for user {} with attributes",
                    featureKey,variableKey, userId);
            return null;
        }
    }

    /**
     * Evaluates the specified string feature variable and returns its value.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/get-feature-variable.
     *
     * @param featureKey   The key of the feature whose variable's value is being accessed.
     * @param variableKey  The key of the variable whose value is being accessed.
     * @param userId       The ID of the participant in the experiment.
     *
     * @return             The value of the string feature variable, or `null` if the feature could not be found.
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
     * Evaluates the specified string feature variable and returns its value.
     *
     * This method takes into account the user `attributes` passed in, to determine if the user
     * is part of the audience that qualifies for the experiment.
     *
     * For more information, see https://docs.developers.optimizely.com/full-stack/docs/get-feature-variable.
     *
     * @param featureKey   The key of the feature whose variable's value is being accessed.
     * @param variableKey  The key of the variable whose value is being accessed.
     * @param userId       The ID of the participant in the experiment.
     * @param attributes   A map of custom key-value string pairs specifying attributes for the user.
     *
     * @return             The value of the string feature variable, or `null` if the feature could not be found.
     */
    public @Nullable
    String getFeatureVariableString(@NonNull String featureKey,
                                    @NonNull String variableKey,
                                    @NonNull String userId,
                                    @NonNull Map<String, ?> attributes) {
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

     * @return             The {@link NotificationCenter} or `null` if Optimizely is not initialized (or
     *                     initialization failed).
     */
    public @Nullable
    NotificationCenter getNotificationCenter() {
        if (isValid()) {
            return optimizely.notificationCenter;
        } else {
            logger.warn("Optimizely is not initialized, could not get the notification listener");
        }

        return null;
    }
}
