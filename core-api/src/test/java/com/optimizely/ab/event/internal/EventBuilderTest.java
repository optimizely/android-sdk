package com.optimizely.ab.event.internal;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import com.optimizely.ab.event.internal.payload.Decision;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.internal.ReservedEventKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV4;
import static com.optimizely.ab.config.ValidProjectConfigV4.AUDIENCE_GRYFFINDOR_VALUE;
import static com.optimizely.ab.config.ValidProjectConfigV4.EVENT_BASIC_EVENT_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.EVENT_PAUSED_EXPERIMENT_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED;
import static com.optimizely.ab.config.ValidProjectConfigV4.PAUSED_EXPERIMENT_FORCED_VARIATION_USER_ID_CONTROL;
import static junit.framework.Assert.assertNotNull;
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

@RunWith(Parameterized.class)
public class EventBuilderTest {

    @Parameterized.Parameters
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

    private Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    private EventBuilder builder = new EventBuilder();

    private static String userId = "userId";
    private int datafileVersion;
    private ProjectConfig validProjectConfig;

    public EventBuilderTest(int datafileVersion,
                            ProjectConfig validProjectConfig) {
        this.datafileVersion = datafileVersion;
        this.validProjectConfig = validProjectConfig;
    }

    /**
     * Verify {@link com.optimizely.ab.event.internal.payload.EventBatch} event creation
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
        Decision expectedDecision = new Decision(activatedExperiment.getLayerId(), activatedExperiment.getId(), bucketedVariation.getId(), false);
        com.optimizely.ab.event.internal.payload.Attribute feature = new com.optimizely.ab.event.internal.payload.Attribute(attribute.getId(),
                attribute.getKey(), com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                "value");
        List<com.optimizely.ab.event.internal.payload.Attribute> expectedUserFeatures = Collections.singletonList(feature);

        LogEvent impressionEvent = builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation,
                userId, attributeMap);

        // verify that request endpoint is correct
        assertThat(impressionEvent.getEndpointUrl(), is(EventBuilder.EVENT_ENDPOINT));

        EventBatch eventBatch = gson.fromJson(impressionEvent.getBody(), EventBatch.class);

        // verify payload information
        assertThat(eventBatch.getVisitors().get(0).getVisitorId(), is(userId));
        assertThat((double) eventBatch.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getTimestamp(), closeTo((double)System.currentTimeMillis(), 1000.0));
        assertFalse(eventBatch.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0).getIsCampaignHoldback());
        assertThat(eventBatch.getAnonymizeIp(), is(projectConfig.getAnonymizeIP()));
        assertThat(eventBatch.getProjectId(), is(projectConfig.getProjectId()));
        assertThat(eventBatch.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0), is(expectedDecision));
        assertThat(eventBatch.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0).getCampaignId(),
                is(activatedExperiment.getLayerId()));
        assertThat(eventBatch.getAccountId(), is(projectConfig.getAccountId()));
        assertThat(eventBatch.getVisitors().get(0).getAttributes(), is(expectedUserFeatures));
        assertThat(eventBatch.getClientName(), is(EventBatch.ClientEngine.JAVA_SDK.getClientEngineValue()));
        assertThat(eventBatch.getClientVersion(), is(BuildVersionInfo.VERSION));
        assertNull(eventBatch.getVisitors().get(0).getSessionId());
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

        EventBatch impression = gson.fromJson(impressionEvent.getBody(), EventBatch.class);

        // verify that no Feature is created for "unknownAtrribute" -> "blahValue"
        for (com.optimizely.ab.event.internal.payload.Attribute feature : impression.getVisitors().get(0).getAttributes()) {
            assertFalse(feature.getKey() == "unknownAttribute");
            assertFalse(feature.getValue() == "blahValue");
        }
    }

    /**
     * Verify that supplying {@link EventBuilder} with a custom client engine and client version results in impression
     * events being sent with the overriden values.
     */
    @Test
    public void createImpressionEventAndroidClientEngineClientVersion() throws Exception {
        EventBuilder builder = new EventBuilder(EventBatch.ClientEngine.ANDROID_SDK, "0.0.0");
        ProjectConfig projectConfig = validProjectConfigV2();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = projectConfig.getAttributes().get(0);
        String userId = "userId";
        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");

        LogEvent impressionEvent = builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation,
                userId, attributeMap);
        EventBatch impression = gson.fromJson(impressionEvent.getBody(), EventBatch.class);

