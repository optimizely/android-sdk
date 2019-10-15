package com.optimizely.ab.fsc_app.bdd.support.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizely.ab.fsc_app.bdd.support.OptimizelyE2EService;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodArrayResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodResponse;

import java.util.List;
import java.util.Map;

import static com.optimizely.ab.fsc_app.bdd.optlyplugins.TestCompositeService.setListenerResponseCollection;

public class BaseResource<TResponse> {

    protected ObjectMapper mapper;

    protected BaseResource() {
        mapper = new ObjectMapper();
    }

    ListenerMethodResponse sendResponse(TResponse result, OptimizelyE2EService optimizelyE2EService) {
        List<Map<String, Object>> responseCollection = setListenerResponseCollection(optimizelyE2EService);
        return new ListenerMethodResponse<TResponse>(result, responseCollection);
    }

    ListenerMethodArrayResponse sendArrayResponse(List<String> result, OptimizelyE2EService optimizelyE2EService) {
        List<Map<String, Object>> responseCollection = setListenerResponseCollection(optimizelyE2EService);
        return new ListenerMethodArrayResponse(result, responseCollection);
    }
}
