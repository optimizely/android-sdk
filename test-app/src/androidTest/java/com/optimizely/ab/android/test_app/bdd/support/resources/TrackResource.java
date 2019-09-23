package com.optimizely.ab.android.test_app.bdd.support.resources;

import com.optimizely.ab.android.test_app.bdd.support.OptimizelyWrapper;
import com.optimizely.ab.android.test_app.bdd.support.requests.TrackRequest;
import com.optimizely.ab.android.test_app.bdd.support.responses.BaseResponse;
import com.optimizely.ab.android.test_app.bdd.support.responses.ListenerMethodResponse;

public class TrackResource extends BaseResource {
    private static TrackResource instance;

    private TrackResource() {
        super();
    }

    public static TrackResource getInstance() {
        if (instance == null) {
            instance = new TrackResource();
        }
        return instance;
    }

    public BaseResponse convertToResourceCall(OptimizelyWrapper optimizelyWrapper, Object desreailizeObject) {
        TrackRequest trackRequest = mapper.convertValue(desreailizeObject, TrackRequest.class);
        ListenerMethodResponse<Boolean> listenerMethodResponse = track(optimizelyWrapper, trackRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<Boolean> track(OptimizelyWrapper optimizelyWrapper, TrackRequest trackRequest) {

        optimizelyWrapper.getOptimizelyManager().getOptimizely().track(
                trackRequest.getEventKey(),
                trackRequest.getUserId(),
                trackRequest.getAttributes(),
                trackRequest.getEventTags()
        );

        return sendResponse(null, optimizelyWrapper);
    }

}
