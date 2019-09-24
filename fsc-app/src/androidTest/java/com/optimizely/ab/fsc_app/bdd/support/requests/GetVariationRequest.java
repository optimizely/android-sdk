package com.optimizely.ab.fsc_app.bdd.support.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetVariationRequest {

    @JsonProperty("experiment_key")
    private String experimentKey;
    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("attributes")
    private Map<String, ?> attributes;

    public GetVariationRequest() {
    }

    public GetVariationRequest(String experimentKey, String userId, Map<String, String> attributes) {
        this.experimentKey = experimentKey;
        this.userId = userId;
        this.attributes = attributes;
    }

    public String getExperimentKey() {
        return experimentKey;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public void setExperimentKey(String experimentKey) {
        this.experimentKey = experimentKey;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAttributes(Map<String, ?> attributes) {
        this.attributes = attributes;
    }
}
