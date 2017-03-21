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

import ch.qos.logback.classic.Level;
import com.optimizely.ab.Optimizely;
import com.optimizely.ab.bucketing.UserProfile;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.EventHandler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV1;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV1;
import static com.optimizely.ab.internal.ProjectValidationUtils.validatePreconditions;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectValidationUtilsTestV1 {

    @Rule
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();
    @Mock EventHandler mockEventHandler;

    private static String datafile;
    private static ProjectConfig projectConfig;

    @BeforeClass
    public static void setUp() throws IOException {
        datafile = validConfigJsonV1();
        projectConfig = validProjectConfigV1();
    }

    /**
     * Verify that
     * {@link ProjectValidationUtils#validatePreconditions(ProjectConfig, UserProfile, Experiment, String, Map)} gives
     * precedence to forced variation bucketing over audience evaluation.
     */
    @Test
    public void validatePreconditionsForcedVariationPrecedesAudienceEval() throws Exception {
        Experiment experiment = projectConfig.getExperiments().get(0);

        assertTrue(
                validatePreconditions(projectConfig, null, experiment, "testUser1", Collections.<String, String>emptyMap()));
    }

    /**
     * Verify that
     * {@link ProjectValidationUtils#validatePreconditions(ProjectConfig, UserProfile, Experiment, String, Map)} gives
     * precedence to experiment status over forced variation bucketing.
     */
    @Test
    public void validatePreconditionsExperimentStatusPrecedesForcedVariation() throws Exception {
        Experiment experiment = projectConfig.getExperiments().get(1);

        Optimizely client = Optimizely.builder(datafile, mockEventHandler).build();
        assertNotNull(client);

        logbackVerifier.expectMessage(Level.INFO, "Experiment \"etag2\" is not running.");
        // testUser3 has a corresponding forced variation, but experiment status should be checked first
        assertFalse(ProjectValidationUtils.validatePreconditions(projectConfig,
                null, experiment, "testUser3", null));
    }

    /**
     * Verify that
     * {@link ProjectValidationUtils#validatePreconditions(ProjectConfig, UserProfile, Experiment, String, Map)}
     * gives precedence to user profile over audience evaluation.
     */
    @Test
    public void validatePreconditionsUserProfilePrecedesAudienceEvaluation() throws Exception {
        Experiment experiment = projectConfig.getExperiments().get(0);
        Variation storedVariation = experiment.getVariations().get(0);
        String userProfileUserId = "userProfileId";
        String normalUserId = "normalUserId";

        UserProfile mockedUserProfile = mock(UserProfile.class);
        when(mockedUserProfile.lookup(userProfileUserId, experiment.getId())).thenReturn(storedVariation.getId());

        Optimizely client = Optimizely.builder(datafile, mockEventHandler)
                .withUserProfile(mockedUserProfile)
                .build();
        assertNotNull(client);

        // ensure that normal users still get excluded from the experiment when they fail audience evaluation
        assertFalse(ProjectValidationUtils.validatePreconditions(projectConfig, mockedUserProfile, experiment,
                normalUserId, Collections.<String, String>emptyMap()));

        // ensure that a user with a saved user profile, sees the same variation regardless of audience evaluation
        assertTrue(ProjectValidationUtils.validatePreconditions(projectConfig, mockedUserProfile, experiment,
                userProfileUserId, Collections.<String, String>emptyMap()));
    }
}
