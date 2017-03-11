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
package com.optimizely.ab;

import ch.qos.logback.classic.Level;
import com.google.common.collect.ImmutableMap;

import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.LiveVariableUsageInstance;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.error.NoOpErrorHandler;
import com.optimizely.ab.error.RaiseExceptionErrorHandler;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.EventBuilder;
import com.optimizely.ab.event.internal.EventBuilderV2;
import com.optimizely.ab.internal.LogbackVerifier;
import com.optimizely.ab.internal.ProjectValidationUtils;
import com.optimizely.ab.internal.ReservedEventKey;
import com.optimizely.ab.notification.NotificationListener;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigJsonV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV3;
import static com.optimizely.ab.event.LogEvent.RequestMethod;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the top-level {@link Optimizely} class.
 */
public class OptimizelyTestV3 {

    @Rule
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    @Mock EventHandler mockEventHandler;
    @Mock Bucketer mockBucketer;
    @Mock ErrorHandler mockErrorHandler;

    private static final String genericUserId = "genericUserId";
    private static String validDatafile;
    private static String noAudienceDatafile;
    private static ProjectConfig validProjectConfig;
    private static ProjectConfig noAudienceProjectConfig;

    @BeforeClass
    public static void setUp() throws Exception {
        validDatafile = validConfigJsonV3();
        noAudienceDatafile = noAudienceProjectConfigJsonV3();
        validProjectConfig = validProjectConfigV3();
        noAudienceProjectConfig = noAudienceProjectConfigV3();
    }

