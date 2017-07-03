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
package com.optimizely.ab;

import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.bucketing.DecisionService;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.LiveVariable;
import com.optimizely.ab.config.LiveVariableUsageInstance;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.config.parser.DefaultConfigParser;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.error.NoOpErrorHandler;
import com.optimizely.ab.error.RaiseExceptionErrorHandler;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.BuildVersionInfo;
import com.optimizely.ab.event.internal.EventBuilder;
import com.optimizely.ab.event.internal.EventBuilderV2;
import com.optimizely.ab.event.internal.payload.Event.ClientEngine;
import com.optimizely.ab.internal.EventTagUtils;
import com.optimizely.ab.internal.ReservedEventKey;
import com.optimizely.ab.notification.NotificationBroadcaster;
import com.optimizely.ab.notification.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Top-level container class for Optimizely functionality.
 * Thread-safe, so can be created as a singleton and safely passed around.
 *
 * Example instantiation:
 * <pre>
 *     Optimizely optimizely = Optimizely.builder(projectWatcher, eventHandler).build();
 * </pre>
 *
 * To activate an experiment and perform variation specific processing:
 * <pre>
 *     Variation variation = optimizely.activate(experimentKey, userId, attributes);
 *     if (variation.is("ALGORITHM_A")) {
 *         // execute code for algorithm A
 *     } else if (variation.is("ALGORITHM_B")) {
 *         // execute code for algorithm B
 *     } else {
 *         // execute code for default algorithm
 *     }
 * </pre>
 *
 * <b>NOTE:</b> by default, all exceptions originating from {@code Optimizely} calls are suppressed.
 * For example, attempting to activate an experiment that does not exist in the project config will cause an error
 * to be logged, and for the "control" variation to be returned.
 */
@ThreadSafe
public class Optimizely {

    private static final Logger logger = LoggerFactory.getLogger(Optimizely.class);

    @VisibleForTesting final DecisionService decisionService;
    @VisibleForTesting final EventBuilder eventBuilder;
    @VisibleForTesting final ProjectConfig projectConfig;
    @VisibleForTesting final EventHandler eventHandler;
    @VisibleForTesting final ErrorHandler errorHandler;
    @VisibleForTesting final NotificationBroadcaster notificationBroadcaster = new NotificationBroadcaster();
    @Nullable private final UserProfileService userProfileService;

    private Optimizely(@Nonnull ProjectConfig projectConfig,
                       @Nonnull DecisionService decisionService,
                       @Nonnull EventHandler eventHandler,
                       @Nonnull EventBuilder eventBuilder,
                       @Nonnull ErrorHandler errorHandler,
                       @Nullable UserProfileService userProfileService) {
        this.projectConfig = projectConfig;
        this.decisionService = decisionService;
        this.eventHandler = eventHandler;
        this.eventBuilder = eventBuilder;
        this.errorHandler = errorHandler;
        this.userProfileService = userProfileService;
    }

    // Do work here that should be done once per Optimizely lifecycle
    @VisibleForTesting
    void initialize() {

    }

    //======== activate calls ========//

    public @Nullable
    Variation activate(@Nonnull String experimentKey,
                       @Nonnull String userId) throws UnknownExperimentException {
        return activate(experimentKey, userId, Collections.<String, String>emptyMap());
    }

    public @Nullable
    Variation activate(@Nonnull String experimentKey,
                       @Nonnull String userId,
                       @Nonnull Map<String, String> attributes) throws UnknownExperimentException {

        if (!validateUserId(userId)) {
            logger.info("Not activating user for experiment \"{}\".", experimentKey);
            return null;
        }

        ProjectConfig currentConfig = getProjectConfig();

        Experiment experiment = getExperimentOrThrow(currentConfig, experimentKey);
        if (experiment == null) {
            // if we're unable to retrieve the associated experiment, return null
            logger.info("Not activating user \"{}\" for experiment \"{}\".", userId, experimentKey);
            return null;
        }

        return activate(currentConfig, experiment, userId, attributes);
    }

    public @Nullable
    Variation activate(@Nonnull Experiment experiment,
                       @Nonnull String userId) {
        return activate(experiment, userId, Collections.<String, String>emptyMap());
    }

