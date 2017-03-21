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
import com.optimizely.ab.bucketing.UserProfile;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.error.NoOpErrorHandler;
import com.optimizely.ab.error.RaiseExceptionErrorHandler;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.EventBuilder;
import com.optimizely.ab.event.internal.EventBuilderV1;
import com.optimizely.ab.internal.LogbackVerifier;
import com.optimizely.ab.internal.ProjectValidationUtils;
import com.optimizely.ab.internal.ReservedEventKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

import static com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigJsonV1;
import static com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigV1;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV1;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV1;
import static com.optimizely.ab.event.LogEvent.RequestMethod;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the top-level {@link Optimizely} class.
 */
public class OptimizelyTestV1 {

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

    //======== activate tests ========//

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} call correctly builds an endpoint url and
     * request params and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateEndToEnd() throws Exception {
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventBuilder)
            .withConfig(projectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation, "userId",
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withConfig(projectConfig)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment unknownExperiment = createUnknownExperiment();
        Variation bucketedVariation = unknownExperiment.getVariations().get(0);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withConfig(projectConfig)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventBuilder)
            .withConfig(projectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(eq(projectConfig), eq(activatedExperiment), eq(bucketedVariation),
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment unknownExperiment = createUnknownExperiment();

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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

        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment unknownExperiment = createUnknownExperiment();

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
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
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), "userId",
                                                        ImmutableMap.of(attribute.getKey(), "attributeValue"));

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, "userId");
        assertThat(actualVariation, is(bucketedVariation));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockEventBuilder).createImpressionEvent(eq(projectConfig),
                                                       eq(activatedExperiment),
                                                       eq(bucketedVariation),
                                                       eq("userId"),
                                                       attributeCaptor.capture());

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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        // setup a mock event builder to return mock params and endpoint
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventBuilder)
            .withConfig(projectConfig)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");
        testUserAttributes.put("unknownAttribute", "dimValue");

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(eq(projectConfig), eq(activatedExperiment), eq(bucketedVariation),
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
        verify(mockEventBuilder).createImpressionEvent(eq(projectConfig), eq(activatedExperiment),
                                                       eq(bucketedVariation), eq("userId"), attributeCaptor.capture());

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, not(hasKey("unknownAttribute")));

        // verify that dispatchEvent was called with the correct LogEvent object.
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} returns null when the experiment id corresponds to a
     * non-running experiment.
     */
    @Test
    public void activateDraftExperiment() throws Exception {
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment draftExperiment = projectConfig.getExperiments().get(1);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment experimentToCheck = projectConfig.getExperiments().get(0);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment experimentToCheck = projectConfig.getExperiments().get(0);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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
        String datafile = noAudienceProjectConfigJsonV1();
        ProjectConfig projectConfig = noAudienceProjectConfigV1();
        Experiment experimentToCheck = projectConfig.getExperiments().get(0);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment experiment = projectConfig.getExperiments().get(0);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
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
        String datafile = noAudienceProjectConfigJsonV1();
        ProjectConfig projectConfig = noAudienceProjectConfigV1();
        Experiment experiment = projectConfig.getExperiments().get(0);
        String experimentKey = experiment.getKey();

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment experiment = projectConfig.getGroups()
            .get(0)
            .getExperiments()
            .get(0);
        Variation variation = experiment.getVariations().get(0);

        when(mockBucketer.bucket(experiment, "user")).thenReturn(variation);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment experiment = projectConfig.getGroups()
            .get(0)
            .getExperiments()
            .get(0);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment experiment = projectConfig.getExperiments().get(0);
        Variation expectedVariation = experiment.getVariations().get(0);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment experiment = projectConfig.getExperiments().get(1);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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
        String datafile = noAudienceProjectConfigJsonV1();
        ProjectConfig projectConfig = noAudienceProjectConfigV1();
        Experiment experiment = projectConfig.getExperiments().get(0);

        doThrow(new Exception("Test Exception")).when(mockEventHandler).dispatchEvent(any(LogEvent.class));

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
            .build();

        logbackVerifier.expectMessage(Level.ERROR, "Unexpected exception in event dispatcher");
        optimizely.activate(experiment.getKey(), "userId");
    }

    //======== track tests ========//

    /**
     * Verify that the {@link Optimizely#track(String, String)} call correctly builds a V1 event and passes it
     * through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void trackEventEndToEnd() throws Exception {
        String datafile = noAudienceProjectConfigJsonV1();
        ProjectConfig projectConfig = noAudienceProjectConfigV1();
        List<Experiment> allExperiments = projectConfig.getExperiments();
        EventType eventType = projectConfig.getEventTypes().get(0);

        EventBuilder eventBuilderV1 = new EventBuilderV1();

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(eventBuilderV1)
            .withConfig(projectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        List<String> experimentIds = projectConfig.getExperimentIdsForGoal(eventType.getKey());

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
            if (ProjectValidationUtils.validatePreconditions(projectConfig, null, experiment, "userId", emptyAttributes) &&
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        EventType unknownEventType = createUnknownEventType();

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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

        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        EventType unknownEventType = createUnknownEventType();

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Attribute attribute = projectConfig.getAttributes().get(0);
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
        when(mockEventBuilder.createConversionEvent(eq(projectConfig), eq(mockBucketer), isNull(UserProfile.class), eq("userId"),
                                                    eq(eventType.getId()), eq(eventType.getKey()),
                                                    anyMapOf(String.class, String.class),
                                                    anyMapOf(String.class, Object.class)))
            .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"clicked_cart\" for user \"userId\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
                                                   testParams + " and payload \"\"");

        // call track
        optimizely.track(eventType.getKey(), "userId", ImmutableMap.of(attribute.getKey(), "attributeValue"));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(eq(projectConfig), eq(mockBucketer), isNull(UserProfile.class), eq("userId"),
                                                       eq(eventType.getId()), eq(eventType.getKey()),
                                                       attributeCaptor.capture(),
                                                       eq(Collections.<String, Object>emptyMap()));

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, hasEntry(attribute.getKey(), "attributeValue"));

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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        EventType eventType = projectConfig.getEventTypes().get(0);

        // setup a mock event builder to return expected conversion params
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventBuilder)
            .withConfig(projectConfig)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createConversionEvent(eq(projectConfig), eq(mockBucketer), isNull(UserProfile.class), eq("userId"),
                                                    eq(eventType.getId()), eq(eventType.getKey()),
                                                    anyMapOf(String.class, String.class),
                                                    anyMapOf(String.class, Object.class)))
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
        verify(mockEventBuilder).createConversionEvent(eq(projectConfig), eq(mockBucketer), isNull(UserProfile.class), eq("userId"),
                                                       eq(eventType.getId()), eq(eventType.getKey()),
                                                       attributeCaptor.capture(),
                                                       eq(Collections.<String, Object>emptyMap()));

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, not(hasKey("unknownAttribute")));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} passes through revenue.
     */
    @Test
    public void trackEventWithRevenue() throws Exception {
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        EventType eventType = projectConfig.getEventTypes().get(0);
        long revenue = 1234L;

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
        when(mockEventBuilder.createConversionEvent(eq(projectConfig), eq(mockBucketer), isNull(UserProfile.class), eq("userId"),
                                                    eq(eventType.getId()), eq(eventType.getKey()),
                                                    eq(Collections.<String, String>emptyMap()),
                                                    eq(Collections.singletonMap(ReservedEventKey.REVENUE.toString(), revenue))))
            .thenReturn(logEventToDispatch);

        // call track
        optimizely.track(eventType.getKey(), "userId", revenue);

        // setup the event tag map captor (capture the revenue value, which is a reserved keyword)
        ArgumentCaptor<Map> eventTagCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected revenue
        verify(mockEventBuilder).createConversionEvent(eq(projectConfig), eq(mockBucketer), isNull(UserProfile.class), eq("userId"),
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
        String datafile = validConfigJsonV1();

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler).build();

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
        String datafile = noAudienceProjectConfigJsonV1();
        ProjectConfig projectConfig = noAudienceProjectConfigV1();
        EventType eventType = projectConfig.getEventTypes().get(0);

        doThrow(new Exception("Test Exception")).when(mockEventHandler).dispatchEvent(any(LogEvent.class));

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
            .build();

        logbackVerifier.expectMessage(Level.ERROR, "Unexpected exception in event dispatcher");
        optimizely.track(eventType.getKey(), "userId");
    }

    //======== getVariation tests ========//

    /**
     * Verify that {@link Optimizely#getVariation(Experiment, String)} correctly makes the
     * {@link Bucketer#bucket(Experiment, String)} call and does NOT dispatch an event.
     */
    @Test
    public void getVariation() throws Exception {
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withConfig(projectConfig)
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
        String datafile = noAudienceProjectConfigJsonV1();
        ProjectConfig projectConfig = noAudienceProjectConfigV1();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withConfig(projectConfig)
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
        String datafile = noAudienceProjectConfigJsonV1();
        Experiment unknownExperiment = createUnknownExperiment();

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment experiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = experiment.getVariations().get(0);

        when(mockBucketer.bucket(experiment, "userId")).thenReturn(bucketedVariation);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment experiment = projectConfig.getExperiments().get(0);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
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
        String datafile = noAudienceProjectConfigJsonV1();
        ProjectConfig projectConfig = noAudienceProjectConfigV1();
        Experiment experiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = experiment.getVariations().get(0);

        when(mockBucketer.bucket(experiment, "userId")).thenReturn(bucketedVariation);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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

        String datafile = noAudienceProjectConfigJsonV1();
        ProjectConfig projectConfig = noAudienceProjectConfigV1();
        Experiment unknownExperiment = createUnknownExperiment();

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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
        String datafile = noAudienceProjectConfigJsonV1();
        ProjectConfig projectConfig = noAudienceProjectConfigV1();
        Experiment experiment = projectConfig.getExperiments().get(0);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment experiment = projectConfig.getGroups()
            .get(0)
            .getExperiments()
            .get(0);
        Variation variation = experiment.getVariations().get(0);

        when(mockBucketer.bucket(experiment, "user")).thenReturn(variation);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment experiment = projectConfig.getGroups()
            .get(0)
            .getExperiments()
            .get(0);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment experiment = projectConfig.getExperiments().get(0);
        Variation expectedVariation = experiment.getVariations().get(0);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
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
        String datafile = validConfigJsonV1();
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment experiment = projectConfig.getExperiments().get(1);

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withConfig(projectConfig)
            .build();

        logbackVerifier.expectMessage(Level.INFO, "Experiment \"etag2\" is not running.");
        // testUser3 has a corresponding forced variation, but experiment status should be checked first
        assertNull(optimizely.getVariation(experiment.getKey(), "testUser3"));
    }

    //======== Helper methods ========//

    private Experiment createUnknownExperiment() {
        return new Experiment("0987", "unknown_experiment", "Running", "1",
                              Collections.<String>emptyList(),
                              Collections.singletonList(new Variation("8765", "unknown_variation")),
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
