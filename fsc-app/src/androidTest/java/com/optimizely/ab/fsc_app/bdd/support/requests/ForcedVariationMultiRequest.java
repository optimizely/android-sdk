package com.optimizely.ab.fsc_app.bdd.support.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ForcedVariationMultiRequest {

    @JsonProperty("experiment_key_1")
    private String experimentKey1;
    @JsonProperty("user_id_1")
    private String userId1;
    @JsonProperty("forced_variation_key_1")
    private String forcedVariationKey1;
    @JsonProperty("experiment_key_2")
    private String experimentKey2;
    @JsonProperty("user_id_2")
    private String userId2;
    @JsonProperty("forced_variation_key_2")
    private String forcedVariationKey2;

    @JsonProperty("attributes")
    private Map<String, ?> attributes;


    public ForcedVariationMultiRequest() {
    }

    public void setAttributes(Map<String, ?> attributes) {
        this.attributes = attributes;
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public ForcedVariationMultiRequest(String experimentKey1, String userId1, String forcedVariationKey1,
                                       String experimentKey2, String userId2, String forcedVariationKey2) {
        this.experimentKey1 = experimentKey1;
        this.userId1 = userId1;
        this.forcedVariationKey1 = forcedVariationKey1;
        this.experimentKey2 = experimentKey2;
        this.userId2 = userId2;
        this.forcedVariationKey2 = forcedVariationKey2;
    }

    public String getExperimentKey1() { return experimentKey1; }

    public String getUserId1() { return userId1; }

    public String getForcedVariationKey1() { return forcedVariationKey1; }

    public String getExperimentKey2() { return experimentKey2; }

    public String getUserId2() { return userId2; }

    public String getForcedVariationKey2() { return forcedVariationKey2; }

    public void setExperimentKey1(String experimentKey1) { this.experimentKey1 = experimentKey1; }

    public void setUserId1(String userId1) { this.userId1 = userId1; }

    public void setForcedVariationKey1(String forcedVariationKey1) { this.forcedVariationKey1 = forcedVariationKey1; }

    public void setExperimentKey2(String experimentKey2) { this.experimentKey2 = experimentKey2; }

    public void setUserId2(String userId2) { this.userId2 = userId2; }

    public void setForcedVariationKey2(String forcedVariationKey2) { this.forcedVariationKey2 = forcedVariationKey2; }
}