    public @Nullable
    Variation activate(@Nonnull Experiment experiment,
                       @Nonnull String userId,
                       @Nonnull Map<String, String> attributes) {

        ProjectConfig currentConfig = getProjectConfig();

        return activate(currentConfig, experiment, userId, attributes);
    }

    private @Nullable
    Variation activate(@Nonnull ProjectConfig projectConfig,
                       @Nonnull Experiment experiment,
                       @Nonnull String userId,
                       @Nonnull Map<String, String> attributes) {

        // determine whether all the given attributes are present in the project config. If not, filter out the unknown
        // attributes.
        Map<String, String> filteredAttributes = filterAttributes(projectConfig, attributes);

        // bucket the user to the given experiment and dispatch an impression event
        Variation variation = decisionService.getVariation(experiment, userId, filteredAttributes);
        if (variation == null) {
            logger.info("Not activating user \"{}\" for experiment \"{}\".", userId, experiment.getKey());
            return null;
        }

        if (experiment.isRunning()) {
            LogEvent impressionEvent = eventBuilder.createImpressionEvent(
                    projectConfig,
                    experiment,
                    variation,
                    userId,
                    filteredAttributes);
            logger.info("Activating user \"{}\" in experiment \"{}\".", userId, experiment.getKey());
            logger.debug(
                    "Dispatching impression event to URL {} with params {} and payload \"{}\".",
                    impressionEvent.getEndpointUrl(), impressionEvent.getRequestParams(), impressionEvent.getBody());
            try {
                eventHandler.dispatchEvent(impressionEvent);
            } catch (Exception e) {
                logger.error("Unexpected exception in event dispatcher", e);
            }

            notificationBroadcaster.broadcastExperimentActivated(experiment, userId, filteredAttributes, variation);
        } else {
            logger.info("Experiment has \"Launched\" status so not dispatching event during activation.");
        }

        return variation;
    }

    //======== track calls ========//

    public void track(@Nonnull String eventName,
                      @Nonnull String userId) throws UnknownEventTypeException {
        track(eventName, userId, Collections.<String, String>emptyMap(), Collections.<String, Object>emptyMap());
    }

    public void track(@Nonnull String eventName,
                      @Nonnull String userId,
                      @Nonnull Map<String, String> attributes) throws UnknownEventTypeException {
        track(eventName, userId, attributes, Collections.<String, String>emptyMap());
    }

    public void track(@Nonnull String eventName,
                      @Nonnull String userId,
                      @Nonnull Map<String, String> attributes,
                      @Nonnull Map<String, ?> eventTags) throws UnknownEventTypeException {

        ProjectConfig currentConfig = getProjectConfig();

        EventType eventType = getEventTypeOrThrow(currentConfig, eventName);
        if (eventType == null) {
            // if no matching event type could be found, do not dispatch an event
            logger.info("Not tracking event \"{}\" for user \"{}\".", eventName, userId);
            return;
        }

        // determine whether all the given attributes are present in the project config. If not, filter out the unknown
        // attributes.
        Map<String, String> filteredAttributes = filterAttributes(currentConfig, attributes);

        Long eventValue = null;
        if (eventTags == null) {
            logger.warn("Event tags is null when non-null was expected. Defaulting to an empty event tags map.");
            eventTags = Collections.<String, String>emptyMap();
        } else {
            eventValue = EventTagUtils.getRevenueValue(eventTags);
        }

        List<Experiment> experimentsForEvent = projectConfig.getExperimentsForEventKey(eventName);
        Map<Experiment, Variation> experimentVariationMap = new HashMap<Experiment, Variation>(experimentsForEvent.size());
        for (Experiment experiment : experimentsForEvent) {
            if (experiment.isRunning()) {
                Variation variation = decisionService.getVariation(experiment, userId, filteredAttributes);
                if (variation != null) {
                    experimentVariationMap.put(experiment, variation);
                }
            } else {
                logger.info(
                        "Not tracking event \"{}\" for experiment \"{}\" because experiment has status \"Launched\".",
                        eventType.getKey(), experiment.getKey());
            }
        }

        // create the conversion event request parameters, then dispatch
        LogEvent conversionEvent = eventBuilder.createConversionEvent(
                projectConfig,
                experimentVariationMap,
                userId,
                eventType.getId(),
                eventType.getKey(),
                filteredAttributes,
                eventTags);

        if (conversionEvent == null) {
            logger.info("There are no valid experiments for event \"{}\" to track.", eventName);
            logger.info("Not tracking event \"{}\" for user \"{}\".", eventName, userId);
            return;
        }

        logger.info("Tracking event \"{}\" for user \"{}\".", eventName, userId);
        logger.debug("Dispatching conversion event to URL {} with params {} and payload \"{}\".",
                conversionEvent.getEndpointUrl(), conversionEvent.getRequestParams(), conversionEvent.getBody());
        try {
            eventHandler.dispatchEvent(conversionEvent);
        } catch (Exception e) {
            logger.error("Unexpected exception in event dispatcher", e);
        }

        notificationBroadcaster.broadcastEventTracked(eventName, userId, filteredAttributes, eventValue,
                conversionEvent);
    }

