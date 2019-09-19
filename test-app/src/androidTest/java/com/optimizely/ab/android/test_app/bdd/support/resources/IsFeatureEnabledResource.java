package com.optimizely.ab.android.test_app.bdd.support.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizely.ab.android.test_app.bdd.support.OptimizelyWrapper;
import com.optimizely.ab.android.test_app.bdd.support.requests.IsFeatureEnabledRequest;
import com.optimizely.ab.android.test_app.bdd.support.response.BaseResponse;
import com.optimizely.ab.android.test_app.bdd.support.response.ListenerMethodResponse;


public class IsFeatureEnabledResource extends BaseResource<Boolean> {

    private static IsFeatureEnabledResource instance;

    public static IsFeatureEnabledResource getInstance() {
        if (instance == null) {
            instance = new IsFeatureEnabledResource();
        }
        return instance;
    }

    public BaseResponse convertToResourceCall(OptimizelyWrapper optimizelyWrapper, Object desreailizeObject) {
        ObjectMapper mapper = new ObjectMapper();
        IsFeatureEnabledRequest isFeatureEnabledRequest = mapper.convertValue(desreailizeObject, IsFeatureEnabledRequest.class);
        ListenerMethodResponse<Boolean> listenerMethodResponse = isFeatureEnabled(optimizelyWrapper, isFeatureEnabledRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<Boolean> isFeatureEnabled(OptimizelyWrapper optimizelyWrapper, IsFeatureEnabledRequest isFeatureEnabledRequest) {

        Boolean isFeatureEnabled =  optimizelyWrapper.getOptimizelyManager().getOptimizely().isFeatureEnabled(
                isFeatureEnabledRequest.getFeatureFlagKey(),
                isFeatureEnabledRequest.getUserId(),
                isFeatureEnabledRequest.getAttributes()
        );

        return sendResponse(isFeatureEnabled, optimizelyWrapper);
    }


}
