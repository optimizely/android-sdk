package com.optimizely.ab.fsc_app.bdd.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class ListenerMethodArrayResponse implements BaseResponse {
    @JsonProperty("result")
    public List<String> result;

    @JsonProperty("listener_called")
    public List<Map<String, Object>> listenerCalled = null;

    public ListenerMethodArrayResponse(List<String> result, List<Map<String, Object>> listenerCalled) {
        this.result = result;
        this.listenerCalled = listenerCalled;
    }

    @Override
    public Boolean compareResults(Object expectedResponse) {
        return null;
    }
}