    //======== live variable getters ========//

    @Deprecated
    public @Nullable
    String getVariableString(@Nonnull String variableKey,
                             @Nonnull String userId,
                             boolean activateExperiment) throws UnknownLiveVariableException {
        return getVariableString(variableKey, userId, Collections.<String, String>emptyMap(), activateExperiment);
    }

    @Deprecated
    public @Nullable
    String getVariableString(@Nonnull String variableKey,
                             @Nonnull String userId,
                             @Nonnull Map<String, String> attributes,
                             boolean activateExperiment)
            throws UnknownLiveVariableException {

        LiveVariable variable = getLiveVariableOrThrow(projectConfig, variableKey);
        if (variable == null) {
            return null;
        }

        List<Experiment> experimentsUsingLiveVariable =
                projectConfig.getLiveVariableIdToExperimentsMapping().get(variable.getId());
        Map<String, Map<String, LiveVariableUsageInstance>> variationToLiveVariableUsageInstanceMapping =
                projectConfig.getVariationToLiveVariableUsageInstanceMapping();

        if (experimentsUsingLiveVariable == null) {
            logger.warn("No experiment is using variable \"{}\".", variable.getKey());
            return variable.getDefaultValue();
        }

        for (Experiment experiment : experimentsUsingLiveVariable) {
            Variation variation;
            if (activateExperiment) {
                variation = activate(experiment, userId, attributes);
            } else {
                variation = getVariation(experiment, userId, attributes);
            }

            if (variation != null) {
                LiveVariableUsageInstance usageInstance =
                        variationToLiveVariableUsageInstanceMapping.get(variation.getId()).get(variable.getId());
                return usageInstance.getValue();
            }
        }

        return variable.getDefaultValue();
    }

    @Deprecated
    public @Nullable
    Boolean getVariableBoolean(@Nonnull String variableKey,
                               @Nonnull String userId,
                               boolean activateExperiment) throws UnknownLiveVariableException {
        return getVariableBoolean(variableKey, userId, Collections.<String, String>emptyMap(), activateExperiment);
    }

    @Deprecated
    public @Nullable
    Boolean getVariableBoolean(@Nonnull String variableKey,
                               @Nonnull String userId,
                               @Nonnull Map<String, String> attributes,
                               boolean activateExperiment)
            throws UnknownLiveVariableException {

        String variableValueString = getVariableString(variableKey, userId, attributes, activateExperiment);
        if (variableValueString != null) {
            return Boolean.parseBoolean(variableValueString);
        }

        return null;
    }

    @Deprecated
    public @Nullable
    Integer getVariableInteger(@Nonnull String variableKey,
                               @Nonnull String userId,
                               boolean activateExperiment) throws UnknownLiveVariableException {
        return getVariableInteger(variableKey, userId, Collections.<String, String>emptyMap(), activateExperiment);
    }

