package com.optimizely.ab.fsc_app.bdd.support.resources;


import com.optimizely.ab.config.Variation;
import com.optimizely.ab.fsc_app.bdd.models.requests.ForcedVariationRequest;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodResponse;
import com.optimizely.ab.fsc_app.bdd.support.OptimizelyE2EService;

public class GetForcedVariationResource extends BaseResource<String> {
    private static GetForcedVariationResource instance;

    private GetForcedVariationResource() {
        super();
    }

    public static GetForcedVariationResource getInstance() {
        if (instance == null) {
            instance = new GetForcedVariationResource();
        }
        return instance;
    }

    public BaseResponse convertToResourceCall(OptimizelyE2EService optimizelyE2EService, Object desreailizeObject) {
        ForcedVariationRequest forcedVariationRequest = mapper.convertValue(desreailizeObject, ForcedVariationRequest.class);
        ListenerMethodResponse<String> listenerMethodResponse = setForcedVariation(optimizelyE2EService, forcedVariationRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<String> setForcedVariation(OptimizelyE2EService optimizelyE2EService, ForcedVariationRequest forcedVariationRequest) {

        Variation forcedVariation = optimizelyE2EService.getOptimizelyManager().getOptimizely().getForcedVariation(
                forcedVariationRequest.getExperimentKey(),
                forcedVariationRequest.getUserId()
        );

        String variationKey = null;
        if (forcedVariation != null)
            variationKey = forcedVariation.getKey();

        return sendResponse(variationKey, optimizelyE2EService);
    }
}
