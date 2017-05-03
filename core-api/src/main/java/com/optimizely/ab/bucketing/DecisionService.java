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

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.internal.ExperimentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Optimizely's decision service that determines which variation of an experiment the user will be allocated to.
 *
 * The decision service contains all logic around how a user decision is made. This includes all of the following:
 *   1. Checking experiment status
 *   2. Checking whitelisting
 *   3. Checking sticky bucketing
 *   4. Checking audience targeting
 *   5. Using Murmurhash3 to bucket the user.
 */
public class DecisionService {

    private final Bucketer bucketer;
    private final ProjectConfig projectConfig;
    private final UserProfile userProfile;
    private static final Logger logger = LoggerFactory.getLogger(DecisionService.class);

    /**
     * Initialize a decision service for the Optimizely client.
     *  @param bucketer Base bucketer to allocate new users to an experiment.
     * @param projectConfig Optimizely Project Config representing the datafile.
     * @param userProfile UserProfile implementation for storing decisions.
     */
    public DecisionService(@Nonnull Bucketer bucketer,
                           @Nonnull ProjectConfig projectConfig,
                           @Nullable UserProfile userProfile) {
        this.bucketer = bucketer;
        this.projectConfig = projectConfig;
        this.userProfile = userProfile;
    }

    /**
     * Get a {@link Variation} of an {@link Experiment} for a user to be allocated into.
     *
     * @param experiment The Experiment the user will be bucketed into.
     * @param userId The userId of the user.
     * @param filteredAttributes The user's attributes. This should be filtered to just attributes in the Datafile.
     * @return The {@link Variation} the user is allocated into.
     */
    public @Nullable Variation getVariation(@Nonnull Experiment experiment,
                                            @Nonnull String userId,
                                            @Nonnull Map<String, String> filteredAttributes) {

        if (!ExperimentUtils.isExperimentActive(experiment)) {
            return null;
        }

        Variation variation;

        // check for whitelisting
        variation = getWhitelistedVariation(experiment, userId);
        if (variation != null) {
            return variation;
        }

        // check if user exists in user profile
        variation = getStoredVariation(experiment, userId);
        if (variation != null) {
            return variation;
        }

        if (ExperimentUtils.isUserInExperiment(projectConfig, experiment, filteredAttributes)) {
            Variation bucketedVariation = bucketer.bucket(experiment, userId);

            if (bucketedVariation != null) {
                storeVariation(experiment, bucketedVariation, userId);
            }

            return bucketedVariation;
        }
        logger.info("User \"{}\" does not meet conditions to be in experiment \"{}\".", userId, experiment.getKey());

        return null;
    }

    /**
     * Get the variation the user has been whitelisted into.
     * @param experiment {@link Experiment} in which user is to be bucketed.
     * @param userId User Identifier
     * @return null if the user is not whitelisted into any variation
     *      {@link Variation} the user is bucketed into if the user has a specified whitelisted variation.
     */
    @Nullable Variation getWhitelistedVariation(@Nonnull Experiment experiment, @Nonnull String userId) {
        // if a user has a forced variation mapping, return the respective variation
        Map<String, String> userIdToVariationKeyMap = experiment.getUserIdToVariationKeyMap();
        if (userIdToVariationKeyMap.containsKey(userId)) {
            String forcedVariationKey = userIdToVariationKeyMap.get(userId);
            Variation forcedVariation = experiment.getVariationKeyToVariationMap().get(forcedVariationKey);
            if (forcedVariation != null) {
                logger.info("User \"{}\" is forced in variation \"{}\".", userId, forcedVariationKey);
            } else {
                logger.error("Variation \"{}\" is not in the datafile. Not activating user \"{}\".", forcedVariationKey,
                        userId);
            }

            return forcedVariation;
        }

        return null;
    }

    /**
     * Get the {@link Variation} that has been stored for the user in the {@link UserProfile} implementation.
     * @param experiment {@link Experiment} in which the user was bucketed.
     * @param userId User Identifier
     * @return null if the {@link UserProfile} implementation is null or the user was not previously bucketed.
     *      else return the {@link Variation} the user was previously bucketed into.
     */
    @Nullable Variation getStoredVariation(@Nonnull Experiment experiment, @Nonnull String userId) {
        // ---------- Check User Profile for Sticky Bucketing ----------
        // If a user profile instance is present then check it for a saved variation
        String experimentId = experiment.getId();
        String experimentKey = experiment.getKey();
        if (userProfile != null) {
            String variationId = userProfile.lookup(userId, experimentId);
            if (variationId != null) {
                Variation savedVariation = projectConfig
                        .getExperimentIdMapping()
                        .get(experimentId)
                        .getVariationIdToVariationMap()
                        .get(variationId);
                logger.info("Returning previously activated variation \"{}\" of experiment \"{}\" "
                                + "for user \"{}\" from user profile.",
                        savedVariation.getKey(), experimentKey, userId);
                // A variation is stored for this combined bucket id
                return savedVariation;
            } else {
                logger.info("No previously activated variation of experiment \"{}\" "
                                + "for user \"{}\" found in user profile.",
                        experimentKey, userId);
            }
        }

        return null;
    }

    /**
     * Store a {@link Variation} of an {@link Experiment} for a user in the {@link UserProfile}.
     *
     * @param experiment The experiment the user was buck
     * @param variation The Variation to store.
     * @param userId The ID of the user.
     */
    void storeVariation(@Nonnull Experiment experiment, @Nonnull Variation variation, @Nonnull String userId) {
        String experimentId = experiment.getId();
        // ---------- Save Variation to User Profile ----------
        // If a user profile is present give it a variation to store
        if (userProfile != null) {
            String bucketedVariationId = variation.getId();
            boolean saved = userProfile.save(userId, experimentId, bucketedVariationId);
            if (saved) {
                logger.info("Saved variation \"{}\" of experiment \"{}\" for user \"{}\".",
                        bucketedVariationId, experimentId, userId);
            } else {
                logger.warn("Failed to save variation \"{}\" of experiment \"{}\" for user \"{}\".",
                        bucketedVariationId, experimentId, userId);
            }
        }
    }
}
