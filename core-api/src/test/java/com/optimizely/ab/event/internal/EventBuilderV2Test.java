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
import com.google.gson.internal.LazilyParsedNumber;
import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.bucketing.DecisionService;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.error.NoOpErrorHandler;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.Conversion;
import com.optimizely.ab.event.internal.payload.Decision;
import com.optimizely.ab.event.internal.payload.Event.ClientEngine;
import com.optimizely.ab.event.internal.payload.EventMetric;
import com.optimizely.ab.event.internal.payload.Feature;
import com.optimizely.ab.event.internal.payload.Impression;
import com.optimizely.ab.event.internal.payload.LayerState;
import com.optimizely.ab.internal.ReservedEventKey;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigJsonV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV4;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV4;
import static com.optimizely.ab.config.ValidProjectConfigV4.AUDIENCE_GRYFFINDOR_VALUE;
import static com.optimizely.ab.config.ValidProjectConfigV4.EVENT_BASIC_EVENT_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.EVENT_PAUSED_EXPERIMENT_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED;
import static com.optimizely.ab.config.ValidProjectConfigV4.PAUSED_EXPERIMENT_FORCED_VARIATION_USER_ID_CONTROL;
import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link EventBuilderV2}
 */
@RunWith(Parameterized.class)
public class EventBuilderV2Test {

