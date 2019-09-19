package com.optimizely.ab.android.test_app.bdd.support.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;

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
