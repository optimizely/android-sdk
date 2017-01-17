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
package com.optimizely.ab.bucketing;

import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.bucketing.internal.MurmurHash3;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Group;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Default Optimizely bucketing algorithm that evenly distributes users using the Murmur3 hash of some provided
 * identifier.
 * <p>
 * The user identifier <i>must</i> be provided in the first data argument passed to
 * {@link #bucket(Experiment, String)} and <i>must</i> be non-null and non-empty.
 *
 * @see <a href="https://en.wikipedia.org/wiki/MurmurHash">MurmurHash</a>
 */
@Immutable
public class Bucketer {

    private final ProjectConfig projectConfig;

    @Nullable private final UserProfile userProfile;

    private static final Logger logger = LoggerFactory.getLogger(Bucketer.class);

    private static final int MURMUR_HASH_SEED = 1;

    /**
     * The maximum bucket value (represents 100 Basis Points).
     */
    @VisibleForTesting
    static final int MAX_TRAFFIC_VALUE = 10000;

    public Bucketer(ProjectConfig projectConfig) {
        this(projectConfig, null);
    }

    public Bucketer(ProjectConfig projectConfig, @Nullable UserProfile userProfile) {
        this.projectConfig = projectConfig;
        this.userProfile = userProfile;
    }

    private String bucketToEntity(int bucketValue, List<TrafficAllocation> trafficAllocations) {
        int currentEndOfRange;
        for (TrafficAllocation currAllocation : trafficAllocations) {
            currentEndOfRange = currAllocation.getEndOfRange();
            if (bucketValue < currentEndOfRange) {
                // for mutually exclusive bucketing, de-allocated space is represented by an empty string
                if (currAllocation.getEntityId().isEmpty()) {
                    return null;
                }
                return currAllocation.getEntityId();
            }
        }

        return null;
    }

    private Experiment bucketToExperiment(@Nonnull Group group,
                                          @Nonnull String userId) {
        // "salt" the bucket id using the group id
        String bucketId = userId + group.getId();

        List<TrafficAllocation> trafficAllocations = group.getTrafficAllocation();

        int hashCode = MurmurHash3.murmurhash3_x86_32(bucketId, 0, bucketId.length(), MURMUR_HASH_SEED);
        int bucketValue = generateBucketValue(hashCode);
        logger.debug("Assigned bucket {} to user \"{}\" during experiment bucketing.", bucketValue, userId);

        String bucketedExperimentId = bucketToEntity(bucketValue, trafficAllocations);
        if (bucketedExperimentId != null) {
            return projectConfig.getExperimentIdMapping().get(bucketedExperimentId);
        }

        // user was not bucketed to an experiment in the group
        logger.info("User \"{}\" is not in any experiment of group {}.", userId, group.getId());
        return null;
    }

    private Variation bucketToVariation(@Nonnull Experiment experiment,
                                        @Nonnull String userId) {
        // "salt" the bucket id using the experiment id
        String experimentId = experiment.getId();
        String experimentKey = experiment.getKey();
        String combinedBucketId = userId + experimentId;

        // If a user profile instance is present then check it for a saved variation
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

        List<TrafficAllocation> trafficAllocations = experiment.getTrafficAllocation();

        int hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length(), MURMUR_HASH_SEED);
        int bucketValue = generateBucketValue(hashCode);
        logger.debug("Assigned bucket {} to user \"{}\" during variation bucketing.", bucketValue, userId);

        String bucketedVariationId = bucketToEntity(bucketValue, trafficAllocations);
        if (bucketedVariationId != null) {
            Variation bucketedVariation = experiment.getVariationIdToVariationMap().get(bucketedVariationId);
            String variationKey = bucketedVariation.getKey();
            logger.info("User \"{}\" is in variation \"{}\" of experiment \"{}\".", userId, variationKey,
                        experimentKey);

            // If a user profile is present give it a variation to store
            if (userProfile != null) {
                boolean saved = userProfile.save(userId, experimentId, bucketedVariationId);
                if (saved) {
                    logger.info("Saved variation \"{}\" of experiment \"{}\" for user \"{}\".",
                                bucketedVariationId, experimentId, userId);
                } else {
                    logger.warn("Failed to save variation \"{}\" of experiment \"{}\" for user \"{}\".",
                                bucketedVariationId, experimentId, userId);
                }
            }

            return bucketedVariation;
        }

        // user was not bucketed to a variation
        logger.info("User \"{}\" is not in any variation of experiment \"{}\".", userId, experimentKey);
        return null;
    }

    public @Nullable Variation bucket(@Nonnull Experiment experiment,
                                      @Nonnull String userId) {

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

        String groupId = experiment.getGroupId();
        // check whether the experiment belongs to a group
        if (!groupId.isEmpty()) {
            Group experimentGroup = projectConfig.getGroupIdMapping().get(groupId);
            // bucket to an experiment only if group entities are to be mutually exclusive
            if (experimentGroup.getPolicy().equals(Group.RANDOM_POLICY)) {
                Experiment bucketedExperiment = bucketToExperiment(experimentGroup, userId);
                if (bucketedExperiment == null) {
                    return null;
                }
                // if the experiment a user is bucketed in within a group isn't the same as the experiment provided,
                // don't perform further bucketing within the experiment
                if (!bucketedExperiment.getId().equals(experiment.getId())) {
                    logger.info("User \"{}\" is not in experiment \"{}\" of group {}.", userId, experiment.getKey(),
                                experimentGroup.getId());
                    return null;
                }

                logger.info("User \"{}\" is in experiment \"{}\" of group {}.", userId, experiment.getKey(),
                            experimentGroup.getId());
            }
        }

        return bucketToVariation(experiment, userId);
    }

    //======== Helper methods ========//

    /**
     * Map the given 32-bit hashcode into the range [0, {@link #MAX_TRAFFIC_VALUE}).
     * @param hashCode the provided hashcode
     * @return a value in the range closed-open range, [0, {@link #MAX_TRAFFIC_VALUE})
     */
    @VisibleForTesting
    int generateBucketValue(int hashCode) {
        // map the hashCode into the range [0, BucketAlgorithm.MAX_TRAFFIC_VALUE)
        double ratio = (double)(hashCode & 0xFFFFFFFFL) / Math.pow(2, 32);
        return (int)Math.floor(MAX_TRAFFIC_VALUE * ratio);
    }

    @Nullable
    public UserProfile getUserProfile() {
        return userProfile;
    }

    /**
     * Gives implementations of {@link UserProfile} a chance to remove records
     * of experiments that are deleted or not running.
     */
    public void cleanUserProfiles() {
        if (userProfile != null) {
            Map<String, Map<String,String>> records = userProfile.getAllRecords();
            if (records != null) {
                for (Map.Entry<String,Map<String,String>> record : records.entrySet()) {
                    for (String experimentId : record.getValue().keySet()) {
                        Experiment experiment = projectConfig.getExperimentIdMapping().get(experimentId);
                        if (experiment == null || !experiment.isRunning()) {
                            userProfile.remove(record.getKey(), experimentId);
                        }
                    }
                }
            }
        }
    }
}