    @Deprecated
    public @Nullable
    Integer getVariableInteger(@Nonnull String variableKey,
                               @Nonnull String userId,
                               @Nonnull Map<String, String> attributes,
                               boolean activateExperiment)
            throws UnknownLiveVariableException {

        String variableValueString = getVariableString(variableKey, userId, attributes, activateExperiment);
        if (variableValueString != null) {
            try {
                return Integer.parseInt(variableValueString);
            } catch (NumberFormatException e) {
                logger.error("Variable value \"{}\" for live variable \"{}\" is not an integer.", variableValueString,
                        variableKey);
            }
        }

        return null;
    }

    @Deprecated
    public @Nullable
    Double getVariableDouble(@Nonnull String variableKey,
                             @Nonnull String userId,
                             boolean activateExperiment) throws UnknownLiveVariableException {
        return getVariableDouble(variableKey, userId, Collections.<String, String>emptyMap(), activateExperiment);
    }

    @Deprecated
    public @Nullable
    Double getVariableDouble(@Nonnull String variableKey,
                             @Nonnull String userId,
                             @Nonnull Map<String, String> attributes,
                             boolean activateExperiment)
            throws UnknownLiveVariableException {

        String variableValueString = getVariableString(variableKey, userId, attributes, activateExperiment);
        if (variableValueString != null) {
            try {
                return Double.parseDouble(variableValueString);
            } catch (NumberFormatException e) {
                logger.error("Variable value \"{}\" for live variable \"{}\" is not a double.", variableValueString,
                        variableKey);
            }
        }

        return null;
    }

    //======== FeatureFlag APIs ========//

    /**
     * Determine whether a boolean feature is enabled.
     * Send an impression event if the user is bucketed into an experiment using the feature.
     *
     * @param featureKey The unique key of the feature.
     * @param userId The ID of the user.
     * @return True if the feature is enabled.
     *         False if the feature is disabled.
     *         Will always return True if toggling the feature is disabled.
     *         Will return Null if the feature is not found.
     */
    public @Nullable Boolean isFeatureEnabled(@Nonnull String featureKey,
                                              @Nonnull String userId) {
        return isFeatureEnabled(featureKey, userId, Collections.<String, String>emptyMap());
    }

    /**
     * Determine whether a boolean feature is enabled.
     * Send an impression event if the user is bucketed into an experiment using the feature.
     *
     * @param featureKey The unique key of the feature.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return True if the feature is enabled.
     *         False if the feature is disabled.
     *         Will always return True if toggling the feature is disabled.
     *         Will return Null if the feature is not found.
     */
    public @Nullable Boolean isFeatureEnabled(@Nonnull String featureKey,
                                              @Nonnull String userId,
                                              @Nonnull Map<String, String> attributes) {
        return getFeatureVariableBoolean(featureKey, "", userId, attributes);
    }

