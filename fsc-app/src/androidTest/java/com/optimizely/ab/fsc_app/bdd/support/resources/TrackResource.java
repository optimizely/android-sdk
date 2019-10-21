/****************************************************************************
 * Copyright 2019, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.fsc_app.bdd.support.resources;

import com.optimizely.ab.fsc_app.bdd.support.OptimizelyE2EService;
import com.optimizely.ab.fsc_app.bdd.models.requests.TrackRequest;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodResponse;

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

    public BaseResponse convertToResourceCall(OptimizelyE2EService optimizelyE2EService, Object desreailizeObject) {
        TrackRequest trackRequest = mapper.convertValue(desreailizeObject, TrackRequest.class);
        ListenerMethodResponse<Boolean> listenerMethodResponse = track(optimizelyE2EService, trackRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<Boolean> track(OptimizelyE2EService optimizelyE2EService, TrackRequest trackRequest) {

        optimizelyE2EService.getOptimizelyManager().getOptimizely().track(
                trackRequest.getEventKey(),
                trackRequest.getUserId(),
                trackRequest.getAttributes(),
                trackRequest.getEventTags()
        );

        return sendResponse(null, optimizelyE2EService);
    }

}
