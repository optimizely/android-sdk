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
import com.optimizely.ab.bucketing.DecisionService;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.config.LiveVariable;
import com.optimizely.ab.config.LiveVariableUsageInstance;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.error.NoOpErrorHandler;
import com.optimizely.ab.error.RaiseExceptionErrorHandler;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.EventBuilder;
import com.optimizely.ab.event.internal.EventBuilderV2;
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
import java.util.ArrayList;
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
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV4;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV4;
import static com.optimizely.ab.config.ValidProjectConfigV4.ATTRIBUTE_HOUSE_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.AUDIENCE_GRYFFINDOR_VALUE;
import static com.optimizely.ab.config.ValidProjectConfigV4.EVENT_BASIC_EVENT_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.EVENT_LAUNCHED_EXPERIMENT_ONLY_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.EXPERIMENT_BASIC_EXPERIMENT_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.EXPERIMENT_LAUNCHED_EXPERIMENT_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.EXPERIMENT_PAUSED_EXPERIMENT_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_FLAG_MULTI_VARIATE_FEATURE;
import static com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_FLAG_SINGLE_VARIABLE_DOUBLE;
import static com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_FLAG_SINGLE_VARIABLE_INTEGER;
import static com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_MULTI_VARIATE_FEATURE_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_SINGLE_VARIABLE_BOOLEAN_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_SINGLE_VARIABLE_DOUBLE_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_SINGLE_VARIABLE_INTEGER_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED;
import static com.optimizely.ab.config.ValidProjectConfigV4.PAUSED_EXPERIMENT_FORCED_VARIATION_USER_ID_CONTROL;
import static com.optimizely.ab.config.ValidProjectConfigV4.VARIABLE_BOOLEAN_VARIABLE_DEFAULT_VALUE;
import static com.optimizely.ab.config.ValidProjectConfigV4.VARIABLE_BOOLEAN_VARIABLE_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.VARIABLE_DOUBLE_DEFAULT_VALUE;
import static com.optimizely.ab.config.ValidProjectConfigV4.VARIABLE_DOUBLE_VARIABLE_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.VARIABLE_FIRST_LETTER_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.VARIABLE_INTEGER_VARIABLE_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.VARIATION_MULTIVARIATE_EXPERIMENT_GRED;
import static com.optimizely.ab.config.ValidProjectConfigV4.VARIATION_MULTIVARIATE_EXPERIMENT_GRED_KEY;
import static com.optimizely.ab.event.LogEvent.RequestMethod;
import static com.optimizely.ab.event.internal.EventBuilderV2Test.createExperimentVariationMap;
import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
                },
                {
                        4,
                        validConfigJsonV4(),
                        validConfigJsonV4(),
                        validProjectConfigV4(),
                        validProjectConfigV4()
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
    @Mock DecisionService mockDecisionService;
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
        Experiment activatedExperiment;
        Map<String, String> testUserAttributes = new HashMap<String, String>();
        if(datafileVersion >= 4) {
            activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
            testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        }
        else {
            activatedExperiment = validProjectConfig.getExperiments().get(0);
            testUserAttributes.put("browser_type", "chrome");
        }
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
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
        when(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, "userId",
                testUserAttributes))
                .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, "userId"))
                .thenReturn(bucketedVariation);

        logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"" +
                activatedExperiment.getKey() + "\".");
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

        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"" +
                activatedExperiment.getKey() + "\".");

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
     * Verify that the {@link Optimizely#activate(String, String, Map<String, String>)} call
     * uses forced variation to force the user into the second variation.  The mock bucket returns
     * the first variation. Then remove the forced variation and confirm that the forced variation is null.
     */
    @Test
    public void activateWithExperimentKeyForced() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation forcedVariation = activatedExperiment.getVariations().get(1);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(validProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        optimizely.setForcedVariation(activatedExperiment.getKey(), "userId", forcedVariation.getKey() );

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        if (datafileVersion >= 4) {
            testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        }
        else {
            testUserAttributes.put("browser_type", "chrome");
        }

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");

        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(forcedVariation),
                eq("userId"), eq(testUserAttributes)))
                .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, "userId"))
                .thenReturn(bucketedVariation);

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), "userId", testUserAttributes);

        assertThat(actualVariation, is(forcedVariation));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);

        optimizely.setForcedVariation(activatedExperiment.getKey(), "userId", null );

        assertEquals(optimizely.getForcedVariation(activatedExperiment.getKey(), "userId"), null);

    }

    /**
     * Verify that the {@link Optimizely#getVariation(String, String, Map<String, String>)} call
     * uses forced variation to force the user into the second variation.  The mock bucket returns
     * the first variation. Then remove the forced variation and confirm that the forced variation is null.
     */
    @Test
    public void getVariationWithExperimentKeyForced() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation forcedVariation = activatedExperiment.getVariations().get(1);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(validProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        optimizely.setForcedVariation(activatedExperiment.getKey(), "userId", forcedVariation.getKey() );

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        if (datafileVersion >= 4) {
            testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        }
        else {
            testUserAttributes.put("browser_type", "chrome");
        }

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");

        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(forcedVariation),
                eq("userId"), eq(testUserAttributes)))
                .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, "userId"))
                .thenReturn(bucketedVariation);

        // activate the experiment
        Variation actualVariation = optimizely.getVariation(activatedExperiment.getKey(), "userId", testUserAttributes);

        assertThat(actualVariation, is(forcedVariation));

        optimizely.setForcedVariation(activatedExperiment.getKey(), "userId", null );

        assertEquals(optimizely.getForcedVariation(activatedExperiment.getKey(), "userId"), null);

        actualVariation = optimizely.getVariation(activatedExperiment.getKey(), "userId", testUserAttributes);

        assertThat(actualVariation, is(bucketedVariation));
    }

    /**
     * Verify that the {@link Optimizely#activate(String, String, Map<String, String>)} call
     * uses forced variation to force the user into the second variation.  The mock bucket returns
     * the first variation. Then remove the forced variation and confirm that the forced variation is null.
     */
    @Test
    public void isFeatureEnabledWithExperimentKeyForced() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
        Variation forcedVariation = activatedExperiment.getVariations().get(1);
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withBucketing(mockBucketer)
                .withEventBuilder(mockEventBuilder)
                .withConfig(validProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        optimizely.setForcedVariation(activatedExperiment.getKey(), "userId", forcedVariation.getKey() );

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        if (datafileVersion < 4)  {
            testUserAttributes.put("browser_type", "chrome");
        }

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");

        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(forcedVariation),
                eq("userId"), eq(testUserAttributes)))
                .thenReturn(logEventToDispatch);

        // activate the experiment
        assertTrue(optimizely.isFeatureEnabled(FEATURE_FLAG_MULTI_VARIATE_FEATURE.getKey(), "userId"));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);

        assertTrue(optimizely.setForcedVariation(activatedExperiment.getKey(), "userId", null ));

        assertNull(optimizely.getForcedVariation(activatedExperiment.getKey(), "userId"));

        assertFalse(optimizely.isFeatureEnabled(FEATURE_FLAG_MULTI_VARIATE_FEATURE.getKey(), "userId"));

    }

    /**
     * Verify that the {@link Optimizely#activate(String, String, Map<String, String>)} call
     * correctly builds an endpoint url and request params
     * and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
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
        if (datafileVersion >= 4) {
            testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        }
        else {
            testUserAttributes.put("browser_type", "chrome");
        }

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
     * Verify that {@link Optimizely#activate(String, String, Map<String, String>)} handles the case
     * where an unknown attribute (i.e., not in the config) is passed through.
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
        if (datafileVersion >= 4) {
            testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        }
        else {
            testUserAttributes.put("browser_type", "chrome");
        }
        testUserAttributes.put("unknownAttribute", "dimValue");

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
                eq("userId"), anyMapOf(String.class, String.class)))
                .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, "userId"))
                .thenReturn(bucketedVariation);

        logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"" +
                activatedExperiment.getKey() + "\".");
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
        Experiment inactiveExperiment;
        if (datafileVersion == 4) {
            inactiveExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_PAUSED_EXPERIMENT_KEY);
        }
        else {
            inactiveExperiment = validProjectConfig.getExperiments().get(1);
        }

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build();

        logbackVerifier.expectMessage(Level.INFO, "Experiment \"" + inactiveExperiment.getKey() +
                "\" is not running.");
        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"" +
                inactiveExperiment.getKey() + "\".");

        Variation variation = optimizely.activate(inactiveExperiment.getKey(), "userId");

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
        Experiment experimentToCheck;
        if (datafileVersion == 4) {
            experimentToCheck = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
        }
        else {
            experimentToCheck = validProjectConfig.getExperiments().get(0);
        }

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "firefox");

        logbackVerifier.expectMessage(Level.INFO,
                "User \"userId\" does not meet conditions to be in experiment \"" +
                        experimentToCheck.getKey() + "\".");
        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"" +
                experimentToCheck.getKey() + "\".");

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
        Experiment experiment;
        if (datafileVersion == 4) {
            experiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
        }
        else {
            experiment = validProjectConfig.getExperiments().get(0);
        }

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .build();

        logbackVerifier.expectMessage(Level.INFO,
                "User \"userId\" does not meet conditions to be in experiment \"" + experiment.getKey() + "\".");
        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"" +
                experiment.getKey() + "\".");

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

        Map<String, String> attributes = new HashMap<String, String>();
        if (datafileVersion == 4) {
            attributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        }
        else {
            attributes.put("browser_type", "chrome");
        }

        when(mockBucketer.bucket(experiment, "user")).thenReturn(variation);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .withBucketing(mockBucketer)
                .build();

        assertThat(optimizely.activate(experiment.getKey(), "user", attributes),
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
        Experiment experiment;
        String whitelistedUserId;
        Variation expectedVariation;
        if (datafileVersion == 4) {
            experiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
            whitelistedUserId = MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED;
            expectedVariation = experiment.getVariationKeyToVariationMap().get(VARIATION_MULTIVARIATE_EXPERIMENT_GRED_KEY);
        }
        else {
            experiment = validProjectConfig.getExperiments().get(0);
            whitelistedUserId = "testUser1";
            expectedVariation = experiment.getVariations().get(0);
        }

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build();

        logbackVerifier.expectMessage(Level.INFO, "User \"" + whitelistedUserId + "\" is forced in variation \"" +
                expectedVariation.getKey() + "\".");
        // no attributes provided for a experiment that has an audience
        assertTrue(experiment.getUserIdToVariationKeyMap().containsKey(whitelistedUserId));
        assertThat(optimizely.activate(experiment.getKey(), whitelistedUserId), is(expectedVariation));
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} gives precedence to experiment status over forced
     * variation bucketing.
     */
    @Test
    public void activateExperimentStatusPrecedesForcedVariation() throws Exception {
        Experiment experiment;
        String whitelistedUserId;
        if (datafileVersion == 4) {
            experiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_PAUSED_EXPERIMENT_KEY);
            whitelistedUserId = PAUSED_EXPERIMENT_FORCED_VARIATION_USER_ID_CONTROL;
        }
        else {
            experiment = validProjectConfig.getExperiments().get(1);
            whitelistedUserId = "testUser3";
        }

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build();

        logbackVerifier.expectMessage(Level.INFO, "Experiment \"" + experiment.getKey() + "\" is not running.");
        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"" + whitelistedUserId +
                "\" for experiment \"" + experiment.getKey() + "\".");
        // testUser3 has a corresponding forced variation, but experiment status should be checked first
        assertTrue(experiment.getUserIdToVariationKeyMap().containsKey(whitelistedUserId));
        assertNull(optimizely.activate(experiment.getKey(), whitelistedUserId));
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
        Experiment launchedExperiment;
        if (datafileVersion == 4) {
            launchedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_LAUNCHED_EXPERIMENT_KEY);
        }
        else {
            launchedExperiment = noAudienceProjectConfig.getExperiments().get(2);
        }

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
    public void trackEventEndToEndForced() throws Exception {
        EventType eventType;
        String datafile;
        ProjectConfig config;
        if (datafileVersion >= 4) {
            config = spy(validProjectConfig);
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
            datafile = validDatafile;
        }
        else {
            config = spy(noAudienceProjectConfig);
            eventType = noAudienceProjectConfig.getEventTypes().get(0);
            datafile = noAudienceDatafile;
        }
        List<Experiment> allExperiments = new ArrayList<Experiment>();
        allExperiments.add(config.getExperiments().get(0));
        EventBuilder eventBuilderV2 = new EventBuilderV2();
        DecisionService spyDecisionService = spy(new DecisionService(mockBucketer,
                mockErrorHandler,
                config,
                null));

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
                .withDecisionService(spyDecisionService)
                .withEventBuilder(eventBuilderV2)
                .withConfig(noAudienceProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        // Bucket to null for all experiments. However, only a subset of the experiments will actually
        // call the bucket function.
        for (Experiment experiment : allExperiments) {
            when(mockBucketer.bucket(experiment, "userId"))
                    .thenReturn(null);
        }
        // Force to the first variation for all experiments. However, only a subset of the experiments will actually
        // call get forced.
        for (Experiment experiment : allExperiments) {
            optimizely.projectConfig.setForcedVariation(experiment.getKey(),
                    "userId", experiment.getVariations().get(0).getKey());
        }

        // call track
        optimizely.track(eventType.getKey(), "userId");

        // verify that the bucketing algorithm was called only on experiments corresponding to the specified goal.
        List<Experiment> experimentsForEvent = config.getExperimentsForEventKey(eventType.getKey());
        for (Experiment experiment : allExperiments) {
            if (experiment.isRunning() && experimentsForEvent.contains(experiment)) {
                verify(spyDecisionService).getVariation(experiment, "userId",
                        Collections.<String, String>emptyMap());
                verify(config).getForcedVariation(experiment.getKey(), "userId");
            } else {
                verify(spyDecisionService, never()).getVariation(experiment, "userId",
                        Collections.<String, String>emptyMap());
            }
        }

        // verify that dispatchEvent was called
        verify(mockEventHandler).dispatchEvent(any(LogEvent.class));

        for (Experiment experiment : allExperiments) {
            assertEquals(optimizely.projectConfig.getForcedVariation(experiment.getKey(), "userId"), experiment.getVariations().get(0));
            optimizely.projectConfig.setForcedVariation(experiment.getKey(), "userId", null);
            assertNull(optimizely.projectConfig.getForcedVariation(experiment.getKey(), "userId"));
        }

    }

    /**
     * Verify that the {@link Optimizely#track(String, String)} call correctly builds a V2 event and passes it
     * through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void trackEventEndToEnd() throws Exception {
        EventType eventType;
        String datafile;
        ProjectConfig config;
        if (datafileVersion >= 4) {
            config = spy(validProjectConfig);
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
            datafile = validDatafile;
        }
        else {
            config = spy(noAudienceProjectConfig);
            eventType = noAudienceProjectConfig.getEventTypes().get(0);
            datafile = noAudienceDatafile;
        }
        List<Experiment> allExperiments = config.getExperiments();

        EventBuilder eventBuilderV2 = new EventBuilderV2();
        DecisionService spyDecisionService = spy(new DecisionService(mockBucketer,
                mockErrorHandler,
                config,
                null));

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
                .withDecisionService(spyDecisionService)
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
        List<Experiment> experimentsForEvent = config.getExperimentsForEventKey(eventType.getKey());
        for (Experiment experiment : allExperiments) {
            if (experiment.isRunning() && experimentsForEvent.contains(experiment)) {
                verify(spyDecisionService).getVariation(experiment, "userId",
                        Collections.<String, String>emptyMap());
                verify(config).getForcedVariation(experiment.getKey(), "userId");
            } else {
                verify(spyDecisionService, never()).getVariation(experiment, "userId",
                        Collections.<String, String>emptyMap());
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
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        }
        else {
            eventType = validProjectConfig.getEventTypes().get(0);
        }

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
                mockDecisionService,
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

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
                "\" for user \"" + genericUserId + "\".");
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
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        }
        else {
            eventType = validProjectConfig.getEventTypes().get(0);
        }

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
                mockDecisionService,
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
                eq(Collections.<String, Object>emptyMap())))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
                "\" for user \"" + genericUserId + "\".");
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
                eq(validProjectConfig),
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
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        }
        else {
            eventType = validProjectConfig.getEventTypes().get(0);
        }

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
                mockDecisionService,
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
                eq(Collections.<String, Object>emptyMap())))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
                "\" for user \"" + genericUserId + "\".");
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
                eq(validProjectConfig),
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
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        }
        else {
            eventType = validProjectConfig.getEventTypes().get(0);
        }

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
                mockDecisionService,
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

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
                "\" for user \"" + genericUserId + "\".");
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
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        }
        else {
            eventType = validProjectConfig.getEventTypes().get(0);
        }

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
                mockDecisionService,
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

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() + "\" for user \""
                + genericUserId + "\".");
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
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        }
        else {
            eventType = validProjectConfig.getEventTypes().get(0);
        }

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
                mockDecisionService,
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

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
                "\" for user \"" + genericUserId + "\".");
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
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        }
        else {
            eventType = validProjectConfig.getEventNameMapping().get("clicked_purchase");
        }

        when(mockDecisionService.getVariation(any(Experiment.class), any(String.class), anyMapOf(String.class, String.class)))
                .thenReturn(null);
        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withDecisionService(mockDecisionService)
                .build();

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("browser_type", "firefox");

        logbackVerifier.expectMessage(Level.INFO,
                "There are no valid experiments for event \"" + eventType.getKey() + "\" to track.");
        logbackVerifier.expectMessage(Level.INFO, "Not tracking event \"" + eventType.getKey() +
                "\" for user \"userId\".");
        optimizely.track(eventType.getKey(), "userId", attributes);

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
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_LAUNCHED_EXPERIMENT_ONLY_KEY);
        }
        else {
            eventType = noAudienceProjectConfig.getEventNameMapping().get("launched_exp_event");
        }
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
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        }
        else {
            eventType = noAudienceProjectConfig.getEventNameMapping().get("event_with_launched_and_running_experiments");
        }
        Bucketer mockBucketAlgorithm = mock(Bucketer.class);
        for (Experiment experiment : validProjectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, genericUserId))
                    .thenReturn(experiment.getVariations().get(0));
        }

        Optimizely client = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(noAudienceProjectConfig)
                .withBucketing(mockBucketAlgorithm)
                .withEventBuilder(mockEventBuilder)
                .build();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                noAudienceProjectConfig,
                client.decisionService,
                eventType.getKey(),
                genericUserId,
                Collections.<String, String>emptyMap());

        // Create an Argument Captor to ensure we are creating a correct experiment variation map
        ArgumentCaptor<Map> experimentVariationMapCaptor = ArgumentCaptor.forClass(Map.class);

        LogEvent conversionEvent = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createConversionEvent(
                eq(noAudienceProjectConfig),
                experimentVariationMapCaptor.capture(),
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

        // Check the argument captor got the correct arguments
        Map<Experiment, Variation> actualExperimentVariationMap = experimentVariationMapCaptor.getValue();
        assertEquals(experimentVariationMap, actualExperimentVariationMap);
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
        Experiment experiment;
        if (datafileVersion >= 4) {
            experiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
        }
        else {
            experiment = validProjectConfig.getExperiments().get(0);
        }

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withErrorHandler(mockErrorHandler)
                .build();

        logbackVerifier.expectMessage(Level.INFO,
                "User \"userId\" does not meet conditions to be in experiment \"" + experiment.getKey() + "\".");

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

        Map<String, String> attributes = new HashMap<String, String>();
        if (datafileVersion >= 4) {
            attributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        }
        else {
            attributes.put("browser_type", "chrome");
        }

        when(mockBucketer.bucket(experiment, "user")).thenReturn(variation);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .withBucketing(mockBucketer)
                .build();

        assertThat(optimizely.getVariation(experiment.getKey(), "user", attributes),
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
        Experiment experiment;
        if (datafileVersion >= 4) {
            experiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_PAUSED_EXPERIMENT_KEY);
        }
        else {
            experiment = validProjectConfig.getExperiments().get(1);
        }

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build();

        logbackVerifier.expectMessage(Level.INFO, "Experiment \"" + experiment.getKey() + "\" is not running.");
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
        Experiment activatedExperiment;
        EventType eventType;
        if (datafileVersion >= 4) {
            activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_BASIC_EXPERIMENT_KEY);
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        }
        else {
            activatedExperiment = validProjectConfig.getExperiments().get(0);
            eventType = validProjectConfig.getEventTypes().get(0);
        }
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventBuilder mockEventBuilder = mock(EventBuilder.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withDecisionService(mockDecisionService)
                .withEventBuilder(mockEventBuilder)
                .withConfig(validProjectConfig)
                .withErrorHandler(mockErrorHandler)
                .build();

        Map<String, String> attributes = Collections.emptyMap();

        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, "");
        when(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment,
                bucketedVariation, genericUserId, attributes))
                .thenReturn(logEventToDispatch);

        when(mockDecisionService.getVariation(
                eq(activatedExperiment),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap())))
                .thenReturn(bucketedVariation);

        // Add listener
        NotificationListener listener = mock(NotificationListener.class);
        optimizely.addNotificationListener(listener);

        // Check if listener is notified when experiment is activated
        Variation actualVariation = optimizely.activate(activatedExperiment, genericUserId, attributes);
        verify(listener, times(1))
                .onExperimentActivated(activatedExperiment, genericUserId, attributes, actualVariation);

        // Check if listener is notified after an event is tracked
        String eventKey = eventType.getKey();

        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                mockDecisionService,
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

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

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

        // Check if listener is notified after a live variable is accessed
        boolean activateExperiment = true;
        verify(listener, never())
                .onExperimentActivated(activatedExperiment, genericUserId, attributes, actualVariation);

        // Check if listener is notified after an event is tracked
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String eventKey = eventType.getKey();

        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                mockDecisionService,
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
        Experiment activatedExperiment;
        Map<String, String> attributes = new HashMap<String, String>();
        if (datafileVersion >= 4) {
            activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
            attributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        }
        else {
            activatedExperiment = validProjectConfig.getExperiments().get(0);
            attributes.put("browser_type", "chrome");
        }
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
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
        when(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment,
                bucketedVariation, genericUserId, attributes))
                .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, genericUserId))
                .thenReturn(bucketedVariation);

        // set up argument captor for the attributes map to compare map equality
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        when(mockEventBuilder.createImpressionEvent(
                eq(validProjectConfig),
                eq(activatedExperiment),
                eq(bucketedVariation),
                eq(genericUserId),
                attributeCaptor.capture()
        )).thenReturn(logEventToDispatch);

        NotificationListener listener = mock(NotificationListener.class);
        optimizely.addNotificationListener(listener);
        optimizely.clearNotificationListeners();

        // Check if listener is notified after an experiment is activated
        Variation actualVariation = optimizely.activate(activatedExperiment, genericUserId, attributes);

        // check that the argument that was captured by the mockEventBuilder attribute captor,
        // was equal to the attributes passed in to activate
        assertEquals(attributes, attributeCaptor.getValue());
        verify(listener, never())
                .onExperimentActivated(activatedExperiment, genericUserId, attributes, actualVariation);

        // Check if listener is notified after a live variable is accessed
        boolean activateExperiment = true;
        verify(listener, never())
                .onExperimentActivated(activatedExperiment, genericUserId, attributes, actualVariation);

        // Check if listener is notified after a event is tracked
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String eventKey = eventType.getKey();

        Map<Experiment, Variation> experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                mockDecisionService,
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

    //======== Feature Accessor Tests ========//

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, LiveVariable.VariableType)}
     * returns null and logs a message
     * when it is called with a feature key that has no corresponding feature in the datafile.
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableValueForTypeReturnsNullWhenFeatureNotFound() throws ConfigParseException {

        String invalidFeatureKey = "nonexistent feature key";
        String invalidVariableKey = "nonexistent variable key";
        Map<String, String> attributes = Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .withDecisionService(mockDecisionService)
                .build();

        String value = optimizely.getFeatureVariableValueForType(
                invalidFeatureKey,
                invalidVariableKey,
                genericUserId,
                Collections.<String, String>emptyMap(),
                LiveVariable.VariableType.STRING);
        assertNull(value);

        value = optimizely.getFeatureVariableString(invalidFeatureKey, invalidVariableKey, genericUserId, attributes);
        assertNull(value);

        logbackVerifier.expectMessage(Level.INFO,
                "No feature flag was found for key \"" + invalidFeatureKey + "\".",
                times(2));

        verify(mockDecisionService, never()).getVariation(
                any(Experiment.class),
                anyString(),
                anyMapOf(String.class, String.class));
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, LiveVariable.VariableType)}
     * returns null and logs a message
     * when the feature key is valid, but no variable could be found for the variable key in the feature.
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableValueForTypeReturnsNullWhenVariableNotFoundInValidFeature() throws ConfigParseException {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        String invalidVariableKey = "nonexistent variable key";

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .withDecisionService(mockDecisionService)
                .build();

        String value = optimizely.getFeatureVariableValueForType(
                validFeatureKey,
                invalidVariableKey,
                genericUserId,
                Collections.<String, String>emptyMap(),
                LiveVariable.VariableType.STRING);
        assertNull(value);

        logbackVerifier.expectMessage(Level.INFO,
                "No feature variable was found for key \"" + invalidVariableKey + "\" in feature flag \"" +
                validFeatureKey + "\".");

        verify(mockDecisionService, never()).getVariation(
                any(Experiment.class),
                anyString(),
                anyMapOf(String.class, String.class)
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, LiveVariable.VariableType)}
     * returns null when the variable's type does not match the type with which it was attempted to be accessed.
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableValueReturnsNullWhenVariableTypeDoesNotMatch() throws ConfigParseException {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        String validVariableKey = VARIABLE_FIRST_LETTER_KEY;

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .withDecisionService(mockDecisionService)
                .build();

        String value = optimizely.getFeatureVariableValueForType(
                validFeatureKey,
                validVariableKey,
                genericUserId,
                Collections.<String, String>emptyMap(),
                LiveVariable.VariableType.INTEGER
        );
        assertNull(value);

        logbackVerifier.expectMessage(
                Level.INFO,
                "The feature variable \"" + validVariableKey +
                        "\" is actually of type \"" + LiveVariable.VariableType.STRING.toString() +
                        "\" type. You tried to access it as type \"" + LiveVariable.VariableType.INTEGER.toString() +
                        "\". Please use the appropriate feature variable accessor."
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, LiveVariable.VariableType)}
     * returns the String default value of a live variable
     * when the feature is not attached to an experiment or a rollout.
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableValueForTypeReturnsDefaultValueWhenFeatureIsNotAttachedToExperimentOrRollout() throws ConfigParseException {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_SINGLE_VARIABLE_BOOLEAN_KEY;
        String validVariableKey = VARIABLE_BOOLEAN_VARIABLE_KEY;
        String defaultValue = VARIABLE_BOOLEAN_VARIABLE_DEFAULT_VALUE;
        Map<String, String> attributes = Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build();

        String value = optimizely.getFeatureVariableValueForType(
                validFeatureKey,
                validVariableKey,
                genericUserId,
                attributes,
                LiveVariable.VariableType.BOOLEAN);
        assertEquals(defaultValue, value);

        logbackVerifier.expectMessage(
                Level.INFO,
                "The feature flag \"" + validFeatureKey + "\" is not used in any experiments."
        );
        logbackVerifier.expectMessage(
                Level.INFO,
                "The feature flag \"" + validFeatureKey + "\" is not used in a rollout."
        );
        logbackVerifier.expectMessage(
                Level.INFO,
                "User \"" + genericUserId + "\" was not bucketed into any variation for feature flag \"" +
                        validFeatureKey + "\". The default value \"" +
                        defaultValue + "\" for \"" +
                        validVariableKey + "\" is being returned."
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, LiveVariable.VariableType)}
     * returns the String default value for a live variable
     * when the feature is attached to an experiment and no rollout, but the user is excluded from the experiment.
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableValueReturnsDefaultValueWhenFeatureIsAttachedToOneExperimentButFailsTargeting() throws ConfigParseException {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_SINGLE_VARIABLE_DOUBLE_KEY;
        String validVariableKey = VARIABLE_DOUBLE_VARIABLE_KEY;
        String expectedValue = VARIABLE_DOUBLE_DEFAULT_VALUE;
        FeatureFlag featureFlag = FEATURE_FLAG_SINGLE_VARIABLE_DOUBLE;
        Experiment experiment = validProjectConfig.getExperimentIdMapping().get(featureFlag.getExperimentIds().get(0));

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build();

        String valueWithImproperAttributes = optimizely.getFeatureVariableValueForType(
                validFeatureKey,
                validVariableKey,
                genericUserId,
                Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, "Ravenclaw"),
                LiveVariable.VariableType.DOUBLE
        );
        assertEquals(expectedValue, valueWithImproperAttributes);

        logbackVerifier.expectMessage(
                Level.INFO,
                "User \"" + genericUserId + "\" does not meet conditions to be in experiment \"" +
                        experiment.getKey() + "\"."
        );
        logbackVerifier.expectMessage(
                Level.INFO,
                "The feature flag \"" + validFeatureKey + "\" is not used in a rollout."
        );
        logbackVerifier.expectMessage(
                Level.INFO,
                "User \"" + genericUserId +
                        "\" was not bucketed into any variation for feature flag \"" + validFeatureKey +
                        "\". The default value \"" + expectedValue +
                        "\" for \"" + validVariableKey + "\" is being returned."
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, LiveVariable.VariableType)}
     * returns the variable value of the variation the user is bucketed into
     * if the variation is not null and the variable has a usage within the variation.
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableValueReturnsVariationValueWhenUserGetsBucketedToVariation() throws ConfigParseException {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        String validVariableKey = VARIABLE_FIRST_LETTER_KEY;
        LiveVariable variable = FEATURE_FLAG_MULTI_VARIATE_FEATURE.getVariableKeyToLiveVariableMap().get(validVariableKey);
        String expectedValue = VARIATION_MULTIVARIATE_EXPERIMENT_GRED.getVariableIdToLiveVariableUsageInstanceMap().get(variable.getId()).getValue();

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .withDecisionService(mockDecisionService)
                .build();

        doReturn(VARIATION_MULTIVARIATE_EXPERIMENT_GRED).when(mockDecisionService).getVariationForFeature(
                FEATURE_FLAG_MULTI_VARIATE_FEATURE,
                genericUserId,
                Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)
        );

        String value = optimizely.getFeatureVariableValueForType(
                validFeatureKey,
                validVariableKey,
                genericUserId,
                Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE),
                LiveVariable.VariableType.STRING
        );

        assertEquals(expectedValue, value);
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, LiveVariable.VariableType)}
     * returns the default value for the feature variable
     * when there is no variable usage present for the variation the user is bucketed into.
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableValueReturnsDefaultValueWhenNoVariationUsageIsPresent() throws ConfigParseException {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_SINGLE_VARIABLE_INTEGER_KEY;
        String validVariableKey = VARIABLE_INTEGER_VARIABLE_KEY;
        LiveVariable variable = FEATURE_FLAG_SINGLE_VARIABLE_INTEGER.getVariableKeyToLiveVariableMap().get(validVariableKey);
        String expectedValue = variable.getDefaultValue();

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build();

        String value = optimizely.getFeatureVariableValueForType(
                validFeatureKey,
                validVariableKey,
                genericUserId,
                Collections.<String, String>emptyMap(),
                LiveVariable.VariableType.INTEGER
        );

        assertEquals(expectedValue, value);
    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} and they both
     * return False
     * when the APIs are called with an feature key that is not in the datafile.
     * @throws Exception
     */
    @Test
    public void isFeatureEnabledReturnsFalseWhenFeatureFlagKeyIsInvalid() throws Exception {

        String invalidFeatureKey = "nonexistent feature key";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .withDecisionService(mockDecisionService)
                .build());

        assertFalse(spyOptimizely.isFeatureEnabled(invalidFeatureKey, genericUserId));

        logbackVerifier.expectMessage(
                Level.INFO,
                "No feature flag was found for key \"" + invalidFeatureKey + "\"."
        );
        verify(spyOptimizely, times(1)).isFeatureEnabled(
                eq(invalidFeatureKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap())
        );
        verify(mockDecisionService, never()).getVariation(
                any(Experiment.class),
                anyString(),
                anyMapOf(String.class, String.class));
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} and they both
     * return False
     * when the user is not bucketed into any variation for the feature.
     * @throws Exception
     */
    @Test
    public void isFeatureEnabledReturnsFalseWhenUserIsNotBucketedIntoAnyVariation() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .withDecisionService(mockDecisionService)
                .build());

        doReturn(null).when(mockDecisionService).getVariationForFeature(
                any(FeatureFlag.class),
                anyString(),
                anyMapOf(String.class, String.class)
        );

        assertFalse(spyOptimizely.isFeatureEnabled(validFeatureKey, genericUserId));

        logbackVerifier.expectMessage(
                Level.INFO,
                "Feature \"" + validFeatureKey +
                        "\" is not enabled for user \"" + genericUserId + "\"."
        );
        verify(spyOptimizely).isFeatureEnabled(
                eq(validFeatureKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap())
        );
        verify(mockDecisionService).getVariationForFeature(
                eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap())
        );
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} and they both
     * return True
     * when the user is bucketed into a variation for the feature.
     * An impression event should not be dispatched since the user was not bucketed into an Experiment.
     * @throws Exception
     */
    @Test
    public void isFeatureEnabledReturnsTrueButDoesNotSendWhenUserIsBucketedIntoVariationWithoutExperiment() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .withDecisionService(mockDecisionService)
                .build());

        doReturn(new Variation("variationId", "variationKey")).when(mockDecisionService).getVariationForFeature(
                eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap())
        );

        assertTrue(spyOptimizely.isFeatureEnabled(validFeatureKey, genericUserId));

        logbackVerifier.expectMessage(
                Level.INFO,
                "The user \"" + genericUserId +
                        "\" is not being experimented on in feature \"" + validFeatureKey + "\"."
        );
        logbackVerifier.expectMessage(
                Level.INFO,
                "Feature \"" + validFeatureKey +
                        "\" is enabled for user \"" + genericUserId + "\"."
        );
        verify(spyOptimizely).isFeatureEnabled(
                eq(validFeatureKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap())
        );
        verify(mockDecisionService).getVariationForFeature(
                eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap())
        );
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /** Integration Test
     * Verify {@link Optimizely#isFeatureEnabled(String, String, Map)}
     * returns True
     * when the user is bucketed into a variation for the feature.
     * The user is also bucketed into an experiment, so we verify that an event is dispatched.
     * @throws Exception
     */
    @Test
    public void isFeatureEnabledReturnsTrueAndDispatchesEventWhenUserIsBucketedIntoAnExperiment() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build();

        assertTrue(optimizely.isFeatureEnabled(
                validFeatureKey,
                genericUserId,
                Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)
        ));

        logbackVerifier.expectMessage(
                Level.INFO,
                "Feature \"" + validFeatureKey +
                        "\" is enabled for user \"" + genericUserId + "\"."
        );
        verify(mockEventHandler, times(1)).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableString(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableString(String, String, String, Map<String, String>)}
     * and returns null
     * when {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, LiveVariable.VariableType)}
     * returns null
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableStringReturnsNullFromInternal() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build());

        doReturn(null).when(spyOptimizely).getFeatureVariableValueForType(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap()),
                eq(LiveVariable.VariableType.STRING)
        );

        assertNull(spyOptimizely.getFeatureVariableString(
                featureKey,
                variableKey,
                genericUserId
        ));

        verify(spyOptimizely).getFeatureVariableString(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap())
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableString(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableString(String, String, String, Map)}
     * and both return the value returned from
     * {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, LiveVariable.VariableType)}.
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableStringReturnsWhatInternalReturns() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        String valueNoAttributes = "valueNoAttributes";
        String valueWithAttributes = "valueWithAttributes";
        Map<String, String> attributes = Collections.singletonMap("key", "value");

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build());


        doReturn(valueNoAttributes).when(spyOptimizely).getFeatureVariableValueForType(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap()),
                eq(LiveVariable.VariableType.STRING)
        );

        doReturn(valueWithAttributes).when(spyOptimizely).getFeatureVariableValueForType(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(attributes),
                eq(LiveVariable.VariableType.STRING)
        );

        assertEquals(valueNoAttributes, spyOptimizely.getFeatureVariableString(
                featureKey,
                variableKey,
                genericUserId
        ));

        verify(spyOptimizely).getFeatureVariableString(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap())
        );

        assertEquals(valueWithAttributes, spyOptimizely.getFeatureVariableString(
                featureKey,
                variableKey,
                genericUserId,
                attributes
        ));
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableBoolean(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableBoolean(String, String, String, Map<String, String>)}
     * and returns null
     * when {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, LiveVariable.VariableType)}
     * returns null
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableBooleanReturnsNullFromInternal() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build());

        doReturn(null).when(spyOptimizely).getFeatureVariableValueForType(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap()),
                eq(LiveVariable.VariableType.BOOLEAN)
        );

        assertNull(spyOptimizely.getFeatureVariableBoolean(
                featureKey,
                variableKey,
                genericUserId
        ));

        verify(spyOptimizely).getFeatureVariableBoolean(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap())
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableBoolean(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableBoolean(String, String, String, Map)}
     * and both return a Boolean representation of the value returned from
     * {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, LiveVariable.VariableType)}.
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableBooleanReturnsWhatInternalReturns() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        Boolean valueNoAttributes = false;
        Boolean valueWithAttributes = true;
        Map<String, String> attributes = Collections.singletonMap("key", "value");

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build());


        doReturn(valueNoAttributes.toString()).when(spyOptimizely).getFeatureVariableValueForType(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap()),
                eq(LiveVariable.VariableType.BOOLEAN)
        );

        doReturn(valueWithAttributes.toString()).when(spyOptimizely).getFeatureVariableValueForType(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(attributes),
                eq(LiveVariable.VariableType.BOOLEAN)
        );

        assertEquals(valueNoAttributes, spyOptimizely.getFeatureVariableBoolean(
                featureKey,
                variableKey,
                genericUserId
        ));

        verify(spyOptimizely).getFeatureVariableBoolean(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap())
        );

        assertEquals(valueWithAttributes, spyOptimizely.getFeatureVariableBoolean(
                featureKey,
                variableKey,
                genericUserId,
                attributes
        ));
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableDouble(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableDouble(String, String, String, Map<String, String>)}
     * and returns null
     * when {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, LiveVariable.VariableType)}
     * returns null
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableDoubleReturnsNullFromInternal() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build());

        doReturn(null).when(spyOptimizely).getFeatureVariableValueForType(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap()),
                eq(LiveVariable.VariableType.DOUBLE)
        );

        assertNull(spyOptimizely.getFeatureVariableDouble(
                featureKey,
                variableKey,
                genericUserId
        ));

        verify(spyOptimizely).getFeatureVariableDouble(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap())
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableDouble(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableDouble(String, String, String, Map)}
     * and both return the parsed Double from the value returned from
     * {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, LiveVariable.VariableType)}.
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableDoubleReturnsWhatInternalReturns() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        Double valueNoAttributes = 0.1;
        Double valueWithAttributes = 0.2;
        Map<String, String> attributes = Collections.singletonMap("key", "value");

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build());


        doReturn(valueNoAttributes.toString()).when(spyOptimizely).getFeatureVariableValueForType(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap()),
                eq(LiveVariable.VariableType.DOUBLE)
        );

        doReturn(valueWithAttributes.toString()).when(spyOptimizely).getFeatureVariableValueForType(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(attributes),
                eq(LiveVariable.VariableType.DOUBLE)
        );

        assertEquals(valueNoAttributes, spyOptimizely.getFeatureVariableDouble(
                featureKey,
                variableKey,
                genericUserId
        ));

        verify(spyOptimizely).getFeatureVariableDouble(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap())
        );

        assertEquals(valueWithAttributes, spyOptimizely.getFeatureVariableDouble(
                featureKey,
                variableKey,
                genericUserId,
                attributes
        ));
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableInteger(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableInteger(String, String, String, Map<String, String>)}
     * and returns null
     * when {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, LiveVariable.VariableType)}
     * returns null
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableIntegerReturnsNullFromInternal() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build());

        doReturn(null).when(spyOptimizely).getFeatureVariableValueForType(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap()),
                eq(LiveVariable.VariableType.INTEGER)
        );

        assertNull(spyOptimizely.getFeatureVariableInteger(
                featureKey,
                variableKey,
                genericUserId
        ));

        verify(spyOptimizely).getFeatureVariableInteger(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap())
        );
    }

    /**
     * Verify that {@link Optimizely#getFeatureVariableDouble(String, String, String)}
     * and {@link Optimizely#getFeatureVariableDouble(String, String, String, Map)}
     * do not throw errors when they are unable to parse the value into an Double.
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableDoubleCatchesExceptionFromParsing() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        String unParsableValue = "not_a_double";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build());

        doReturn(unParsableValue).when(spyOptimizely).getFeatureVariableValueForType(
                anyString(),
                anyString(),
                anyString(),
                anyMapOf(String.class, String.class),
                eq(LiveVariable.VariableType.DOUBLE)
        );

        assertNull(spyOptimizely.getFeatureVariableDouble(
                featureKey,
                variableKey,
                genericUserId
        ));

        logbackVerifier.expectMessage(
                Level.ERROR,
                "NumberFormatException while trying to parse \"" + unParsableValue +
                        "\" as Double. "
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableInteger(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableInteger(String, String, String, Map)}
     * and both return the parsed Integer value from the value returned from
     * {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, LiveVariable.VariableType)}.
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableIntegerReturnsWhatInternalReturns() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        Integer valueNoAttributes = 1;
        Integer valueWithAttributes = 2;
        Map<String, String> attributes = Collections.singletonMap("key", "value");

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build());


        doReturn(valueNoAttributes.toString()).when(spyOptimizely).getFeatureVariableValueForType(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap()),
                eq(LiveVariable.VariableType.INTEGER)
        );

        doReturn(valueWithAttributes.toString()).when(spyOptimizely).getFeatureVariableValueForType(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(attributes),
                eq(LiveVariable.VariableType.INTEGER)
        );

        assertEquals(valueNoAttributes, spyOptimizely.getFeatureVariableInteger(
                featureKey,
                variableKey,
                genericUserId
        ));

        verify(spyOptimizely).getFeatureVariableInteger(
                eq(featureKey),
                eq(variableKey),
                eq(genericUserId),
                eq(Collections.<String, String>emptyMap())
        );

        assertEquals(valueWithAttributes, spyOptimizely.getFeatureVariableInteger(
                featureKey,
                variableKey,
                genericUserId,
                attributes
        ));
    }

    /**
     * Verify that {@link Optimizely#getFeatureVariableInteger(String, String, String)}
     * and {@link Optimizely#getFeatureVariableInteger(String, String, String, Map)}
     * do not throw errors when they are unable to parse the value into an Integer.
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableIntegerCatchesExceptionFromParsing() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        String unParsableValue = "not_an_integer";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
                .withConfig(validProjectConfig)
                .build());

        doReturn(unParsableValue).when(spyOptimizely).getFeatureVariableValueForType(
                anyString(),
                anyString(),
                anyString(),
                anyMapOf(String.class, String.class),
                eq(LiveVariable.VariableType.INTEGER)
        );

        assertNull(spyOptimizely.getFeatureVariableInteger(
                featureKey,
                variableKey,
                genericUserId
        ));

        logbackVerifier.expectMessage(
                Level.ERROR,
                "NumberFormatException while trying to parse \"" + unParsableValue +
                        "\" as Integer. "
        );
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
