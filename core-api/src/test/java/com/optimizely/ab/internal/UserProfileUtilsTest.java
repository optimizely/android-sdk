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
package com.optimizely.ab.internal;

import com.optimizely.ab.bucketing.UserProfile;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Test;

import java.util.Collections;

import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserProfileUtilsTest {

    static final private String userId = "userId";


    /**
     * Verify {@link UserProfileUtils#cleanUserProfiles(UserProfile, ProjectConfig)} handles a null {@link UserProfile}.
     */
    @Test
    public void nullUserProfileWhenCleaning() {
        UserProfileUtils.cleanUserProfiles(null, validProjectConfigV2());
    }

    /**
     * Verify {@link UserProfileUtils#cleanUserProfiles(UserProfile, ProjectConfig)} handles a null returned from
     * {@link UserProfile#getAllRecords()}.
     */
    @SuppressFBWarnings
    @Test
    public void nullUserProfiles() throws Exception {
        // mock User Profile
        UserProfile userProfile = mock(UserProfile.class);

        UserProfileUtils.cleanUserProfiles(userProfile, validProjectConfigV2());
        verify(userProfile).getAllRecords();
    }

    /**
     * Verify {@link UserProfileUtils#cleanUserProfiles(UserProfile, ProjectConfig)} removes experiments
     * that are no longer in the {@link ProjectConfig}.
     */
    @Test
    public void cleanRemovesRecordsOfExperimentsThatNoLongerExist() throws Exception {
        // mock user profile
        UserProfile userProfile = mock(UserProfile.class);

        String experimentId = "experimentId";
        String variationId = "variationId";

        when(userProfile.getAllRecords()).thenReturn(Collections.singletonMap(userId, Collections.singletonMap(experimentId, variationId)));

        // clean user profile
        UserProfileUtils.cleanUserProfiles(userProfile, validProjectConfigV2());

        verify(userProfile).remove(userId, experimentId);
        assertNull(userProfile.lookup(userId, experimentId));
    }

    /**
     * Verify {@link UserProfileUtils#cleanUserProfiles(UserProfile, ProjectConfig)} removes experiments
     * if the variation stored for that experiment no longer exists in the {@link ProjectConfig}.
     */
    @Test
    public void cleanRemovesRecordsOfExperimentsWhereTheVariationNoLongerExists() {
        // mock user profile
        UserProfile userProfile = mock(UserProfile.class);

        String experimentId = null;
        String variationId = "someOldVariationId";
        for (Experiment experiment : validProjectConfigV2().getExperiments()) {
            if(experiment.isActive()) {
                experimentId = experiment.getId();
                break;
            }
        }

        // make sure experiment id is not null and that the variation id we set does not appear in the experiment's variations
        assertNotNull(experimentId);
        for (Variation variation : validProjectConfigV2().getExperimentIdMapping().get(experimentId).getVariations()) {
            assertNotEquals(variationId, variation.getId());
        }

        when(userProfile.getAllRecords()).thenReturn(Collections.singletonMap(userId, Collections.singletonMap(experimentId, variationId)));

        // clean user profile
        UserProfileUtils.cleanUserProfiles(userProfile, validProjectConfigV2());

        verify(userProfile).remove(userId, experimentId);
    }

    /**
     * Verify {@link UserProfileUtils#cleanUserProfiles(UserProfile, ProjectConfig)} removes experiments
     * that are paused in the {@link ProjectConfig}.
     */
    @Test
    public void cleanRemovesRecordsOfExperimentsThatAreNotActive() throws Exception {
        // make mock User Profile
        UserProfile userProfile = mock(UserProfile.class);

        String experimentId = null;
        String variationId = null;
        for (Experiment experiment : validProjectConfigV2().getExperiments()) {
            if (!experiment.isActive()) {
                experimentId = experiment.getId();
                variationId = experiment.getVariations().get(0).getId();
                break;
            }
        }

        assertNotNull(experimentId);
        assertNotNull(variationId);

        when(userProfile.getAllRecords()).thenReturn(Collections.singletonMap(userId, Collections.singletonMap(experimentId, variationId)));

        // clean user profile
        UserProfileUtils.cleanUserProfiles(userProfile, validProjectConfigV2());

        verify(userProfile).remove(userId, experimentId);
    }
}
