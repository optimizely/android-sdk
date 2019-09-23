package com.optimizely.ab.android.test_app.bdd.support.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListenerMethodArrayResponse {
    @JsonProperty("result")
    public List<String> result;

    @JsonProperty("listener_called")
    public ArrayList<Map<String, Object>> listenerCalled = null;

    public ListenerMethodArrayResponse(List<String> result, ArrayList<Map<String, Object>> listenerCalled) {
        this.result = result;
        this.listenerCalled = listenerCalled;
    }
}
