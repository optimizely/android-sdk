package com.optimizely.ab.android.test_app.bdd.support.resources;

import com.optimizely.ab.android.test_app.bdd.support.OptimizelyWrapper;
import com.optimizely.ab.android.test_app.bdd.support.requests.ActivateRequest;
import com.optimizely.ab.android.test_app.bdd.support.responses.BaseResponse;
import com.optimizely.ab.android.test_app.bdd.support.responses.ListenerMethodResponse;
import com.optimizely.ab.config.Variation;

public class ActivateResource extends BaseResource<String> {
    private static ActivateResource instance;

    private ActivateResource() {
        super();
    }

    public static ActivateResource getInstance() {
        if (instance == null) {
            instance = new ActivateResource();
        }
        return instance;
    }

    public BaseResponse convertToResourceCall(OptimizelyWrapper optimizelyWrapper, Object desreailizeObject) {
        ActivateRequest activateRequest = mapper.convertValue(desreailizeObject, ActivateRequest.class);
        ListenerMethodResponse<Boolean> listenerMethodResponse = activate(optimizelyWrapper, activateRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<Boolean> activate(OptimizelyWrapper optimizelyWrapper, ActivateRequest activateRequest) {

        Variation variation =  optimizelyWrapper.getOptimizelyManager().getOptimizely().activate(
                activateRequest.getExperimentKey(),
                activateRequest.getUserId(),
                activateRequest.getAttributes()
        );
        String variationKey = variation != null ? variation.getKey() : null;

        return sendResponse(variationKey, optimizelyWrapper);
    }

}
