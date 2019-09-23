package com.optimizely.ab.android.test_app.bdd.support.resources;

import com.optimizely.ab.android.test_app.bdd.support.OptimizelyWrapper;
import com.optimizely.ab.android.test_app.bdd.support.requests.ForcedVariationRequest;
import com.optimizely.ab.android.test_app.bdd.support.responses.BaseResponse;
import com.optimizely.ab.android.test_app.bdd.support.responses.ListenerMethodResponse;

public class ForcedVariationResource extends BaseResource<Boolean> {
    private static ForcedVariationResource instance;

    private ForcedVariationResource() {
        super();
    }

    public static ForcedVariationResource getInstance() {
        if (instance == null) {
            instance = new ForcedVariationResource();
        }
        return instance;
    }

    public BaseResponse convertToResourceCall(OptimizelyWrapper optimizelyWrapper, Object desreailizeObject) {
        ForcedVariationRequest forcedVariationRequest = mapper.convertValue(desreailizeObject, ForcedVariationRequest.class);
        ListenerMethodResponse<Boolean> listenerMethodResponse = setForcedVariation(optimizelyWrapper, forcedVariationRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<Boolean> setForcedVariation(OptimizelyWrapper optimizelyWrapper, ForcedVariationRequest forcedVariationRequest) {

        Boolean forcedVariation =  optimizelyWrapper.getOptimizelyManager().getOptimizely().setForcedVariation(
                forcedVariationRequest.getExperimentKey(),
                forcedVariationRequest.getUserId(),
                forcedVariationRequest.getForcedVariationKey()
        );

        return sendResponse(forcedVariation, optimizelyWrapper);
    }
}
