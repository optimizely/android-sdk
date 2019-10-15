package com.optimizely.ab.fsc_app.bdd.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MethodResponse<T> {

    @JsonProperty("result")
    private T result;

    public MethodResponse(T result) {
        this.result = result;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
