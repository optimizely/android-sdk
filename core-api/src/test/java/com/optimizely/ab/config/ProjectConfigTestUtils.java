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
package com.optimizely.ab.config;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.NotCondition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.config.audience.UserAttribute;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Helper class that provides common functionality and resources for testing {@link ProjectConfig}.
 */
public final class ProjectConfigTestUtils {

    private static final ProjectConfig VALID_PROJECT_CONFIG_V2 = generateValidProjectConfigV2();
    private static ProjectConfig generateValidProjectConfigV2() {
        List<Experiment> experiments = asList(
            new Experiment("223", "etag1", "Running", "1",
                           singletonList("100"),
                           asList(new Variation("276", "vtag1"),
                                  new Variation("277", "vtag2")),
                           Collections.singletonMap("testUser1", "vtag1"),
                           asList(new TrafficAllocation("276", 3500),
                                  new TrafficAllocation("277", 9000)),
                           ""),
            new Experiment("118", "etag2", "Not started", "2",
                           singletonList("100"),
                           asList(new Variation("278", "vtag3"),
                                  new Variation("279", "vtag4")),
                           Collections.singletonMap("testUser3", "vtag3"),
                           asList(new TrafficAllocation("278", 4500),
                                  new TrafficAllocation("279", 9000)),
                           ""),
            new Experiment("119", "etag3", "Not started", null,
                           singletonList("100"),
                           asList(new Variation("280", "vtag5"),
                                  new Variation("281", "vtag6")),
                           Collections.singletonMap("testUser4", "vtag5"),
                           asList(new TrafficAllocation("280", 4500),
                                  new TrafficAllocation("281", 9000)),
                           "")
        );

        List<Attribute> attributes = singletonList(new Attribute("134", "browser_type"));

        List<String> singleExperimentId = singletonList("223");
        List<String> multipleExperimentIds = asList("118", "223");
        List<EventType> events = asList(new EventType("971", "clicked_cart", singleExperimentId),
                                        new EventType("098", "Total Revenue", singleExperimentId),
                                        new EventType("099", "clicked_purchase", multipleExperimentIds),
                                        new EventType("100", "no_running_experiments", singletonList("118")));

        List<Condition> userAttributes = new ArrayList<Condition>();
        userAttributes.add(new UserAttribute("browser_type", "custom_dimension", "firefox"));

        OrCondition orInner = new OrCondition(userAttributes);

        NotCondition notCondition = new NotCondition(orInner);
        List<Condition> outerOrList = new ArrayList<Condition>();
        outerOrList.add(notCondition);

        OrCondition orOuter = new OrCondition(outerOrList);
        List<Condition> andList = new ArrayList<Condition>();
        andList.add(orOuter);

        AndCondition andCondition = new AndCondition(andList);

        List<Audience> audiences = singletonList(new Audience("100", "not_firefox_users", andCondition));

        Map<String, String> userIdToVariationKeyMap = new HashMap<String, String>();
        userIdToVariationKeyMap.put("testUser1", "e1_vtag1");
        userIdToVariationKeyMap.put("testUser2", "e1_vtag2");

        List<Experiment> randomGroupExperiments = asList(
            new Experiment("301", "group_etag2", "Running", "3",
                           singletonList("100"),
                           asList(new Variation("282", "e2_vtag1"),
                                  new Variation("283", "e2_vtag2")),
                           Collections.<String, String>emptyMap(),
                           asList(new TrafficAllocation("282", 5000),
                                  new TrafficAllocation("283", 10000)),
                           "42"),
            new Experiment("300", "group_etag1", "Running", "4",
                           singletonList("100"),
                           asList(new Variation("280", "e1_vtag1"),
                                  new Variation("281", "e1_vtag2")),
                           userIdToVariationKeyMap,
                           asList(new TrafficAllocation("280", 3000),
                                  new TrafficAllocation("281", 10000)),
                           "42")
        );

        List<Experiment> overlappingGroupExperiments = asList(
            new Experiment("302", "overlapping_etag1", "Running", "5",
                           singletonList("100"),
                           asList(new Variation("284", "e1_vtag1"),
                                  new Variation("285", "e1_vtag2")),
                           userIdToVariationKeyMap,
                           asList(new TrafficAllocation("284", 1500),
                                  new TrafficAllocation("285", 3000)),
                           "43")
        );

        Group randomPolicyGroup = new Group("42", "random",
                                            randomGroupExperiments,
                                            asList(new TrafficAllocation("300", 3000),
                                                   new TrafficAllocation("301", 9000),
                                                   new TrafficAllocation("", 10000)));
        Group overlappingPolicyGroup = new Group("43", "overlapping",
                                                 overlappingGroupExperiments,
                                                 Collections.<TrafficAllocation>emptyList());
        List<Group> groups = asList(randomPolicyGroup, overlappingPolicyGroup);

        return new ProjectConfig("789", "1234", "2", "42", groups, experiments, attributes, events, audiences);
    }

