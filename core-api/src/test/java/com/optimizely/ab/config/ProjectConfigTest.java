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
package com.optimizely.ab.config;

import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.NotCondition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.config.audience.UserAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
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
        projectConfig = ProjectConfigTestUtils.validProjectConfigV2();
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
}