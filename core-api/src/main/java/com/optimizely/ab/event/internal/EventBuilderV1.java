/**
 *
 *    Copyright 2016, Optimizely
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.event.internal;

import com.optimizely.ab.BuildConfig;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.internal.ProjectValidationUtils;
import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import static com.optimizely.ab.event.LogEvent.RequestMethod;

/**
 * Event builder that produces
 * <a href="https://help.optimizely.com/hc/en-us/articles/200040195-Tracking-offline-conversion-events-with-Optimizely">
 *     Optimizely offline conversion</a> events.
 */
public class EventBuilderV1 extends EventBuilder {

    private static final Logger logger = LoggerFactory.getLogger(EventBuilderV1.class);

    private static final String ENDPOINT_FORMAT = "https://%s.log.optimizely.com/event";

    // offline conversion parameter names and prefixes
    private static final String ACCOUNT_ID_PARAM = "d";
    private static final String BUILD_VERSION = "java-sdk-" + BuildConfig.VERSION;
    private static final String EMPTY_BODY = "";
    private static final String EXPERIMENT_PARAM_PREFIX = "x";
    private static final String GOAL_ID_PARAM = "g";
    private static final String GOAL_NAME_PARAM = "n";
    private static final String PROJECT_ID_PARAM = "a";
    private static final String EVENT_VALUE_PARAM = "v";
    private static final String SEGMENT_PARAM_PREFIX = "s";
    private static final String SOURCE_PARAM = "src";
    private static final String TIME_PARAM = "time";
    private static final String USER_ID_PARAM = "u";

    public LogEvent createImpressionEvent(@Nonnull ProjectConfig projectConfig,
                                          @Nonnull Experiment activatedExperiment,
                                          @Nonnull Variation variation,
                                          @Nonnull String userId,
                                          @Nonnull Map<String, String> attributes) {

        Map<String, String> requestParams = new HashMap<String, String>();
        addCommonRequestParams(requestParams, projectConfig, userId, attributes);
        addImpressionGoal(requestParams, activatedExperiment);
        addExperiment(requestParams, activatedExperiment, variation);

        return new LogEvent(RequestMethod.GET,
                            String.format(ENDPOINT_FORMAT, projectConfig.getProjectId()), requestParams, EMPTY_BODY);
    }

    LogEvent createConversionEvent(@Nonnull ProjectConfig projectConfig,
                                   @Nonnull Bucketer bucketer,
                                   @Nonnull String userId,
                                   @Nonnull String eventId,
                                   @Nonnull String eventName,
                                   @Nonnull Map<String, String> attributes,
                                   @CheckForNull Long eventValue) {

        Map<String, String> requestParams = new HashMap<String, String>();
        List<Experiment> addedExperiments =
                addExperimentBucketMap(requestParams, projectConfig, bucketer, userId, eventName, attributes);

        if (addedExperiments.isEmpty()) {
            return null;
        }

        addCommonRequestParams(requestParams, projectConfig, userId, attributes);
        addConversionGoal(requestParams, projectConfig, eventId, eventName, eventValue);

        return new LogEvent(RequestMethod.GET,
                            String.format(ENDPOINT_FORMAT, projectConfig.getProjectId()), requestParams, EMPTY_BODY);
    }

    //======== Helper methods ========//

    /**
     * Helper method to populate the request params that are common across impressions and conversions.
     */
    private void addCommonRequestParams(Map<String, String> requestParams, ProjectConfig projectConfig,
                                        String userId, Map<String, String> attributes) {

        addProjectId(requestParams, projectConfig.getProjectId());
        addAccountId(requestParams, projectConfig.getAccountId());
        addUserId(requestParams, userId);
        addSegments(requestParams, attributes, projectConfig);
        addTime(requestParams);
        addSource(requestParams);
    }

    private void addUserId(Map<String, String> requestParams, String userId) {
        requestParams.put(USER_ID_PARAM, userId);
    }

    /**
     * Helper method to populate the account id param.
     */
    private void addAccountId(Map<String, String> requestParams, String accountId) {
        requestParams.put(ACCOUNT_ID_PARAM, accountId);
    }

    /**
     * Helper method to populate the project id param.
     */
    private void addProjectId(Map<String, String> requestParams, String projectId) {
        requestParams.put(PROJECT_ID_PARAM, projectId);
    }