    private static final ProjectConfig NO_AUDIENCE_PROJECT_CONFIG_V2 = generateNoAudienceProjectConfigV2();
    private static ProjectConfig generateNoAudienceProjectConfigV2() {
        Map<String, String> userIdToVariationKeyMap = new HashMap<String, String>();
        userIdToVariationKeyMap.put("testUser1", "vtag1");
        userIdToVariationKeyMap.put("testUser2", "vtag2");

        List<Experiment> experiments = asList(
            new Experiment("223", "etag1", "Running", "1",
                           Collections.<String>emptyList(),
                           asList(new Variation("276", "vtag1"),
                                  new Variation("277", "vtag2")),
                           userIdToVariationKeyMap,
                           asList(new TrafficAllocation("276", 3500),
                                  new TrafficAllocation("277", 9000)),
                           ""),
            new Experiment("118", "etag2", "Not started", "2",
                           Collections.<String>emptyList(),
                           asList(new Variation("278", "vtag3"),
                                  new Variation("279", "vtag4")),
                           Collections.<String, String>emptyMap(),
                           asList(new TrafficAllocation("278", 4500),
                                  new TrafficAllocation("279", 9000)),
                           ""),
            new Experiment("119", "etag3", "Launched", "3",
                           Collections.<String>emptyList(),
                           asList(new Variation("280", "vtag5"),
                                  new Variation("281", "vtag6")),
                           Collections.<String, String>emptyMap(),
                           asList(new TrafficAllocation("280", 5000),
                                  new TrafficAllocation("281", 10000)),
                           "")
        );

        List<Attribute> attributes = singletonList(new Attribute("134", "browser_type"));

        List<String> singleExperimentId = singletonList("223");
        List<String> multipleExperimentIds = asList("118", "223");
        List<EventType> events = asList(
                new EventType("971", "clicked_cart", singleExperimentId),
                new EventType("098", "Total Revenue", singleExperimentId),
                new EventType("099", "clicked_purchase", multipleExperimentIds),
                new EventType("100", "launched_exp_event", singletonList("119")),
                new EventType("101", "event_with_launched_and_running_experiments", Arrays.asList("119", "223"))
        );

        return new ProjectConfig("789", "1234", "2", "42", Collections.<Group>emptyList(), experiments, attributes,
                                 events, Collections.<Audience>emptyList());
    }

