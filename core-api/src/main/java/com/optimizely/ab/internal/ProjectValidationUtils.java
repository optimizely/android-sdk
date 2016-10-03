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
package com.optimizely.ab.internal;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.audience.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public final class ProjectValidationUtils {

    private static final Logger logger = LoggerFactory.getLogger(ProjectValidationUtils.class);

    private ProjectValidationUtils() {}

    /**
     * Helper method to validate all pre-conditions before bucketing a user.
     *
     * @param projectConfig the current projectConfig
     * @param experiment the experiment we are validating pre-conditions for
     * @param userId the ID of the user
     * @param attributes the attributes of the user
     * @return whether the pre-conditions are satisfied
     */
    public static boolean validatePreconditions(ProjectConfig projectConfig, Experiment experiment, String userId,
                                                Map<String, String> attributes) {
        if (!experiment.isRunning()) {
            logger.info("Experiment \"{}\" is not running.", experiment.getKey(), userId);
            return false;
        }

        if (experiment.getUserIdToVariationKeyMap().containsKey(userId)) {
            return true;
        }

        if (!isUserInExperiment(projectConfig, experiment, attributes)) {
            logger.info("User \"{}\" does not meet conditions to be in experiment \"{}\".", userId, experiment.getKey());
            return false;
        }

        return true;
    }

    /**
     * Determines whether a user satisfies audience conditions for the experiment.
     *
     * @param projectConfig the current projectConfig
     * @param experiment the experiment we are evaluating audiences for
     * @param attributes the attributes of the user
     * @return whether the user meets the criteria for the experiment
     */
    private static boolean isUserInExperiment(ProjectConfig projectConfig, Experiment experiment,
                                              Map<String, String> attributes) {
        List<String> experimentAudienceIds = experiment.getAudienceIds();

        // if there are no audiences, ALL users should be part of the experiment
        if (experimentAudienceIds.isEmpty()) {
            return true;
        }

        // if there are audiences, but no user attributes, the user is not in the experiment.
        if (attributes.isEmpty()) {
            return false;
        }

        for (String audienceId : experimentAudienceIds) {
            Condition conditions = projectConfig.getAudienceConditionsFromId(audienceId);
            if (conditions.evaluate(attributes)) {
                return true;
            }
        }

        return false;
    }
}
