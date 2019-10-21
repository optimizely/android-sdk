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
public class GetFeatureVariableIntegerRequest {
    @JsonProperty("feature_flag_key")
    private String featureFlagKey;

    @JsonProperty("variable_key")
    private String variableKey;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("attributes")
    private Map<String, ?> attributes;

    public String getFeatureFlagKey() {
        return featureFlagKey;
    }

    public void setFeatureFlagKey(String featureFlagKey) {
        this.featureFlagKey = featureFlagKey;
    }

    public String getVariableKey() {
        return variableKey;
    }

    public void setVariableKey(String variableKey) {
        this.variableKey = variableKey;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, ?> attributes) {
        this.attributes = attributes;
    }

}
