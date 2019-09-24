package com.optimizely.ab.fsc_app.bdd.support.resources;

import com.optimizely.ab.config.Variation;
import com.optimizely.ab.fsc_app.bdd.OptimizelyE2EService;
import com.optimizely.ab.fsc_app.bdd.support.requests.ActivateRequest;
import com.optimizely.ab.fsc_app.bdd.support.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.support.responses.ListenerMethodResponse;

public class ActivateResource extends BaseResource<String> {
    private static ActivateResource instance;

    private ActivateResource() {
        super();
    }

    public static ActivateResource getInstance() {
        if (instance == null) {
            instance = new ActivateResource();
        }
        return instance;
    }

    public BaseResponse convertToResourceCall(OptimizelyE2EService optimizelyE2EService, Object desreailizeObject) {
        ActivateRequest activateRequest = mapper.convertValue(desreailizeObject, ActivateRequest.class);
        ListenerMethodResponse<Boolean> listenerMethodResponse = activate(optimizelyE2EService, activateRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<Boolean> activate(OptimizelyE2EService optimizelyE2EService, ActivateRequest activateRequest) {

        Variation variation = optimizelyE2EService.getOptimizelyManager().getOptimizely().activate(
                activateRequest.getExperimentKey(),
                activateRequest.getUserId(),
                activateRequest.getAttributes()
        );
        String variationKey = variation != null ? variation.getKey() : null;

        return sendResponse(variationKey, optimizelyE2EService);
    }

}
