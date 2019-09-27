package com.optimizely.ab.fsc_app.bdd.support.resources;

import com.optimizely.ab.config.Variation;
import com.optimizely.ab.fsc_app.bdd.OptimizelyE2EService;
import com.optimizely.ab.fsc_app.bdd.support.requests.GetEnabledFeaturesRequest;
import com.optimizely.ab.fsc_app.bdd.support.requests.GetVariationRequest;
import com.optimizely.ab.fsc_app.bdd.support.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.support.responses.ListenerMethodArrayResponse;
import com.optimizely.ab.fsc_app.bdd.support.responses.ListenerMethodResponse;

import java.util.List;

public class GetEnabledFeaturesResource extends BaseResource<String> {

    private static GetEnabledFeaturesResource instance;

    private GetEnabledFeaturesResource() {
        super();
    }

    public static GetEnabledFeaturesResource getInstance() {
        if (instance == null) {
            instance = new GetEnabledFeaturesResource();
        }
        return instance;
    }

    public BaseResponse convertToResourceCall(OptimizelyE2EService optimizelyE2EService, Object desreailizeObject) {
        GetEnabledFeaturesRequest getEnabledFeaturesRequest = mapper.convertValue(desreailizeObject, GetEnabledFeaturesRequest.class);
        ListenerMethodArrayResponse listenerMethodArrayResponse = getEnabledFeatures(optimizelyE2EService, getEnabledFeaturesRequest);
        return listenerMethodArrayResponse;
    }

    ListenerMethodArrayResponse getEnabledFeatures(OptimizelyE2EService optimizelyE2EService, GetEnabledFeaturesRequest getEnabledFeaturesRequest) {

        List<String> enabledFeatures = optimizelyE2EService.getOptimizelyManager().getOptimizely().getEnabledFeatures(
                    getEnabledFeaturesRequest.getUserId(),
                    getEnabledFeaturesRequest.getAttributes()
                );

        return sendArrayResponse(enabledFeatures, optimizelyE2EService);
    }

}