    private static final ProjectConfig VALID_PROJECT_CONFIG_V3 = generateValidProjectConfigV3();
    private static ProjectConfig generateValidProjectConfigV3() {
        List<LiveVariableUsageInstance> variationVtag1VariableUsageInstances = asList(
            new LiveVariableUsageInstance("6", "True"),
            new LiveVariableUsageInstance("2", "10"),
            new LiveVariableUsageInstance("3", "string_var_vtag1"),
            new LiveVariableUsageInstance("4", "5.3")
        );

        List<LiveVariableUsageInstance> variationVtag2VariableUsageInstances = asList(
            new LiveVariableUsageInstance("6", "False"),
            new LiveVariableUsageInstance("2", "20"),
            new LiveVariableUsageInstance("3", "string_var_vtag2"),
            new LiveVariableUsageInstance("4", "6.3")
        );

        List<Experiment> experiments = asList(
            new Experiment("223", "etag1", "Running", "1",
                           singletonList("100"),
                           asList(new Variation("276", "vtag1", variationVtag1VariableUsageInstances),
                                  new Variation("277", "vtag2", variationVtag2VariableUsageInstances)),
                           Collections.singletonMap("testUser1", "vtag1"),
                           asList(new TrafficAllocation("276", 3500),
                                  new TrafficAllocation("277", 9000)),
                           ""),
            new Experiment("118", "etag2", "Not started", "2",
                           singletonList("100"),
                           asList(new Variation("278", "vtag3", Collections.<LiveVariableUsageInstance>emptyList()),
                                  new Variation("279", "vtag4", Collections.<LiveVariableUsageInstance>emptyList())),
                           Collections.singletonMap("testUser3", "vtag3"),
                           asList(new TrafficAllocation("278", 4500),
                                  new TrafficAllocation("279", 9000)),
                           "")
        );

        List<Attribute> attributes = singletonList(new Attribute("134", "browser_type"));

        List<String> singleExperimentId = singletonList("223");
        List<String> multipleExperimentIds = asList("118", "223");
        List<EventType> events = asList(new EventType("971", "clicked_cart", singleExperimentId),
                                        new EventType("098", "Total Revenue", singleExperimentId),
                                        new EventType("099", "clicked_purchase", multipleExperimentIds),
                                        new EventType("100", "no_running_experiments", singletonList("118")));

        List<Condition> userAttributes = new ArrayList<Condition>();
        userAttributes.add(new UserAttribute("browser_type", "custom_dimension", "firefox"));

        OrCondition orInner = new OrCondition(userAttributes);

        NotCondition notCondition = new NotCondition(orInner);
        List<Condition> outerOrList = new ArrayList<Condition>();
        outerOrList.add(notCondition);

        OrCondition orOuter = new OrCondition(outerOrList);
        List<Condition> andList = new ArrayList<Condition>();
        andList.add(orOuter);

        AndCondition andCondition = new AndCondition(andList);

        List<Audience> audiences = singletonList(new Audience("100", "not_firefox_users", andCondition));

        Map<String, String> userIdToVariationKeyMap = new HashMap<String, String>();
        userIdToVariationKeyMap.put("testUser1", "e1_vtag1");
        userIdToVariationKeyMap.put("testUser2", "e1_vtag2");

        List<Experiment> randomGroupExperiments = asList(
            new Experiment("301", "group_etag2", "Running", "3",
                           singletonList("100"),
                           asList(new Variation("282", "e2_vtag1", Collections.<LiveVariableUsageInstance>emptyList()),
                                  new Variation("283", "e2_vtag2", Collections.<LiveVariableUsageInstance>emptyList())),
                           Collections.<String, String>emptyMap(),
                           asList(new TrafficAllocation("282", 5000),
                                  new TrafficAllocation("283", 10000)),
                           "42"),
            new Experiment("300", "group_etag1", "Running", "4",
                           singletonList("100"),
                           asList(new Variation("280", "e1_vtag1",
                                                Collections.singletonList(new LiveVariableUsageInstance("7", "True"))),
                                  new Variation("281", "e1_vtag2",
                                                Collections.singletonList(new LiveVariableUsageInstance("7", "False")))),
                           userIdToVariationKeyMap,
                           asList(new TrafficAllocation("280", 3000),
                                  new TrafficAllocation("281", 10000)),
                           "42")
        );

        List<Experiment> overlappingGroupExperiments = asList(
            new Experiment("302", "overlapping_etag1", "Running", "5",
                           singletonList("100"),
                           asList(new Variation("284", "e1_vtag1", Collections.<LiveVariableUsageInstance>emptyList()),
                                  new Variation("285", "e1_vtag2", Collections.<LiveVariableUsageInstance>emptyList())),
                           userIdToVariationKeyMap,
                           asList(new TrafficAllocation("284", 1500),
                                  new TrafficAllocation("285", 3000)),
                           "43")
        );

        Group randomPolicyGroup = new Group("42", "random",
                                            randomGroupExperiments,
                                            asList(new TrafficAllocation("300", 3000),
                                                   new TrafficAllocation("301", 9000),
                                                   new TrafficAllocation("", 10000)));
        Group overlappingPolicyGroup = new Group("43", "overlapping",
                                                 overlappingGroupExperiments,
                                                 Collections.<TrafficAllocation>emptyList());
        List<Group> groups = asList(randomPolicyGroup, overlappingPolicyGroup);

        List<LiveVariable> liveVariables = asList(
            new LiveVariable("1", "boolean_variable", "False", LiveVariable.VariableStatus.ACTIVE,
                             LiveVariable.VariableType.BOOLEAN),
            new LiveVariable("2", "integer_variable", "5", LiveVariable.VariableStatus.ACTIVE,
                             LiveVariable.VariableType.INTEGER),
            new LiveVariable("3", "string_variable", "string_live_variable", LiveVariable.VariableStatus.ACTIVE,
                             LiveVariable.VariableType.STRING),
            new LiveVariable("4", "double_variable", "13.37", LiveVariable.VariableStatus.ACTIVE,
                             LiveVariable.VariableType.DOUBLE),
            new LiveVariable("5", "archived_variable", "True", LiveVariable.VariableStatus.ARCHIVED,
                             LiveVariable.VariableType.BOOLEAN),
            new LiveVariable("6", "etag1_variable", "False", LiveVariable.VariableStatus.ACTIVE,
                             LiveVariable.VariableType.BOOLEAN),
            new LiveVariable("7", "group_etag1_variable", "False", LiveVariable.VariableStatus.ACTIVE,
                             LiveVariable.VariableType.BOOLEAN),
            new LiveVariable("8", "unused_string_variable", "unused_variable", LiveVariable.VariableStatus.ACTIVE,
                             LiveVariable.VariableType.STRING)
        );

        return new ProjectConfig("789", "1234", "3", "42", groups, experiments, attributes, events, audiences,
                                 true, liveVariables);
    }

