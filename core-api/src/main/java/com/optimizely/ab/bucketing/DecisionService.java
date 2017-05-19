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

import com.optimizely.ab.OptimizelyRuntimeException;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.internal.ExperimentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
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
    private final ErrorHandler errorHandler;
    private final ProjectConfig projectConfig;
    private final UserProfileService userProfileService;
    private static final Logger logger = LoggerFactory.getLogger(DecisionService.class);

    /**
     * Initialize a decision service for the Optimizely client.
     * @param bucketer Base bucketer to allocate new users to an experiment.
     * @param errorHandler The error handler of the Optimizely client.
     * @param projectConfig Optimizely Project Config representing the datafile.
     * @param userProfileService UserProfileService implementation for storing user info.
     */
    public DecisionService(@Nonnull Bucketer bucketer,
                           @Nonnull ErrorHandler errorHandler,
                           @Nonnull ProjectConfig projectConfig,
                           @Nullable UserProfileService userProfileService) {
        this.bucketer = bucketer;
        this.errorHandler = errorHandler;
        this.projectConfig = projectConfig;
        this.userProfileService = userProfileService;
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

        // check for whitelisting
        Variation variation = getWhitelistedVariation(experiment, userId);
        if (variation != null) {
            return variation;
        }

        // fetch the user profile map from the user profile service
        UserProfile userProfile = null;
        if (userProfileService != null) {
            try {
                Map<String, Object> userProfileMap = userProfileService.lookup(userId);
                if (userProfileMap == null) {
                    logger.info("We were unable to get a user profile map from the UserProfileService.");
                } else if (UserProfileUtils.isValidUserProfileMap(userProfileMap)) {
                    userProfile = UserProfileUtils.convertMapToUserProfile(userProfileMap);
                } else {
                    logger.warn("The UserProfileService returned an invalid map.");
                }
            } catch (Exception exception) {
                logger.error(exception.getMessage());
                errorHandler.handleError(new OptimizelyRuntimeException(exception));
            }
        }

        // check if user exists in user profile
        if (userProfile != null) {
            variation = getStoredVariation(experiment, userProfile);
            // return the stored variation if it exists
            if (variation != null) {
                return variation;
            }
        } else { // if we could not find a user profile, make a new one
            userProfile = new UserProfile(userId, new HashMap<String, Decision>());
        }

        if (ExperimentUtils.isUserInExperiment(projectConfig, experiment, filteredAttributes)) {
            variation = bucketer.bucket(experiment, userId);

            if (variation != null) {
                if (userProfileService != null) {
                    saveVariation(experiment, variation, userProfile);
                } else {
                    logger.info("This decision will not be saved since the UserProfileService is null.");
                }
            }

            return variation;
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
     * Get the {@link Variation} that has been stored for the user in the {@link UserProfileService} implementation.
     * @param experiment {@link Experiment} in which the user was bucketed.
     * @param userProfile {@link UserProfile} of the user.
     * @return null if the {@link UserProfileService} implementation is null or the user was not previously bucketed.
     *      else return the {@link Variation} the user was previously bucketed into.
     */
    @Nullable Variation getStoredVariation(@Nonnull Experiment experiment,
                                           @Nonnull UserProfile userProfile) {
        // ---------- Check User Profile for Sticky Bucketing ----------
        // If a user profile instance is present then check it for a saved variation
        String experimentId = experiment.getId();
        String experimentKey = experiment.getKey();
        Decision decision = userProfile.experimentBucketMap.get(experimentId);
        if (decision != null) {
            String variationId = decision.variationId;
            Variation savedVariation = projectConfig
                    .getExperimentIdMapping()
                    .get(experimentId)
                    .getVariationIdToVariationMap()
                    .get(variationId);
            if (savedVariation != null) {
                logger.info("Returning previously activated variation \"{}\" of experiment \"{}\" "
                                + "for user \"{}\" from user profile.",
                        savedVariation.getKey(), experimentKey, userProfile.userId);
                // A variation is stored for this combined bucket id
                return savedVariation;
            } else {
                logger.info("User \"{}\" was previously bucketed into variation with ID \"{}\" for experiment \"{}\"," +
                                " but no matching variation was found for that user. We will re-bucket the user.",
                        userProfile.userId, variationId, experimentKey);
                return null;
            }
        } else {
            logger.info("No previously activated variation of experiment \"{}\" "
                            + "for user \"{}\" found in user profile.",
                    experimentKey, userProfile.userId);
            return null;
        }
    }

    /**
     * Save a {@link Variation} of an {@link Experiment} for a user in the {@link UserProfileService}.
     *
     * @param experiment The experiment the user was buck
     * @param variation The Variation to save.
     * @param userProfile A {@link UserProfile} instance of the user information.
     */
    void saveVariation(@Nonnull Experiment experiment,
                       @Nonnull Variation variation,
                       @Nonnull UserProfile userProfile) {
        // only save if the user has implemented a user profile service
        if (userProfileService != null) {
            String experimentId = experiment.getId();
            String variationId = variation.getId();
            Decision decision;
            if (userProfile.experimentBucketMap.containsKey(experimentId)) {
                decision = userProfile.experimentBucketMap.get(experimentId);
                decision.variationId = variationId;
            } else {
                decision = new Decision(variationId);
            }
            userProfile.experimentBucketMap.put(experimentId, decision);

            try {
                userProfileService.save(userProfile.toMap());
                logger.info("Saved variation \"{}\" of experiment \"{}\" for user \"{}\".",
                    variationId, experimentId, userProfile.userId);
            } catch (Exception exception) {
                logger.warn("Failed to save variation \"{}\" of experiment \"{}\" for user \"{}\".",
                        variationId, experimentId, userProfile.userId);
                errorHandler.handleError(new OptimizelyRuntimeException(exception));
            }
        }
    }
}
