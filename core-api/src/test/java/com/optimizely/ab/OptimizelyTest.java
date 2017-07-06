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
import com.optimizely.ab.internal.ExperimentUtils;
import com.optimizely.ab.internal.LogbackVerifier;
import com.optimizely.ab.notification.NotificationListener;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigJsonV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigJsonV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV3;
import static com.optimizely.ab.event.LogEvent.RequestMethod;
import static com.optimizely.ab.event.internal.EventBuilderV2Test.createExperimentVariationMap;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the top-level {@link Optimizely} class.
 */
@RunWith(Parameterized.class)
public class OptimizelyTest {

    @Parameters
    public static Collection<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][] {
                {
                        2,
                        validConfigJsonV2(),
                        noAudienceProjectConfigJsonV2(),
                        validProjectConfigV2(),
                        noAudienceProjectConfigV2()
                },
                {
                        3,
                        validConfigJsonV3(),
                        noAudienceProjectConfigJsonV3(),
                        validProjectConfigV3(),
                        noAudienceProjectConfigV3()
                }
        });
    }

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
    private int datafileVersion;
    private String validDatafile;
    private String noAudienceDatafile;
    private ProjectConfig validProjectConfig;
    private ProjectConfig noAudienceProjectConfig;

    public OptimizelyTest(int datafileVersion,
                          String validDatafile,
                          String noAudienceDatafile,
                          ProjectConfig validProjectConfig,
                          ProjectConfig noAudienceProjectConfig) {
        this.datafileVersion = datafileVersion;
        this.validDatafile = validDatafile;
        this.noAudienceDatafile = noAudienceDatafile;
        this.validProjectConfig = validProjectConfig;
        this.noAudienceProjectConfig = noAudienceProjectConfig;
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
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} DOES NOT dispatch an impression event
     * when the user isn't bucketed to a variation.
     */
    @Test
    public void activateForNullVariation() throws Exception {
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
        Experiment activatedExperiment = noAudienceProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        // setup a mock event builder to return expected impression params
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(noAudienceProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(eq(noAudienceProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
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
        verify(mockEventBuilder).createImpressionEvent(eq(noAudienceProjectConfig), eq(activatedExperiment),
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
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(attribute.getKey(), null);
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), "userId", attributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, "userId");
        assertThat(actualVariation, is(bucketedVariation));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockEventBuilder).createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment),
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

    /**
     * Verify that {@link Optimizely#activate(String, String)} doesn't dispatch an event for an experiment with a
     * "Launched" status.
     */
    @Test
    public void activateLaunchedExperimentDoesNotDispatchEvent() throws Exception {
        Experiment launchedExperiment = noAudienceProjectConfig.getExperiments().get(2);

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withConfig(noAudienceProjectConfig)
                .build();

        Variation expectedVariation = launchedExperiment.getVariations().get(0);

        when(mockBucketer.bucket(launchedExperiment, "userId"))
                .thenReturn(launchedExperiment.getVariations().get(0));

        logbackVerifier.expectMessage(Level.INFO,
                "Experiment has \"Launched\" status so not dispatching event during activation.");
        Variation variation = optimizely.activate(launchedExperiment.getKey(), "userId");

        assertNotNull(variation);
        assertThat(variation.getKey(), is(expectedVariation.getKey()));

        // verify that we did NOT dispatch an event
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
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

        // Bucket to the first variation for all experiments. However, only a subset of the experiments will actually
        // call the bucket function.
        for (Experiment experiment : allExperiments) {
            when(mockBucketer.bucket(experiment, "userId"))
                    .thenReturn(experiment.getVariations().get(0));
        }

        // call track
        optimizely.track(eventType.getKey(), "userId");

        // verify that the bucketing algorithm was called only on experiments corresponding to the specified goal.
        List<Experiment> experimentsForEvent = noAudienceProjectConfig.getExperimentsForEventKey(eventType.getKey());
        for (Experiment experiment : allExperiments) {
            if (ExperimentUtils.isExperimentActive(experiment) &&
                    experimentsForEvent.contains(experiment)) {
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
        Map<String, String> attributes = ImmutableMap.of(attribute.getKey(), "attributeValue");
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                mockBucketer,
                eventType.getKey(),
                genericUserId,
                attributes);
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createConversionEvent(
                eq(validProjectConfig),
                eq(experimentVariationMap),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                anyMapOf(String.class, String.class),
                eq(Collections.<String, Object>emptyMap())))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"clicked_cart\" for user \"" + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
                testParams + " and payload \"\"");

        // call track
        optimizely.track(eventType.getKey(), genericUserId, attributes);

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(
                eq(validProjectConfig),
                eq(experimentVariationMap),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                attributeCaptor.capture(),
                eq(Collections.<String, Object>emptyMap()));

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
        EventType eventType = noAudienceProjectConfig.getEventTypes().get(0);

        // setup a mock event builder to return expected conversion params
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(noAudienceProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                noAudienceProjectConfig,
                mockBucketer,
                eventType.getKey(),
                genericUserId,
                Collections.<String, String>emptyMap());
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createConversionEvent(
                eq(noAudienceProjectConfig),
                eq(experimentVariationMap),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                eq(Collections.<String, String>emptyMap()),
                eq(Collections.<String, Object>emptyMap())))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"clicked_cart\" for user \"" + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
                testParams + " and payload \"\"");

        // call track
        Map<String, String> attributes = null;
        optimizely.track(eventType.getKey(), genericUserId, attributes);

        logbackVerifier.expectMessage(Level.WARN, "Attributes is null when non-null was expected. Defaulting to an empty attributes map.");

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(
                eq(noAudienceProjectConfig),
                eq(experimentVariationMap),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                attributeCaptor.capture(),
                eq(Collections.<String, Object>emptyMap()));

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, is(Collections.<String, String>emptyMap()));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} gracefully handles null attribute values.
     */
    @Test
    public void trackEventWithNullAttributeValues() throws Exception {
        EventType eventType = noAudienceProjectConfig.getEventTypes().get(0);

        // setup a mock event builder to return expected conversion params
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(noAudienceProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                noAudienceProjectConfig,
                mockBucketer,
                eventType.getKey(),
                genericUserId,
                Collections.<String, String>emptyMap());
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createConversionEvent(
                eq(noAudienceProjectConfig),
                eq(experimentVariationMap),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                eq(Collections.<String, String>emptyMap()),
                eq(Collections.<String, Object>emptyMap())))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"clicked_cart\" for user \"" + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
                testParams + " and payload \"\"");

        // call track
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("test", null);
        optimizely.track(eventType.getKey(), genericUserId, attributes);

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(
                eq(noAudienceProjectConfig),
                eq(experimentVariationMap),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                attributeCaptor.capture(),
                eq(Collections.<String, Object>emptyMap()));

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
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                mockBucketer,
                eventType.getKey(),
                genericUserId,
                Collections.<String, String>emptyMap());
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createConversionEvent(
                eq(validProjectConfig),
                eq(experimentVariationMap),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                anyMapOf(String.class, String.class),
                eq(Collections.<String, Object>emptyMap())))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"clicked_cart\" for user \"" + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.WARN, "Attribute(s) [unknownAttribute] not in the datafile.");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
                testParams + " and payload \"\"");

        // call track
        optimizely.track(eventType.getKey(), genericUserId, ImmutableMap.of("unknownAttribute", "attributeValue"));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(
                eq(validProjectConfig),
                eq(experimentVariationMap),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                attributeCaptor.capture(),
                eq(Collections.<String, Object>emptyMap()));

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, not(hasKey("unknownAttribute")));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map, Map)} passes event features to
     * {@link EventBuilder#createConversionEvent(ProjectConfig, Map, String, String, String, Map, Map)}
     */
    @Test
    public void trackEventWithEventTags() throws Exception {
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

        Map<String, Object> eventTags = new HashMap<String, Object>();
        eventTags.put("int_param", 123);
        eventTags.put("string_param", "123");
        eventTags.put("boolean_param", false);
        eventTags.put("float_param", 12.3f);
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                mockBucketer,
                eventType.getKey(),
                genericUserId,
                Collections.<String, String>emptyMap());

        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createConversionEvent(
                eq(validProjectConfig),
                eq(experimentVariationMap),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                anyMapOf(String.class, String.class),
                eq(eventTags)))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"clicked_cart\" for user \"" + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
                testParams + " and payload \"\"");

        // call track
        optimizely.track(eventType.getKey(), genericUserId, Collections.<String, String>emptyMap(), eventTags);

        // setup the event map captor (so we can verify its content)
        ArgumentCaptor<Map> eventTagCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(
                eq(validProjectConfig),
                eq(experimentVariationMap),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                eq(Collections.<String, String>emptyMap()),
                eventTagCaptor.capture());

        Map<String, ?> actualValue = eventTagCaptor.getValue();
        assertThat(actualValue, hasEntry("int_param", eventTags.get("int_param")));
        assertThat(actualValue, hasEntry("string_param", eventTags.get("string_param")));
        assertThat(actualValue, hasEntry("boolean_param", eventTags.get("boolean_param")));
        assertThat(actualValue, hasEntry("float_param", eventTags.get("float_param")));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map, Map)} called with null event tags will default to
     * an empty map when calling {@link EventBuilder#createConversionEvent(ProjectConfig, Map, String, String, String, Map, Map)}
     */
    @Test
    @SuppressFBWarnings(
            value="NP_NONNULL_PARAM_VIOLATION",
            justification="testing nullness contract violation")
    public void trackEventWithNullEventTags() throws Exception {
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
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                mockBucketer,
                eventType.getKey(),
                genericUserId,
                Collections.<String, String>emptyMap());
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createConversionEvent(
                eq(validProjectConfig),
                eq(experimentVariationMap),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                eq(Collections.<String, String>emptyMap()),
                eq(Collections.<String, String>emptyMap())))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"clicked_cart\" for user \"" + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
                testParams + " and payload \"\"");

        // call track
        optimizely.track(eventType.getKey(), genericUserId, Collections.<String, String>emptyMap(), null);

        // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(
                eq(validProjectConfig),
                eq(experimentVariationMap),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                eq(Collections.<String, String>emptyMap()),
                eq(Collections.<String, String>emptyMap()));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map)} doesn't dispatch an event when no valid experiments
     * correspond to an event.
     */
    @Test
    public void trackEventWithNoValidExperiments() throws Exception {

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

    /**
     * Verify that {@link Optimizely#track(String, String, Map)}
     * doesn't dispatch events when the event links only to launched experiments
     */
    @Test
    public void trackDoesNotSendEventWhenExperimentsAreLaunchedOnly() throws Exception {
        EventType eventType = noAudienceProjectConfig.getEventNameMapping().get("launched_exp_event");
        Bucketer mockBucketAlgorithm = mock(Bucketer.class);
        for (Experiment experiment : noAudienceProjectConfig.getExperiments()) {
            Variation variation = experiment.getVariations().get(0);
            when(mockBucketAlgorithm.bucket(
                    eq(experiment),
                    eq(genericUserId)))
                    .thenReturn(variation);
        }

        Optimizely client = Optimizely.builder(noAudienceDatafile, mockEventHandler)
                .withConfig(noAudienceProjectConfig)
                .withBucketing(mockBucketAlgorithm)
                .build();

        List<Experiment> eventExperiments = noAudienceProjectConfig.getExperimentsForEventKey(eventType.getKey());
        for (Experiment experiment : eventExperiments) {
            logbackVerifier.expectMessage(
                    Level.INFO,
                    "Not tracking event \"" + eventType.getKey() + "\" for experiment \"" + experiment.getKey() +
                            "\" because experiment has status \"Launched\"."
            );
        }

        logbackVerifier.expectMessage(
                Level.INFO,
                "There are no valid experiments for event \"" + eventType.getKey() + "\" to track."
        );
        logbackVerifier.expectMessage(
                Level.INFO,
                "Not tracking event \"" + eventType.getKey() + "\" for user \"" + genericUserId + "\"."
        );

        // only 1 experiment uses the event and it has a "Launched" status so experimentsForEvent map is empty
        // and the returned event will be null
        // this means we will never call the dispatcher
        client.track(eventType.getKey(), genericUserId, Collections.<String, String>emptyMap());
        // bucket should never be called since experiments are launched so we never get variation for them
        verify(mockBucketAlgorithm, never()).bucket(
                any(Experiment.class),
                anyString());
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map)}
     * dispatches log events when the tracked event links to both launched and running experiments.
     */
    @Test
    public void trackDispatchesWhenEventHasLaunchedAndRunningExperiments() throws Exception {
        EventBuilder mockEventBuilder = mock(EventBuilder.class);
        EventType eventType = noAudienceProjectConfig.getEventNameMapping().get("event_with_launched_and_running_experiments");
        Bucketer mockBucketAlgorithm = mock(Bucketer.class);
        for (Experiment experiment : noAudienceProjectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, genericUserId))
                    .thenReturn(experiment.getVariations().get(0));
        }

        Optimizely client = Optimizely.builder(noAudienceDatafile, mockEventHandler)
                .withConfig(noAudienceProjectConfig)
                .withBucketing(mockBucketAlgorithm)
                .withEventBuilder(mockEventBuilder)
                .build();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                noAudienceProjectConfig,
                mockBucketAlgorithm,
                eventType.getKey(),
                genericUserId,
                null);
        LogEvent conversionEvent = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createConversionEvent(
                eq(noAudienceProjectConfig),
                eq(experimentVariationMap),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                eq(Collections.<String, String>emptyMap()),
                eq(Collections.<String, Object>emptyMap())
        )).thenReturn(conversionEvent);

        List<Experiment> eventExperiments = noAudienceProjectConfig.getExperimentsForEventKey(eventType.getKey());
        for (Experiment experiment : eventExperiments) {
            if (experiment.isLaunched()) {
                logbackVerifier.expectMessage(
                        Level.INFO,
                        "Not tracking event \"" + eventType.getKey() + "\" for experiment \"" + experiment.getKey() +
                                "\" because experiment has status \"Launched\"."
                );
            }
        }

        // The event has 1 launched experiment and 1 running experiment.
        // It should send a track event with the running experiment
        client.track(eventType.getKey(), genericUserId, Collections.<String, String>emptyMap());
        verify(client.eventHandler).dispatchEvent(eq(conversionEvent));
    }

    /**
     * Verify that an event is not dispatched if a user doesn't satisfy audience conditions for an experiment.
     */
    @Test
    public void trackDoesNotSendEventWhenUserDoesNotSatisfyAudiences() throws Exception {
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        EventType eventType = validProjectConfig.getEventTypes().get(2);

        // the audience for the experiments is "NOT firefox" so this user shouldn't satisfy audience conditions
        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "firefox");

        Optimizely client = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build();

        logbackVerifier.expectMessage(Level.INFO, "There are no valid experiments for event \"" + eventType.getKey()
                + "\" to track.");

        client.track(eventType.getKey(), genericUserId, attributeMap);
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    //======== getVariation tests ========//

    /**
     * Verify that {@link Optimizely#getVariation(Experiment, String)} correctly makes the
     * {@link Bucketer#bucket(Experiment, String)} call and does NOT dispatch an event.
     */
    @Test
    public void getVariation() throws Exception {
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
     * Verify that {@link Optimizely#getVariation(String, String)} gives precedence to experiment status over forced
     * variation bucketing.
     */
    @Test
    public void getVariationExperimentStatusPrecedesForcedVariation() throws Exception {
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
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(validProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> attributes = Collections.singletonMap("browser_type", "chrome");

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment,
                bucketedVariation, genericUserId, attributes))
                .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, genericUserId))
                .thenReturn(bucketedVariation);

        when(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment,
                bucketedVariation, genericUserId, attributes))
                .thenReturn(logEventToDispatch);

        // Add listener
        NotificationListener listener = mock(NotificationListener.class);
        optimizely.addNotificationListener(listener);

        // Check if listener is notified when experiment is activated
        Variation actualVariation = optimizely.activate(activatedExperiment, genericUserId, attributes);

        if (datafileVersion == 3 || datafileVersion == 2) {
            verify(listener, times(1))
                    .onExperimentActivated(activatedExperiment, genericUserId, attributes, actualVariation);
        }

        // Check if listener is notified after an event is tracked
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String eventKey = eventType.getKey();

        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                mockBucketer,
                eventType.getKey(),
                genericUserId,
                attributes);
        when(mockEventBuilder.createConversionEvent(
                eq(validProjectConfig),
                eq(experimentVariationMap),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventKey),
                eq(attributes),
                anyMapOf(String.class, Object.class)))
                .thenReturn(logEventToDispatch);

        optimizely.track(eventKey, genericUserId, attributes);
        verify(listener, times(1))
                .onEventTracked(eventKey, genericUserId, attributes, null, logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#removeNotificationListener(NotificationListener)} properly
     * calls through to {@link com.optimizely.ab.notification.NotificationBroadcaster} and the
     * listener is removed and no longer notified when an experiment is activated.
     */
    @Test
    public void removeNotificationListener() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(validProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> attributes = Collections.singletonMap("browser_type", "chrome");

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment,
                bucketedVariation, genericUserId, attributes))
                .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, genericUserId))
                .thenReturn(bucketedVariation);

        when(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, genericUserId,
                attributes))
                .thenReturn(logEventToDispatch);

        // Add and remove listener
        NotificationListener listener = mock(NotificationListener.class);
        optimizely.addNotificationListener(listener);
        optimizely.removeNotificationListener(listener);

        // Check if listener is notified after an experiment is activated
        Variation actualVariation = optimizely.activate(activatedExperiment, genericUserId, attributes);
        verify(listener, never())
                .onExperimentActivated(activatedExperiment, genericUserId, attributes, actualVariation);

        verify(listener, never())
                .onExperimentActivated(activatedExperiment, genericUserId, attributes, actualVariation);

        // Check if listener is notified after an event is tracked
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String eventKey = eventType.getKey();

        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                mockBucketer,
                eventType.getKey(),
                genericUserId,
                attributes);
        when(mockEventBuilder.createConversionEvent(
                eq(validProjectConfig),
                eq(experimentVariationMap),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventKey),
                eq(attributes),
                anyMapOf(String.class, Object.class)))
                .thenReturn(logEventToDispatch);

        optimizely.track(eventKey, genericUserId, attributes);
        verify(listener, never())
                .onEventTracked(eventKey, genericUserId, attributes, null, logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#clearNotificationListeners()} properly calls through to
     * {@link com.optimizely.ab.notification.NotificationBroadcaster} and all listeners are removed
     * and no longer notified when an experiment is activated.
     */
    @Test
    public void clearNotificationListeners() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(validProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> attributes = Collections.singletonMap("browser_type", "chrome");

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment,
                bucketedVariation, genericUserId, attributes))
                .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, genericUserId))
                .thenReturn(bucketedVariation);

        when(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, genericUserId,
                attributes))
                .thenReturn(logEventToDispatch);

        NotificationListener listener = mock(NotificationListener.class);
        optimizely.addNotificationListener(listener);
        optimizely.clearNotificationListeners();

        // Check if listener is notified after an experiment is activated
        Variation actualVariation = optimizely.activate(activatedExperiment, genericUserId, attributes);
        verify(listener, never())
                .onExperimentActivated(activatedExperiment, genericUserId, attributes, actualVariation);

        verify(listener, never())
                .onExperimentActivated(activatedExperiment, genericUserId, attributes, actualVariation);

        // Check if listener is notified after a event is tracked
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String eventKey = eventType.getKey();

        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                mockBucketer,
                eventType.getKey(),
                OptimizelyTest.genericUserId,
                attributes);
        when(mockEventBuilder.createConversionEvent(
                eq(validProjectConfig),
                eq(experimentVariationMap),
                eq(OptimizelyTest.genericUserId),
                eq(eventType.getId()),
                eq(eventKey),
                eq(attributes),
                anyMapOf(String.class, Object.class)))
                .thenReturn(logEventToDispatch);

        optimizely.track(eventKey, genericUserId, attributes);
        verify(listener, never())
                .onEventTracked(eventKey, genericUserId, attributes, null, logEventToDispatch);
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
