package com.optimizely.ab.fsc_app.bdd.support.resources;

import com.optimizely.ab.fsc_app.bdd.support.OptimizelyE2EService;
import com.optimizely.ab.fsc_app.bdd.models.requests.GetFeatureVariableBooleanRequest;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodResponse;

public class GetFeatureVariableBooleanResource extends BaseResource<Boolean> {

    private static GetFeatureVariableBooleanResource instance;

    private GetFeatureVariableBooleanResource() {
        super();
    }

    public static GetFeatureVariableBooleanResource getInstance() {
        if (instance == null) {
            instance = new GetFeatureVariableBooleanResource();
        }
        return instance;
    }

    public BaseResponse convertToResourceCall(OptimizelyE2EService optimizelyE2EService, Object desreailizeObject) {
        GetFeatureVariableBooleanRequest getFeatureVariableBooleanRequest = mapper.convertValue(desreailizeObject, GetFeatureVariableBooleanRequest.class);
        ListenerMethodResponse<Boolean> listenerMethodResponse = getFeatureVariableBoolean(optimizelyE2EService, getFeatureVariableBooleanRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<Boolean> getFeatureVariableBoolean(OptimizelyE2EService optimizelyE2EService, GetFeatureVariableBooleanRequest getFeatureVariableBooleanRequest) {

        Boolean variableValue = optimizelyE2EService.getOptimizelyManager().getOptimizely().getFeatureVariableBoolean(
                getFeatureVariableBooleanRequest.getFeatureFlagKey(),
                getFeatureVariableBooleanRequest.getVariableKey(),
                getFeatureVariableBooleanRequest.getUserId(),
                getFeatureVariableBooleanRequest.getAttributes()
                );

        return sendResponse(variableValue, optimizelyE2EService);
    }

}