    /**
     * Helper method to populate the experiment bucket map. That is, the map of {@code {experimentId -> variationId}}
     * for all active experiments.
     * <p>
     * Note, although the user may not have <i>activated</i> all experiments, we include all <i>possible</i> experiment
     * mappings so that  activated experiment state doesn't have to be maintained and passed around server-side.
     * <p>
     * For example, in a project with multiple experiments, if user 'user1' activates experiment 'exp1' then converts,
     * we need to ensure we attribute that conversion to 'exp1'. We can either do that by recording activation
     * and passing that state around so it's available on conversion, or we can send all possible activations at
     * conversion-time and attribute conversions <i>only</i> to the experiments for which we also received an
     * impression.
     * <p>
     * This is referred to as "visitor first counting". It's important to filter the bucket map as much as possible,
     * as any experiments sent through that don't have a corresponding impression are simply occupying space for
     * no good reason.
     *
     * @param requestParams the request params
     * @param projectConfig the current project config
     * @param bucketer the bucketing algorithm to use
     * @param userId the user's id for the impression event
     * @param goalKey the goal that the bucket map will be filtered by
     * @param attributes the user's attributes
     */
    private List<Experiment> addExperimentBucketMap(Map<String, String> requestParams, ProjectConfig projectConfig,
                                                    Bucketer bucketer, String userId, String goalKey,
                                                    Map<String, String> attributes) {
        List<Experiment> allExperiments = projectConfig.getExperiments();
        List<String> experimentIds = projectConfig.getExperimentIdsForGoal(goalKey);
        List<Experiment> validExperiments = new ArrayList<Experiment>();

        for (Experiment experiment : allExperiments) {
            if (experimentIds.contains(experiment.getId()) &&
                    ProjectValidationUtils.validatePreconditions(projectConfig, experiment, userId, attributes)) {
                Variation bucketedVariation = bucketer.bucket(experiment, userId);
                if (bucketedVariation != null) {
                    requestParams.put(EXPERIMENT_PARAM_PREFIX + experiment.getId(), bucketedVariation.getId());
                    validExperiments.add(experiment);
                }
            }
        }

        return validExperiments;
    }

    /**
     * Helper method to add the {@code {experiment -> variation}} mapping for an impression event.
     */
    private void addExperiment(Map<String, String> requestParams, Experiment activatedExperiment, Variation variation) {
        requestParams.put(EXPERIMENT_PARAM_PREFIX + activatedExperiment.getId(), variation.getId());
    }

    /**
     * Helper method to populate the segment map request params from the given {@code {attributeKey -> value}} mapping.
     *
     * @param requestParams the request params
     * @param attributes the {@code {attributeKey -> value}} mapping
     * @param projectConfig the current project config
     */
    private void addSegments(Map<String, String> requestParams, Map<String, String> attributes, ProjectConfig projectConfig) {
        Map<String, Attribute> attributeKeyMapping = projectConfig.getAttributeKeyMapping();

        for (Map.Entry<String, String> attributeEntry : attributes.entrySet()) {
            String attributeKey = attributeEntry.getKey();
            Attribute attribute = attributeKeyMapping.get(attributeKey);

            if (attribute == null) {
                logger.warn("Attempting to use unknown attribute key: {}. Attribute will be ignored", attributeKey);
                continue;
            }

            requestParams.put(SEGMENT_PARAM_PREFIX + attribute.getSegmentId(), attributeEntry.getValue());
        }
    }

    /**
     * Helper method to populate the impression goal param. For impressions, the goal id is the activated experiment id.
     *
     * @param requestParams the request params
     * @param activatedExperiment the activate experiment for which we want to track an impression
     */
    private void addImpressionGoal(Map<String, String> requestParams, Experiment activatedExperiment) {
        requestParams.put(GOAL_ID_PARAM, activatedExperiment.getId());
    }

    /**
     * Helper method to populate the conversion goal param. For conversions, the id of the event (or goal id) is used.
     *
     * @param requestParams the request params
     * @param projectConfig the project config
     * @param eventId the goal being converted on
     * @param eventName the name of the custom event goal
     * @param eventValue the optional event value for the event
     */
    private void addConversionGoal(Map<String, String> requestParams, ProjectConfig projectConfig, String eventId,
                                   String eventName, @CheckForNull Long eventValue) {

        String eventIds = eventId;
        if (eventValue != null) {
            // record the event value for the total revenue goal
            requestParams.put(EVENT_VALUE_PARAM, eventValue.toString());
            EventType revenueGoal = projectConfig.getEventNameMapping().get(EventType.TOTAL_REVENUE_GOAL_KEY);
            if (revenueGoal != null) {
                eventIds = eventId + "," + revenueGoal.getId();
            }
        }

        requestParams.put(GOAL_ID_PARAM, eventIds);
        requestParams.put(GOAL_NAME_PARAM, eventName);
    }

    /**
     * Helper method to populate the unix time (in seconds, not ms) {@code time} param.
     */
    private void addTime(Map<String, String> requestParams) {
        requestParams.put(TIME_PARAM, String.format("%.3f", (double)System.currentTimeMillis() / 1000));
    }

    /**
     * Helper method to populate the current SDK version in the source param.
     */
    private void addSource(Map<String, String> requestParams) {
        requestParams.put(SOURCE_PARAM, BUILD_VERSION);
    }
}
