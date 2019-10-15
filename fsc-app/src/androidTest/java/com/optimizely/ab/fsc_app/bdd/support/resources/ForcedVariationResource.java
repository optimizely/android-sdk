package com.optimizely.ab.fsc_app.bdd.support.resources;


import com.optimizely.ab.fsc_app.bdd.support.OptimizelyE2EService;
import com.optimizely.ab.fsc_app.bdd.models.requests.ForcedVariationRequest;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodResponse;

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

    public BaseResponse convertToResourceCall(OptimizelyE2EService optimizelyE2EService, Object desreailizeObject) {
        ForcedVariationRequest forcedVariationRequest = mapper.convertValue(desreailizeObject, ForcedVariationRequest.class);
        ListenerMethodResponse<Boolean> listenerMethodResponse = setForcedVariation(optimizelyE2EService, forcedVariationRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<Boolean> setForcedVariation(OptimizelyE2EService optimizelyE2EService, ForcedVariationRequest forcedVariationRequest) {

        Boolean forcedVariation =  optimizelyE2EService.getOptimizelyManager().getOptimizely().setForcedVariation(
                forcedVariationRequest.getExperimentKey(),
                forcedVariationRequest.getUserId(),
                forcedVariationRequest.getForcedVariationKey()
        );

        return sendResponse(forcedVariation, optimizelyE2EService);
    }
}
