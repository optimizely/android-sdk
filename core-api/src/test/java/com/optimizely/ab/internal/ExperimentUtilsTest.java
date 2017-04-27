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
package com.optimizely.ab.internal;

import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Experiment.ExperimentStatus;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2;
import static com.optimizely.ab.internal.ExperimentUtils.isExperimentActive;
import static com.optimizely.ab.internal.ExperimentUtils.isUserInExperiment;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test the Experiment Utils methods.
 */
public class ExperimentUtilsTest {

    private static ProjectConfig projectConfig;
    private static ProjectConfig noAudienceProjectConfig;

    @BeforeClass
    public static void setUp() throws IOException {
        projectConfig = validProjectConfigV2();
        noAudienceProjectConfig = noAudienceProjectConfigV2();
    }

    /**
     * If the {@link Experiment#status} is {@link ExperimentStatus#RUNNING},
     * then {@link ExperimentUtils#isExperimentActive(Experiment)} should return true.
     */
    @Test
    public void isExperimentActiveReturnsTrueWhenTheExperimentIsRunning() {
        Experiment mockExperiment = makeMockExperimentWithStatus(ExperimentStatus.RUNNING);

        assertTrue(isExperimentActive(mockExperiment));
    }

    /**
     * If the {@link Experiment#status} is {@link ExperimentStatus#LAUNCHED},
     * then {@link ExperimentUtils#isExperimentActive(Experiment)} should return true.
     */
    @Test
    public void isExperimentActiveReturnsTrueWhenTheExperimentIsLaunched() {
        Experiment mockExperiment = makeMockExperimentWithStatus(ExperimentStatus.LAUNCHED);

        assertTrue(isExperimentActive(mockExperiment));
    }

    /**
     * If the {@link Experiment#status} is {@link ExperimentStatus#PAUSED},
     * then {@link ExperimentUtils#isExperimentActive(Experiment)} should return false.
     */
    @Test
    public void isExperimentActiveReturnsFalseWhenTheExperimentIsPaused() {
        Experiment mockExperiment = makeMockExperimentWithStatus(ExperimentStatus.PAUSED);

        assertFalse(isExperimentActive(mockExperiment));
    }

    /**
     * If the {@link Experiment#status} is {@link ExperimentStatus#ARCHIVED},
     * then {@link ExperimentUtils#isExperimentActive(Experiment)} should return false.
     */
    @Test
    public void isExperimentActiveReturnsFalseWhenTheExperimentIsArchived() {
        Experiment mockExperiment = makeMockExperimentWithStatus(ExperimentStatus.ARCHIVED);

        assertFalse(isExperimentActive(mockExperiment));
    }

    /**
     * If the {@link Experiment#status} is {@link ExperimentStatus#NOT_STARTED},
     * then {@link ExperimentUtils#isExperimentActive(Experiment)} should return false.
     */
    @Test
    public void isExperimentActiveReturnsFalseWhenTheExperimentIsNotStarted() {
        Experiment mockExperiment = makeMockExperimentWithStatus(ExperimentStatus.NOT_STARTED);

        assertFalse(isExperimentActive(mockExperiment));
    }

    /**
     * If the {@link Experiment} does not have any {@link Audience}s,
     * then {@link ExperimentUtils#isUserInExperiment(ProjectConfig, Experiment, Map)} should return true;
     */
    @Test
    public void isUserInExperimentReturnsTrueIfExperimentHasNoAudiences() {
        Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);

        assertTrue(isUserInExperiment(noAudienceProjectConfig, experiment, Collections.<String, String>emptyMap()));
    }

    /**
     * If the {@link Experiment} contains at least one {@link Audience}, but attributes is empty,
     * then {@link ExperimentUtils#isUserInExperiment(ProjectConfig, Experiment, Map)} should return false.
     */
    @Test
    public void isUserInExperimentReturnsFalseIfExperimentHasAudiencesButUserHasNoAttributes() {
        Experiment experiment = projectConfig.getExperiments().get(0);

        assertFalse(isUserInExperiment(projectConfig, experiment, Collections.<String, String>emptyMap()));
    }

    /**
     * If the attributes satisfies at least one {@link Condition} in an {@link Audience} of the {@link Experiment},
     * then {@link ExperimentUtils#isUserInExperiment(ProjectConfig, Experiment, Map)} should return true.
     */
    @Test
    public void isUserInExperimentReturnsTrueIfUserSatisfiesAnAudience() {
        Experiment experiment = projectConfig.getExperiments().get(0);
        Map<String, String> attributes = Collections.singletonMap("browser_type", "chrome");

        assertTrue(isUserInExperiment(projectConfig, experiment, attributes));
    }

    /**
     * If the attributes satisfies no {@link Condition} of any {@link Audience} of the {@link Experiment},
     * then {@link ExperimentUtils#isUserInExperiment(ProjectConfig, Experiment, Map)} should return false.
     */
    @Test
    public void isUserInExperimentReturnsTrueIfUserDoesNotSatisfyAnyAudiences() {
        Experiment experiment = projectConfig.getExperiments().get(0);
        Map<String, String> attributes = Collections.singletonMap("browser_type", "firefox");

        assertFalse(isUserInExperiment(projectConfig, experiment, attributes));
    }

    /**
     * Helper method to create an {@link Experiment} object with the provided status.
     *
     * @param status What the desired {@link Experiment#status} should be.
     * @return The newly created {@link Experiment}.
     */
    private Experiment makeMockExperimentWithStatus(ExperimentStatus status) {
        return new Experiment("12345",
                "mockExperimentKey",
                status.toString(),
                "layerId",
                Collections.<String>emptyList(),
                Collections.<Variation>emptyList(),
                Collections.<String, String>emptyMap(),
                Collections.<TrafficAllocation>emptyList()
                );
    }
}
