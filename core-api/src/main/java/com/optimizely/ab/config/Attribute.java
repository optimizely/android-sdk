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

import javax.annotation.concurrent.Immutable;

/**
 * Represents the Optimizely Attribute configuration.
 *
 * @see <a href="http://developers.optimizely.com/server/reference/index.html#json">Project JSON</a>
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class Attribute implements IdKeyMapped {

    private final String id;
    private final String key;
    private final String segmentId;

    public Attribute(String id, String key) {
        this(id, key, null);
    }

    @JsonCreator
    public Attribute(@JsonProperty("id") String id,
                     @JsonProperty("key") String key,
                     @JsonProperty("segmentId") String segmentId) {
        this.id = id;
        this.key = key;
        this.segmentId = segmentId;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getSegmentId() {
        return segmentId;
    }

    @Override
    public String toString() {
        return "Attribute{" +
               "id='" + id + '\'' +
               ", key='" + key + '\'' +
               ", segmentId='" + segmentId + '\'' +
               '}';
    }
}
