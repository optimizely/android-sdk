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
package com.optimizely.ab.config;

import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.NotCondition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.config.audience.UserAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests for {@link ProjectConfig}.
 */
public class ProjectConfigTest {

    private ProjectConfig projectConfig;

    @Before
    public void initialize() {
        projectConfig = ProjectConfigTestUtils.validProjectConfigV3();
    }

    /**
     * Verify that {@link ProjectConfig#toString()} doesn't throw an exception.
     */
    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public void toStringDoesNotFail() throws Exception {
        projectConfig.toString();
    }

    /**
     * Asserts that getExperimentIdsForGoal returns the respective experiment ids for experiments using a goal,
     * provided that the goal parameter is valid.
     */
    @Test
    public void verifyGetExperimentIdsForValidGoal() throws Exception {
        List<String> expectedSingleExperimentId = asList("223");
        List<String> actualSingleExperimentId = projectConfig.getExperimentIdsForGoal("clicked_cart");
        assertThat(actualSingleExperimentId, is(expectedSingleExperimentId));

        List<String> expectedMultipleExperimentIds = asList("118", "223");
        List<String> actualMultipleExperimentIds = projectConfig.getExperimentIdsForGoal("clicked_purchase");
        assertThat(actualMultipleExperimentIds, is(expectedMultipleExperimentIds));
    }

    /**
     * Asserts that getExperimentIdsForGoal returns an empty List given an invalid goal parameter.
     */
    @Test
    public void verifyGetExperimentIdsForInvalidGoal() throws Exception {
        List<String> expectedExperimentIds = Collections.emptyList();
        List<String> actualExperimentIds = projectConfig.getExperimentIdsForGoal("a_fake_goal");
        assertThat(actualExperimentIds, is(expectedExperimentIds));
    }

    /**
     * Asserts that getAudienceConditionsFromId returns the respective conditions for an audience, provided the
     * audience ID parameter is valid.
     */
    @Test
    public void verifyGetAudienceConditionsFromValidId() throws Exception {
        List<Condition> userAttributes = new ArrayList<Condition>();
        userAttributes.add(new UserAttribute("browser_type", "custom_dimension", "firefox"));

        OrCondition orInner = new OrCondition(userAttributes);

        NotCondition notCondition = new NotCondition(orInner);
        List<Condition> outerOrList = new ArrayList<Condition>();
        outerOrList.add(notCondition);

        OrCondition orOuter = new OrCondition(outerOrList);
        List<Condition> andList = new ArrayList<Condition>();
        andList.add(orOuter);

        Condition expectedConditions = new AndCondition(andList);
        Condition actualConditions = projectConfig.getAudienceConditionsFromId("100");
        assertThat(actualConditions, is(expectedConditions));
    }

    /**
     * Asserts that getAudienceConditionsFromId returns null given an invalid audience ID parameter.
     */
    @Test
    public void verifyGetAudienceConditionsFromInvalidId() throws Exception {
        assertNull(projectConfig.getAudienceConditionsFromId("invalid_id"));
    }

    /**
     * Asserts that getLiveVariableIdToExperimentsMapping returns a correct mapping between live variable IDs and
     * corresponding experiments using these live variables.
     */
    @Test
    public void verifyGetLiveVariableIdToExperimentsMapping() throws Exception {
        Experiment ungroupedExpWithVariables = projectConfig.getExperiments().get(0);
        Experiment groupedExpWithVariables = projectConfig.getGroups().get(0).getExperiments().get(1);

        Map<String, List<Experiment>> expectedLiveVariableIdToExperimentsMapping =
                new HashMap<String, List<Experiment>>();
        expectedLiveVariableIdToExperimentsMapping.put("6", Collections.singletonList(ungroupedExpWithVariables));
        expectedLiveVariableIdToExperimentsMapping.put("2", Collections.singletonList(ungroupedExpWithVariables));
        expectedLiveVariableIdToExperimentsMapping.put("3", Collections.singletonList(ungroupedExpWithVariables));
        expectedLiveVariableIdToExperimentsMapping.put("4", Collections.singletonList(ungroupedExpWithVariables));

        expectedLiveVariableIdToExperimentsMapping.put("7", Collections.singletonList(groupedExpWithVariables));

        assertThat(projectConfig.getLiveVariableIdToExperimentsMapping(),
                   is(expectedLiveVariableIdToExperimentsMapping));
    }

    /**
     * Asserts that getVariationToLiveVariableUsageInstanceMapping returns a correct mapping between variation IDs and
     * the values of the live variables for the variation.
     */
    @Test
    public void verifyGetVariationToLiveVariableUsageInstanceMapping() throws Exception {
        Map<String, Map<String, LiveVariableUsageInstance>> expectedVariationToLiveVariableUsageInstanceMapping =
                new HashMap<String, Map<String, LiveVariableUsageInstance>>();

        Map<String, LiveVariableUsageInstance> ungroupedVariation276VariableValues =
                new HashMap<String, LiveVariableUsageInstance>();
        ungroupedVariation276VariableValues.put("6", new LiveVariableUsageInstance("6", "True"));
        ungroupedVariation276VariableValues.put("2", new LiveVariableUsageInstance("2", "10"));
        ungroupedVariation276VariableValues.put("3", new LiveVariableUsageInstance("3", "string_var_vtag1"));
        ungroupedVariation276VariableValues.put("4", new LiveVariableUsageInstance("4", "5.3"));


        Map<String, LiveVariableUsageInstance> ungroupedVariation277VariableValues =
                new HashMap<String, LiveVariableUsageInstance>();
        ungroupedVariation277VariableValues.put("6", new LiveVariableUsageInstance("6", "False"));
        ungroupedVariation277VariableValues.put("2", new LiveVariableUsageInstance("2", "20"));
        ungroupedVariation277VariableValues.put("3", new LiveVariableUsageInstance("3", "string_var_vtag2"));
        ungroupedVariation277VariableValues.put("4", new LiveVariableUsageInstance("4", "6.3"));

        expectedVariationToLiveVariableUsageInstanceMapping.put("276", ungroupedVariation276VariableValues);
        expectedVariationToLiveVariableUsageInstanceMapping.put("277", ungroupedVariation277VariableValues);

        Map<String, LiveVariableUsageInstance> groupedVariation280VariableValues =
                new HashMap<String, LiveVariableUsageInstance>();
        groupedVariation280VariableValues.put("7", new LiveVariableUsageInstance("7", "True"));

        Map<String, LiveVariableUsageInstance> groupedVariation281VariableValues =
                new HashMap<String, LiveVariableUsageInstance>();
        groupedVariation281VariableValues.put("7", new LiveVariableUsageInstance("7", "False"));

        expectedVariationToLiveVariableUsageInstanceMapping.put("280", groupedVariation280VariableValues);
        expectedVariationToLiveVariableUsageInstanceMapping.put("281", groupedVariation281VariableValues);

        assertThat(projectConfig.getVariationToLiveVariableUsageInstanceMapping(),
                   is(expectedVariationToLiveVariableUsageInstanceMapping));
    }

    /**
     * Asserts that anonymizeIP is set to false if not explicitly passed into the constructor (in the case of V1 or V2
     * projects).
     * @throws Exception
     */
    @Test
    public void verifyAnonymizeIPIsFalseByDefault() throws Exception {
        ProjectConfig v2ProjectConfig = ProjectConfigTestUtils.validProjectConfigV2();
        assertFalse(v2ProjectConfig.getAnonymizeIP());
    }
}