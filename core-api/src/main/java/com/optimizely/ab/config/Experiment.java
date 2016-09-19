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
package com.optimizely.ab.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Represents the Optimizely Experiment configuration.
 *
 * @see <a href="http://developers.optimizely.com/server/reference/index.html#json">Project JSON</a>
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class Experiment implements IdKeyMapped {

    private final String id;
    private final String key;
    private final String status;
    private final String layerId;
    private final String groupId;

    private final List<String> audienceIds;
    private final List<Variation> variations;
    private final List<TrafficAllocation> trafficAllocation;

    private final Map<String, Variation> variationKeyToVariationMap;
    private final Map<String, Variation> variationIdToVariationMap;
    private final Map<String, String> userIdToVariationKeyMap;

    // constant storing the status of a running experiment. Other possible statuses for an experiment
    // include 'Not started', 'Paused', and 'Archived'
    private static final String STATUS_RUNNING = "Running";

    @JsonCreator
    public Experiment(@JsonProperty("id") String id,
                      @JsonProperty("key") String key,
                      @JsonProperty("status") String status,
                      @JsonProperty("layerId") String layerId,
                      @JsonProperty("audienceIds") List<String> audienceIds,
                      @JsonProperty("variations") List<Variation> variations,
                      @JsonProperty("forcedVariations") Map<String, String> userIdToVariationKeyMap,
                      @JsonProperty("trafficAllocation") List<TrafficAllocation> trafficAllocation) {
        this(id, key, status, layerId, audienceIds, variations, userIdToVariationKeyMap, trafficAllocation, "");
    }

    public Experiment(@Nonnull String id,
                      @Nonnull String key,
                      @Nonnull String status,
                      @Nullable String layerId,
                      @Nonnull List<String> audienceIds,
                      @Nonnull List<Variation> variations,
                      @Nonnull Map<String, String> userIdToVariationKeyMap,
                      @Nonnull List<TrafficAllocation> trafficAllocation,
                      @Nonnull String groupId) {
        this.id = id;
        this.key = key;
        this.status = status;
        this.layerId = layerId;
        this.audienceIds = Collections.unmodifiableList(audienceIds);
        this.variations = Collections.unmodifiableList(variations);
        this.trafficAllocation = Collections.unmodifiableList(trafficAllocation);
        this.groupId = groupId;
        this.userIdToVariationKeyMap = userIdToVariationKeyMap;
        this.variationKeyToVariationMap = ProjectConfigUtils.generateNameMapping(variations);
        this.variationIdToVariationMap = ProjectConfigUtils.generateIdMapping(variations);
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getStatus() {
        return status;
    }

    public String getLayerId() {
        return layerId;
    }

    public List<String> getAudienceIds() {
        return audienceIds;
    }

    public List<Variation> getVariations() {
        return variations;
    }

    public Map<String, Variation> getVariationKeyToVariationMap() {
        return variationKeyToVariationMap;
    }

    public Map<String, Variation> getVariationIdToVariationMap() {
        return variationIdToVariationMap;
    }

    public Map<String, String> getUserIdToVariationKeyMap() {
        return userIdToVariationKeyMap;
    }

    public List<TrafficAllocation> getTrafficAllocation() {
        return trafficAllocation;
    }

    public String getGroupId() {
        return groupId;
    }

    public boolean isRunning() {
        return status.equals(STATUS_RUNNING);
    }

    @Override
    public String toString() {
        return "Experiment{" +
                "id='" + id + '\'' +
                ", key='" + key + '\'' +
                ", groupId='" + groupId + '\'' +
                ", status='" + status + '\'' +
                ", audienceIds=" + audienceIds +
                ", variations=" + variations +
                ", variationKeyToVariationMap=" + variationKeyToVariationMap +
                ", userIdToVariationKeyMap=" + userIdToVariationKeyMap +
                ", trafficAllocation=" + trafficAllocation +
                '}';
    }
}
