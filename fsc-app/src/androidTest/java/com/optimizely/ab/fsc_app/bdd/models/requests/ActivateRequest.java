/****************************************************************************
 * Copyright 2019, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.fsc_app.bdd.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ActivateRequest {

    @JsonProperty("experiment_key")
    private String experimentKey;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("attributes")
    private Map<String, Object> attributes;

    public String getExperimentKey() {
        return experimentKey;
    }

    public String getUserId() {
        return userId;
    }

    public void setExperimentKey(String experimentKey) {
        this.experimentKey = experimentKey;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }
}
