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
