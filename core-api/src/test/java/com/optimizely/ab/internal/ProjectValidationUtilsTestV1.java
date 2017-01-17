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
package com.optimizely.ab.internal;

import ch.qos.logback.classic.Level;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;

import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV1;
import static com.optimizely.ab.internal.ProjectValidationUtils.validatePreconditions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProjectValidationUtilsTestV1 {

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    /**
     * Verify that {@link ProjectValidationUtils#validatePreconditions(ProjectConfig, Experiment, String, Map)} gives
     * precedence to forced variation bucketing over audience evaluation.
     */
    @Test
    public void validatePreconditionsForcedVariationPrecedesAudienceEval() throws Exception {
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment experiment = projectConfig.getExperiments().get(0);

        assertTrue(
                validatePreconditions(projectConfig, experiment, "testUser1", Collections.<String, String>emptyMap()));
    }

    /**
     * Verify that {@link ProjectValidationUtils#validatePreconditions(ProjectConfig, Experiment, String, Map)} gives
     * precedence to experiment status over forced variation bucketing.
     */
    @Test
    public void validatePreconditionsExperimentStatusPrecedesForcedVariation() throws Exception {
        ProjectConfig projectConfig = validProjectConfigV1();
        Experiment experiment = projectConfig.getExperiments().get(1);

        logbackVerifier.expectMessage(Level.INFO, "Experiment \"etag2\" is not running.");
        // testUser3 has a corresponding forced variation, but experiment status should be checked first
        assertFalse(
                validatePreconditions(projectConfig, experiment, "testUser3", Collections.<String, String>emptyMap()));
    }
}
