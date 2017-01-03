/*
 *    Copyright 2017, Optimizely
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

import com.optimizely.ab.bucketing.internal.MurmurHash3;
import com.optimizely.ab.categories.ExhaustiveTest;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.internal.LogbackVerifier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ch.qos.logback.classic.Level;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link Bucketer}.
 */
@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class BucketerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    /**
     * Verify that {@link Bucketer#generateBucketValue(int)} correctly handles negative hashCodes.
     */
    @Test
    public void generateBucketValueForNegativeHashCodes() throws Exception {
        Bucketer algorithm = new Bucketer(validProjectConfigV2());
        int actual = algorithm.generateBucketValue(-1);
        assertTrue("generated bucket value is not in range: " + actual,
                   actual > 0 && actual < Bucketer.MAX_TRAFFIC_VALUE);
    }

    /**
     * Verify that across the entire 32-bit hashCode space, all generated bucket values fall within the range
     * [0, {@link Bucketer#MAX_TRAFFIC_VALUE}) and that there's an even distribution over 50/50 split.
     */
    @Test
    @Category(ExhaustiveTest.class)
    public void generateBucketValueDistribution() throws Exception {
        long lowerHalfCount = 0;
        long totalCount = 0;
        int outOfRangeCount = 0;

        Bucketer algorithm = new Bucketer(validProjectConfigV2());
        for (int i = Integer.MIN_VALUE; i < Integer.MAX_VALUE; i++) {
            int bucketValue = algorithm.generateBucketValue(i);

            totalCount++;
            if (bucketValue < (Bucketer.MAX_TRAFFIC_VALUE / 2)) {
                lowerHalfCount++;
            }
            if (bucketValue < 0 || bucketValue >= Bucketer.MAX_TRAFFIC_VALUE) {
                outOfRangeCount++;
            }
        }

        // verify that all values are in the expected range and that 50% of the values are in the lower half
        assertThat(outOfRangeCount, is(0));
        assertThat(Math.round(((double)lowerHalfCount/totalCount) * 100), is(50L));
    }

    /**
     * Verify that generated bucket values match expected output.
     */
    @Test
    public void bucketNumberGeneration() throws Exception {
        int MURMUR_HASH_SEED = 1;
        int experimentId = 1886780721;
        int hashCode;

        Bucketer algorithm = new Bucketer(validProjectConfigV2());

        String combinedBucketId;

        combinedBucketId = "ppid1" + experimentId;
        hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length(), MURMUR_HASH_SEED);
        assertThat(algorithm.generateBucketValue(hashCode), is(5254));

        combinedBucketId = "ppid2" + experimentId;
        hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length(), MURMUR_HASH_SEED);
        assertThat(algorithm.generateBucketValue(hashCode), is(4299));

        combinedBucketId = "ppid2" + (experimentId + 1);
        hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length(), MURMUR_HASH_SEED);
        assertThat(algorithm.generateBucketValue(hashCode), is(2434));

        combinedBucketId = "ppid3" + experimentId;
        hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length(), MURMUR_HASH_SEED);
        assertThat(algorithm.generateBucketValue(hashCode), is(5439));

        combinedBucketId = "a very very very very very very very very very very very very very very very long ppd " +
                           "string" + experimentId;
        hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length(), MURMUR_HASH_SEED);
        assertThat(algorithm.generateBucketValue(hashCode), is(6128));
    }

    /**
     * Given an experiment with 4 variations, verify that bucket values are correctly mapped to the associated range.
     */
    @Test
    public void bucketToMultipleVariations() throws Exception {
        List<String> audienceIds = Collections.emptyList();

        // create an experiment with 4 variations using ranges: [0 -> 999, 1000 -> 4999, 5000 -> 5999, 6000 -> 9999]
        List<Variation> variations = Arrays.asList(
            new Variation("1", "var1"),
            new Variation("2", "var2"),
            new Variation("3", "var3"),
            new Variation("4", "var4")
        );

        List<TrafficAllocation> trafficAllocations = Arrays.asList(
            new TrafficAllocation("1", 1000),
            new TrafficAllocation("2", 5000),
            new TrafficAllocation("3", 6000),
            new TrafficAllocation("4", 10000)
        );

        Experiment experiment = new Experiment("1234", "exp_key", "Running", "1", audienceIds, variations,
                                               Collections.<String, String>emptyMap(), trafficAllocations, "");

        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer algorithm = mockBucketAlgorithm(bucketValue);

        // verify bucketing to the first variation
        bucketValue.set(0);
        assertThat(algorithm.bucket(experiment, "blah"), is(variations.get(0)));
        bucketValue.set(500);
        assertThat(algorithm.bucket(experiment, "blah"), is(variations.get(0)));
        bucketValue.set(999);
        assertThat(algorithm.bucket(experiment, "blah"), is(variations.get(0)));

        // verify the second variation
        bucketValue.set(1000);
        assertThat(algorithm.bucket(experiment, "blah"), is(variations.get(1)));
        bucketValue.set(4000);
        assertThat(algorithm.bucket(experiment, "blah"), is(variations.get(1)));
        bucketValue.set(4999);
        assertThat(algorithm.bucket(experiment, "blah"), is(variations.get(1)));

        // ...and the rest
        bucketValue.set(5100);
        assertThat(algorithm.bucket(experiment, "blah"), is(variations.get(2)));
        bucketValue.set(6500);
        assertThat(algorithm.bucket(experiment, "blah"), is(variations.get(3)));
    }

    /**
     * Verify that in certain cases, users aren't assigned any variation and null is returned.
     */
    @Test
    public void bucketToControl() throws Exception {
        List<String> audienceIds = Collections.emptyList();

        List<Variation> variations = Collections.singletonList(
            new Variation("1", "var1")
        );

        List<TrafficAllocation> trafficAllocations = Collections.singletonList(
            new TrafficAllocation("1", 999)
        );

        Experiment experiment = new Experiment("1234", "exp_key", "Running", "1", audienceIds, variations,
                                               Collections.<String, String>emptyMap(), trafficAllocations, "");

        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer algorithm = mockBucketAlgorithm(bucketValue);

        logbackVerifier.expectMessage(Level.DEBUG, "Assigned bucket 0 to user \"blah\" during variation bucketing.");
        logbackVerifier.expectMessage(Level.INFO, "User \"blah\" is in variation \"var1\" of experiment \"exp_key\".");

        // verify bucketing to the first variation
        bucketValue.set(0);
        assertThat(algorithm.bucket(experiment, "blah"), is(variations.get(0)));

        logbackVerifier.expectMessage(Level.DEBUG, "Assigned bucket 1000 to user \"blah\" during variation bucketing.");
        logbackVerifier.expectMessage(Level.INFO, "User \"blah\" is not in any variation of experiment \"exp_key\".");

        // verify bucketing to no variation (null)
        bucketValue.set(1000);
        assertNull(algorithm.bucket(experiment, "blah"));
    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment, String)} returns the specified forced variation for user.
     */
    @Test
    public void bucketUserInForcedVariation() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer algorithm = mockBucketAlgorithm(bucketValue);

        List<Variation> variations = Collections.singletonList(
            new Variation("1", "var1")
        );

        List<TrafficAllocation> trafficAllocations = Collections.singletonList(
            new TrafficAllocation("1", 1000)
        );

        Map<String, String> userIdToVariationKeyMap = Collections.singletonMap("testUser1", "var1");

        Experiment experiment = new Experiment("1234", "exp_key", "Running", "1", Collections.<String>emptyList(),
                                               variations, userIdToVariationKeyMap, trafficAllocations, "");

        logbackVerifier.expectMessage(Level.INFO, "User \"testUser1\" is forced in variation \"var1\".");

        // user is assigned bucket value of 1001 which is out of the traffic allocation range, but the user should
        // still be forced into var1 because of the forcedVariation mapping.
        bucketValue.set(1001);
        assertThat(algorithm.bucket(experiment, "testUser1").getKey(), is("var1"));
    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment, String)} returns the proper variation when the user doesn't
     * have a forced variation mapping.
     */
    @Test
    public void bucketUserNotInForcedVariation() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer algorithm = mockBucketAlgorithm(bucketValue);

        List<String> audienceIds = Collections.emptyList();

        List<Variation> variations = Arrays.asList(
            new Variation("1", "var1"),
            new Variation("2", "var2")
        );

        List<TrafficAllocation> trafficAllocations = Arrays.asList(
            new TrafficAllocation("1", 1000),
            new TrafficAllocation("2", 2000)
        );

        Map<String, String> userIdToVariationKeyMap = Collections.singletonMap("testUser1", "var1");

        Experiment experiment = new Experiment("1234", "exp_key", "Running", "1", audienceIds, variations,
                                               userIdToVariationKeyMap, trafficAllocations, "");
        bucketValue.set(1500);
        assertThat(algorithm.bucket(experiment, "testUser2").getKey(), is("var2"));
    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment, String)} returns null when an invalid variation key is found
     * in the forced variations mapping.
     */
    @Test
    public void bucketInvalidVariationKeyForcedVariation() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer algorithm = mockBucketAlgorithm(bucketValue);

        List<Variation> variations = Collections.singletonList(
            new Variation("1", "var1")
        );

        List<TrafficAllocation> trafficAllocations = Collections.singletonList(
            new TrafficAllocation("1", 1000)
        );

        Map<String, String> userIdToVariationKeyMap = Collections.singletonMap("testUser1", "invalidVarKey");

        Experiment experiment = new Experiment("1234", "exp_key", "Running", "1", Collections.<String>emptyList(),
                                               variations, userIdToVariationKeyMap, trafficAllocations);

        logbackVerifier.expectMessage(
                Level.ERROR,
                "Variation \"invalidVarKey\" is not in the datafile. Not activating user \"testUser1\".");

        bucketValue.set(0);
        assertNull(algorithm.bucket(experiment, "testUser1"));
    }

    /**
     * Verifies the positive case where a non-empty user id is provided.
     */
    @Test
    public void checkPreconditionsForValidUserId() throws Exception {
        ProjectConfig projectConfig = validProjectConfigV2();
        Bucketer algorithm = new Bucketer(projectConfig);
        Experiment experiment = projectConfig.getExperiments().get(0);
        algorithm.bucket(experiment, "1234");
    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment, String)} returns the proper variation when a user is
     * in the group experiment.
     */
    @Test
    public void bucketUserInExperiment() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer algorithm = mockBucketAlgorithm(bucketValue);
        bucketValue.set(3000);

        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(0).getExperiments();
        Experiment groupExperiment = groupExperiments.get(0);
        logbackVerifier.expectMessage(Level.DEBUG,
                                      "Assigned bucket 3000 to user \"blah\" during experiment bucketing.");
        logbackVerifier.expectMessage(Level.INFO, "User \"blah\" is in experiment \"group_etag2\" of group 42.");
        logbackVerifier.expectMessage(Level.DEBUG, "Assigned bucket 3000 to user \"blah\" during variation bucketing.");
        logbackVerifier.expectMessage(Level.INFO,
                                      "User \"blah\" is in variation \"e2_vtag1\" of experiment \"group_etag2\".");
        assertThat(algorithm.bucket(groupExperiment, "blah"), is(groupExperiment.getVariations().get(0)));
    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment, String)} doesn't return a variation when a user isn't bucketed
     * into the group experiment.
     */
    @Test
    public void bucketUserNotInExperiment() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer algorithm = mockBucketAlgorithm(bucketValue);
        bucketValue.set(3000);

        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(0).getExperiments();
        Experiment groupExperiment = groupExperiments.get(1);
        // the user should be bucketed to a different experiment than the one provided, resulting in no variation being
        // returned.
        logbackVerifier.expectMessage(Level.DEBUG,
                                      "Assigned bucket 3000 to user \"blah\" during experiment bucketing.");
        logbackVerifier.expectMessage(Level.INFO,
                                      "User \"blah\" is not in experiment \"group_etag1\" of group 42");
        assertNull(algorithm.bucket(groupExperiment, "blah"));
    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment, String)} gives higher priority to forced bucketing than to
     * experiment bucketing.
     */
    @Test
    public void bucketUserToForcedVariationOverridesExperimentBucketing() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer algorithm = mockBucketAlgorithm(bucketValue);
        bucketValue.set(2000);

        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(0).getExperiments();
        Experiment groupExperiment = groupExperiments.get(1);

        // normally, without forced bucketing this user would be bucketed to variation e1_vtag1, but instead is forced
        // into e1_vtag2.
        logbackVerifier.expectMessage(Level.INFO, "User \"testUser2\" is forced in variation \"e1_vtag2\".");
        assertThat(algorithm.bucket(groupExperiment, "testUser2"), is(groupExperiment.getVariations().get(1)));
    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment, String)} doesn't return a variation when the user is bucketed to
     * the traffic space of a deleted experiment within a random group.
     */
    @Test
    public void bucketUserToDeletedExperimentSpace() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer algorithm = mockBucketAlgorithm(bucketValue);
        bucketValue.set(9000);

        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(0).getExperiments();
        Experiment groupExperiment = groupExperiments.get(1);

        logbackVerifier.expectMessage(Level.INFO, "User \"blah\" is not in any experiment of group 42.");
        assertNull(algorithm.bucket(groupExperiment, "blah"));
    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment, String)} returns a variation when the user falls into an
     * experiment within an overlapping group.
     */
    @Test
    public void bucketUserToVariationInOverlappingGroupExperiment() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer algorithm = mockBucketAlgorithm(bucketValue);
        bucketValue.set(0);

        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(1).getExperiments();
        Experiment groupExperiment = groupExperiments.get(0);
        Variation expectedVariation = groupExperiment.getVariations().get(0);

        logbackVerifier.expectMessage(
                Level.INFO,
                "User \"blah\" is in variation \"e1_vtag1\" of experiment \"overlapping_etag1\".");
        assertThat(algorithm.bucket(groupExperiment, "blah"), is(expectedVariation));
    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment, String)} doesn't return a variation when the user doesn't fall
     * into an experiment within an overlapping group.
     */
    @Test
    public void bucketUserNotInOverlappingGroupExperiment() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer algorithm = mockBucketAlgorithm(bucketValue);
        bucketValue.set(3000);

        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(1).getExperiments();
        Experiment groupExperiment = groupExperiments.get(0);

        logbackVerifier.expectMessage(Level.INFO,
                                      "User \"blah\" is not in any variation of experiment \"overlapping_etag1\".");

        assertNull(algorithm.bucket(groupExperiment, "blah"));
    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment,String)} saves a variation of an experiment for a user
     * when a {@link UserProfile} is present.
     */
    @Test public void bucketUserSaveActivationWithUserProfile() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        UserProfile userProfile = mock(UserProfile.class);
        Bucketer algorithm = mockUserProfileAlgorithm(bucketValue, userProfile);
        bucketValue.set(3000);

        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(0).getExperiments();
        Experiment groupExperiment = groupExperiments.get(0);
        final Variation variation = groupExperiment.getVariations().get(0);

        when(userProfile.save("blah", groupExperiment.getKey(), variation.getKey())).thenReturn(true);

        assertThat(algorithm.bucket(groupExperiment, "blah"),  is(variation));

        logbackVerifier.expectMessage(Level.INFO,
                "Saved variation \"e2_vtag1\" of experiment \"group_etag2\" for user \"blah\".");

        verify(userProfile).save("blah", groupExperiment.getKey(), variation.getKey());
    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment,String)} logs correctly
     * when a {@link UserProfile} is present and fails to save an activation.
     */
    @Test public void bucketUserSaveActivationFailWithUserProfile() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        UserProfile userProfile = mock(UserProfile.class);
        Bucketer algorithm = mockUserProfileAlgorithm(bucketValue, userProfile);
        bucketValue.set(3000);

        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(0).getExperiments();
        Experiment groupExperiment = groupExperiments.get(0);
        final Variation variation = groupExperiment.getVariations().get(0);

        when(userProfile.save("blah", groupExperiment.getKey(), variation.getKey())).thenReturn(false);

        assertThat(algorithm.bucket(groupExperiment, "blah"),  is(variation));

        logbackVerifier.expectMessage(Level.WARN,
                "Failed to save variation \"e2_vtag1\" of experiment \"group_etag2\" for user \"blah\".");

        verify(userProfile).save("blah", groupExperiment.getKey(), variation.getKey());
    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment,String)} returns a variation that is
     * stored in the provided {@link UserProfile}.
     */
    @Test public void bucketUserRestoreActivationWithUserProfile() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        UserProfile userProfile = mock(UserProfile.class);
        Bucketer algorithm = mockUserProfileAlgorithm(bucketValue, userProfile);
        bucketValue.set(3000);

        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(0).getExperiments();
        Experiment groupExperiment = groupExperiments.get(0);
        final Variation variation = groupExperiment.getVariations().get(0);

        when(userProfile.lookup("blah", groupExperiment.getKey())).thenReturn(variation.getKey());

        assertThat(algorithm.bucket(groupExperiment, "blah"),  is(variation));

        logbackVerifier.expectMessage(Level.INFO,
                "Returning previously activated variation \"e2_vtag1\" of experiment \"group_etag2\""
                                      + " for user \"blah\" from user profile.");

        verify(userProfile).lookup("blah", groupExperiment.getKey());
    }

    /**
     * Verify {@link Bucketer#bucket(Experiment,String)} handles a present {@link UserProfile}
     * returning null when looking up a variation.
     */
    @Test public void bucketUserRestoreActivationNullWithUserProfile() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        UserProfile userProfile = mock(UserProfile.class);
        Bucketer algorithm = mockUserProfileAlgorithm(bucketValue, userProfile);
        bucketValue.set(3000);

        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(0).getExperiments();
        Experiment groupExperiment = groupExperiments.get(0);
        final Variation variation = groupExperiment.getVariations().get(0);

        when(userProfile.lookup("blah", groupExperiment.getKey())).thenReturn(null);

        assertThat(algorithm.bucket(groupExperiment, "blah"),  is(variation));

        logbackVerifier.expectMessage(Level.INFO, "No previously activated variation of experiment " +
                                      "\"group_etag2\" for user \"blah\" found in user profile.");
        verify(userProfile).lookup("blah", groupExperiment.getKey());
    }

    /**
     * Verify {@link Bucketer#cleanUserProfiles()} handles a null {@link UserProfile}.
     */
    @Test
    public void nullUserProfileWhenCleaning() {
        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer algorithm = mockBucketAlgorithm(bucketValue);
        bucketValue.set(3000);
        try {
            algorithm.cleanUserProfiles();
        } catch (NullPointerException e) {
            fail();
        }
    }

    /**
     * Verify {@link Bucketer#cleanUserProfiles()} handles a null returned from
     * {@link UserProfile#getAllRecords()}.
     */
    @Test
    public void nullUserProfiles() {
        final AtomicInteger bucketValue = new AtomicInteger();
        UserProfile userProfile = mock(UserProfile.class);
        Bucketer algorithm = mockUserProfileAlgorithm(bucketValue, userProfile);
        bucketValue.set(3000);

        when(userProfile.getAllRecords()).thenReturn(null);
        try {
            algorithm.cleanUserProfiles();
        } catch (NullPointerException e) {
            fail();
        }
    }

    /**
     * Verify {@link Bucketer#cleanUserProfiles()} removes experiments
     * that are no longer in the {@link ProjectConfig}.
     */
    @Test
    public void cleanRemovesRecordsOfExperimentsThatNoLongerExist() {
        final AtomicInteger bucketValue = new AtomicInteger();
        UserProfile userProfile = mock(UserProfile.class);
        Bucketer algorithm = mockUserProfileAlgorithm(bucketValue, userProfile);
        bucketValue.set(3000);

        Map<String,Map<String,String>> records = new HashMap<String, Map<String, String>>();
        Map<String,String> activation = new HashMap<String, String>();
        activation.put("exp1", "var1");
        records.put("blah",  activation);
        when(userProfile.getAllRecords()).thenReturn(records);

        algorithm.cleanUserProfiles();

        verify(userProfile).remove("blah", "exp1");
    }

    /**
     * Verify {@link Bucketer#cleanUserProfiles()} removes experiments
     * that are paused in the {@link ProjectConfig}.
     */
    @Test
    public void cleanRemovesRecordsOfExperimentsThatAreNotRunning() {
        final AtomicInteger bucketValue = new AtomicInteger();
        UserProfile userProfile = mock(UserProfile.class);
        Bucketer algorithm = mockUserProfileAlgorithm(bucketValue, userProfile);
        bucketValue.set(3000);

        Map<String,Map<String,String>> records = new HashMap<String, Map<String, String>>();
        Map<String,String> activation = new HashMap<String, String>();
        activation.put("exp1", "var1");
        records.put("blah",  activation);
        when(userProfile.getAllRecords()).thenReturn(records);

        algorithm.cleanUserProfiles();

        verify(userProfile).remove("blah", "exp1");
    }

    //======== Helper methods ========//

    /**
     * Sets up a mock algorithm that returns an expected bucket value.
     * @param bucketValue the expected bucket value holder
     * @return the mock bucket algorithm
     */
    private Bucketer mockBucketAlgorithm(final AtomicInteger bucketValue) {
        return new Bucketer(validProjectConfigV2()) {
            @Override
            int generateBucketValue(int hashCode) {
                return bucketValue.get();
            }
        };
    }

    /**
     * Sets up a mock algorithm that returns an expected bucket value.
     *
     * Includes a composed {@link UserProfile} mock instance
     *
     * @param bucketValue the expected bucket value holder
     * @return the mock bucket algorithm
     */
    private Bucketer mockUserProfileAlgorithm(final AtomicInteger bucketValue, final UserProfile userProfile) {
        return new Bucketer(validProjectConfigV2(), userProfile) {
            @Override
            int generateBucketValue(int hashCode) {
                return bucketValue.get();
            }
        };
    }
}
