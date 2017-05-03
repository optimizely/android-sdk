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
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.internal.LogbackVerifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV3;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DecisionServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(DecisionServiceTest.class);
    private static final String genericUserId = "genericUserId";
    private static final String whitelistedUserId = "testUser1";
    private static final String userProfileId = "userProfileId";

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
     * Verify that {@link DecisionService#getVariation(Experiment, String, Map)} gives precedence to forced variation bucketing
     * over audience evaluation.
     */
    @Test
    public void getVariationForcedVariationPrecedesAudienceEval() throws Exception {
        Bucketer bucketer = spy(new Bucketer(validProjectConfig));
        DecisionService decisionService = spy(new DecisionService(bucketer, validProjectConfig, null));
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation expectedVariation = experiment.getVariations().get(0);

        // user excluded without audiences and whitelisting
        assertNull(decisionService.getVariation(experiment, genericUserId, Collections.<String, String>emptyMap()));

        logbackVerifier.expectMessage(Level.INFO, "User \"" + whitelistedUserId + "\" is forced in variation \"vtag1\".");

        // no attributes provided for a experiment that has an audience
        assertThat(decisionService.getVariation(experiment, whitelistedUserId, Collections.<String, String>emptyMap()), is(expectedVariation));

        verify(decisionService).getWhitelistedVariation(experiment, whitelistedUserId);
        verify(decisionService, never()).getStoredVariation(experiment, whitelistedUserId);
    }

    //========= white list tests ==========/

    /**
     * Test {@link DecisionService#getWhitelistedVariation(Experiment, String)} correctly returns a whitelisted variation.
     */
    @Test
    public void getForcedVariationReturnsForcedVariation() {
        Bucketer bucketer = new Bucketer(validProjectConfig);
        DecisionService decisionService = new DecisionService(bucketer, validProjectConfig, null);

        logbackVerifier.expectMessage(Level.INFO, "User \"" + whitelistedUserId + "\" is forced in variation \""
                + whitelistedVariation.getKey() + "\".");
        assertEquals(whitelistedVariation, decisionService.getWhitelistedVariation(whitelistedExperiment, whitelistedUserId));
    }

    /**
     * Verify that {@link DecisionService#getWhitelistedVariation(Experiment, String)} returns null
     * when an invalid variation key is found in the forced variations mapping.
     */
    @Test
    public void getForcedVariationWithInvalidVariation() throws Exception {
        String userId = "testUser1";
        String invalidVariationKey = "invalidVarKey";

        Bucketer bucketer = new Bucketer(validProjectConfig);
        DecisionService decisionService = new DecisionService(bucketer, validProjectConfig, null);

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
    public void getForcedVariationReturnsNullWhenUserIsNotWhitelisted() throws Exception {
        Bucketer bucketer = new Bucketer(validProjectConfig);
        DecisionService decisionService = new DecisionService(bucketer, validProjectConfig, null);

        assertNull(decisionService.getWhitelistedVariation(whitelistedExperiment, genericUserId));
    }

    //======== User Profile tests =========//

    /**
     * Verify that {@link DecisionService#getStoredVariation(Experiment, String)} returns a variation that is
     * stored in the provided {@link UserProfile}.
     */
    @SuppressFBWarnings
    @Test
    public void bucketReturnsVariationStoredInUserProfile() throws Exception {
        final Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        final Variation storedVariation = experiment.getVariations().get(0);

        UserProfile userProfile = mock(UserProfile.class);
        when(userProfile.lookup(userProfileId, experiment.getId())).thenReturn(storedVariation.getId());

        Bucketer bucketer = new Bucketer(noAudienceProjectConfig);
        DecisionService decisionService = new DecisionService(bucketer, noAudienceProjectConfig, userProfile);

        logbackVerifier.expectMessage(Level.INFO,
                "Returning previously activated variation \"" + storedVariation.getKey() + "\" of experiment \"" + experiment.getKey() + "\""
                        + " for user \"" + userProfileId + "\" from user profile.");

        // ensure user with an entry in the user profile is bucketed into the corresponding stored variation
        assertEquals(storedVariation, decisionService.getStoredVariation(experiment, userProfileId));

        verify(userProfile).lookup(userProfileId, experiment.getId());
    }

    /**
     * Verify that {@link DecisionService#getStoredVariation(Experiment, String)} returns null and logs properly
     * when there is no stored variation for that user in that @{link Experiment} in the {@link UserProfile}.
     */
    @Test
    public void getStoredVariationLogsWhenLookupReturnsNull() {
        final String userId = "someUser";

        UserProfile userProfile = mock(UserProfile.class);
        Bucketer bucketer = new Bucketer(noAudienceProjectConfig);
        DecisionService decisionService = new DecisionService(bucketer, noAudienceProjectConfig, userProfile);
        final Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);

        when(userProfile.lookup(userId, experiment.getId())).thenReturn(null);

        logbackVerifier.expectMessage(Level.INFO, "No previously activated variation of experiment " +
                "\"" + experiment.getKey() + "\" for user \"" +userId + "\" found in user profile.");

        assertNull(decisionService.getStoredVariation(experiment, userId));
    }

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, String, Map)}
     * saves a {@link Variation}of an {@link Experiment} for a user when a {@link UserProfile} is present.
     */
    @SuppressFBWarnings
    @Test
    public void getVariationSavesBucketedVariationIntoUserProfile() throws Exception {
        final Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        final Variation variation = experiment.getVariations().get(0);

        UserProfile userProfile = mock(UserProfile.class);
        when(userProfile.save(genericUserId, experiment.getId(), variation.getId())).thenReturn(true);

        Bucketer mockBucketer = mock(Bucketer.class);
        when(mockBucketer.bucket(experiment, genericUserId)).thenReturn(variation);

        DecisionService decisionService = new DecisionService(mockBucketer, noAudienceProjectConfig, userProfile);




        assertThat(decisionService.getVariation(experiment, genericUserId, Collections.<String, String>emptyMap()),  is(variation));
        logbackVerifier.expectMessage(Level.INFO,
                String.format("Saved variation \"%s\" of experiment \"%s\" for user \"" + genericUserId + "\".", variation.getId(),
                        experiment.getId()));

        verify(userProfile).save(genericUserId, experiment.getId(), variation.getId());
    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment,String)} logs correctly
     * when a {@link UserProfile} is present but fails to save an activation.
     */
    @Test
    public void bucketLogsCorrectlyWhenUserProfileFailsToSave() throws Exception {

        UserProfile userProfile = mock(UserProfile.class);
        Bucketer bucketer = new Bucketer(noAudienceProjectConfig);
        DecisionService decisionService = new DecisionService(bucketer, noAudienceProjectConfig, userProfile);
        final Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        final Variation variation = experiment.getVariations().get(0);

        when(userProfile.save(userProfileId, experiment.getId(), variation.getId())).thenReturn(false);

        decisionService.storeVariation(experiment, variation, userProfileId);

        logbackVerifier.expectMessage(Level.WARN,
                String.format("Failed to save variation \"%s\" of experiment \"%s\" for user \"" + userProfileId + "\".", variation.getId(),
                        experiment.getId()));

        verify(userProfile).save(userProfileId, experiment.getId(), variation.getId());
    }
}