    @Parameters
    public static Collection<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][] {
                {
                        2,
                        validProjectConfigV2()
                },
                {
                        4,
                        validProjectConfigV4()
                }
        });
    }

    private Gson gson = new Gson();
    private EventBuilderV2 builder = new EventBuilderV2();

    private static String userId = "userId";
    private int datafileVersion;
    private ProjectConfig validProjectConfig;

    public EventBuilderV2Test(int datafileVersion,
                              ProjectConfig validProjectConfig) {
        this.datafileVersion = datafileVersion;
        this.validProjectConfig = validProjectConfig;
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
            assertFalse(feature.getName() == "unknownAttribute");
            assertFalse(feature.getValue() == "blahValue");
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
        DecisionService decisionService = new DecisionService(
                mockBucketAlgorithm,
                mock(ErrorHandler.class),
                validProjectConfig,
                mock(UserProfileService.class)
        );

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), AUDIENCE_GRYFFINDOR_VALUE);
        Map<String, Object> eventTagMap = new HashMap<String, Object>();
        eventTagMap.put("boolean_param", false);
        eventTagMap.put("string_param", "123");
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                decisionService,
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
            if (experiment.isRunning()) {
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
        assertThat((double)conversion.getTimestamp(), closeTo((double)System.currentTimeMillis(), 120.0));
        assertThat(conversion.getProjectId(), is(validProjectConfig.getProjectId()));
        assertThat(conversion.getAccountId(), is(validProjectConfig.getAccountId()));

        Feature feature = new Feature(attribute.getId(), attribute.getKey(), Feature.CUSTOM_ATTRIBUTE_FEATURE_TYPE,
                AUDIENCE_GRYFFINDOR_VALUE, true);
        List<Feature> expectedUserFeatures = Collections.singletonList(feature);

        // Event Features
        List<Feature> expectedEventFeatures = new ArrayList<Feature>();
        expectedEventFeatures.add(new Feature("", "boolean_param", Feature.EVENT_FEATURE_TYPE,
                false, false));
        expectedEventFeatures.add(new Feature("", "string_param", Feature.EVENT_FEATURE_TYPE,
                "123", false));

        assertEquals(conversion.getUserFeatures(), expectedUserFeatures);
        assertThat(conversion.getLayerStates(), containsInAnyOrder(expectedLayerStates.toArray()));
        assertEquals(conversion.getEventEntityId(), eventType.getId());
        assertEquals(conversion.getEventName(), eventType.getKey());
        assertEquals(conversion.getEventMetrics(), Collections.<EventMetric>emptyList());
        assertTrue(conversion.getEventFeatures().containsAll(expectedEventFeatures));
        assertTrue(expectedEventFeatures.containsAll(conversion.getEventFeatures()));
        assertFalse(conversion.getIsGlobalHoldback());
        assertEquals(conversion.getAnonymizeIP(), validProjectConfig.getAnonymizeIP());
        assertEquals(conversion.getClientEngine(), ClientEngine.JAVA_SDK.getClientEngineValue());
        assertEquals(conversion.getClientVersion(), BuildVersionInfo.VERSION);
    }

    /**
     * Verify that "revenue" and "value" are properly recorded in a conversion request as {@link EventMetric} objects.
     * "revenue" is fixed-point and "value" is floating-point.
     */
    @Test
    public void createConversionParamsWithEventMetrics() throws Exception {
        Long revenue = 1234L;
        Double value = 13.37;

        // use the "valid" project config and its associated experiment, variation, and attributes
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        EventType eventType = validProjectConfig.getEventTypes().get(0);

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);

        // Bucket to the first variation for all experiments.
        for (Experiment experiment : validProjectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, userId))
                    .thenReturn(experiment.getVariations().get(0));
        }
        DecisionService decisionService = new DecisionService(
                mockBucketAlgorithm,
                mock(ErrorHandler.class),
                validProjectConfig,
                mock(UserProfileService.class)
        );

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");
        Map<String, Object> eventTagMap = new HashMap<String, Object>();
        eventTagMap.put(ReservedEventKey.REVENUE.toString(), revenue);
        eventTagMap.put(ReservedEventKey.VALUE.toString(), value);
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                decisionService,
                eventType.getKey(),
                userId,
                attributeMap);
        LogEvent conversionEvent = builder.createConversionEvent(validProjectConfig, experimentVariationMap, userId,
                eventType.getId(), eventType.getKey(), attributeMap,
                eventTagMap);

        Conversion conversion = gson.fromJson(conversionEvent.getBody(), Conversion.class);
        List<EventMetric> eventMetrics = Arrays.asList(
                new EventMetric(EventMetric.REVENUE_METRIC_TYPE, new LazilyParsedNumber(revenue.toString())),
                new EventMetric(EventMetric.NUMERIC_METRIC_TYPE, new LazilyParsedNumber(value.toString())));
        // we're not going to verify everything, only the event metrics
        assertThat(conversion.getEventMetrics(), is(eventMetrics));
    }

    /**
     * Verify that precedence is given to forced variation bucketing over audience evaluation when constructing a
     * conversion event.
     */
    @Test
    public void createConversionEventForcedVariationBucketingPrecedesAudienceEval() {
        EventType eventType;
        String whitelistedUserId;
        if (datafileVersion == 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
            whitelistedUserId = MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED;
        }
        else {
            eventType = validProjectConfig.getEventTypes().get(0);
            whitelistedUserId = "testUser1";
        }

        DecisionService decisionService = new DecisionService(
                new Bucketer(validProjectConfig),
                new NoOpErrorHandler(),
                validProjectConfig,
                mock(UserProfileService.class)
        );

        // attributes are empty so user won't be in the audience for experiment using the event, but bucketing
        // will still take place
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                decisionService,
                eventType.getKey(),
                whitelistedUserId,
                Collections.<String, String>emptyMap());
        LogEvent conversionEvent = builder.createConversionEvent(
                validProjectConfig,
                experimentVariationMap,
                whitelistedUserId,
                eventType.getId(),
                eventType.getKey(),
                Collections.<String, String>emptyMap(),
                Collections.<String, Object>emptyMap());
        assertNotNull(conversionEvent);

        Conversion conversion = gson.fromJson(conversionEvent.getBody(), Conversion.class);
        if (datafileVersion == 4) {
            // 2 experiments use the event
            // basic experiment has no audience
            // user is whitelisted in to one audience
            assertEquals(2, conversion.getLayerStates().size());
        }
        else {
            assertEquals(1, conversion.getLayerStates().size());
        }
    }

    /**
     * Verify that precedence is given to experiment status over forced variation bucketing when constructing a
     * conversion event.
     */
    @Test
    public void createConversionEventExperimentStatusPrecedesForcedVariation() {
        EventType eventType;
        if (datafileVersion == 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_PAUSED_EXPERIMENT_KEY);
        }
        else {
            eventType = validProjectConfig.getEventTypes().get(3);
        }
        String whitelistedUserId = PAUSED_EXPERIMENT_FORCED_VARIATION_USER_ID_CONTROL;

        Bucketer bucketer = spy(new Bucketer(validProjectConfig));
        DecisionService decisionService = new DecisionService(
                bucketer,
                mock(ErrorHandler.class),
                validProjectConfig,
                mock(UserProfileService.class)
        );

        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                decisionService,
                eventType.getKey(),
                whitelistedUserId,
                Collections.<String, String>emptyMap());
        LogEvent conversionEvent = builder.createConversionEvent(
                validProjectConfig,
                experimentVariationMap,
                whitelistedUserId,
                eventType.getId(),
                eventType.getKey(),
                Collections.<String, String>emptyMap(),
                Collections.<String, Object>emptyMap());

        for (Experiment experiment : validProjectConfig.getExperiments()) {
            verify(bucketer, never()).bucket(experiment, whitelistedUserId);
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
        DecisionService decisionService = new DecisionService(
                mockBucketAlgorithm,
                mock(ErrorHandler.class),
                validProjectConfig,
                mock(UserProfileService.class)
        );

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                decisionService,
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

    /**
     * Verify that supplying an empty Experiment Variation map to
     * {@link EventBuilderV2#createConversionEvent(ProjectConfig, Map, String, String, String, Map, Map)}
     * returns a null {@link LogEvent}.
     */
    @Test
    public void createConversionEventReturnsNullWhenExperimentVariationMapIsEmpty() {
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        EventBuilderV2 builder = new EventBuilderV2();

        LogEvent conversionEvent = builder.createConversionEvent(
                validProjectConfig,
                Collections.<Experiment, Variation>emptyMap(),
                userId,
                eventType.getId(),
                eventType.getKey(),
                Collections.<String, String>emptyMap(),
                Collections.<String, String>emptyMap()
        );

        assertNull(conversionEvent);
    }

    /**
     * Verify {@link Impression} event creation
     */
    @Test
    public void createImpressionEventWithBucketingId() throws Exception {
        // use the "valid" project config and its associated experiment, variation, and attributes
        ProjectConfig projectConfig = validProjectConfigV2();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = projectConfig.getAttributes().get(0);
        String userId = "userId";
        Map<String, String> attributeMap = new HashMap<String, String>();
        attributeMap.put(attribute.getKey(), "value");

        attributeMap.put(com.optimizely.ab.bucketing.DecisionService.BUCKETING_ATTRIBUTE, "variation");

        Decision expectedDecision = new Decision(bucketedVariation.getId(), false, activatedExperiment.getId());
        Feature feature = new Feature(attribute.getId(), attribute.getKey(), Feature.CUSTOM_ATTRIBUTE_FEATURE_TYPE,
                "value", true);
        Feature feature1 = new Feature(com.optimizely.ab.bucketing.DecisionService.BUCKETING_ATTRIBUTE,
                com.optimizely.ab.event.internal.EventBuilderV2.ATTRIBUTE_KEY_FOR_BUCKETING_ATTRIBUTE,
                Feature.CUSTOM_ATTRIBUTE_FEATURE_TYPE,
                "variation", true);
        List<Feature> expectedUserFeatures = new java.util.ArrayList<Feature>();
        expectedUserFeatures.add(feature);
        expectedUserFeatures.add(feature1);

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
     * Verify {@link Conversion} event creation
     */
    @Test
    public void createConversionEventWithBucketingId() throws Exception {
        // use the "valid" project config and its associated experiment, variation, and attributes
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String userId = "userId";
        String bucketingId = "bucketingId";

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);

        List<Experiment> allExperiments = validProjectConfig.getExperiments();
        List<Experiment> experimentsForEventKey = validProjectConfig.getExperimentsForEventKey(eventType.getKey());

        // Bucket to the first variation for all experiments. However, only a subset of the experiments will actually
        // call the bucket function.
        for (Experiment experiment : allExperiments) {
            when(mockBucketAlgorithm.bucket(experiment, bucketingId))
                    .thenReturn(experiment.getVariations().get(0));
        }
        DecisionService decisionService = new DecisionService(
                mockBucketAlgorithm,
                mock(ErrorHandler.class),
                validProjectConfig,
                mock(UserProfileService.class)
        );

        Map<String, String> attributeMap = new java.util.HashMap<String, String>();
        attributeMap.put(attribute.getKey(), AUDIENCE_GRYFFINDOR_VALUE);
        attributeMap.put(com.optimizely.ab.bucketing.DecisionService.BUCKETING_ATTRIBUTE, bucketingId);

        Map<String, Object> eventTagMap = new HashMap<String, Object>();
        eventTagMap.put("boolean_param", false);
        eventTagMap.put("string_param", "123");
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                decisionService,
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
            if (experiment.isRunning()) {
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
        assertThat((double)conversion.getTimestamp(), closeTo((double)System.currentTimeMillis(), 120.0));
        assertThat(conversion.getProjectId(), is(validProjectConfig.getProjectId()));
        assertThat(conversion.getAccountId(), is(validProjectConfig.getAccountId()));

        Feature feature = new Feature(attribute.getId(), attribute.getKey(), Feature.CUSTOM_ATTRIBUTE_FEATURE_TYPE,
                AUDIENCE_GRYFFINDOR_VALUE, true);
        Feature feature1 = new Feature(com.optimizely.ab.bucketing.DecisionService.BUCKETING_ATTRIBUTE,
                com.optimizely.ab.event.internal.EventBuilderV2.ATTRIBUTE_KEY_FOR_BUCKETING_ATTRIBUTE,
                Feature.CUSTOM_ATTRIBUTE_FEATURE_TYPE,
                bucketingId, true);
        List<Feature> expectedUserFeatures = new ArrayList<Feature>();
        expectedUserFeatures.add(feature);
        expectedUserFeatures.add(feature1);

        // Event Features
        List<Feature> expectedEventFeatures = new ArrayList<Feature>();
        expectedEventFeatures.add(new Feature("", "boolean_param", Feature.EVENT_FEATURE_TYPE,
                false, false));
        expectedEventFeatures.add(new Feature("", "string_param", Feature.EVENT_FEATURE_TYPE,
                "123", false));

        assertEquals(conversion.getUserFeatures(), expectedUserFeatures);
        assertThat(conversion.getLayerStates(), containsInAnyOrder(expectedLayerStates.toArray()));
        assertEquals(conversion.getEventEntityId(), eventType.getId());
        assertEquals(conversion.getEventName(), eventType.getKey());
        assertEquals(conversion.getEventMetrics(), Collections.<EventMetric>emptyList());
        assertTrue(conversion.getEventFeatures().containsAll(expectedEventFeatures));
        assertTrue(expectedEventFeatures.containsAll(conversion.getEventFeatures()));
        assertFalse(conversion.getIsGlobalHoldback());
        assertEquals(conversion.getAnonymizeIP(), validProjectConfig.getAnonymizeIP());
        assertEquals(conversion.getClientEngine(), ClientEngine.JAVA_SDK.getClientEngineValue());
        assertEquals(conversion.getClientVersion(), BuildVersionInfo.VERSION);
    }


    //========== helper methods =========//
    public static Map<Experiment, Variation> createExperimentVariationMap(ProjectConfig projectConfig,
                                                                          DecisionService decisionService,
                                                                          String eventName,
                                                                          String userId,
                                                                          @Nullable Map<String, String> attributes) {

        List<Experiment> eventExperiments = projectConfig.getExperimentsForEventKey(eventName);
        Map<Experiment, Variation> experimentVariationMap = new HashMap<Experiment, Variation>(eventExperiments.size());
        for (Experiment experiment : eventExperiments) {
            if (experiment.isRunning()) {
                Variation variation = decisionService.getVariation(experiment, userId, attributes);
                if (variation != null) {
                    experimentVariationMap.put(experiment, variation);
                }
            }
        }

        return experimentVariationMap;
    }
}
