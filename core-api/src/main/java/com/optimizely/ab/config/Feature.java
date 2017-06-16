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
package com.optimizely.ab.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a Feature definition at the project level
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Feature implements IdKeyMapped{

    private final String id;
    private final String key;
    private final String layerId;
    private final List<String> experimentIds;
    private final List<LiveVariable> variables;

    @JsonCreator
    public Feature(@JsonProperty("id") String id,
                   @JsonProperty("key") String key,
                   @JsonProperty("layerId") String layerId,
                   @JsonProperty("experimentIds") List<String> experimentIds,
                   @JsonProperty("variables") List<LiveVariable> variables) {
        this.id = id;
        this.key = key;
        this.layerId = layerId;
        this.experimentIds = experimentIds;
        this.variables = variables;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getLayerId() {
        return layerId;
    }

    public List<String> getExperimentIds() {
        return experimentIds;
    }

    public List<LiveVariable> getVariables() {
        return variables;
    }

    @Override
    public String toString() {
        return "Feature{" +
                "id='" + id + '\'' +
                ", key='" + key + '\'' +
                ", layerId='" + layerId + '\'' +
                ", experimentIds=" + experimentIds +
                ", variables=" + variables +
                '}';
    }
}
