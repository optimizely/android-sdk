/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
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

import com.google.gson.Gson;
import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.bucketing.UserProfile;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.Conversion;
import com.optimizely.ab.event.internal.payload.Decision;
import com.optimizely.ab.event.internal.payload.Event.ClientEngine;
import com.optimizely.ab.event.internal.payload.EventMetric;
import com.optimizely.ab.event.internal.payload.Feature;
import com.optimizely.ab.event.internal.payload.Impression;
import com.optimizely.ab.event.internal.payload.LayerState;
import com.optimizely.ab.internal.ProjectValidationUtils;
import com.optimizely.ab.internal.ReservedEventKey;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link EventBuilderV2}
 */
public class EventBuilderV2Test {

    private Gson gson = new Gson();
    private EventBuilderV2 builder = new EventBuilderV2();

    private static String userId = "userId";
    private static ProjectConfig validProjectConfig;

    @BeforeClass
    public static void setUp() throws IOException {
        validProjectConfig = validProjectConfigV2();
    }

    /**
     * Verify {@link Impression} event creation
     */
    @Test
    public void createImpressionEvent() throws Exception {
        // use the "valid" project config and its associated experiment, variation, and attributes
        ProjectConfig projectConfig = validProjectConfigV2();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = projectConfig.getAttributes().get(0);
        String userId = "userId";
        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");
        Decision expectedDecision = new Decision(bucketedVariation.getId(), false, activatedExperiment.getId());
        Feature feature = new Feature(attribute.getId(), attribute.getKey(), Feature.CUSTOM_ATTRIBUTE_FEATURE_TYPE,
                                      "value", true);
        List<Feature> expectedUserFeatures = Collections.singletonList(feature);

        LogEvent impressionEvent = builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation,
                                                                 userId, attributeMap);

        // verify that request endpoint is correct
        assertThat(impressionEvent.getEndpointUrl(), is(EventBuilderV2.IMPRESSION_ENDPOINT));

        Impression impression = gson.fromJson(impressionEvent.getBody(), Impression.class);