    private static final ProjectConfig NO_AUDIENCE_PROJECT_CONFIG_V3 = generateNoAudienceProjectConfigV3();
    private static ProjectConfig generateNoAudienceProjectConfigV3() {
        Map<String, String> userIdToVariationKeyMap = new HashMap<String, String>();
        userIdToVariationKeyMap.put("testUser1", "vtag1");
        userIdToVariationKeyMap.put("testUser2", "vtag2");

        List<Experiment> experiments = asList(
                new Experiment("223", "etag1", "Running", "1",
                        Collections.<String>emptyList(),
                        asList(new Variation("276", "vtag1", Collections.<LiveVariableUsageInstance>emptyList()),
                                new Variation("277", "vtag2", Collections.<LiveVariableUsageInstance>emptyList())),
                        userIdToVariationKeyMap,
                        asList(new TrafficAllocation("276", 3500),
                                new TrafficAllocation("277", 9000)),
                        ""),
                new Experiment("118", "etag2", "Not started", "2",
                        Collections.<String>emptyList(),
                        asList(new Variation("278", "vtag3", Collections.<LiveVariableUsageInstance>emptyList()),
                                new Variation("279", "vtag4", Collections.<LiveVariableUsageInstance>emptyList())),
                        Collections.<String, String>emptyMap(),
                        asList(new TrafficAllocation("278", 4500),
                                new TrafficAllocation("279", 9000)),
                        ""),
                new Experiment("119", "etag3", "Launched", "3",
                        Collections.<String>emptyList(),
                        asList(new Variation("280", "vtag5"),
                                new Variation("281", "vtag6")),
                        Collections.<String, String>emptyMap(),
                        asList(new TrafficAllocation("280", 5000),
                                new TrafficAllocation("281", 10000)),
                        "")
        );

        List<Attribute> attributes = singletonList(new Attribute("134", "browser_type"));

        List<String> singleExperimentId = singletonList("223");
        List<String> multipleExperimentIds = asList("118", "223");
        List<EventType> events = asList(
                new EventType("971", "clicked_cart", singleExperimentId),
                new EventType("098", "Total Revenue", singleExperimentId),
                new EventType("099", "clicked_purchase", multipleExperimentIds),
                new EventType("100", "launched_exp_event", singletonList("119")),
                new EventType("101", "event_with_launched_and_running_experiments", Arrays.asList("119", "223"))
        );

        return new ProjectConfig("789", "1234", "3", "42", Collections.<Group>emptyList(), experiments, attributes,
                                 events, Collections.<Audience>emptyList(), true, Collections.<LiveVariable>emptyList());
    }

    private ProjectConfigTestUtils() { }

    public static String validConfigJsonV2() throws IOException {
        return Resources.toString(Resources.getResource("config/valid-project-config-v2.json"), Charsets.UTF_8);
    }

    public static String noAudienceProjectConfigJsonV2() throws IOException {
        return Resources.toString(Resources.getResource("config/no-audience-project-config-v2.json"), Charsets.UTF_8);
    }

    public static String validConfigJsonV3() throws IOException {
        return Resources.toString(Resources.getResource("config/valid-project-config-v3.json"), Charsets.UTF_8);
    }

    public static String noAudienceProjectConfigJsonV3() throws IOException {
        return Resources.toString(Resources.getResource("config/no-audience-project-config-v3.json"), Charsets.UTF_8);
    }

    /**
     * @return the expected {@link ProjectConfig} for the json produced by {@link #validConfigJsonV2()} ()}
     */
    public static ProjectConfig validProjectConfigV2() {
        return VALID_PROJECT_CONFIG_V2;
    }

