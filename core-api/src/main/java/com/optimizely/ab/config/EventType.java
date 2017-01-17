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
package com.optimizely.ab.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.Immutable;

/**
 * Represents the Optimizely Event configuration.
 *
 * @see <a href="http://developers.optimizely.com/server/reference/index.html#json">Project JSON</a>
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventType implements IdKeyMapped {

    /**
     * "total revenue" is the default revenue goal that is provided for all projects.
     */
    public static final String TOTAL_REVENUE_GOAL_KEY = "Total Revenue";

    private final String id;
    private final String key;
    private final List<String> experimentIds;

    @JsonCreator
    public EventType(@JsonProperty("id") String id,
                     @JsonProperty("key") String key,
                     @JsonProperty("experimentIds") List<String> experimentIds) {
        this.id = id;
        this.key = key;
        this.experimentIds = experimentIds;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public List<String> getExperimentIds() {
        return experimentIds;
    }

    @Override
    public String toString() {
        return "EventType{" +
               "id='" + id + '\'' +
               ", key='" + key + '\'' +
               ", experimentIds=" + experimentIds +
               '}';
    }
}
