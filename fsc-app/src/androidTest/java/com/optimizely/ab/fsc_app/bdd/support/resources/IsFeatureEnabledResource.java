package com.optimizely.ab.fsc_app.bdd.support.resources;

import com.optimizely.ab.fsc_app.bdd.OptimizelyE2EService;
import com.optimizely.ab.fsc_app.bdd.support.requests.IsFeatureEnabledRequest;
import com.optimizely.ab.fsc_app.bdd.support.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.support.responses.ListenerMethodResponse;

public class IsFeatureEnabledResource extends BaseResource<Boolean> {

    private static IsFeatureEnabledResource instance;

    private IsFeatureEnabledResource() {
        super();
    }

    public static IsFeatureEnabledResource getInstance() {
        if (instance == null) {
            instance = new IsFeatureEnabledResource();
        }
        return instance;
    }

    public BaseResponse convertToResourceCall(OptimizelyE2EService optimizelyE2EService, Object desreailizeObject) {
        IsFeatureEnabledRequest isFeatureEnabledRequest = mapper.convertValue(desreailizeObject, IsFeatureEnabledRequest.class);
        ListenerMethodResponse<Boolean> listenerMethodResponse = isFeatureEnabled(optimizelyE2EService, isFeatureEnabledRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<Boolean> isFeatureEnabled(OptimizelyE2EService optimizelyE2EService, IsFeatureEnabledRequest isFeatureEnabledRequest) {

        Boolean isFeatureEnabled =  optimizelyE2EService.getOptimizelyManager().getOptimizely().isFeatureEnabled(
                isFeatureEnabledRequest.getFeatureFlagKey(),
                isFeatureEnabledRequest.getUserId(),
                isFeatureEnabledRequest.getAttributes()
        );

        return sendResponse(isFeatureEnabled, optimizelyE2EService);
    }

}