        // verify payload information
        assertThat(impression.getVisitorId(), is(userId));
        assertThat((double)impression.getTimestamp(), closeTo((double)System.currentTimeMillis(), 60.0));
        assertFalse(impression.getIsGlobalHoldback());
        assertThat(impression.getAnonymizeIP(), is(projectConfig.getAnonymizeIP()));
        assertThat(impression.getProjectId(), is(projectConfig.getProjectId()));
        assertThat(impression.getDecision(), is(expectedDecision));
        assertThat(impression.getLayerId(), is(activatedExperiment.getLayerId()));
        assertThat(impression.getAccountId(), is(projectConfig.getAccountId()));
        assertThat(impression.getUserFeatures(), is(expectedUserFeatures));
        assertThat(impression.getClientEngine(), is(ClientEngine.JAVA_SDK.getClientEngineValue()));
        assertThat(impression.getClientVersion(), is(BuildVersionInfo.VERSION));
        assertNull(impression.getSessionId());
    }

    /**
     * Verify that passing through an unknown attribute causes that attribute to be ignored, rather than
     * causing an exception to be thrown.
     */
    @Test
    public void createImpressionEventIgnoresUnknownAttributes() throws Exception {
        // use the "valid" project config and its associated experiment, variation, and attributes
        ProjectConfig projectConfig = validProjectConfigV2();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        LogEvent impressionEvent =
                builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation, "userId",
                                              Collections.singletonMap("unknownAttribute", "blahValue"));

        Impression impression = gson.fromJson(impressionEvent.getBody(), Impression.class);

        // verify that no Feature is created for "unknownAtrribute" -> "blahValue"
        for (Feature feature : impression.getUserFeatures()) {
            assertNotEquals(feature.getName(), "unknownAttribute");
            assertNotEquals(feature.getValue(), "blahValue");
        }
    }

    /**
     * Verify that supplying {@link EventBuilderV2} with a custom client engine and client version results in impression
     * events being sent with the overriden values.
     */
    @Test
    public void createImpressionEventAndroidClientEngineClientVersion() throws Exception {
        EventBuilderV2 builder = new EventBuilderV2(ClientEngine.ANDROID_SDK, "0.0.0");
        ProjectConfig projectConfig = validProjectConfigV2();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = projectConfig.getAttributes().get(0);
        String userId = "userId";
        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");

        LogEvent impressionEvent = builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation,
                                                                 userId, attributeMap);
        Impression impression = gson.fromJson(impressionEvent.getBody(), Impression.class);

        assertThat(impression.getClientEngine(), is(ClientEngine.ANDROID_SDK.getClientEngineValue()));
        assertThat(impression.getClientVersion(), is("0.0.0"));
    }

    /**
     * Verify that supplying {@link EventBuilderV2} with a custom Android TV client engine and client version
     * results in impression events being sent with the overriden values.
     */
    @Test
    public void createImpressionEventAndroidTVClientEngineClientVersion() throws Exception {
        String clientVersion = "0.0.0";
        EventBuilderV2 builder = new EventBuilderV2(ClientEngine.ANDROID_TV_SDK, clientVersion);
        ProjectConfig projectConfig = validProjectConfigV2();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = projectConfig.getAttributes().get(0);
        String userId = "userId";
        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");

        LogEvent impressionEvent = builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation,
                                                                 userId, attributeMap);
        Impression impression = gson.fromJson(impressionEvent.getBody(), Impression.class);

        assertThat(impression.getClientEngine(), is(ClientEngine.ANDROID_TV_SDK.getClientEngineValue()));
        assertThat(impression.getClientVersion(), is(clientVersion));
    }

    /**
     * Verify {@link Conversion} event creation
     */
    @Test
    public void createConversionEvent() throws Exception {
        // use the "valid" project config and its associated experiment, variation, and attributes
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String userId = "userId";

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);

        List<Experiment> allExperiments = validProjectConfig.getExperiments();
        List<Experiment> experimentsForEventKey = validProjectConfig.getExperimentsForEventKey(eventType.getKey());

        // Bucket to the first variation for all experiments. However, only a subset of the experiments will actually
        // call the bucket function.
        for (Experiment experiment : allExperiments) {
            when(mockBucketAlgorithm.bucket(experiment, userId))
                .thenReturn(experiment.getVariations().get(0));
        }

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");
        Map<String, Object> eventTagMap = new HashMap<String, Object>();
        eventTagMap.put("boolean_param", false);
        eventTagMap.put("string_param", "123");
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                mockBucketAlgorithm,
                null,
                eventType.getKey(),
                userId,
                attributeMap);
        LogEvent conversionEvent = builder.createConversionEvent(
                validProjectConfig,
                experimentVariationMap,
                userId,
                eventType.getId(),
                eventType.getKey(),
                attributeMap,
                eventTagMap);

        List<LayerState> expectedLayerStates = new ArrayList<LayerState>();

        for (Experiment experiment : experimentsForEventKey) {
            if (ProjectValidationUtils.validatePreconditions(validProjectConfig, null, experiment, userId, attributeMap)) {
                LayerState layerState = new LayerState(experiment.getLayerId(), validProjectConfig.getRevision(),
                        new Decision(experiment.getVariations().get(0).getId(), false, experiment.getId()), true);
                expectedLayerStates.add(layerState);
            }
        }

        // verify that the request endpoint is correct
        assertThat(conversionEvent.getEndpointUrl(), is(EventBuilderV2.CONVERSION_ENDPOINT));

        Conversion conversion = gson.fromJson(conversionEvent.getBody(), Conversion.class);

        // verify payload information
        assertThat(conversion.getVisitorId(), is(userId));
        assertThat((double)conversion.getTimestamp(), closeTo((double)System.currentTimeMillis(), 60.0));
        assertThat(conversion.getProjectId(), is(validProjectConfig.getProjectId()));
        assertThat(conversion.getAccountId(), is(validProjectConfig.getAccountId()));

        Feature feature = new Feature(attribute.getId(), attribute.getKey(), Feature.CUSTOM_ATTRIBUTE_FEATURE_TYPE,
                                      "value", true);
        List<Feature> expectedUserFeatures = Collections.singletonList(feature);

        // Event Features
        List<Feature> expectedEventFeatures = new ArrayList<Feature>();
        expectedEventFeatures.add(new Feature("", "boolean_param", Feature.EVENT_FEATURE_TYPE,
                false, false));
        expectedEventFeatures.add(new Feature("", "string_param", Feature.EVENT_FEATURE_TYPE,
                "123", false));

        assertThat(conversion.getUserFeatures(), is(expectedUserFeatures));
        assertThat(conversion.getLayerStates(), is(expectedLayerStates));
        assertThat(conversion.getEventEntityId(), is(eventType.getId()));
        assertThat(conversion.getEventName(), is(eventType.getKey()));
        assertThat(conversion.getEventMetrics(), is(Collections.<EventMetric>emptyList()));
        assertTrue(conversion.getEventFeatures().containsAll(expectedEventFeatures));
        assertTrue(expectedEventFeatures.containsAll(conversion.getEventFeatures()));
        assertFalse(conversion.getIsGlobalHoldback());
        assertThat(conversion.getAnonymizeIP(), is(validProjectConfig.getAnonymizeIP()));
        assertThat(conversion.getClientEngine(), is(ClientEngine.JAVA_SDK.getClientEngineValue()));
        assertThat(conversion.getClientVersion(), is(BuildVersionInfo.VERSION));
    }

    /**
     * Verify that eventValue is properly recorded in a conversion request as an {@link EventMetric}
     */
    @Test
    public void createConversionParamsWithRevenue() throws Exception {
        long revenue = 1234L;

        // use the "valid" project config and its associated experiment, variation, and attributes
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        EventType eventType = validProjectConfig.getEventTypes().get(0);

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);

        // Bucket to the first variation for all experiments.
        for (Experiment experiment : validProjectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, userId))
                .thenReturn(experiment.getVariations().get(0));
        }

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");
        Map<String, Object> eventTagMap = new HashMap<String, Object>();
        eventTagMap.put(ReservedEventKey.REVENUE.toString(), revenue);
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                mockBucketAlgorithm,
                null,
                eventType.getKey(),
                userId,
                attributeMap);
        LogEvent conversionEvent = builder.createConversionEvent(validProjectConfig, experimentVariationMap, userId,
                                                                 eventType.getId(), eventType.getKey(), attributeMap,
                                                                 eventTagMap);

        Conversion conversion = gson.fromJson(conversionEvent.getBody(), Conversion.class);

        // we're not going to verify everything, only revenue
        assertThat(conversion.getEventMetrics(),
                   is(Collections.singletonList(new EventMetric(EventMetric.REVENUE_METRIC_TYPE, revenue))));
    }

    /**
     * Verify that a {@link LayerState} isn't created if a user doesn't satisfy audience conditions for an experiment.
     */
    @Test
    public void createConversionParamsUserNotInAudience() throws Exception {
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        EventType eventType = validProjectConfig.getEventTypes().get(2);
        String userId = "userId";

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);

        // Bucket to the first variation for all experiments.
        for (Experiment experiment : validProjectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, userId))
                .thenReturn(experiment.getVariations().get(0));
        }

        // the audience for the experiments is "NOT firefox" so this user shouldn't satisfy audience conditions
        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "firefox");
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                mockBucketAlgorithm,
                null,
                eventType.getKey(),
                userId,
                attributeMap);
        LogEvent conversionEvent = builder.createConversionEvent(validProjectConfig, experimentVariationMap, userId,
                                                                 eventType.getId(), eventType.getKey(), attributeMap,
                Collections.<String, Object>emptyMap());

        assertNull(conversionEvent);
    }

    /**
     * Verify that precedence is given to forced variation bucketing over audience evaluation when constructing a
     * conversion event.
     */
    @Test
    public void createConversionEventForcedVariationBucketingPrecedesAudienceEval() {
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String userId = "testUser1";

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);
        for (Experiment experiment : validProjectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, userId))
                .thenReturn(experiment.getVariations().get(0));
        }

        // attributes are empty so user won't be in the audience for experiment using the event, but bucketing
        // will still take place
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                mockBucketAlgorithm,
                null,
                eventType.getKey(),
                userId,
                Collections.<String, String>emptyMap());
        LogEvent conversionEvent = builder.createConversionEvent(
                validProjectConfig,
                experimentVariationMap,
                userId,
                eventType.getId(),
                eventType.getKey(),
                Collections.<String, String>emptyMap(),
                Collections.<String, Object>emptyMap());

        Conversion conversion = gson.fromJson(conversionEvent.getBody(), Conversion.class);
        // 1 experiment uses the event
        assertThat(conversion.getLayerStates().size(), is(1));
    }

    /**
     * Verify that precedence is given to experiment status over forced variation bucketing when constructing a
     * conversion event.
     */
    @Test
    public void createConversionEventExperimentStatusPrecedesForcedVariation() {
        EventType eventType = validProjectConfig.getEventTypes().get(3);
        String userId = "userId";

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);
        for (Experiment experiment : validProjectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, userId))
                .thenReturn(experiment.getVariations().get(0));
        }

        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                mockBucketAlgorithm,
                null,
                eventType.getKey(),
                userId,
                Collections.<String, String>emptyMap());
        LogEvent conversionEvent = builder.createConversionEvent(
                validProjectConfig,
                experimentVariationMap,
                userId,
                eventType.getId(),
                eventType.getKey(),
                Collections.<String, String>emptyMap(),
                Collections.<String, Object>emptyMap());

        for (Experiment experiment : validProjectConfig.getExperiments()) {
            verify(mockBucketAlgorithm, never()).bucket(experiment, userId);
        }

        assertNull(conversionEvent);
    }

    /**
     * Verify that supplying {@link EventBuilderV2} with a custom client engine and client version results in conversion
     * events being sent with the overriden values.
     */
    @Test
    public void createConversionEventAndroidClientEngineClientVersion() throws Exception {
        EventBuilderV2 builder = new EventBuilderV2(ClientEngine.ANDROID_SDK, "0.0.0");
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        EventType eventType = validProjectConfig.getEventTypes().get(0);

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);
        for (Experiment experiment : validProjectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, userId))
                    .thenReturn(experiment.getVariations().get(0));
        }

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                mockBucketAlgorithm,
                null,
                eventType.getKey(),
                userId,
                attributeMap);
        LogEvent conversionEvent = builder.createConversionEvent(
                validProjectConfig,
                experimentVariationMap,
                userId,
                eventType.getId(),
                eventType.getKey(),
                attributeMap,
                Collections.<String, Object>emptyMap());

        Conversion conversion = gson.fromJson(conversionEvent.getBody(), Conversion.class);

        assertThat(conversion.getClientEngine(), is(ClientEngine.ANDROID_SDK.getClientEngineValue()));
        assertThat(conversion.getClientVersion(), is("0.0.0"));
    }

    /**
     * Verify that supplying {@link EventBuilderV2} with a Android TV client engine and client version results in
     * conversion events being sent with the overriden values.
     */
    @Test
    public void createConversionEventAndroidTVClientEngineClientVersion() throws Exception {
        String clientVersion = "0.0.0";
        EventBuilderV2 builder = new EventBuilderV2(ClientEngine.ANDROID_TV_SDK, clientVersion);
        ProjectConfig projectConfig = validProjectConfigV2();
        Attribute attribute = projectConfig.getAttributes().get(0);
        EventType eventType = projectConfig.getEventTypes().get(0);
        String userId = "userId";

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);
        for (Experiment experiment : projectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, userId))
                    .thenReturn(experiment.getVariations().get(0));
        }

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");
        List<Experiment> experimentList = projectConfig.getExperimentsForEventKey(eventType.getKey());
        Map<Experiment, Variation> experimentVariationMap = new HashMap<Experiment, Variation>(experimentList.size());
        for (Experiment experiment : experimentList) {
            experimentVariationMap.put(experiment, experiment.getVariations().get(0));
        }

        LogEvent conversionEvent = builder.createConversionEvent(
                projectConfig,
                experimentVariationMap,
                userId,
                eventType.getId(),
                eventType.getKey(),
                attributeMap,
                Collections.<String, Object>emptyMap());
        Conversion conversion = gson.fromJson(conversionEvent.getBody(), Conversion.class);

        assertThat(conversion.getClientEngine(), is(ClientEngine.ANDROID_TV_SDK.getClientEngineValue()));
        assertThat(conversion.getClientVersion(), is(clientVersion));
    }

    //========== helper methods =========//
    public static Map<Experiment, Variation> createExperimentVariationMap(ProjectConfig projectConfig,
                                                                          Bucketer bucketer,
                                                                          UserProfile userProfile,
                                                                          String eventName,
                                                                          String userId,
                                                                          @Nullable Map<String, String> attributes) {

        List<Experiment> eventExperiments = projectConfig.getExperimentsForEventKey(eventName);
        Map<Experiment, Variation> experimentVariationMap = new HashMap<Experiment, Variation>(eventExperiments.size());
        for (Experiment experiment : eventExperiments) {
            if (ProjectValidationUtils.validatePreconditions(projectConfig, userProfile, experiment, userId, attributes)
                    && experiment.isRunning()) {
                Variation variation = bucketer.bucket(experiment, userId);
                if (variation != null) {
                    experimentVariationMap.put(experiment, variation);
                }
            }
        }

        return experimentVariationMap;
    }
}
