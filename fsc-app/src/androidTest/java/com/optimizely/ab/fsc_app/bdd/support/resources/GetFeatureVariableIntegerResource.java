package com.optimizely.ab.fsc_app.bdd.support.resources;

import com.optimizely.ab.fsc_app.bdd.support.OptimizelyE2EService;
import com.optimizely.ab.fsc_app.bdd.models.requests.GetFeatureVariableIntegerRequest;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodResponse;

public class GetFeatureVariableIntegerResource extends BaseResource<Integer> {

    private static GetFeatureVariableIntegerResource instance;

    private GetFeatureVariableIntegerResource() {
        super();
    }

    public static GetFeatureVariableIntegerResource getInstance() {
        if (instance == null) {
            instance = new GetFeatureVariableIntegerResource();
        }
        return instance;
    }

    public BaseResponse convertToResourceCall(OptimizelyE2EService optimizelyE2EService, Object desreailizeObject) {
        GetFeatureVariableIntegerRequest getFeatureVariableIntegerRequest = mapper.convertValue(desreailizeObject, GetFeatureVariableIntegerRequest.class);
        ListenerMethodResponse<Integer> listenerMethodResponse = getFeatureVariableInteger(optimizelyE2EService, getFeatureVariableIntegerRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<Integer> getFeatureVariableInteger(OptimizelyE2EService optimizelyE2EService, GetFeatureVariableIntegerRequest getFeatureVariableIntegerRequest) {

        Integer variableValue = optimizelyE2EService.getOptimizelyManager().getOptimizely().getFeatureVariableInteger(
                getFeatureVariableIntegerRequest.getFeatureFlagKey(),
                getFeatureVariableIntegerRequest.getVariableKey(),
                getFeatureVariableIntegerRequest.getUserId(),
                getFeatureVariableIntegerRequest.getAttributes()
                );

        return sendResponse(variableValue, optimizelyE2EService);
    }

}