    /**
     * @return the expected {@link ProjectConfig} for the json produced by {@link #noAudienceProjectConfigJsonV2()}
     */
    public static ProjectConfig noAudienceProjectConfigV2() {
        return NO_AUDIENCE_PROJECT_CONFIG_V2;
    }

    /**
     * @return the expected {@link ProjectConfig} for the json produced by {@link #validConfigJsonV3()} ()}
     */
    public static ProjectConfig validProjectConfigV3() {
        return VALID_PROJECT_CONFIG_V3;
    }

    /**
     * @return the expected {@link ProjectConfig} for the json produced by {@link #noAudienceProjectConfigJsonV3()}
     */
    public static ProjectConfig noAudienceProjectConfigV3() {
        return NO_AUDIENCE_PROJECT_CONFIG_V3;
    }

    /**
     * Asserts that the provided project configs are equivalent.
     */
    public static void verifyProjectConfig(@CheckForNull ProjectConfig actual, @Nonnull ProjectConfig expected) {
        assertNotNull(actual);

        // verify the project-level values
        assertThat(actual.getAccountId(), is(expected.getAccountId()));
        assertThat(actual.getProjectId(), is(expected.getProjectId()));
        assertThat(actual.getVersion(), is(expected.getVersion()));
        assertThat(actual.getRevision(), is(expected.getRevision()));

        verifyGroups(actual.getGroups(), expected.getGroups());
        verifyExperiments(actual.getExperiments(), expected.getExperiments());
        verifyAttributes(actual.getAttributes(), expected.getAttributes());
        verifyEvents(actual.getEventTypes(), expected.getEventTypes());
        verifyAudiences(actual.getAudiences(), expected.getAudiences());
        verifyLiveVariables(actual.getLiveVariables(), expected.getLiveVariables());
    }

    /**
     * Asserts that the provided experiment configs are equivalent.
     */
    private static void verifyExperiments(List<Experiment> actual, List<Experiment> expected) {
        assertThat(actual.size(), is(expected.size()));

        for (int i = 0; i < actual.size(); i++) {
            Experiment actualExperiment = actual.get(i);
            Experiment expectedExperiment = expected.get(i);

            assertThat(actualExperiment.getId(), is(expectedExperiment.getId()));
            assertThat(actualExperiment.getKey(), is(expectedExperiment.getKey()));
            assertThat(actualExperiment.getGroupId(), is(expectedExperiment.getGroupId()));
            assertThat(actualExperiment.getStatus(), is(expectedExperiment.getStatus()));
            assertThat(actualExperiment.getAudienceIds(), is(expectedExperiment.getAudienceIds()));
            assertThat(actualExperiment.getUserIdToVariationKeyMap(),
                       is(expectedExperiment.getUserIdToVariationKeyMap()));

            verifyVariations(actualExperiment.getVariations(), expectedExperiment.getVariations());
            verifyTrafficAllocations(actualExperiment.getTrafficAllocation(),
                                     expectedExperiment.getTrafficAllocation());
        }
    }

    /**
     * Asserts that the provided variation configs are equivalent.
     */
    private static void verifyVariations(List<Variation> actual, List<Variation> expected) {
        assertThat(actual.size(), is(expected.size()));

        for (int i = 0; i < actual.size(); i++) {
            Variation actualVariation = actual.get(i);
            Variation expectedVariation = expected.get(i);

            assertThat(actualVariation.getId(), is(expectedVariation.getId()));
            assertThat(actualVariation.getKey(), is(expectedVariation.getKey()));
            verifyLiveVariableInstances(actualVariation.getLiveVariableUsageInstances(),
                                        expectedVariation.getLiveVariableUsageInstances());
        }
    }

    /**
     * Asserts that the provided traffic allocation configs are equivalent.
     */
    private static void verifyTrafficAllocations(List<TrafficAllocation> actual,
                                          List<TrafficAllocation> expected) {
        assertThat(actual.size(), is(expected.size()));

        for (int i = 0; i < actual.size(); i++) {
            TrafficAllocation actualDistribution = actual.get(i);
            TrafficAllocation expectedDistribution = expected.get(i);

            assertThat(actualDistribution.getEntityId(), is(expectedDistribution.getEntityId()));
            assertThat(actualDistribution.getEndOfRange(), is(expectedDistribution.getEndOfRange()));
        }
    }

