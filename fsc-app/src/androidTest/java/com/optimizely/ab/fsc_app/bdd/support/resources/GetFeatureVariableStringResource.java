package com.optimizely.ab.fsc_app.bdd.support.resources;

import com.optimizely.ab.fsc_app.bdd.support.OptimizelyE2EService;
import com.optimizely.ab.fsc_app.bdd.models.requests.GetFeatureVariableStringRequest;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodResponse;

public class GetFeatureVariableStringResource extends BaseResource<String> {

    private static GetFeatureVariableStringResource instance;

    private GetFeatureVariableStringResource() {
        super();
    }

    public static GetFeatureVariableStringResource getInstance() {
        if (instance == null) {
            instance = new GetFeatureVariableStringResource();
        }
        return instance;
    }

    public BaseResponse convertToResourceCall(OptimizelyE2EService optimizelyE2EService, Object desreailizeObject) {
        GetFeatureVariableStringRequest getFeatureVariableStringRequest = mapper.convertValue(desreailizeObject, GetFeatureVariableStringRequest.class);
        ListenerMethodResponse<String> listenerMethodResponse = getFeatureVariableString(optimizelyE2EService, getFeatureVariableStringRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<String> getFeatureVariableString(OptimizelyE2EService optimizelyE2EService, GetFeatureVariableStringRequest getFeatureVariableStringRequest) {

        String variableValue = optimizelyE2EService.getOptimizelyManager().getOptimizely().getFeatureVariableString(
                getFeatureVariableStringRequest.getFeatureFlagKey(),
                getFeatureVariableStringRequest.getVariableKey(),
                getFeatureVariableStringRequest.getUserId(),
                getFeatureVariableStringRequest.getAttributes()
                );

        return sendResponse(variableValue, optimizelyE2EService);
    }

}
