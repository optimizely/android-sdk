package com.optimizely.ab.fsc_app.bdd.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetEnabledFeaturesRequest{
    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("attributes")
    private Map<String, ?> attributes;

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public String getUserId() {
        return userId;
    }

    public void setAttributes(Map<String, ?> attributes) {
        this.attributes = attributes;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}