        assertThat(impression.getClientName(), is(EventBatch.ClientEngine.ANDROID_SDK.getClientEngineValue()));
        assertThat(impression.getClientVersion(), is("0.0.0"));
    }

    /**
     * Verify that supplying {@link EventBuilder} with a custom Android TV client engine and client version
     * results in impression events being sent with the overriden values.
     */
    @Test
    public void createImpressionEventAndroidTVClientEngineClientVersion() throws Exception {
        String clientVersion = "0.0.0";
        EventBuilder builder = new EventBuilder(EventBatch.ClientEngine.ANDROID_TV_SDK, clientVersion);
        ProjectConfig projectConfig = validProjectConfigV2();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = projectConfig.getAttributes().get(0);
        String userId = "userId";
        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");

        LogEvent impressionEvent = builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation,
                userId, attributeMap);
        EventBatch impression = gson.fromJson(impressionEvent.getBody(), EventBatch.class);

        assertThat(impression.getClientName(), is(EventBatch.ClientEngine.ANDROID_TV_SDK.getClientEngineValue()));
        assertThat(impression.getClientVersion(), is(clientVersion));
    }

    /**
     * Verify {@link com.optimizely.ab.event.internal.payload.EventBatch} event creation
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

        List<Decision> expectedDecisions = new ArrayList<Decision>();

        for (Experiment experiment : experimentsForEventKey) {
            if (experiment.isRunning()) {
                Decision layerState = new Decision(experiment.getLayerId(), experiment.getId(),
                        experiment.getVariations().get(0).getId(), false);
                expectedDecisions.add(layerState);
            }
        }

        // verify that the request endpoint is correct
        assertThat(conversionEvent.getEndpointUrl(), is(EventBuilder.EVENT_ENDPOINT));

        EventBatch conversion = gson.fromJson(conversionEvent.getBody(), EventBatch.class);

        // verify payload information
        assertThat(conversion.getVisitors().get(0).getVisitorId(), is(userId));
        assertThat((double)conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getTimestamp(),
                closeTo((double)System.currentTimeMillis(), 120.0));
        assertThat(conversion.getProjectId(), is(validProjectConfig.getProjectId()));
        assertThat(conversion.getAccountId(), is(validProjectConfig.getAccountId()));

        com.optimizely.ab.event.internal.payload.Attribute feature = new com.optimizely.ab.event.internal.payload.Attribute(attribute.getId(), attribute.getKey(),
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                AUDIENCE_GRYFFINDOR_VALUE);
        List<com.optimizely.ab.event.internal.payload.Attribute> expectedUserFeatures = Collections.singletonList(feature);

        assertEquals(conversion.getVisitors().get(0).getAttributes(), expectedUserFeatures);
        assertThat(conversion.getVisitors().get(0).getSnapshots().get(0).getDecisions(), containsInAnyOrder(expectedDecisions.toArray()));
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getEntityId(), eventType.getId());
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getKey(), eventType.getKey());
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getRevenue(), null);
        assertTrue(conversion.getVisitors().get(0).getAttributes().containsAll(expectedUserFeatures));
        assertTrue(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getTags().equals(eventTagMap));
        assertFalse(conversion.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0).getIsCampaignHoldback());
        assertEquals(conversion.getAnonymizeIp(), validProjectConfig.getAnonymizeIP());
        assertEquals(conversion.getClientName(), EventBatch.ClientEngine.JAVA_SDK.getClientEngineValue());
        assertEquals(conversion.getClientVersion(), BuildVersionInfo.VERSION);
    }

    /**
     * Verify that "revenue" and "value" are properly recorded in a conversion request as {@link com.optimizely.ab.event.internal.payload.Event} objects.
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

        EventBatch conversion = gson.fromJson(conversionEvent.getBody(), EventBatch.class);
        // we're not going to verify everything, only the event metrics
        assertThat(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getRevenue().longValue(), is(revenue));
        assertThat(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getValue().doubleValue(), is(value));
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

        EventBatch conversion = gson.fromJson(conversionEvent.getBody(), EventBatch.class);
        if (datafileVersion == 4) {
            // 2 experiments use the event
            // basic experiment has no audience
            // user is whitelisted in to one audience
            assertEquals(2, conversion.getVisitors().get(0).getSnapshots().get(0).getDecisions().size());
        }
        else {
            assertEquals(1, conversion.getVisitors().get(0).getSnapshots().get(0).getDecisions().size());
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
     * Verify that supplying {@link EventBuilder} with a custom client engine and client version results in conversion
     * events being sent with the overriden values.
     */
    @Test
    public void createConversionEventAndroidClientEngineClientVersion() throws Exception {
        EventBuilder builder = new EventBuilder(EventBatch.ClientEngine.ANDROID_SDK, "0.0.0");
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

        EventBatch conversion = gson.fromJson(conversionEvent.getBody(), EventBatch.class);

        assertThat(conversion.getClientName(), is(EventBatch.ClientEngine.ANDROID_SDK.getClientEngineValue()));
        assertThat(conversion.getClientVersion(), is("0.0.0"));
    }

    /**
     * Verify that supplying {@link EventBuilder} with a Android TV client engine and client version results in
     * conversion events being sent with the overriden values.
     */
    @Test
    public void createConversionEventAndroidTVClientEngineClientVersion() throws Exception {
        String clientVersion = "0.0.0";
        EventBuilder builder = new EventBuilder(EventBatch.ClientEngine.ANDROID_TV_SDK, clientVersion);
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
        EventBatch conversion = gson.fromJson(conversionEvent.getBody(), EventBatch.class);

        assertThat(conversion.getClientName(), is(EventBatch.ClientEngine.ANDROID_TV_SDK.getClientEngineValue()));
        assertThat(conversion.getClientVersion(), is(clientVersion));
    }

    /**
     * Verify that supplying an empty Experiment Variation map to
     * {@link EventBuilder#createConversionEvent(ProjectConfig, Map, String, String, String, Map, Map)}
     * returns a null {@link LogEvent}.
     */
    @Test
    public void createConversionEventReturnsNullWhenExperimentVariationMapIsEmpty() {
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        EventBuilder builder = new EventBuilder();

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
     * Verify {@link com.optimizely.ab.event.internal.payload.EventBatch} event creation
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

        Decision expectedDecision = new Decision(activatedExperiment.getLayerId(), activatedExperiment.getId(), bucketedVariation.getId(), false);

        com.optimizely.ab.event.internal.payload.Attribute feature = new com.optimizely.ab.event.internal.payload.Attribute(attribute.getId(), attribute.getKey(),
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                "value");
        com.optimizely.ab.event.internal.payload.Attribute feature1 = new com.optimizely.ab.event.internal.payload.Attribute(com.optimizely.ab.bucketing.DecisionService.BUCKETING_ATTRIBUTE,
                EventBuilder.ATTRIBUTE_KEY_FOR_BUCKETING_ATTRIBUTE,
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                "variation");

        List<com.optimizely.ab.event.internal.payload.Attribute> expectedUserFeatures = Arrays.asList(feature, feature1);

        LogEvent impressionEvent = builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation,
                userId, attributeMap);

        // verify that request endpoint is correct
        assertThat(impressionEvent.getEndpointUrl(), is(EventBuilder.EVENT_ENDPOINT));

        EventBatch impression = gson.fromJson(impressionEvent.getBody(), EventBatch.class);

        // verify payload information
        assertThat(impression.getVisitors().get(0).getVisitorId(), is(userId));
        assertThat((double)impression.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getTimestamp(), closeTo((double)System.currentTimeMillis(), 1000.0));
        assertFalse(impression.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0).getIsCampaignHoldback());
        assertThat(impression.getAnonymizeIp(), is(projectConfig.getAnonymizeIP()));
        assertThat(impression.getProjectId(), is(projectConfig.getProjectId()));
        assertThat(impression.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0), is(expectedDecision));
        assertThat(impression.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0).getCampaignId(), is(activatedExperiment.getLayerId()));
        assertThat(impression.getAccountId(), is(projectConfig.getAccountId()));

        assertThat(impression.getVisitors().get(0).getAttributes(), is(expectedUserFeatures));
        assertThat(impression.getClientName(), is(EventBatch.ClientEngine.JAVA_SDK.getClientEngineValue()));
        assertThat(impression.getClientVersion(), is(BuildVersionInfo.VERSION));
        assertNull(impression.getVisitors().get(0).getSessionId());
    }

    /**
     * Verify {@link EventBatch} event creation
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

        List<Decision> expectedDecisions = new ArrayList<Decision>();

        for (Experiment experiment : experimentsForEventKey) {
            if (experiment.isRunning()) {
                Decision decision = new Decision(experiment.getLayerId(), experiment.getId(),
                        experiment.getVariations().get(0).getId(), false);
                expectedDecisions.add(decision);
            }
        }

        // verify that the request endpoint is correct
        assertThat(conversionEvent.getEndpointUrl(), is(EventBuilder.EVENT_ENDPOINT));

        EventBatch conversion = gson.fromJson(conversionEvent.getBody(), EventBatch.class);

        // verify payload information
        assertThat(conversion.getVisitors().get(0).getVisitorId(), is(userId));
        assertThat((double)conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getTimestamp(), closeTo((double)System.currentTimeMillis(), 1000.0));
        assertThat(conversion.getProjectId(), is(validProjectConfig.getProjectId()));
        assertThat(conversion.getAccountId(), is(validProjectConfig.getAccountId()));

        com.optimizely.ab.event.internal.payload.Attribute attribute1 = new com.optimizely.ab.event.internal.payload.Attribute(attribute.getId(), attribute.getKey(),
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                AUDIENCE_GRYFFINDOR_VALUE);
        com.optimizely.ab.event.internal.payload.Attribute attribute2 = new com.optimizely.ab.event.internal.payload.Attribute(com.optimizely.ab.bucketing.DecisionService.BUCKETING_ATTRIBUTE,
                EventBuilder.ATTRIBUTE_KEY_FOR_BUCKETING_ATTRIBUTE,
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                bucketingId);
        List<com.optimizely.ab.event.internal.payload.Attribute> expectedUserFeatures = Arrays.asList(attribute1, attribute2);

        assertEquals(conversion.getVisitors().get(0).getAttributes(), expectedUserFeatures);
        assertThat(conversion.getVisitors().get(0).getSnapshots().get(0).getDecisions(), containsInAnyOrder(expectedDecisions.toArray()));
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getEntityId(), eventType.getId());
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getType(), eventType.getKey());
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getKey(), eventType.getKey());
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getRevenue(), null);
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getQuantity(), null);
        assertTrue(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getTags().equals(eventTagMap));
        assertFalse(conversion.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0).getIsCampaignHoldback());
        assertEquals(conversion.getAnonymizeIp(), validProjectConfig.getAnonymizeIP());
        assertEquals(conversion.getClientName(), EventBatch.ClientEngine.JAVA_SDK.getClientEngineValue());
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

