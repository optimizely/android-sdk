package com.optimizely.ab.fsc_app.bdd.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ForcedVariationRequest {

    @JsonProperty("experiment_key")
    private String experimentKey;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("forced_variation_key")
    private String forcedVariationKey;

    @JsonProperty("attributes")
    private Map<String, ?> attributes;

    public ForcedVariationRequest() {
    }

    public ForcedVariationRequest(String experimentKey, String userId, String forcedVariationKey) {
        this.experimentKey = experimentKey;
        this.userId = userId;
        this.forcedVariationKey = forcedVariationKey;
    }

    public String getExperimentKey() { return experimentKey; }

    public String getUserId() { return userId; }

    public String getForcedVariationKey() { return forcedVariationKey; }

    public void setAttributes(Map<String, ?> attributes) {
        this.attributes = attributes;
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public void setExperimentKey(String experimentKey) { this.experimentKey = experimentKey; }

    public void setUserId(String userId) { this.userId = userId; }

    public void setForcedVariationKey(String forcedVariationKey) { this.forcedVariationKey = forcedVariationKey; }
}