    /**
     * Get the Boolean value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @return The Boolean value of the boolean single variable feature.
     *         Null if the feature could not be found.
     */
    public @Nullable Boolean getFeatureVariableBoolean(@Nonnull String featureKey,
                                                       @Nonnull String variableKey,
                                                       @Nonnull String userId) {
        return getFeatureVariableBoolean(featureKey, variableKey, userId, Collections.<String, String>emptyMap());
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
    public @Nullable Boolean getFeatureVariableBoolean(@Nonnull String featureKey,
                                                       @Nonnull String variableKey,
                                                       @Nonnull String userId,
                                                       @Nonnull Map<String, String> attributes) {
        return null;
    }

    /**
     * Get the Double value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @return The Double value of the double single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable Double getFeatureVariableDouble(@Nonnull String featureKey,
                                                     @Nonnull String variableKey,
                                                     @Nonnull String userId) {
        return getFeatureVariableDouble(featureKey, variableKey, userId, Collections.<String, String>emptyMap());
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
    public @Nullable Double getFeatureVariableDouble(@Nonnull String featureKey,
                                                     @Nonnull String variableKey,
                                                     @Nonnull String userId,
                                                     @Nonnull Map<String, String> attributes) {
        return null;
    }

    /**
     * Get the Integer value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @return The Integer value of the integer single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable Integer getFeatureVariableInteger(@Nonnull String featureKey,
                                                       @Nonnull String variableKey,
                                                       @Nonnull String userId) {
        return getFeatureVariableInteger(featureKey, variableKey, userId, Collections.<String, String>emptyMap());
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
    public @Nullable Integer getFeatureVariableInteger(@Nonnull String featureKey,
                                                       @Nonnull String variableKey,
                                                       @Nonnull String userId,
                                                       @Nonnull Map<String, String> attributes) {
        return null;
    }

    /**
     * Get the String value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @return The String value of the string single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable String getFeatureVariableString(@Nonnull String featureKey,
                                                     @Nonnull String variableKey,
                                                     @Nonnull String userId) {
        return getFeatureVariableString(featureKey, variableKey, userId, Collections.<String, String>emptyMap());
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
    public @Nullable String getFeatureVariableString(@Nonnull String featureKey,
                                                     @Nonnull String variableKey,
                                                     @Nonnull String userId,
                                                     @Nonnull Map<String, String> attributes) {
        return null;
    }

    //======== getVariation calls ========//

    public @Nullable
    Variation getVariation(@Nonnull Experiment experiment,
                           @Nonnull String userId) throws UnknownExperimentException {

        return getVariation(experiment, userId, Collections.<String, String>emptyMap());
    }

    public @Nullable
    Variation getVariation(@Nonnull Experiment experiment,
                           @Nonnull String userId,
                           @Nonnull Map<String, String> attributes) throws UnknownExperimentException {

        Map<String, String> filteredAttributes = filterAttributes(projectConfig, attributes);

        return decisionService.getVariation(experiment, userId, filteredAttributes);
    }

    public @Nullable
    Variation getVariation(@Nonnull String experimentKey,
                           @Nonnull String userId) throws UnknownExperimentException {

        return getVariation(experimentKey, userId, Collections.<String, String>emptyMap());
    }

    public @Nullable
    Variation getVariation(@Nonnull String experimentKey,
                           @Nonnull String userId,
                           @Nonnull Map<String, String> attributes) {
        if (!validateUserId(userId)) {
            return null;
        }

        ProjectConfig currentConfig = getProjectConfig();

        Experiment experiment = getExperimentOrThrow(currentConfig, experimentKey);
        if (experiment == null) {
            // if we're unable to retrieve the associated experiment, return null
            return null;
        }

        Map<String, String> filteredAttributes = filterAttributes(projectConfig, attributes);

        return decisionService.getVariation(experiment,userId,filteredAttributes);
    }

    /**
     * @return the current {@link ProjectConfig} instance.
     */
    public @Nonnull ProjectConfig getProjectConfig() {
        return projectConfig;
    }

    /**
     * @return a {@link ProjectConfig} instance given a json string
     */
    private static ProjectConfig getProjectConfig(String datafile) throws ConfigParseException {
        if (datafile == null) {
            throw new ConfigParseException("Unable to parse null datafile.");
        }
        if (datafile.length() == 0) {
            throw new ConfigParseException("Unable to parse empty datafile.");
        }

        ProjectConfig projectConfig = DefaultConfigParser.getInstance().parseProjectConfig(datafile);

        if (projectConfig.getVersion().equals("1")) {
            throw new ConfigParseException("This version of the Java SDK does not support version 1 datafiles. " +
                    "Please use a version 2 or 3 datafile with this SDK.");
        }

        return projectConfig;
    }

    @Nullable
    public UserProfileService getUserProfileService() {
        return userProfileService;
    }

    //======== Notification listeners ========//

    /**
     * Add a {@link NotificationListener} if it does not exist already.
     *
     * @param listener listener to add
     */
    public void addNotificationListener(@Nonnull NotificationListener listener) {
        notificationBroadcaster.addListener(listener);
    }

    /**
     * Remove a {@link NotificationListener} if it exists.
     *
     * @param listener listener to remove
     */
    public void removeNotificationListener(@Nonnull NotificationListener listener) {
        notificationBroadcaster.removeListener(listener);
    }

    /**
     * Remove all {@link NotificationListener}.
     */
    public void clearNotificationListeners() {
        notificationBroadcaster.clearListeners();
    }

    //======== Helper methods ========//

    /**
     * Helper method to retrieve the {@link Experiment} for the given experiment key.
     * If {@link RaiseExceptionErrorHandler} is provided, either an experiment is returned, or an exception is thrown.
     * If {@link NoOpErrorHandler} is used, either an experiment or {@code null} is returned.
     *
     * @param projectConfig the current project config
     * @param experimentKey the experiment to retrieve from the current project config
     * @return the experiment for given experiment key
     *
     * @throws UnknownExperimentException if there are no experiments in the current project config with the given
     * experiment key
     */
    private @CheckForNull Experiment getExperimentOrThrow(@Nonnull ProjectConfig projectConfig,
                                                          @Nonnull String experimentKey)
        throws UnknownExperimentException {

        Experiment experiment = projectConfig
            .getExperimentKeyMapping()
            .get(experimentKey);

        // if the given experiment key isn't present in the config, log and potentially throw an exception
        if (experiment == null) {
            String unknownExperimentError = String.format("Experiment \"%s\" is not in the datafile.", experimentKey);
            logger.error(unknownExperimentError);
            errorHandler.handleError(new UnknownExperimentException(unknownExperimentError));
        }

        return experiment;
    }

    /**
     * Helper method to retrieve the {@link EventType} for the given event name.
     * If {@link RaiseExceptionErrorHandler} is provided, either an event type is returned, or an exception is thrown.
     * If {@link NoOpErrorHandler} is used, either an event type or {@code null} is returned.
     *
     * @param projectConfig the current project config
     * @param eventName the event type to retrieve from the current project config
     * @return the event type for the given event name
     *
     * @throws UnknownEventTypeException if there are no event types in the current project config with the given name
     */
    private EventType getEventTypeOrThrow(ProjectConfig projectConfig, String eventName)
        throws UnknownEventTypeException {

        EventType eventType = projectConfig
            .getEventNameMapping()
            .get(eventName);

        // if the given event name isn't present in the config, log and potentially throw an exception
        if (eventType == null) {
            String unknownEventTypeError = String.format("Event \"%s\" is not in the datafile.", eventName);
            logger.error(unknownEventTypeError);
            errorHandler.handleError(new UnknownEventTypeException(unknownEventTypeError));
        }

        return eventType;
    }

    /**
     * Helper method to retrieve the {@link LiveVariable} for the given variable key.
     * If {@link RaiseExceptionErrorHandler} is provided, either a live variable is returned, or an exception is
     * thrown.
     * If {@link NoOpErrorHandler} is used, either a live variable or {@code null} is returned.
     *
     * @param projectConfig the current project config
     * @param variableKey the key for the live variable being retrieved from the current project config
     * @return the live variable to retrieve for the given variable key
     *
     * @throws UnknownLiveVariableException if there are no event types in the current project config with the given
     * name
     */
    @Deprecated
    private LiveVariable getLiveVariableOrThrow(ProjectConfig projectConfig, String variableKey)
        throws UnknownLiveVariableException {

        LiveVariable liveVariable = projectConfig
            .getLiveVariableKeyMapping()
            .get(variableKey);

        if (liveVariable == null) {
            String unknownLiveVariableKeyError =
                    String.format("Live variable \"%s\" is not in the datafile.", variableKey);
            logger.error(unknownLiveVariableKeyError);
            errorHandler.handleError(new UnknownLiveVariableException(unknownLiveVariableKeyError));
        }

        return liveVariable;
    }

    /**
     * Helper method to verify that the given attributes map contains only keys that are present in the
     * {@link ProjectConfig}.
     *
     * @param projectConfig the current project config
     * @param attributes the attributes map to validate and potentially filter
     * @return the filtered attributes map (containing only attributes that are present in the project config) or an
     * empty map if a null attributes object is passed in
     */
    private Map<String, String> filterAttributes(@Nonnull ProjectConfig projectConfig,
                                                 @Nonnull Map<String, String> attributes) {
        if (attributes == null) {
            logger.warn("Attributes is null when non-null was expected. Defaulting to an empty attributes map.");
            return Collections.<String, String>emptyMap();
        }

        List<String> unknownAttributes = null;

        Map<String, Attribute> attributeKeyMapping = projectConfig.getAttributeKeyMapping();
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            if (!attributeKeyMapping.containsKey(attribute.getKey())) {
                if (unknownAttributes == null) {
                    unknownAttributes = new ArrayList<String>();
                }
                unknownAttributes.add(attribute.getKey());
            }
        }

        if (unknownAttributes != null) {
            logger.warn("Attribute(s) {} not in the datafile.", unknownAttributes);
            // make a copy of the passed through attributes, then remove the unknown list
            attributes = new HashMap<String, String>(attributes);
            for (String unknownAttribute : unknownAttributes) {
                attributes.remove(unknownAttribute);
            }
        }

        return attributes;
    }

