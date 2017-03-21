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

import javax.annotation.Nonnull;
import java.util.Map;

public class UserProfileUtils {

    /**
     * Removes experiments that are no longer in the datafile as well as inactive experiments
     * from the {@link UserProfile} implementation.
     * @param userProfile The user profile instance to clean.
     * @param projectConfig The project config to clean against.
     */
    public static void cleanUserProfiles(UserProfile userProfile, @Nonnull ProjectConfig projectConfig) {

        if (userProfile == null) {
            return;
        }

        // don't bother cleaning if there are no stored records
        Map<String, Map<String,String>> records = userProfile.getAllRecords();
        if (records == null) {
            return;
        }

        for (Map.Entry<String,Map<String,String>> record : records.entrySet()) {
            for (String experimentId : record.getValue().keySet()) {
                Experiment experiment = projectConfig.getExperimentIdMapping().get(experimentId);
                if (experiment != null && experiment.isActive()) {
                    String variationId = record.getValue().get(experimentId);
                    Variation variation = experiment.getVariationIdToVariationMap().get(variationId);
                    if (variation != null) {
                        continue;
                    }
                }
                userProfile.remove(record.getKey(), experimentId);
            }
        }
    }
}
