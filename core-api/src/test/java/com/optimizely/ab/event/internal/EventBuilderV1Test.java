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

import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.ProjectConfigTestUtils;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.internal.ProjectValidationUtils;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link EventBuilderV1}.
 */
public class EventBuilderV1Test {

    /**
     * Verify the positive case of creating an impression event.
     */
    @Test
    public void createImpressionParams() throws Exception {
        EventBuilderV1 builder = new EventBuilderV1();

        // use the "valid" project config and its associated experiment, variation, and attributes
        ProjectConfig projectConfig = ProjectConfigTestUtils.validProjectConfigV1();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = projectConfig.getAttributes().get(0);
        String userId = "userId";

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");
        LogEvent impressionEvent = builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation,
                                                                 userId, attributeMap);
        Map<String, String> requestParams = impressionEvent.getRequestParams();

        assertThat(requestParams.size(), is(9));

        verifyCommonRequestParams(projectConfig, requestParams, userId, attribute);

        // verify goal includes activated experiment id and event name
        assertThat(requestParams, hasEntry("g", activatedExperiment.getId()));

        // verify the experiment -> variation mapping
        assertThat(requestParams, hasEntry("x" + activatedExperiment.getId(), bucketedVariation.getId()));

        // verify the time sent value is close to the current time (within 60s)
        double unixTimeInSec = Double.parseDouble(requestParams.get("time"));
        assertThat(unixTimeInSec, closeTo((double)System.currentTimeMillis() / 1000, 60.0));
    }

    /**
     * Verify the positive case of creating a conversion event.
     */
    @Test
    public void createConversionParams() throws Exception {
        EventBuilderV1 builder = new EventBuilderV1();

        // use the "valid" project config and its associated experiment, variation, and attributes
        ProjectConfig projectConfig = ProjectConfigTestUtils.validProjectConfigV1();
        Attribute attribute = projectConfig.getAttributes().get(0);
        EventType eventType = projectConfig.getEventTypes().get(0);
        String userId = "userId";

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);

        List<Experiment> allExperiments = projectConfig.getExperiments();
        List<String> experimentIds = projectConfig.getExperimentIdsForGoal(eventType.getKey());

        // Bucket to the first variation for all experiments. However, only a subset of the experiments will actually
        // call the bucket function.
        for (Experiment experiment : allExperiments) {
            when(mockBucketAlgorithm.bucket(experiment, userId))
                .thenReturn(experiment.getVariations().get(0));
        }

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");
        LogEvent conversionEvent = builder.createConversionEvent(projectConfig, mockBucketAlgorithm,
                                                                 userId, eventType.getId(),
                                                                 eventType.getKey(), attributeMap);
        Map<String, String> requestParams = conversionEvent.getRequestParams();

        for (Experiment experiment : allExperiments) {
            if (experimentIds.contains(experiment.getId()) &&
                    ProjectValidationUtils.validatePreconditions(projectConfig, experiment, userId, attributeMap)) {
                verify(mockBucketAlgorithm).bucket(experiment, userId);
            } else {
                verify(mockBucketAlgorithm, never()).bucket(experiment, userId);
            }
        }

        assertThat(requestParams.size(), is(10));

        verifyCommonRequestParams(projectConfig, requestParams, userId, attribute);

        // verify event id gets passed through in the goal param
        assertThat(requestParams, hasEntry("g", eventType.getId()));

        // verify the event name param
        assertThat(requestParams, hasEntry("n", eventType.getKey()));

        verifyExperimentBucketMap(requestParams, allExperiments, experimentIds, projectConfig, userId, attributeMap);

        // verify the time sent value is close to the current time (within 60s)
        double unixTimeInSec = Double.parseDouble(requestParams.get("time"));
        assertThat(unixTimeInSec, closeTo((double)System.currentTimeMillis() / 1000, 60.0));
    }

    @Test
    public void createConversionParamsWithRevenue() throws Exception {
        EventBuilderV1 builder = new EventBuilderV1();
        long revenue = 1234L;

        // use the "valid" project config and its associated experiment, variation, and attributes
        ProjectConfig projectConfig = ProjectConfigTestUtils.validProjectConfigV1();
        Attribute attribute = projectConfig.getAttributes().get(0);
        EventType eventType = projectConfig.getEventTypes().get(0);

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);

        // Bucket to the first variation for all experiments.
        for (Experiment experiment : projectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, "userId"))
                .thenReturn(experiment.getVariations().get(0));
        }

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");
        LogEvent conversionEvent = builder.createConversionEvent(projectConfig, mockBucketAlgorithm, "userId",
                                                                 eventType.getId(), eventType.getKey(), attributeMap,
                                                                 revenue);
        Map<String, String> requestParams = conversionEvent.getRequestParams();

        // we're not going to verify everything, just revenue and the associated goals
        assertThat(requestParams, hasEntry("v", Long.toString(revenue)));

        // verify that we have both the triggered goal and the default, "total revenue" goal
        List<String> goalIds = Arrays.asList(requestParams.get("g").split(","));
        assertThat(goalIds, contains(eventType.getId(),
                                     projectConfig.getEventNameMapping().get(EventType.TOTAL_REVENUE_GOAL_KEY).getId()));
    }

    /**
     * Verify that an experiment isn't added to the experiment bucket map if a user doesn't satisfy audience
     * conditions for it.
     */
    @Test
    public void createConversionParamsUserNotInAudience() throws Exception {
        EventBuilderV1 builder = new EventBuilderV1();

        ProjectConfig projectConfig = ProjectConfigTestUtils.validProjectConfigV1();
        Attribute attribute = projectConfig.getAttributes().get(0);
        EventType eventType = projectConfig.getEventTypes().get(2);
        String userId = "userId";

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);

        // Bucket to the first variation for all experiments.
        for (Experiment experiment : projectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, userId))
                .thenReturn(experiment.getVariations().get(0));
        }

        // the audience for the experiments is "NOT firefox" so this user shouldn't satisfy audience conditions
        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "firefox");
        LogEvent conversionEvent = builder.createConversionEvent(projectConfig, mockBucketAlgorithm, userId,
                                                                 eventType.getId(), eventType.getKey(), attributeMap);

        assertNull(conversionEvent);
    }

    /**
     * Verify that passing through an unknown attribute causes that attribute to be ignored, rather than
     * causing an exception to be thrown.
     */
    @Test
    public void createImpressionParamsIgnoresUnknownAttributes() throws Exception {
        EventBuilderV1 builder = new EventBuilderV1();

        // use the "valid" project config and its associated experiment, variation, and attributes
        ProjectConfig projectConfig = ProjectConfigTestUtils.validProjectConfigV1();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        LogEvent impressionEvent =
            builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation, "userId",
                                          Collections.singletonMap("unknownAttribute", "blahValue"));

        // verify that no request param has the value, "blahValue"
        assertThat(impressionEvent.getRequestParams(), not(hasValue("blahValue")));
    }

    /**
     * Verify that precedence is given to forced variation bucketing over audience evaluation when constructing the
     * experiment bucket map.
     */
    @Test
    public void createConversionParamsForcedVariationBucketingPrecedesAudienceEval() {
        EventBuilderV1 builder = new EventBuilderV1();

        ProjectConfig projectConfig = ProjectConfigTestUtils.validProjectConfigV1();
        EventType eventType = projectConfig.getEventTypes().get(0);
        String userId = "testUser1";

        List<String> experimentIds = projectConfig.getExperimentIdsForGoal(eventType.getKey());

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);
        for (Experiment experiment : projectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, userId))
                .thenReturn(experiment.getVariations().get(0));
        }

        // attributes are empty so user won't be in the audience for experiment using the event, but bucketing
        // will still take place
        LogEvent conversionEvent = builder.createConversionEvent(projectConfig, mockBucketAlgorithm, userId,
                                                                 eventType.getId(), eventType.getKey(),
                                                                 Collections.<String, String>emptyMap());

        for (Experiment experiment : projectConfig.getExperiments()) {
            if (experimentIds.contains(experiment.getId()) &&
                    ProjectValidationUtils.validatePreconditions(projectConfig, experiment, userId,
                                                                 Collections.<String, String>emptyMap())) {
                verify(mockBucketAlgorithm).bucket(experiment, userId);
            } else {
                verify(mockBucketAlgorithm, never()).bucket(experiment, userId);
            }
        }

        assertThat(conversionEvent.getRequestParams().size(), is(9));
    }

    /**
     * Verify that precedence is given to experiment status over forced variation bucketing when constructing the
     * experiment bucket map.
     */
    @Test
    public void createConversionParamsExperimentStatusPrecedesForcedVariation() {
        EventBuilderV1 builder = new EventBuilderV1();

        ProjectConfig projectConfig = ProjectConfigTestUtils.validProjectConfigV1();
        EventType eventType = projectConfig.getEventTypes().get(3);
        String userId = "userId";

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);
        for (Experiment experiment : projectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, userId))
                .thenReturn(experiment.getVariations().get(0));
        }

        LogEvent conversionEvent = builder.createConversionEvent(projectConfig, mockBucketAlgorithm, userId,
                                                                 eventType.getId(), eventType.getKey(),
                                                                 Collections.<String, String>emptyMap());

        for (Experiment experiment : projectConfig.getExperiments()) {
            verify(mockBucketAlgorithm, never()).bucket(experiment, userId);
        }

        assertNull(conversionEvent);
    }

    //======== Helper methods ========//

    private void verifyCommonRequestParams(ProjectConfig projectConfig, Map<String, String> requestParams,
                                           String userId, Attribute attribute) {
        // verify project id
        assertThat(requestParams, hasEntry("a", projectConfig.getProjectId()));

        // verify account id
        assertThat(requestParams, hasEntry("d", projectConfig.getAccountId()));

        // verify user id
        assertThat(requestParams, hasEntry("u", userId));

        // verify segments
        assertThat(requestParams, hasEntry("s" + attribute.getSegmentId(), "value"));

        // verify ppid
        assertThat(requestParams, hasEntry("p", "userId"));

        // verify source
        String sourceValue = requestParams.get("src");
        assertThat(sourceValue, startsWith("java-sdk-"));
    }

    private void verifyExperimentBucketMap(Map<String, String> requestParams, List<Experiment> allExperiments,
                                           List<String> experimentIds, ProjectConfig projectConfig, String userId,
                                           Map<String, String> attributes) {

        for (Experiment experiment : allExperiments) {
            if (experimentIds.contains(experiment.getId()) &&
                    ProjectValidationUtils.validatePreconditions(projectConfig, experiment, userId, attributes)) {
                assertThat(requestParams,
                        hasEntry("x" + experiment.getId(), experiment.getVariations().get(0).getId()));
            } else {
                assertThat(requestParams,
                        not(hasEntry("x" + experiment.getId(), experiment.getVariations().get(0).getId())));
            }
        }
    }
}