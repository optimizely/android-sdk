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

import javax.annotation.concurrent.Immutable;
import java.util.List;

/**
 * Represents a Optimizely Rollout configuration
 *
 * @see <a href="http://developers.optimizely.com/server/reference/index.html#json">Project JSON</a>
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class Rollout implements IdMapped {

    private final String id;
    private final List<Experiment> experiments;

    @JsonCreator
    public Rollout(@JsonProperty("id") String id,
                   @JsonProperty("experiments") List<Experiment> experiments) {
        this.id = id;
        this.experiments = experiments;
    }

    @Override
    public String getId() {
        return id;
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    @Override
    public String toString() {
        return "Rollout{" +
                "id='" + id + '\'' +
                ", experiments=" + experiments +
                '}';
    }
}