    /**
     * Helper function to check that the provided userId is valid
     *
     * @param userId the userId being validated
     * @return whether the user ID is valid
     */
    private boolean validateUserId(String userId) {
        if (userId.trim().isEmpty()) {
            logger.error("Non-empty user ID required");
            return false;
        }

        return true;
    }

    //======== Builder ========//

    public static Builder builder(@Nonnull String datafile,
                                  @Nonnull EventHandler eventHandler) {
        return new Builder(datafile, eventHandler);
    }

    /**
     * {@link Optimizely} instance builder.
     * <p>
     * <b>NOTE</b>, the default value for {@link #eventHandler} is a {@link NoOpErrorHandler} instance, meaning that the
     * created {@link Optimizely} object will <b>NOT</b> throw exceptions unless otherwise specified.
     *
     * @see #builder(String, EventHandler)
     */
    public static class Builder {

        private String datafile;
        private Bucketer bucketer;
        private DecisionService decisionService;
        private ErrorHandler errorHandler;
        private EventHandler eventHandler;
        private EventBuilder eventBuilder;
        private ClientEngine clientEngine;
        private String clientVersion;
        private ProjectConfig projectConfig;
        private UserProfileService userProfileService;

        public Builder(@Nonnull String datafile,
                       @Nonnull EventHandler eventHandler) {
            this.datafile = datafile;
            this.eventHandler = eventHandler;
        }