    //======== activate tests ========//

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} call correctly builds an endpoint url and
     * request params and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateEndToEnd() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventBuilder)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, "userId",
                                                    testUserAttributes))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, "userId"))
                .thenReturn(bucketedVariation);

        logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"etag1\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching impression event to URL test_url with params " +
                testParams + " and payload \"\"");

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), "userId", testUserAttributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, "userId");
        assertThat(actualVariation, is(bucketedVariation));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that bucketer initialization happens after building the Optimizely object
     * @throws Exception
     */
    @Test
    public void initializationOccursForBucketerWhenBuildingOptly() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventBuilder)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        verify(mockBucketer).cleanUserProfiles();
    }

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} DOES NOT dispatch an impression event
     * when the user isn't bucketed to a variation.
     */
    @Test
    public void activateForNullVariation() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withConfig(validProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");

        when(mockBucketer.bucket(activatedExperiment, "userId"))
                .thenReturn(null);

        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"etag1\".");

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), "userId", testUserAttributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, "userId");
        assertNull(actualVariation);

        // verify that dispatchEvent was NOT called
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify the case were {@link Optimizely#activate(Experiment, String)} is called with an {@link Experiment}
     * that is not present in the current {@link ProjectConfig}. We should NOT throw an error in that case.
     *
     * This may happen if an experiment is retrieved from the project config, the project config is updated and the
     * referenced experiment removed, then activate is called given the now removed experiment.
     * Could also happen if an experiment was manually created and passed through.
     */
    @Test
    public void activateWhenExperimentIsNotInProject() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment unknownExperiment = createUnknownExperiment();
        Variation bucketedVariation = unknownExperiment.getVariations().get(0);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withConfig(validProjectConfig)
                .withErrorHandler(new RaiseExceptionErrorHandler())
                .build();

        when(mockBucketer.bucket(unknownExperiment, "userId"))
                .thenReturn(bucketedVariation);

        optimizely.activate(unknownExperiment, "userId");
    }

    /**
     * Verify that the {@link Optimizely#activate(String, String)} call correctly builds an endpoint url and
     * request params and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateWithExperimentKey() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(validProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
                                                    eq("userId"), eq(testUserAttributes)))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, "userId"))
                .thenReturn(bucketedVariation);

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), "userId", testUserAttributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, "userId");
        assertThat(actualVariation, is(bucketedVariation));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} handles the case where an unknown experiment
     * (i.e., not in the config) is passed through and a {@link NoOpErrorHandler} is used by default.
     */
    @Test
    public void activateWithUnknownExperimentKeyAndNoOpErrorHandler() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment unknownExperiment = createUnknownExperiment();

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build();

        logbackVerifier.expectMessage(Level.ERROR, "Experiment \"unknown_experiment\" is not in the datafile.");
        logbackVerifier.expectMessage(Level.INFO,
                "Not activating user \"userId\" for experiment \"unknown_experiment\".");

        // since we use a NoOpErrorHandler, we should fail and return null
        Variation actualVariation = optimizely.activate(unknownExperiment.getKey(), "userId");

        // verify that null is returned, as no project config was available
        assertNull(actualVariation);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} handles the case where an unknown experiment
     * (i.e., not in the config) is passed through and a {@link RaiseExceptionErrorHandler} is provided.
     */
    @Test
    public void activateWithUnknownExperimentKeyAndRaiseExceptionErrorHandler() throws Exception {
        thrown.expect(UnknownExperimentException.class);

        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment unknownExperiment = createUnknownExperiment();

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        // since we use a RaiseExceptionErrorHandler, we should throw an error
        optimizely.activate(unknownExperiment.getKey(), "userId");
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map)} passes through attributes.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void activateWithAttributes() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = validProjectConfig.getAttributes().get(0);

        // setup a mock event builder to return expected impression params
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(validProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
                                                    eq("userId"), anyMapOf(String.class, String.class)))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, "userId"))
                .thenReturn(bucketedVariation);

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), "userId",
                ImmutableMap.of(attribute.getKey(), "attributeValue"));

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, "userId");
        assertThat(actualVariation, is(bucketedVariation));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockEventBuilder).createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment),
                                                       eq(bucketedVariation), eq("userId"), attributeCaptor.capture());

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, hasEntry(attribute.getKey(), "attributeValue"));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} handles the case where an unknown attribute
     * (i.e., not in the config) is passed through.
     *
     * In this case, the activate call should remove the unknown attribute from the given map.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void activateWithUnknownAttribute() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        // setup a mock event builder to return mock params and endpoint
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(validProjectConfig)
                .withErrorHandler(new RaiseExceptionErrorHandler())
                .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");
        testUserAttributes.put("unknownAttribute", "dimValue");

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
                                                    eq("userId"), anyMapOf(String.class, String.class)))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, "userId"))
                .thenReturn(bucketedVariation);

        logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"etag1\".");
        logbackVerifier.expectMessage(Level.WARN, "Attribute(s) [unknownAttribute] not in the datafile.");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching impression event to URL test_url with params " +
                testParams + " and payload \"\"");

        // Use an immutable map to also check that we're not attempting to change the provided attribute map
        Variation actualVariation =
                optimizely.activate(activatedExperiment.getKey(), "userId", testUserAttributes);

        assertThat(actualVariation, is(bucketedVariation));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment),
                                                       eq(bucketedVariation), eq("userId"), attributeCaptor.capture());

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, not(hasKey("unknownAttribute")));

        // verify that dispatchEvent was called with the correct LogEvent object.
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map)} ignores null attributes.
     */
    @Test
    @SuppressFBWarnings(
            value="NP_NONNULL_PARAM_VIOLATION",
            justification="testing nullness contract violation")
    public void activateWithNullAttributes() throws Exception {
        String datafile = noAudienceProjectConfigJsonV3();
        ProjectConfig projectConfig = noAudienceProjectConfigV3();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        // setup a mock event builder to return expected impression params
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(projectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(eq(projectConfig), eq(activatedExperiment), eq(bucketedVariation),
                eq("userId"), eq(Collections.<String, String>emptyMap())))
                .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, "userId"))
                .thenReturn(bucketedVariation);

        // activate the experiment
        Map<String, String> attributes = null;
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), "userId", attributes);

        logbackVerifier.expectMessage(Level.WARN, "Attributes is null when non-null was expected. Defaulting to an empty attributes map.");

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, "userId");
        assertThat(actualVariation, is(bucketedVariation));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockEventBuilder).createImpressionEvent(eq(projectConfig), eq(activatedExperiment),
                eq(bucketedVariation), eq("userId"), attributeCaptor.capture());

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, is(Collections.<String, String>emptyMap()));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map)} gracefully handles null attribute values.
     */
    @Test
    public void activateWithNullAttributeValues() throws Exception {
        String datafile = validConfigJsonV3();
        ProjectConfig projectConfig = validProjectConfigV3();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = projectConfig.getAttributes().get(0);

        // setup a mock event builder to return expected impression params
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(projectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(eq(projectConfig), eq(activatedExperiment), eq(bucketedVariation),
                eq("userId"), anyMapOf(String.class, String.class)))
                .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, "userId"))
                .thenReturn(bucketedVariation);

        // activate the experiment
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(attribute.getKey(), null);
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), "userId", attributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, "userId");
        assertThat(actualVariation, is(bucketedVariation));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockEventBuilder).createImpressionEvent(eq(projectConfig), eq(activatedExperiment),
                eq(bucketedVariation), eq("userId"), attributeCaptor.capture());

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, hasEntry(attribute.getKey(), null));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} returns null when the experiment id corresponds to a
     * non-running experiment.
     */
    @Test
    public void activateDraftExperiment() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment draftExperiment = validProjectConfig.getExperiments().get(1);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        logbackVerifier.expectMessage(Level.INFO, "Experiment \"etag2\" is not running.");
        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"etag2\".");

        Variation variation = optimizely.activate(draftExperiment.getKey(), "userId");

        // verify that null is returned, as the experiment isn't running
        assertNull(variation);
    }

    /**
     * Verify that a user who falls in an experiment's audience is assigned a variation.
     */
    @Test
    public void activateUserInAudience() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment experimentToCheck = validProjectConfig.getExperiments().get(0);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");

        Variation actualVariation = optimizely.activate(experimentToCheck.getKey(), "userId", testUserAttributes);
        assertNotNull(actualVariation);
    }

    /**
     * Verify that a user not in any of an experiment's audiences isn't assigned to a variation.
     */
    @Test
    public void activateUserNotInAudience() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment experimentToCheck = validProjectConfig.getExperiments().get(0);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "firefox");

        logbackVerifier.expectMessage(Level.INFO,
                                      "User \"userId\" does not meet conditions to be in experiment \"etag1\".");
        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"etag1\".");

        Variation actualVariation = optimizely.activate(experimentToCheck.getKey(), "userId", testUserAttributes);
        assertNull(actualVariation);
    }

    /**
     * Verify that when no audiences are provided, the user is included in the experiment (i.e., no audiences means
     * the experiment is targeted to "everyone").
     */
    @Test
    public void activateUserWithNoAudiences() throws Exception {
        Experiment experimentToCheck = noAudienceProjectConfig.getExperiments().get(0);

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withErrorHandler(mockErrorHandler)
            .build();

        assertNotNull(optimizely.activate(experimentToCheck.getKey(), "userId"));
    }

    /**
     * Verify that when an experiment has audiences, but no attributes are provided, the user is not assigned a
     * variation.
     */
    @Test
    public void activateUserNoAttributesWithAudiences() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment experiment = validProjectConfig.getExperiments().get(0);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .build();

        logbackVerifier.expectMessage(Level.INFO,
                "User \"userId\" does not meet conditions to be in experiment \"etag1\".");
        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"etag1\".");

        assertNull(optimizely.activate(experiment.getKey(), "userId"));
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} doesn't return a variation when provided an empty string.
     */
    @Test
    public void activateWithEmptyUserId() throws Exception {
        Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        String experimentKey = experiment.getKey();

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withConfig(noAudienceProjectConfig)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        logbackVerifier.expectMessage(Level.ERROR, "Non-empty user ID required");
        logbackVerifier.expectMessage(Level.INFO, "Not activating user for experiment \"" + experimentKey + "\".");
        assertNull(optimizely.activate(experimentKey, ""));
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map)} returns a variation when given matching
     * user attributes.
     */
    @Test
    public void activateForGroupExperimentWithMatchingAttributes() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment experiment = validProjectConfig.getGroups()
            .get(0)
            .getExperiments()
            .get(0);
        Variation variation = experiment.getVariations().get(0);

        when(mockBucketer.bucket(experiment, "user")).thenReturn(variation);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withBucketing(mockBucketer)
            .build();

        assertThat(optimizely.activate(experiment.getKey(), "user", Collections.singletonMap("browser_type", "chrome")),
                is(variation));
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map)} doesn't return a variation when given
     * non-matching user attributes.
     */
    @Test
    public void activateForGroupExperimentWithNonMatchingAttributes() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment experiment = validProjectConfig.getGroups()
            .get(0)
            .getExperiments()
            .get(0);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        String experimentKey = experiment.getKey();
        logbackVerifier.expectMessage(
                Level.INFO,
                "User \"user\" does not meet conditions to be in experiment \"" + experimentKey + "\".");
        logbackVerifier.expectMessage(Level.INFO,
                "Not activating user \"user\" for experiment \"" + experimentKey + "\".");
        assertNull(optimizely.activate(experiment.getKey(), "user",
                Collections.singletonMap("browser_type", "firefox")));
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map)} gives precedence to forced variation bucketing
     * over audience evaluation.
     */
    @Test
    public void activateForcedVariationPrecedesAudienceEval() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation expectedVariation = experiment.getVariations().get(0);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        logbackVerifier.expectMessage(Level.INFO, "User \"testUser1\" is forced in variation \"vtag1\".");
        // no attributes provided for a experiment that has an audience
        assertThat(optimizely.activate(experiment.getKey(), "testUser1"), is(expectedVariation));
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} gives precedence to experiment status over forced
     * variation bucketing.
     */
    @Test
    public void activateExperimentStatusPrecedesForcedVariation() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment experiment = validProjectConfig.getExperiments().get(1);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        logbackVerifier.expectMessage(Level.INFO, "Experiment \"etag2\" is not running.");
        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"testUser3\" for experiment \"etag2\".");
        // testUser3 has a corresponding forced variation, but experiment status should be checked first
        assertNull(optimizely.activate(experiment.getKey(), "testUser3"));
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} handles exceptions thrown by
     * {@link EventHandler#dispatchEvent(LogEvent)} gracefully.
     */
    @Test
    public void activateDispatchEventThrowsException() throws Exception {
        Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);

        doThrow(new Exception("Test Exception")).when(mockEventHandler).dispatchEvent(any(LogEvent.class));

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withConfig(noAudienceProjectConfig)
            .build();

        logbackVerifier.expectMessage(Level.ERROR, "Unexpected exception in event dispatcher");
        optimizely.activate(experiment.getKey(), "userId");
    }

    //======== track tests ========//

    /**
     * Verify that the {@link Optimizely#track(String, String)} call correctly builds a V2 event and passes it
     * through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void trackEventEndToEnd() throws Exception {
        List<Experiment> allExperiments = noAudienceProjectConfig.getExperiments();
        EventType eventType = noAudienceProjectConfig.getEventTypes().get(0);

        EventBuilder eventBuilderV2 = new EventBuilderV2();

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(eventBuilderV2)
            .withConfig(noAudienceProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        List<String> experimentIds = noAudienceProjectConfig.getExperimentIdsForGoal(eventType.getKey());

        // Bucket to the first variation for all experiments. However, only a subset of the experiments will actually
        // call the bucket function.
        for (Experiment experiment : allExperiments) {
            when(mockBucketer.bucket(experiment, "userId"))
                    .thenReturn(experiment.getVariations().get(0));
        }

        Map<String, String> emptyAttributes = Collections.emptyMap();

        // call track
        optimizely.track(eventType.getKey(), "userId");

        // verify that the bucketing algorithm was called only on experiments corresponding to the specified goal.
        for (Experiment experiment : allExperiments) {
            if (ProjectValidationUtils.validatePreconditions(noAudienceProjectConfig, experiment, "userId", emptyAttributes) &&
                    experimentIds.contains(experiment.getId())) {
                verify(mockBucketer).bucket(experiment, "userId");
            } else {
                verify(mockBucketer, never()).bucket(experiment, "userId");
            }
        }

        // verify that dispatchEvent was called
        verify(mockEventHandler).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} handles the case where an unknown event type
     * (i.e., not in the config) is passed through and a {@link NoOpErrorHandler} is used by default.
     */
    @Test
    public void trackEventWithUnknownEventKeyAndNoOpErrorHandler() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        EventType unknownEventType = createUnknownEventType();

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withErrorHandler(new NoOpErrorHandler())
            .build();

        logbackVerifier.expectMessage(Level.ERROR, "Event \"unknown_event_type\" is not in the datafile.");
        logbackVerifier.expectMessage(Level.INFO, "Not tracking event \"unknown_event_type\" for user \"userId\".");
        optimizely.track(unknownEventType.getKey(), "userId");

        // verify that we did NOT dispatch an event
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} handles the case where an unknown event type
     * (i.e., not in the config) is passed through and a {@link RaiseExceptionErrorHandler} is provided.
     */
    @Test
    public void trackEventWithUnknownEventKeyAndRaiseExceptionErrorHandler() throws Exception {
        thrown.expect(UnknownEventTypeException.class);

        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        EventType unknownEventType = createUnknownEventType();

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        // since we use a RaiseExceptionErrorHandler, we should throw an error
        optimizely.track(unknownEventType.getKey(), "userId");
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} passes through attributes.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void trackEventWithAttributes() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        EventType eventType = validProjectConfig.getEventTypes().get(0);

        // setup a mock event builder to return expected conversion params
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(validProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createConversionEvent(eq(validProjectConfig), eq(mockBucketer), eq("userId"),
                                                    eq(eventType.getId()), eq(eventType.getKey()),
                                                    anyMapOf(String.class, String.class), anyMapOf(String.class, Object.class)))
            .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"clicked_cart\" for user \"userId\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
                testParams + " and payload \"\"");

        // call track
        optimizely.track(eventType.getKey(), "userId", ImmutableMap.of(attribute.getKey(), "attributeValue"));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(eq(validProjectConfig), eq(mockBucketer), eq("userId"),
                                                       eq(eventType.getId()), eq(eventType.getKey()),
                                                       attributeCaptor.capture(), anyMapOf(String.class, Object.class));

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, hasEntry(attribute.getKey(), "attributeValue"));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} ignores null attributes.
     */
    @Test
    @SuppressFBWarnings(
            value="NP_NONNULL_PARAM_VIOLATION",
            justification="testing nullness contract violation")
    public void trackEventWithNullAttributes() throws Exception {
        String datafile = noAudienceProjectConfigJsonV3();
        ProjectConfig projectConfig = noAudienceProjectConfigV3();
        EventType eventType = projectConfig.getEventTypes().get(0);

        // setup a mock event builder to return expected conversion params
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(projectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createConversionEvent(eq(projectConfig), eq(mockBucketer), eq("userId"),
                eq(eventType.getId()), eq(eventType.getKey()),
                eq(Collections.<String, String>emptyMap()), eq(Collections.<String, Object>emptyMap())))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"clicked_cart\" for user \"userId\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
                testParams + " and payload \"\"");

        // call track
        Map<String, String> attributes = null;
        optimizely.track(eventType.getKey(), "userId", attributes);

        logbackVerifier.expectMessage(Level.WARN, "Attributes is null when non-null was expected. Defaulting to an empty attributes map.");

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(eq(projectConfig), eq(mockBucketer), eq("userId"),
                eq(eventType.getId()), eq(eventType.getKey()),
                attributeCaptor.capture(), eq(Collections.<String, Object>emptyMap()));

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, is(Collections.<String, String>emptyMap()));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} gracefully handles null attribute values.
     */
    @Test
    public void trackEventWithNullAttributeValues() throws Exception {
        String datafile = validConfigJsonV3();
        ProjectConfig projectConfig = validProjectConfigV3();
        EventType eventType = projectConfig.getEventTypes().get(0);

        // setup a mock event builder to return expected conversion params
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(projectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createConversionEvent(eq(projectConfig), eq(mockBucketer), eq("userId"),
                eq(eventType.getId()), eq(eventType.getKey()),
                anyMapOf(String.class, String.class), anyMapOf(String.class, Object.class)))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"clicked_cart\" for user \"userId\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
                testParams + " and payload \"\"");

        // call track
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("test", null);
        optimizely.track(eventType.getKey(), "userId", attributes);

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(eq(projectConfig), eq(mockBucketer), eq("userId"),
                eq(eventType.getId()), eq(eventType.getKey()),
                attributeCaptor.capture(), eq(Collections.<String, Object>emptyMap()));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} handles the case where an unknown attribute
     * (i.e., not in the config) is passed through.
     *
     * In this case, the track event call should remove the unknown attribute from the given map.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void trackEventWithUnknownAttribute() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        EventType eventType = validProjectConfig.getEventTypes().get(0);

        // setup a mock event builder to return expected conversion params
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(validProjectConfig)
                .withErrorHandler(new RaiseExceptionErrorHandler())
                .build();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createConversionEvent(eq(validProjectConfig), eq(mockBucketer), eq("userId"),
                                                    eq(eventType.getId()), eq(eventType.getKey()),
                                                    anyMapOf(String.class, String.class), anyMapOf(String.class, Object.class)))
            .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"clicked_cart\" for user \"userId\".");
        logbackVerifier.expectMessage(Level.WARN, "Attribute(s) [unknownAttribute] not in the datafile.");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
                testParams + " and payload \"\"");

        // call track
        optimizely.track(eventType.getKey(), "userId", ImmutableMap.of("unknownAttribute", "attributeValue"));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(eq(validProjectConfig), eq(mockBucketer), eq("userId"),
                                                       eq(eventType.getId()), eq(eventType.getKey()),
                                                       attributeCaptor.capture(), anyMapOf(String.class, Object.class));

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, not(hasKey("unknownAttribute")));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map, Map)} passes event features to
     * @{link EventBuilder#createConversionEvent(ProjectConfig, Bucketer, String, String, String, Map, Map)}
     */
    @Test
    public void trackEventWithEventTags() throws Exception {
        String datafile = validConfigJsonV3();
        ProjectConfig projectConfig = validProjectConfigV3();
        EventType eventType = projectConfig.getEventTypes().get(0);

        // setup a mock event builder to return expected conversion params
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(projectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");

        Map<String, Object> eventTags = new HashMap<String, Object>();
        eventTags.put("int_param", 123);
        eventTags.put("string_param", "123");
        eventTags.put("boolean_param", false);
        eventTags.put("float_param", 12.3f);

        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createConversionEvent(eq(projectConfig), eq(mockBucketer), eq("userId"),
                eq(eventType.getId()), eq(eventType.getKey()),
                anyMapOf(String.class, String.class), eq(eventTags)))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"clicked_cart\" for user \"userId\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
                testParams + " and payload \"\"");

        // call track
        optimizely.track(eventType.getKey(), "userId", Collections.<String, String>emptyMap(), eventTags);

        // setup the event map captor (so we can verify its content)
        ArgumentCaptor<Map> eventTagCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(eq(projectConfig), eq(mockBucketer), eq("userId"),
                eq(eventType.getId()), eq(eventType.getKey()),
                eq(Collections.<String, String>emptyMap()), eventTagCaptor.capture());

        Map<String, ?> actualValue = eventTagCaptor.getValue();
        assertThat(actualValue, hasEntry("int_param", eventTags.get("int_param")));
        assertThat(actualValue, hasEntry("string_param", eventTags.get("string_param")));
        assertThat(actualValue, hasEntry("boolean_param", eventTags.get("boolean_param")));
        assertThat(actualValue, hasEntry("float_param", eventTags.get("float_param")));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map, Map)} called with null event tags will default to
     * an empty map when calling @{link EventBuilder#createConversionEvent(ProjectConfig, Bucketer, String, String, String, Map, Map)}
     */
    @Test
    @SuppressFBWarnings(
            value="NP_NONNULL_PARAM_VIOLATION",
            justification="testing nullness contract violation")
    public void trackEventWithNullEventTags() throws Exception {
        String datafile = validConfigJsonV3();
        ProjectConfig projectConfig = validProjectConfigV3();
        EventType eventType = projectConfig.getEventTypes().get(0);

        // setup a mock event builder to return expected conversion params
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(projectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createConversionEvent(eq(projectConfig), eq(mockBucketer), eq("userId"),
                eq(eventType.getId()), eq(eventType.getKey()),
                eq(Collections.<String, String>emptyMap()), eq(Collections.<String, String>emptyMap())))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"clicked_cart\" for user \"userId\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
                testParams + " and payload \"\"");

        // call track
        optimizely.track(eventType.getKey(), "userId", Collections.<String, String>emptyMap(), null);

        // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(eq(projectConfig), eq(mockBucketer), eq("userId"),
                eq(eventType.getId()), eq(eventType.getKey()),
                eq(Collections.<String, String>emptyMap()), eq(Collections.<String, String>emptyMap()));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} passes through revenue.
     */
    @Test
    public void trackEventWithRevenue() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        long revenue = 1234L;

        // setup a mock event builder to return expected conversion params
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(validProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        Map<String, Object> eventTags= new HashMap<String, Object>();
        eventTags.put(ReservedEventKey.REVENUE.toString(), revenue);
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createConversionEvent(eq(validProjectConfig), eq(mockBucketer), eq("userId"),
                                                    eq(eventType.getId()), eq(eventType.getKey()),
                                                    eq(Collections.<String, String>emptyMap()), eq(eventTags)))
            .thenReturn(logEventToDispatch);

        // call track
        optimizely.track(eventType.getKey(), "userId", revenue);

        // setup the event tag map captor (so we can verify its content)
        ArgumentCaptor<Map> eventTagCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected revenue
        verify(mockEventBuilder).createConversionEvent(eq(validProjectConfig), eq(mockBucketer), eq("userId"),
                                                       eq(eventType.getId()), eq(eventType.getKey()),
                                                       eq(Collections.<String, String>emptyMap()),
                                                       eventTagCaptor.capture());

        Long actualValue = (Long)eventTagCaptor.getValue().get(ReservedEventKey.REVENUE.toString());
        assertThat(actualValue, is(revenue));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map)} doesn't dispatch an event when no valid experiments
     * correspond to an event.
     */
    @Test
    public void trackEventWithNoValidExperiments() throws Exception {
        String validDatafile = validConfigJsonV3();

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler).build();

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("browser_type", "firefox");

        logbackVerifier.expectMessage(Level.INFO,
                                      "There are no valid experiments for event \"clicked_purchase\" to track.");
        logbackVerifier.expectMessage(Level.INFO, "Not tracking event \"clicked_purchase\" for user \"userId\".");
        optimizely.track("clicked_purchase", "userId", attributes);

        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} handles exceptions thrown by
     * {@link EventHandler#dispatchEvent(LogEvent)} gracefully.
     */
    @Test
    public void trackDispatchEventThrowsException() throws Exception {
        EventType eventType = noAudienceProjectConfig.getEventTypes().get(0);

        doThrow(new Exception("Test Exception")).when(mockEventHandler).dispatchEvent(any(LogEvent.class));

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withConfig(noAudienceProjectConfig)
            .build();

        logbackVerifier.expectMessage(Level.ERROR, "Unexpected exception in event dispatcher");
        optimizely.track(eventType.getKey(), "userId");
    }

    //======== live variable getters tests ========//

    /**
     * Verify that {@link Optimizely#getVariableString(String, String, boolean)} returns null and logs properly when
     * an invalid live variable key is provided and the {@link NoOpErrorHandler} is used.
     */
    @Test
    public void getVariableInvalidVariableKeyNoOpErrorHandler() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        logbackVerifier.expectMessage(Level.ERROR, "Live variable \"invalid_key\" is not in the datafile.");
        assertNull(optimizely.getVariableString("invalid_key", "userId", false));
    }

    /**
     * Verify that {@link Optimizely#getVariableString(String, String, boolean)} returns throws an
     * {@link UnknownLiveVariableException} when an invalid live variable key is provided and the
     * {@link RaiseExceptionErrorHandler} is used.
     */
    @Test
    public void getVariableInvalidVariableKeyRaiseExceptionErrorHandler() throws Exception {
        thrown.expect(UnknownLiveVariableException.class);

        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        optimizely.getVariableString("invalid_key", "userId", false);
    }

    /**
     * Verify that {@link Optimizely#getVariableString(String, String, Map, boolean)} returns a string live variable
     * value when an proper variable key is provided and dispatches an impression when activateExperiment is true.
     */
    @Test
    public void getVariableStringActivateExperimentTrue() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();

        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        when(mockBucketer.bucket(activatedExperiment, "userId"))
            .thenReturn(bucketedVariation);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withBucketing(mockBucketer)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        assertThat(optimizely.getVariableString("string_variable", "userId",
                                                Collections.singletonMap("browser_type", "chrome"), true),
                   is("string_var_vtag1"));
        verify(mockEventHandler).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that {@link Optimizely#getVariableString(String, String, Map, boolean)} returns a string live variable
     * value when an proper variable key is provided and doesn't dispatch an impression when activateExperiment is
     * false.
     */
    @Test
    public void getVariableStringActivateExperimentFalse() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();

        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        when(mockBucketer.bucket(activatedExperiment, "userId"))
            .thenReturn(bucketedVariation);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withBucketing(mockBucketer)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        assertThat(optimizely.getVariableString("string_variable", "userId",
                                                Collections.singletonMap("browser_type", "chrome"), false),
                   is("string_var_vtag1"));
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that {@link Optimizely#getVariableString(String, String, boolean)} returns the default value of
     * a live variable when no experiments are using the live variable.
     */
    @Test
    public void getVariableStringReturnsDefaultValueNoExperimentsUsingLiveVariable() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        logbackVerifier.expectMessage(Level.WARN, "No experiment is using variable \"unused_string_variable\".");
        assertThat(optimizely.getVariableString("unused_string_variable", "userId", true), is("unused_variable"));
    }

    /**
     * Verify that {@link Optimizely#getVariableString(String, String, Map, boolean)} returns the default value when
     * a user isn't bucketed into a variation in the experiment.
     */
    @Test
    public void getVariableStringReturnsDefaultValueUserNotInVariation() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();

        // user isn't bucketed into a variation in any experiment
        when(mockBucketer.bucket(any(Experiment.class), any(String.class)))
            .thenReturn(null);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withBucketing(mockBucketer)
            .build();

        assertThat(optimizely.getVariableString("string_variable", "userId",
                                                Collections.singletonMap("browser_type", "chrome"), true),
                   is("string_live_variable"));
    }

    /**
     * Verify that {@link Optimizely#getVariableBoolean(String, String, Map, boolean)} returns a boolean live variable
     * value when an proper variable key is provided and dispatches an impression when activateExperiment is true.
     */
    @Test
    public void getVariableBoolean() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();

        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        when(mockBucketer.bucket(activatedExperiment, "userId"))
            .thenReturn(bucketedVariation);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withBucketing(mockBucketer)
            .build();

        assertTrue(optimizely.getVariableBoolean("etag1_variable", "userId",
                                                 Collections.singletonMap("browser_type", "chrome"), true));
    }

    /**
     * Verify that {@link Optimizely#getVariableDouble(String, String, Map, boolean)} returns a double live variable
     * value when an proper variable key is provided and dispatches an impression when activateExperiment is true.
     */
    @Test
    public void getVariableDouble() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();

        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        when(mockBucketer.bucket(activatedExperiment, "userId"))
            .thenReturn(bucketedVariation);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withBucketing(mockBucketer)
            .build();

        assertThat(optimizely.getVariableDouble("double_variable", "userId",
                                               Collections.singletonMap("browser_type", "chrome"), true),
                   is(5.3));
        verify(mockEventHandler).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that {@link Optimizely#getVariableInteger(String, String, Map, boolean)} returns a integer live variable
     * value when an proper variable key is provided and dispatches an impression when activateExperiment is true.
     */
    @Test
    public void getVariableInteger() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();

        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        when(mockBucketer.bucket(activatedExperiment, "userId"))
            .thenReturn(bucketedVariation);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withBucketing(mockBucketer)
            .build();

        assertThat(optimizely.getVariableInteger("integer_variable", "userId",
                                                 Collections.singletonMap("browser_type", "chrome"), true),
                   is(10));
        verify(mockEventHandler).dispatchEvent(any(LogEvent.class));
    }

    //======== getVariation tests ========//

    /**
     * Verify that {@link Optimizely#getVariation(Experiment, String)} correctly makes the
     * {@link Bucketer#bucket(Experiment, String)} call and does NOT dispatch an event.
     */
    @Test
    public void getVariation() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        when(mockBucketer.bucket(activatedExperiment, "userId")).thenReturn(bucketedVariation);

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");

        // activate the experiment
        Variation actualVariation = optimizely.getVariation(activatedExperiment.getKey(), "userId",
                testUserAttributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, "userId");
        assertThat(actualVariation, is(bucketedVariation));

        // verify that we didn't attempt to dispatch an event
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} correctly makes the
     * {@link Bucketer#bucket(Experiment, String)} call and does NOT dispatch an event.
     */
    @Test
    public void getVariationWithExperimentKey() throws Exception {
        Experiment activatedExperiment = noAudienceProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withConfig(noAudienceProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        when(mockBucketer.bucket(activatedExperiment, "userId")).thenReturn(bucketedVariation);

        // activate the experiment
        Variation actualVariation = optimizely.getVariation(activatedExperiment.getKey(), "userId");

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, "userId");
        assertThat(actualVariation, is(bucketedVariation));

        // verify that we didn't attempt to dispatch an event
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} handles the case where an unknown experiment
     * (i.e., not in the config) is passed through and a {@link NoOpErrorHandler} is used by default.
     */
    @Test
    public void getVariationWithUnknownExperimentKeyAndNoOpErrorHandler() throws Exception {
        Experiment unknownExperiment = createUnknownExperiment();

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
                .withErrorHandler(new NoOpErrorHandler())
                .build();

        logbackVerifier.expectMessage(Level.ERROR, "Experiment \"unknown_experiment\" is not in the datafile");

        // since we use a NoOpErrorHandler, we should fail and return null
        Variation actualVariation = optimizely.getVariation(unknownExperiment.getKey(), "userId");

        // verify that null is returned, as no project config was available
        assertNull(actualVariation);
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String, Map)} returns a valid variation for a user who
     * falls into the experiment.
     */
    @Test
    public void getVariationWithAudiences() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = experiment.getVariations().get(0);

        when(mockBucketer.bucket(experiment, "userId")).thenReturn(bucketedVariation);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .withBucketing(mockBucketer)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");

        Variation actualVariation = optimizely.getVariation(experiment.getKey(), "userId", testUserAttributes);

        verify(mockBucketer).bucket(experiment, "userId");
        assertThat(actualVariation, is(bucketedVariation));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} doesn't return a variation when
     * given an experiment with audiences but no attributes.
     */
    @Test
    public void getVariationWithAudiencesNoAttributes() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment experiment = validProjectConfig.getExperiments().get(0);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withErrorHandler(mockErrorHandler)
                .build();

        logbackVerifier.expectMessage(Level.INFO,
                "User \"userId\" does not meet conditions to be in experiment \"etag1\".");

        Variation actualVariation = optimizely.getVariation(experiment.getKey(), "userId");
        assertNull(actualVariation);
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} returns a variation when given an experiment
     * with no audiences and no user attributes.
     */
    @Test
    public void getVariationNoAudiences() throws Exception {
        Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = experiment.getVariations().get(0);

        when(mockBucketer.bucket(experiment, "userId")).thenReturn(bucketedVariation);

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
                .withConfig(noAudienceProjectConfig)
                .withBucketing(mockBucketer)
                .withErrorHandler(mockErrorHandler)
                .build();

        Variation actualVariation = optimizely.getVariation(experiment.getKey(), "userId");

        verify(mockBucketer).bucket(experiment, "userId");
        assertThat(actualVariation, is(bucketedVariation));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} handles the case where an unknown experiment
     * (i.e., not in the config) is passed through and a {@link RaiseExceptionErrorHandler} is provided.
     */
    @Test
    public void getVariationWithUnknownExperimentKeyAndRaiseExceptionErrorHandler() throws Exception {
        thrown.expect(UnknownExperimentException.class);

        Experiment unknownExperiment = createUnknownExperiment();

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withConfig(noAudienceProjectConfig)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        // since we use a RaiseExceptionErrorHandler, we should throw an error
        optimizely.getVariation(unknownExperiment.getKey(), "userId");
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} doesn't return a variation when provided an
     * empty string.
     */
    @Test
    public void getVariationWithEmptyUserId() throws Exception {
        Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
                .withConfig(noAudienceProjectConfig)
                .withErrorHandler(new RaiseExceptionErrorHandler())
                .build();

        logbackVerifier.expectMessage(Level.ERROR, "Non-empty user ID required");
        assertNull(optimizely.getVariation(experiment.getKey(), ""));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String, Map)} returns a variation when given matching
     * user attributes.
     */
    @Test
    public void getVariationForGroupExperimentWithMatchingAttributes() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment experiment = validProjectConfig.getGroups()
                .get(0)
                .getExperiments()
                .get(0);
        Variation variation = experiment.getVariations().get(0);

        when(mockBucketer.bucket(experiment, "user")).thenReturn(variation);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .withBucketing(mockBucketer)
                .build();

        assertThat(optimizely.getVariation(experiment.getKey(), "user", Collections.singletonMap("browser_type", "chrome")),
                is(variation));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String, Map)} doesn't return a variation when given
     * non-matching user attributes.
     */
    @Test
    public void getVariationForGroupExperimentWithNonMatchingAttributes() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment experiment = validProjectConfig.getGroups()
                .get(0)
                .getExperiments()
                .get(0);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build();

        assertNull(optimizely.getVariation(experiment.getKey(), "user",
                Collections.singletonMap("browser_type", "firefox")));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String, Map)} gives precedence to forced variation bucketing
     * over audience evaluation.
     */
    @Test
    public void getVariationForcedVariationPrecedesAudienceEval() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation expectedVariation = experiment.getVariations().get(0);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        logbackVerifier.expectMessage(Level.INFO, "User \"testUser1\" is forced in variation \"vtag1\".");
        // no attributes provided for a experiment that has an audience
        assertThat(optimizely.getVariation(experiment.getKey(), "testUser1"), is(expectedVariation));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} gives precedence to experiment status over forced
     * variation bucketing.
     */
    @Test
    public void getVariationExperimentStatusPrecedesForcedVariation() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment experiment = validProjectConfig.getExperiments().get(1);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        logbackVerifier.expectMessage(Level.INFO, "Experiment \"etag2\" is not running.");
        // testUser3 has a corresponding forced variation, but experiment status should be checked first
        assertNull(optimizely.getVariation(experiment.getKey(), "testUser3"));
    }

    //======== Notification listeners ========//

    /**
     * Verify that {@link Optimizely#addNotificationListener(NotificationListener)} properly calls
     * through to {@link com.optimizely.ab.notification.NotificationBroadcaster} and the listener is
     * added and notified when an experiment is activated.
     */
    @Test
    public void addNotificationListener() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(validProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        String userId = "userId";
        Map<String, String> attributes = Collections.singletonMap("browser_type", "chrome");

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment,
                                                    bucketedVariation, userId, attributes))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, userId))
                .thenReturn(bucketedVariation);

        when(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment,
                                                    bucketedVariation, userId, attributes))
            .thenReturn(logEventToDispatch);

        // Add listener
        NotificationListener listener = mock(NotificationListener.class);
        optimizely.addNotificationListener(listener);

        // Check if listener is notified when experiment is activated
        Variation actualVariation = optimizely.activate(activatedExperiment, userId, attributes);
        verify(listener, times(1))
                .onExperimentActivated(activatedExperiment, userId, attributes, actualVariation);

        // Check if listener is notified when live variable is accessed
        boolean activateExperiment = true;
        optimizely.getVariableString("string_variable", userId, attributes, activateExperiment);
        verify(listener, times(2))
                .onExperimentActivated(activatedExperiment, userId, attributes, actualVariation);

        // Check if listener is notified after an event is tracked
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String eventKey = eventType.getKey();

        when(mockEventBuilder.createConversionEvent(eq(validProjectConfig), eq(mockBucketer), eq(userId),
                                                    eq(eventType.getId()), eq(eventKey),
                                                    anyMapOf(String.class, String.class), anyMapOf(String.class, Object.class)))
            .thenReturn(logEventToDispatch);

        optimizely.track(eventKey, userId, attributes);
        verify(listener, times(1))
                .onEventTracked(eventKey, userId, attributes, null, logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#removeNotificationListener(NotificationListener)} properly
     * calls through to {@link com.optimizely.ab.notification.NotificationBroadcaster} and the
     * listener is removed and no longer notified when an experiment is activated.
     */
    @Test
    public void removeNotificationListener() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(validProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        String userId = "userId";
        Map<String, String> attributes = Collections.singletonMap("browser_type", "chrome");

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment,
                                                    bucketedVariation, userId, attributes))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, userId))
                .thenReturn(bucketedVariation);

        when(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, userId,
                                                    attributes))
            .thenReturn(logEventToDispatch);

        // Add and remove listener
        NotificationListener listener = mock(NotificationListener.class);
        optimizely.addNotificationListener(listener);
        optimizely.removeNotificationListener(listener);

        // Check if listener is notified after an experiment is activated
        Variation actualVariation = optimizely.activate(activatedExperiment, userId, attributes);
        verify(listener, never())
                .onExperimentActivated(activatedExperiment, userId, attributes, actualVariation);

        // Check if listener is notified after a live variable is accessed
        boolean activateExperiment = true;
        optimizely.getVariableString("string_variable", userId, attributes, activateExperiment);
        verify(listener, never())
                .onExperimentActivated(activatedExperiment, userId, attributes, actualVariation);

        // Check if listener is notified after an event is tracked
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String eventKey = eventType.getKey();

        when(mockEventBuilder.createConversionEvent(eq(validProjectConfig), eq(mockBucketer), eq(userId),
                eq(eventType.getId()), eq(eventKey),
                anyMapOf(String.class, String.class)))
                .thenReturn(logEventToDispatch);

        optimizely.track(eventKey, userId, attributes);
        verify(listener, never())
                .onEventTracked(eventKey, userId, attributes, null, logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#clearNotificationListeners()} properly calls through to
     * {@link com.optimizely.ab.notification.NotificationBroadcaster} and all listeners are removed
     * and no longer notified when an experiment is activated.
     */
    @Test
    public void clearNotificationListeners() throws Exception {
        String validDatafile = validConfigJsonV3();
        ProjectConfig validProjectConfig = validProjectConfigV3();
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(validProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        String userId = "userId";
        Map<String, String> attributes = Collections.singletonMap("browser_type", "chrome");

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment,
                                                    bucketedVariation, userId, attributes))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, userId))
                .thenReturn(bucketedVariation);

        when(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, userId,
                                                    attributes))
            .thenReturn(logEventToDispatch);

        NotificationListener listener = mock(NotificationListener.class);
        optimizely.addNotificationListener(listener);
        optimizely.clearNotificationListeners();

        // Check if listener is notified after an experiment is activated
        Variation actualVariation = optimizely.activate(activatedExperiment, userId, attributes);
        verify(listener, never())
                .onExperimentActivated(activatedExperiment, userId, attributes, actualVariation);

        // Check if listener is notified after a live variable is accessed
        boolean activateExperiment = true;
        optimizely.getVariableString("string_variable", userId, attributes, activateExperiment);
        verify(listener, never())
                .onExperimentActivated(activatedExperiment, userId, attributes, actualVariation);

        // Check if listener is notified after a event is tracked
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String eventKey = eventType.getKey();

        when(mockEventBuilder.createConversionEvent(eq(validProjectConfig), eq(mockBucketer), eq(userId),
                eq(eventType.getId()), eq(eventKey),
                anyMapOf(String.class, String.class)))
                .thenReturn(logEventToDispatch);

        optimizely.track(eventKey, userId, attributes);
        verify(listener, never())
                .onEventTracked(eventKey, userId, attributes, null, logEventToDispatch);
    }

    //======== Helper methods ========//

    private Experiment createUnknownExperiment() {
        return new Experiment("0987", "unknown_experiment", "Running", "1",
                Collections.<String>emptyList(),
                Collections.singletonList(
                        new Variation("8765", "unknown_variation", Collections.<LiveVariableUsageInstance>emptyList())),
                Collections.<String, String>emptyMap(),
                Collections.singletonList(new TrafficAllocation("8765", 4999)));
    }

    private EventType createUnknownEventType() {
        List<String> experimentIds = asList(
                "223"
        );
        return new EventType("8765", "unknown_event_type", experimentIds);
    }
}
