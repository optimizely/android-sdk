/**
 *
 *    Copyright 2017, Optimizely and contributors
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
package com.optimizely.ab.bucketing;

import ch.qos.logback.classic.Level;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.ValidProjectConfigV4;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.internal.LogbackVerifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV4;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
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
import static org.mockito.Mockito.times;

public class DecisionServiceTest {

    private static final String genericUserId = "genericUserId";
    private static final String whitelistedUserId = "testUser1";
    private static final String userProfileId = "userProfileId";

    @Rule
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock private ErrorHandler mockErrorHandler;

    private static ProjectConfig noAudienceProjectConfig;
    private static ProjectConfig validProjectConfig;
    private static Experiment whitelistedExperiment;
    private static Variation whitelistedVariation;

    @BeforeClass
    public static void setUp() throws Exception {
        validProjectConfig = validProjectConfigV3();
        noAudienceProjectConfig = noAudienceProjectConfigV3();
        whitelistedExperiment = validProjectConfig.getExperimentIdMapping().get("223");
        whitelistedVariation = whitelistedExperiment.getVariationKeyToVariationMap().get("vtag1");
    }

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    //========= getVariation tests =========/

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, String, Map)}
     * gives precedence to forced variation bucketing over audience evaluation.
     */
    @Test
    public void getVariationWhitelistedPrecedesAudienceEval() throws Exception {
        Bucketer bucketer = spy(new Bucketer(validProjectConfig));
        DecisionService decisionService = spy(new DecisionService(bucketer, mockErrorHandler, validProjectConfig, null));
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation expectedVariation = experiment.getVariations().get(0);

        // user excluded without audiences and whitelisting
        assertNull(decisionService.getVariation(experiment, genericUserId, Collections.<String, String>emptyMap()));

        logbackVerifier.expectMessage(Level.INFO, "User \"" + whitelistedUserId + "\" is forced in variation \"vtag1\".");

        // no attributes provided for a experiment that has an audience
        assertThat(decisionService.getVariation(experiment, whitelistedUserId, Collections.<String, String>emptyMap()), is(expectedVariation));

        verify(decisionService).getWhitelistedVariation(experiment, whitelistedUserId);
        verify(decisionService, never()).getStoredVariation(eq(experiment), any(UserProfile.class));
    }

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, String, Map)}
     * gives precedence to forced variation bucketing over whitelisting.
     */
    @Test
    public void getForcedVariationBeforeWhitelisting() throws Exception {
        Bucketer bucketer = new Bucketer(validProjectConfig);
        DecisionService decisionService = spy(new DecisionService(bucketer, mockErrorHandler, validProjectConfig, null));
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation whitelistVariation = experiment.getVariations().get(0);
        Variation expectedVariation = experiment.getVariations().get(1);

        // user excluded without audiences and whitelisting
        assertNull(decisionService.getVariation(experiment, genericUserId, Collections.<String, String>emptyMap()));

        logbackVerifier.expectMessage(Level.INFO, "User \"" + genericUserId + "\" does not meet conditions to be in experiment \"etag1\".");

        // set the runtimeForcedVariation
        validProjectConfig.setForcedVariation(experiment.getKey(), whitelistedUserId, expectedVariation.getKey());
        // no attributes provided for a experiment that has an audience
        assertThat(decisionService.getVariation(experiment, whitelistedUserId, Collections.<String, String>emptyMap()), is(expectedVariation));

        //verify(decisionService).getForcedVariation(experiment.getKey(), whitelistedUserId);
        verify(decisionService, never()).getStoredVariation(eq(experiment), any(UserProfile.class));
        assertEquals(decisionService.getWhitelistedVariation(experiment, whitelistedUserId), whitelistVariation);
        assertTrue(validProjectConfig.setForcedVariation(experiment.getKey(), whitelistedUserId,null));
        assertNull(validProjectConfig.getForcedVariation(experiment.getKey(), whitelistedUserId));
        assertThat(decisionService.getVariation(experiment, whitelistedUserId, Collections.<String, String>emptyMap()), is(whitelistVariation));
    }

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, String, Map)}
     * gives precedence to forced variation bucketing over audience evaluation.
     */
    @Test
    public void getVariationForcedPrecedesAudienceEval() throws Exception {
        Bucketer bucketer = spy(new Bucketer(validProjectConfig));
        DecisionService decisionService = spy(new DecisionService(bucketer, mockErrorHandler, validProjectConfig, null));
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation expectedVariation = experiment.getVariations().get(1);

        // user excluded without audiences and whitelisting
        assertNull(decisionService.getVariation(experiment, genericUserId, Collections.<String, String>emptyMap()));

        logbackVerifier.expectMessage(Level.INFO, "User \"" + genericUserId + "\" does not meet conditions to be in experiment \"etag1\".");

        // set the runtimeForcedVariation
        validProjectConfig.setForcedVariation(experiment.getKey(), genericUserId, expectedVariation.getKey());
        // no attributes provided for a experiment that has an audience
        assertThat(decisionService.getVariation(experiment, genericUserId, Collections.<String, String>emptyMap()), is(expectedVariation));

        verify(decisionService, never()).getStoredVariation(eq(experiment), any(UserProfile.class));
        assertEquals(validProjectConfig.setForcedVariation(experiment.getKey(), genericUserId,null), true);
        assertNull(validProjectConfig.getForcedVariation(experiment.getKey(), genericUserId));
    }

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, String, Map)}
     * gives precedence to forced variation bucketing over user profile.
     */
    @Test
    public void getVariationForcedBeforeUserProfile() throws Exception {
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation variation = experiment.getVariations().get(0);
        Bucketer bucketer = spy(new Bucketer(validProjectConfig));
        Decision decision = new Decision(variation.getId());
        UserProfile userProfile = new UserProfile(userProfileId,
                Collections.singletonMap(experiment.getId(), decision));
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userProfileService.lookup(userProfileId)).thenReturn(userProfile.toMap());

        DecisionService decisionService = spy(new DecisionService(bucketer,
                mockErrorHandler, validProjectConfig, userProfileService));

        // ensure that normal users still get excluded from the experiment when they fail audience evaluation
        assertNull(decisionService.getVariation(experiment, genericUserId, Collections.<String, String>emptyMap()));

        logbackVerifier.expectMessage(Level.INFO,
                "User \"" + genericUserId + "\" does not meet conditions to be in experiment \""
                        + experiment.getKey() + "\".");

        // ensure that a user with a saved user profile, sees the same variation regardless of audience evaluation
        assertEquals(variation,
                decisionService.getVariation(experiment, userProfileId, Collections.<String, String>emptyMap()));

        Variation forcedVariation = experiment.getVariations().get(1);
        validProjectConfig.setForcedVariation(experiment.getKey(), userProfileId, forcedVariation.getKey());
        assertEquals(forcedVariation,
                decisionService.getVariation(experiment, userProfileId, Collections.<String, String>emptyMap()));
        assertTrue(validProjectConfig.setForcedVariation(experiment.getKey(), userProfileId, null));
        assertNull(validProjectConfig.getForcedVariation(experiment.getKey(), userProfileId));


    }

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, String, Map)}
     * gives precedence to user profile over audience evaluation.
     */
    @Test
    public void getVariationEvaluatesUserProfileBeforeAudienceTargeting() throws Exception {
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation variation = experiment.getVariations().get(0);
        Bucketer bucketer = spy(new Bucketer(validProjectConfig));
        Decision decision = new Decision(variation.getId());
        UserProfile userProfile = new UserProfile(userProfileId,
                Collections.singletonMap(experiment.getId(), decision));
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userProfileService.lookup(userProfileId)).thenReturn(userProfile.toMap());

        DecisionService decisionService = spy(new DecisionService(bucketer,
                mockErrorHandler, validProjectConfig, userProfileService));

        // ensure that normal users still get excluded from the experiment when they fail audience evaluation
        assertNull(decisionService.getVariation(experiment, genericUserId, Collections.<String, String>emptyMap()));

        logbackVerifier.expectMessage(Level.INFO,
                "User \"" + genericUserId + "\" does not meet conditions to be in experiment \""
                        + experiment.getKey() + "\".");

        // ensure that a user with a saved user profile, sees the same variation regardless of audience evaluation
        assertEquals(variation,
                decisionService.getVariation(experiment, userProfileId, Collections.<String, String>emptyMap()));

    }

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, String, Map)}
     * gives a null variation on a Experiment that is not running. Set the forced variation.
     * And, test to make sure that after setting forced variation, the getVariation still returns
     * null.
     */
    @Test
    public void getVariationOnNonRunningExperimentWithForcedVariation() {
        Experiment experiment = validProjectConfig.getExperiments().get(1);
        assertFalse(experiment.isRunning());
        Variation variation = experiment.getVariations().get(0);
        Bucketer bucketer = new Bucketer(validProjectConfig);

        DecisionService decisionService = spy(new DecisionService(bucketer,
                mockErrorHandler, validProjectConfig, null));

        // ensure that the not running variation returns null with no forced variation set.
        assertNull(decisionService.getVariation(experiment, "userId", Collections.<String, String>emptyMap()));

        // we call getVariation 3 times on an experiment that is not running.
        logbackVerifier.expectMessage(Level.INFO,
                "Experiment \"etag2\" is not running.", times(3));

        // set a forced variation on the user that got back null
        assertTrue(validProjectConfig.setForcedVariation(experiment.getKey(), "userId", variation.getKey()));

        // ensure that a user with a forced variation set
        // still gets back a null variation if the variation is not running.
        assertNull(decisionService.getVariation(experiment, "userId", Collections.<String, String>emptyMap()));

        // set the forced variation back to null
        assertTrue(validProjectConfig.setForcedVariation(experiment.getKey(), "userId", null));
        // test one more time that the getVariation returns null for the experiment that is not running.
        assertNull(decisionService.getVariation(experiment, "userId", Collections.<String, String>emptyMap()));


    }

    //========== get Variation for Feature tests ==========//

    /**
     * Verify that {@link DecisionService#getVariationForFeature(FeatureFlag, String, Map)}
     * returns null when the {@link FeatureFlag} is not used in an experiments.
     */
    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public void getVariationForFeatureReturnsNullWhenFeatureFlagExperimentIdsIsEmpty() {
        FeatureFlag emptyFeatureFlag = mock(FeatureFlag.class);
        when(emptyFeatureFlag.getExperimentIds()).thenReturn(Collections.<String>emptyList());
        String featureKey = "testFeatureFlagKey";
        when(emptyFeatureFlag.getKey()).thenReturn(featureKey);

        DecisionService decisionService = new DecisionService(
                mock(Bucketer.class),
                mockErrorHandler,
                validProjectConfig,
                null);

        logbackVerifier.expectMessage(Level.INFO,
                "The feature flag \"" + featureKey + "\" is not used in any experiments");

        assertNull(decisionService.getVariationForFeature(
                emptyFeatureFlag,
                genericUserId,
                Collections.<String, String>emptyMap()));

        verify(emptyFeatureFlag, times(1)).getExperimentIds();
        verify(emptyFeatureFlag, times(1)).getKey();
    }

    /**
     * Verify that {@link DecisionService#getVariationForFeature(FeatureFlag, String, Map)}
     * returns null when the user is not bucketed into any experiments for the {@link FeatureFlag}.
     */
    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public void getVariationForFeatureReturnsNullWhenItGetsNoVariationsForExperiments() {
        FeatureFlag spyFeatureFlag = spy(ValidProjectConfigV4.FEATURE_FLAG_MULTI_VARIATE_FEATURE);

        DecisionService spyDecisionService = spy(new DecisionService(
                mock(Bucketer.class),
                mockErrorHandler,
                validProjectConfig,
                null)
        );

        doReturn(null).when(spyDecisionService).getVariation(
                any(Experiment.class),
                anyString(),
                anyMapOf(String.class, String.class)
        );

        assertNull(spyDecisionService.getVariationForFeature(
                spyFeatureFlag,
                genericUserId,
                Collections.<String, String>emptyMap()
        ));

        verify(spyFeatureFlag, times(2)).getExperimentIds();
        verify(spyFeatureFlag, never()).getKey();
    }

    /**
     * Verify that {@link DecisionService#getVariationForFeature(FeatureFlag, String, Map)}
     * returns the variation of the experiment a user gets bucketed into for an experiment.
     */
    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public void getVariationForFeatureReturnsVariationReturnedFromGetVarition() {
        FeatureFlag spyFeatureFlag = spy(ValidProjectConfigV4.FEATURE_FLAG_MUTEX_GROUP_FEATURE);

        DecisionService spyDecisionService = spy(new DecisionService(
                mock(Bucketer.class),
                mockErrorHandler,
                validProjectConfigV4(),
                null)
        );

        doReturn(null).when(spyDecisionService).getVariation(
                eq(ValidProjectConfigV4.EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1),
                anyString(),
                anyMapOf(String.class, String.class)
        );

        doReturn(ValidProjectConfigV4.VARIATION_MUTEX_GROUP_EXP_2_VAR_1).when(spyDecisionService).getVariation(
                eq(ValidProjectConfigV4.EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2),
                anyString(),
                anyMapOf(String.class, String.class)
        );

        assertEquals(ValidProjectConfigV4.VARIATION_MUTEX_GROUP_EXP_2_VAR_1,
                spyDecisionService.getVariationForFeature(
                        spyFeatureFlag,
                        genericUserId,
                        Collections.<String, String>emptyMap()
                ));

        verify(spyFeatureFlag, times(2)).getExperimentIds();
        verify(spyFeatureFlag, never()).getKey();
    }

    //========= white list tests ==========/

    /**
     * Test {@link DecisionService#getWhitelistedVariation(Experiment, String)} correctly returns a whitelisted variation.
     */
    @Test
    public void getWhitelistedReturnsForcedVariation() {
        Bucketer bucketer = new Bucketer(validProjectConfig);
        DecisionService decisionService = new DecisionService(bucketer, mockErrorHandler, validProjectConfig, null);

        logbackVerifier.expectMessage(Level.INFO, "User \"" + whitelistedUserId + "\" is forced in variation \""
                + whitelistedVariation.getKey() + "\".");
        assertEquals(whitelistedVariation, decisionService.getWhitelistedVariation(whitelistedExperiment, whitelistedUserId));
    }

    /**
     * Verify that {@link DecisionService#getWhitelistedVariation(Experiment, String)} returns null
     * when an invalid variation key is found in the forced variations mapping.
     */
    @Test
    public void getWhitelistedWithInvalidVariation() throws Exception {
        String userId = "testUser1";
        String invalidVariationKey = "invalidVarKey";

        Bucketer bucketer = new Bucketer(validProjectConfig);
        DecisionService decisionService = new DecisionService(bucketer, mockErrorHandler, validProjectConfig, null);

        List<Variation> variations = Collections.singletonList(
                new Variation("1", "var1")
        );

        List<TrafficAllocation> trafficAllocations = Collections.singletonList(
                new TrafficAllocation("1", 1000)
        );

        Map<String, String> userIdToVariationKeyMap = Collections.singletonMap(userId, invalidVariationKey);

        Experiment experiment = new Experiment("1234", "exp_key", "Running", "1", Collections.<String>emptyList(),
                variations, userIdToVariationKeyMap, trafficAllocations);

        logbackVerifier.expectMessage(
                Level.ERROR,
                "Variation \"" + invalidVariationKey + "\" is not in the datafile. Not activating user \"" + userId + "\".");

        assertNull(decisionService.getWhitelistedVariation(experiment, userId));
    }

    /**
     * Verify that {@link DecisionService#getWhitelistedVariation(Experiment, String)} returns null when user is not whitelisted.
     */
    @Test
    public void getWhitelistedReturnsNullWhenUserIsNotWhitelisted() throws Exception {
        Bucketer bucketer = new Bucketer(validProjectConfig);
        DecisionService decisionService = new DecisionService(bucketer, mockErrorHandler, validProjectConfig, null);

        assertNull(decisionService.getWhitelistedVariation(whitelistedExperiment, genericUserId));
    }

    //======== User Profile tests =========//

    /**
     * Verify that {@link DecisionService#getStoredVariation(Experiment, UserProfile)} returns a variation that is
     * stored in the provided {@link UserProfile}.
     */
    @SuppressFBWarnings
    @Test
    public void bucketReturnsVariationStoredInUserProfile() throws Exception {
        final Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        final Variation variation = experiment.getVariations().get(0);
        Decision decision = new Decision(variation.getId());

        UserProfile userProfile = new UserProfile(userProfileId,
                Collections.singletonMap(experiment.getId(), decision));
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userProfileService.lookup(userProfileId)).thenReturn(userProfile.toMap());

        Bucketer bucketer = new Bucketer(noAudienceProjectConfig);
        DecisionService decisionService = new DecisionService(bucketer,
                mockErrorHandler,
                noAudienceProjectConfig,
                userProfileService);

        logbackVerifier.expectMessage(Level.INFO,
                "Returning previously activated variation \"" + variation.getKey() + "\" of experiment \"" + experiment.getKey() + "\""
                        + " for user \"" + userProfileId + "\" from user profile.");

        // ensure user with an entry in the user profile is bucketed into the corresponding stored variation
        assertEquals(variation,
                decisionService.getVariation(experiment, userProfileId, Collections.<String, String>emptyMap()));

        verify(userProfileService).lookup(userProfileId);
    }

    /**
     * Verify that {@link DecisionService#getStoredVariation(Experiment, UserProfile)} returns null and logs properly
     * when there is no stored variation for that user in that {@link Experiment} in the {@link UserProfileService}.
     */
    @Test
    public void getStoredVariationLogsWhenLookupReturnsNull() throws Exception {
        final Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);

        Bucketer bucketer = new Bucketer(noAudienceProjectConfig);
        UserProfileService userProfileService = mock(UserProfileService.class);
        UserProfile userProfile = new UserProfile(userProfileId,
                Collections.<String, Decision>emptyMap());
        when(userProfileService.lookup(userProfileId)).thenReturn(userProfile.toMap());

        DecisionService decisionService = new DecisionService(bucketer,
                mockErrorHandler, noAudienceProjectConfig, userProfileService);

        logbackVerifier.expectMessage(Level.INFO, "No previously activated variation of experiment " +
                "\"" + experiment.getKey() + "\" for user \"" + userProfileId + "\" found in user profile.");

        assertNull(decisionService.getStoredVariation(experiment, userProfile));
    }

    /**
     * Verify that {@link DecisionService#getStoredVariation(Experiment, UserProfile)} returns null
     * when a {@link UserProfile} is present, contains a decision for the experiment in question,
     * but the variation ID for that decision does not exist in the datafile.
     */
    @Test
    public void getStoredVariationReturnsNullWhenVariationIsNoLongerInConfig() throws Exception {
        final Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        final String storedVariationId = "missingVariation";
        final Decision storedDecision = new Decision(storedVariationId);
        final Map<String, Decision> storedDecisions = new HashMap<String, Decision>();
        storedDecisions.put(experiment.getId(), storedDecision);
        final UserProfile storedUserProfile = new UserProfile(userProfileId,
                storedDecisions);

        Bucketer bucketer = mock(Bucketer.class);
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userProfileService.lookup(userProfileId)).thenReturn(storedUserProfile.toMap());

        DecisionService decisionService = new DecisionService(bucketer, mockErrorHandler, noAudienceProjectConfig,
                userProfileService);

        logbackVerifier.expectMessage(Level.INFO,
                "User \"" + userProfileId + "\" was previously bucketed into variation with ID \"" + storedVariationId + "\" for " +
                        "experiment \"" + experiment.getKey() + "\", but no matching variation " +
                        "was found for that user. We will re-bucket the user.");

        assertNull(decisionService.getStoredVariation(experiment, storedUserProfile));
    }

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, String, Map)}
     * saves a {@link Variation}of an {@link Experiment} for a user when a {@link UserProfileService} is present.
     */
    @SuppressFBWarnings
    @Test
    public void getVariationSavesBucketedVariationIntoUserProfile() throws Exception {
        final Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        final Variation variation = experiment.getVariations().get(0);
        Decision decision = new Decision(variation.getId());

        UserProfileService userProfileService = mock(UserProfileService.class);
        UserProfile originalUserProfile = new UserProfile(userProfileId,
                new HashMap<String, Decision>());
        when(userProfileService.lookup(userProfileId)).thenReturn(originalUserProfile.toMap());
        UserProfile expectedUserProfile = new UserProfile(userProfileId,
                Collections.singletonMap(experiment.getId(), decision));

        Bucketer mockBucketer = mock(Bucketer.class);
        when(mockBucketer.bucket(experiment, userProfileId)).thenReturn(variation);

        DecisionService decisionService = new DecisionService(mockBucketer,
                mockErrorHandler, noAudienceProjectConfig, userProfileService);

        assertEquals(variation, decisionService.getVariation(experiment, userProfileId, Collections.<String, String>emptyMap()));
        logbackVerifier.expectMessage(Level.INFO,
                String.format("Saved variation \"%s\" of experiment \"%s\" for user \"" + userProfileId + "\".", variation.getId(),
                        experiment.getId()));

        verify(userProfileService).save(eq(expectedUserProfile.toMap()));
    }

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, String, Map)} logs correctly
     * when a {@link UserProfileService} is present but fails to save an activation.
     */
    @Test
    public void bucketLogsCorrectlyWhenUserProfileFailsToSave() throws Exception {
        final Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        final Variation variation = experiment.getVariations().get(0);
        Decision decision = new Decision(variation.getId());
        Bucketer bucketer = new Bucketer(noAudienceProjectConfig);
        UserProfileService userProfileService = mock(UserProfileService.class);
        doThrow(new Exception()).when(userProfileService).save(anyMapOf(String.class, Object.class));

        Map<String, Decision> experimentBucketMap = new HashMap<String, Decision>();
        experimentBucketMap.put(experiment.getId(), decision);
        UserProfile expectedUserProfile = new UserProfile(userProfileId,
                experimentBucketMap);
        UserProfile saveUserProfile = new UserProfile(userProfileId,
                new HashMap<String, Decision>());

        DecisionService decisionService = new DecisionService(bucketer,
                mockErrorHandler, noAudienceProjectConfig, userProfileService);


        decisionService.saveVariation(experiment, variation, saveUserProfile);

        logbackVerifier.expectMessage(Level.WARN,
                String.format("Failed to save variation \"%s\" of experiment \"%s\" for user \"" + userProfileId + "\".", variation.getId(),
                        experiment.getId()));

        verify(userProfileService).save(eq(expectedUserProfile.toMap()));
    }

    /**
     * Verify that a {@link UserProfile} is saved when the user is brand new and did not have anything returned from
     * {@link UserProfileService#lookup(String)}.
     */
    @Test
    public void getVariationSavesANewUserProfile() throws Exception {
        final Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        final Variation variation = experiment.getVariations().get(0);
        final Decision decision = new Decision(variation.getId());
        final UserProfile expectedUserProfile = new UserProfile(userProfileId,
                Collections.singletonMap(experiment.getId(), decision));

        Bucketer bucketer = mock(Bucketer.class);
        UserProfileService userProfileService = mock(UserProfileService.class);
        DecisionService decisionService = new DecisionService(bucketer, mockErrorHandler, noAudienceProjectConfig,
                userProfileService);

        when(bucketer.bucket(experiment, userProfileId)).thenReturn(variation);
        when(userProfileService.lookup(userProfileId)).thenReturn(null);

        assertEquals(variation, decisionService.getVariation(experiment, userProfileId, Collections.<String, String>emptyMap()));
        verify(userProfileService).save(expectedUserProfile.toMap());
    }
}