        protected Builder withBucketing(Bucketer bucketer) {
            this.bucketer = bucketer;
            return this;
        }

        protected Builder withDecisionService(DecisionService decisionService) {
            this.decisionService = decisionService;
            return this;
        }

        public Builder withErrorHandler(ErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        public Builder withUserProfileService(UserProfileService userProfileService) {
            this.userProfileService = userProfileService;
            return this;
        }

        public Builder withClientEngine(ClientEngine clientEngine) {
            this.clientEngine = clientEngine;
            return this;
        }

        public Builder withClientVersion(String clientVersion) {
            this.clientVersion = clientVersion;
            return this;
        }

        protected Builder withEventBuilder(EventBuilder eventBuilder) {
            this.eventBuilder = eventBuilder;
            return this;
        }

        // Helper function for making testing easier
        protected Builder withConfig(ProjectConfig projectConfig) {
            this.projectConfig = projectConfig;
            return this;
        }

        public Optimizely build() throws ConfigParseException {
            if (projectConfig == null) {
                projectConfig = Optimizely.getProjectConfig(datafile);
            }

            if (bucketer == null) {
                bucketer = new Bucketer(projectConfig);
            }

            if (clientEngine == null) {
                clientEngine = ClientEngine.JAVA_SDK;
            }

            if (clientVersion == null) {
                clientVersion = BuildVersionInfo.VERSION;
            }


            if (eventBuilder == null) {
                eventBuilder = new EventBuilderV2(clientEngine, clientVersion);
            }

            if (errorHandler == null) {
                errorHandler = new NoOpErrorHandler();
            }

            if (decisionService == null) {
                decisionService = new DecisionService(bucketer, errorHandler, projectConfig, userProfileService);
            }

            Optimizely optimizely = new Optimizely(projectConfig, decisionService, eventHandler, eventBuilder, errorHandler, userProfileService);
            optimizely.initialize();
            return optimizely;
        }
    }
}
