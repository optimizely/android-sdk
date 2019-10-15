package com.optimizely.ab.fsc_app.bdd.support.resources;

import com.optimizely.ab.fsc_app.bdd.support.OptimizelyE2EService;
import com.optimizely.ab.fsc_app.bdd.models.requests.GetFeatureVariableDoubleRequest;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodResponse;

public class GetFeatureVariableDoubleResource extends BaseResource<Double> {

    private static GetFeatureVariableDoubleResource instance;

    private GetFeatureVariableDoubleResource() {
        super();
    }

    public static GetFeatureVariableDoubleResource getInstance() {
        if (instance == null) {
            instance = new GetFeatureVariableDoubleResource();
        }
        return instance;
    }

    public BaseResponse convertToResourceCall(OptimizelyE2EService optimizelyE2EService, Object desreailizeObject) {
        GetFeatureVariableDoubleRequest getFeatureVariableDoubleRequest = mapper.convertValue(desreailizeObject, GetFeatureVariableDoubleRequest.class);
        ListenerMethodResponse<Double> listenerMethodResponse = getFeatureVariableDouble(optimizelyE2EService, getFeatureVariableDoubleRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<Double> getFeatureVariableDouble(OptimizelyE2EService optimizelyE2EService, GetFeatureVariableDoubleRequest getFeatureVariableDoubleRequest) {

        Double variableValue = optimizelyE2EService.getOptimizelyManager().getOptimizely().getFeatureVariableDouble(
                getFeatureVariableDoubleRequest.getFeatureFlagKey(),
                getFeatureVariableDoubleRequest.getVariableKey(),
                getFeatureVariableDoubleRequest.getUserId(),
                getFeatureVariableDoubleRequest.getAttributes()
                );

        return sendResponse(variableValue, optimizelyE2EService);
    }

}
