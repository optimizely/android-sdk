package com.optimizely.ab.fsc_app.bdd.support.requests;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IsFeatureEnabledRequest {

    @JsonProperty("feature_flag_key")
    private String featureFlagKey;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("attributes")
    private Map<String, ?> attributes;

    public String getFeatureFlagKey() {
        return featureFlagKey;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public void setFeatureFlagKey(String featureFlagKey) {
        this.featureFlagKey = featureFlagKey;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAttributes(Map<String, ?> attributes) {
        this.attributes = attributes;
    }

}
