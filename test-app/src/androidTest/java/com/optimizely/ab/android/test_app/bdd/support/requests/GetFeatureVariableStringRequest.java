package com.optimizely.ab.android.test_app.bdd.support.requests;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetFeatureVariableStringRequest {
    @JsonProperty("feature_flag_key")
    private String featureFlagKey;

    @JsonProperty("variable_key")
    private String variableKey;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("attributes")
    private Map<String, ?> attributes;

    public GetFeatureVariableStringRequest() {
    }

    public GetFeatureVariableStringRequest(String featureFlagKey,
                                           String variableKey,
                                           String userId,
                                           Map<String, ?> attributes) {
        this.featureFlagKey = featureFlagKey;
        this.variableKey = variableKey;
        this.userId = userId;
        this.attributes = attributes;
    }

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