    /**
     * Asserts that the provided attributes configs are equivalent.
     */
    private static void verifyAttributes(List<Attribute> actual, List<Attribute> expected) {
        assertThat(actual.size(), is(expected.size()));

        for (int i = 0; i < actual.size(); i++) {
            Attribute actualAttribute = actual.get(i);
            Attribute expectedAttribute = expected.get(i);

            assertThat(actualAttribute.getId(), is(expectedAttribute.getId()));
            assertThat(actualAttribute.getKey(), is(expectedAttribute.getKey()));
            assertThat(actualAttribute.getSegmentId(), is(expectedAttribute.getSegmentId()));
        }
    }

    /**
     * Asserts that the provided event configs are equivalent.
     */
    private static void verifyEvents(List<EventType> actual, List<EventType> expected) {
        assertThat(actual.size(), is(expected.size()));

        for (int i = 0; i < actual.size(); i++) {
            EventType actualEvent = actual.get(i);
            EventType expectedEvent = expected.get(i);

            assertThat(actualEvent.getExperimentIds(), is(expectedEvent.getExperimentIds()));
            assertThat(actualEvent.getId(), is(expectedEvent.getId()));
            assertThat(actualEvent.getKey(), is(expectedEvent.getKey()));
        }
    }

    /**
     * Asserts that the provided audience configs are equivalent.
     */
    private static void verifyAudiences(List<Audience> actual, List<Audience> expected) {
        assertThat(actual.size(), is(expected.size()));

        for (int i = 0; i < actual.size(); i++) {
            Audience actualAudience = actual.get(i);
            Audience expectedAudience = expected.get(i);

            assertThat(actualAudience.getId(), is(expectedAudience.getId()));
            assertThat(actualAudience.getKey(), is(expectedAudience.getKey()));
            assertThat(actualAudience.getConditions(), is(expectedAudience.getConditions()));
            assertThat(actualAudience.getConditions(), is(expectedAudience.getConditions()));
        }
    }

    /**
     * Assert that the provided group configs are equivalent.
     */
    private static void verifyGroups(List<Group> actual, List<Group> expected) {
        assertThat(actual.size(), is(expected.size()));

        for (int i = 0; i < actual.size(); i++) {
            Group actualGroup = actual.get(i);
            Group expectedGroup = expected.get(i);

            assertThat(actualGroup.getId(), is(expectedGroup.getId()));
            assertThat(actualGroup.getPolicy(), is(expectedGroup.getPolicy()));
            verifyTrafficAllocations(actualGroup.getTrafficAllocation(), expectedGroup.getTrafficAllocation());
            verifyExperiments(actualGroup.getExperiments(), expectedGroup.getExperiments());
        }
    }

    /**
     * Verify that the provided live variable definitions are equivalent.
     */
    private static void verifyLiveVariables(List<LiveVariable> actual, List<LiveVariable> expected) {
        // if using V2, live variables will be null
        if (expected == null) {
            assertNull(actual);
        } else {
            assertThat(actual.size(), is(expected.size()));

            for (int i = 0; i < actual.size(); i++) {
                LiveVariable actualLiveVariable = actual.get(i);
                LiveVariable expectedLiveVariable = expected.get(i);

                assertThat(actualLiveVariable.getId(), is(expectedLiveVariable.getId()));
                assertThat(actualLiveVariable.getKey(), is(expectedLiveVariable.getKey()));
                assertThat(actualLiveVariable.getDefaultValue(), is(expectedLiveVariable.getDefaultValue()));
                assertThat(actualLiveVariable.getType(), is(expectedLiveVariable.getType()));
                assertThat(actualLiveVariable.getStatus(), is(expectedLiveVariable.getStatus()));
            }
        }
    }

    /**
     * Verify that the provided variation-level live variable usage instances are equivalent.
     */
    private static void verifyLiveVariableInstances(List<LiveVariableUsageInstance> actual,
                                                    List<LiveVariableUsageInstance> expected) {
        // if using V2, live variable instances will be null
        if (expected == null) {
            assertNull(actual);
        } else {
            assertThat(actual.size(), is(expected.size()));

            for (int i = 0; i < actual.size(); i++) {
                LiveVariableUsageInstance actualLiveVariableUsageInstance = actual.get(i);
                LiveVariableUsageInstance expectedLiveVariableUsageInstance = expected.get(i);

                assertThat(actualLiveVariableUsageInstance.getId(), is(expectedLiveVariableUsageInstance.getId()));
                assertThat(actualLiveVariableUsageInstance.getValue(), is(expectedLiveVariableUsageInstance.getValue()));
            }
        }
    }
}
