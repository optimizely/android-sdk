/**
 *
 *    Copyright 2016, Optimizely and contributors
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

import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.Conversion;
import com.optimizely.ab.event.internal.payload.Decision;
import com.optimizely.ab.event.internal.payload.EventMetric;
import com.optimizely.ab.event.internal.payload.Event.ClientEngine;
import com.optimizely.ab.event.internal.payload.Feature;
import com.optimizely.ab.event.internal.payload.Impression;
import com.optimizely.ab.event.internal.payload.LayerState;
import com.optimizely.ab.event.internal.serializer.DefaultJsonSerializer;
import com.optimizely.ab.event.internal.serializer.Serializer;
import com.optimizely.ab.internal.ProjectValidationUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.event.LogEvent.RequestMethod;

public class EventBuilderV2 extends EventBuilder {

    private static final Logger logger = LoggerFactory.getLogger(EventBuilderV2.class);

    static final String IMPRESSION_ENDPOINT = "https://logx.optimizely.com/log/decision";
    static final String CONVERSION_ENDPOINT = "https://logx.optimizely.com/log/event";

    @VisibleForTesting
    public final ClientEngine clientEngine;

    @VisibleForTesting
    public final String clientVersion;

    private Serializer serializer;

    public EventBuilderV2() {
        this(ClientEngine.JAVA_SDK, BuildVersionInfo.VERSION);
    }

    public EventBuilderV2(ClientEngine clientEngine, String clientVersion) {
        this.clientEngine = clientEngine;
        this.clientVersion = clientVersion;
        this.serializer = DefaultJsonSerializer.getInstance();
    }

    public LogEvent createImpressionEvent(@Nonnull ProjectConfig projectConfig,
                                          @Nonnull Experiment activatedExperiment,
                                          @Nonnull Variation variation,
                                          @Nonnull String userId,
                                          @Nonnull Map<String, String> attributes) {

        Impression impressionPayload = new Impression();
        impressionPayload.setVisitorId(userId);
        impressionPayload.setTimestamp(System.currentTimeMillis());
        impressionPayload.setIsGlobalHoldback(false);
        impressionPayload.setProjectId(projectConfig.getProjectId());

        Decision decision = new Decision();
        decision.setVariationId(variation.getId());
        decision.setIsLayerHoldback(false);
        decision.setExperimentId(activatedExperiment.getId());
        impressionPayload.setDecision(decision);

        impressionPayload.setLayerId(activatedExperiment.getLayerId());
        impressionPayload.setAccountId(projectConfig.getAccountId());
        impressionPayload.setUserFeatures(createFeatures(attributes, projectConfig));
        impressionPayload.setClientEngine(clientEngine);
        impressionPayload.setClientVersion(clientVersion);
        impressionPayload.setAnonymizeIP(projectConfig.getAnonymizeIP());

        String payload = this.serializer.serialize(impressionPayload);
        return new LogEvent(RequestMethod.POST, IMPRESSION_ENDPOINT, Collections.<String, String>emptyMap(), payload);
    }

    public LogEvent createConversionEvent(@Nonnull ProjectConfig projectConfig,
                                          @Nonnull Bucketer bucketer,
                                          @Nonnull String userId,
                                          @Nonnull String eventId,
                                          @Nonnull String eventName,
                                          @Nonnull Map<String, String> attributes,
                                          @CheckForNull Long eventValue) {

        Conversion conversionPayload = new Conversion();
        conversionPayload.setVisitorId(userId);
        conversionPayload.setTimestamp(System.currentTimeMillis());
        conversionPayload.setProjectId(projectConfig.getProjectId());
        conversionPayload.setAccountId(projectConfig.getAccountId());
        conversionPayload.setUserFeatures(createFeatures(attributes, projectConfig));

        List<LayerState> layerStates = createLayerStates(projectConfig, bucketer, userId, eventName, attributes);
        if (layerStates.isEmpty()) {
            return null;
        }
        conversionPayload.setLayerStates(layerStates);

        conversionPayload.setEventEntityId(eventId);
        conversionPayload.setEventName(eventName);

        if (eventValue != null) {
            conversionPayload.setEventMetrics(
                    Collections.singletonList(new EventMetric(EventMetric.REVENUE_METRIC_TYPE, eventValue)));
        } else {
            conversionPayload.setEventMetrics(Collections.<EventMetric>emptyList());
        }

        conversionPayload.setEventFeatures(Collections.<Feature>emptyList());
        conversionPayload.setIsGlobalHoldback(false);
        conversionPayload.setAnonymizeIP(projectConfig.getAnonymizeIP());
        conversionPayload.setClientEngine(clientEngine);
        conversionPayload.setClientVersion(clientVersion);

        String payload = this.serializer.serialize(conversionPayload);
        return new LogEvent(RequestMethod.POST, CONVERSION_ENDPOINT, Collections.<String, String>emptyMap(), payload);
    }

    /**
     * Helper method to generate {@link Feature} objects from the given {@code {attributeKey -> value}} mapping.
     *
     * @param attributes the {@code {attributeKey -> value}} mapping
     * @param projectConfig the current project config
     */
    private List<Feature> createFeatures(Map<String, String> attributes, ProjectConfig projectConfig) {
        Map<String, Attribute> attributeKeyMapping = projectConfig.getAttributeKeyMapping();
        List<Feature> features = new ArrayList<Feature>();

        for (Map.Entry<String, String> attributeEntry : attributes.entrySet()) {
            String attributeKey = attributeEntry.getKey();
            Attribute attribute = attributeKeyMapping.get(attributeKey);

            if (attribute == null) {
                logger.warn("Attempting to use unknown attribute key: {}. Attribute will be ignored", attributeKey);
                continue;
            }

            features.add(new Feature(attribute.getId(), attributeKey, Feature.CUSTOM_ATTRIBUTE_FEATURE_TYPE,
                                     attributeEntry.getValue(), true));
        }

        return features;
    }

    /**
     * Helper method to create {@link LayerState} objects for all experiments mapped to an event.
     * <p>
     * Note, although the user may not have <i>activated</i> all experiments, we include all <i>possible</i> experiment
     * mappings so that activated experiment state doesn't have to be maintained and passed around server-side.
     * <p>
     * For example, in a project with multiple experiments, if user 'user1' activates experiment 'exp1' and then
     * converts, we need to ensure we attribute that conversion to 'exp1'. We can either do that by recording activation
     * and passing that state around so it's available on conversion, or we can send all possible activations at
     * conversion-time and attribute conversions <i>only</i> to the experiments for which we also received an
     * impression.
     * <p>
     * This is referred to as "visitor first counting". It's important to filter the bucket map as much as possible,
     * as any experiments sent through that don't have a corresponding impression are simply occupying space for
     * no good reason.
     *
     * @param projectConfig the current project config
     * @param bucketer the bucketing algorithm to use
     * @param userId the user's id for the impression event
     * @param eventKey the goal that the bucket map will be filtered by
     * @param attributes the user's attributes
     */
    private List<LayerState> createLayerStates(ProjectConfig projectConfig, Bucketer bucketer, String userId,
                                               String eventKey, Map<String, String> attributes) {
        List<Experiment> allExperiments = projectConfig.getExperiments();
        List<String> experimentIds = projectConfig.getExperimentIdsForGoal(eventKey);
        List<LayerState> layerStates = new ArrayList<LayerState>();

        for (Experiment experiment : allExperiments) {
            if (experimentIds.contains(experiment.getId()) &&
                    ProjectValidationUtils.validatePreconditions(projectConfig, experiment, userId, attributes)) {
                Variation bucketedVariation = bucketer.bucket(experiment, userId);
                if (bucketedVariation != null) {
                    Decision decision = new Decision(bucketedVariation.getId(), false, experiment.getId());
                    layerStates.add(new LayerState(experiment.getLayerId(), decision, true));
                }
            }
        }

        return layerStates;
    }
}
