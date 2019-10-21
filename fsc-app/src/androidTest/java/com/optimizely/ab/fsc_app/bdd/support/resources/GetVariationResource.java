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

import com.optimizely.ab.config.Variation;
import com.optimizely.ab.fsc_app.bdd.support.OptimizelyE2EService;
import com.optimizely.ab.fsc_app.bdd.models.requests.GetVariationRequest;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodResponse;

public class GetVariationResource extends BaseResource<String> {

    private static GetVariationResource instance;

    private GetVariationResource() {
        super();
    }

    public static GetVariationResource getInstance() {
        if (instance == null) {
            instance = new GetVariationResource();
        }
        return instance;
    }

    public BaseResponse convertToResourceCall(OptimizelyE2EService optimizelyE2EService, Object desreailizeObject) {
        GetVariationRequest getVariationRequest = mapper.convertValue(desreailizeObject, GetVariationRequest.class);
        ListenerMethodResponse<String> listenerMethodResponse = getVariation(optimizelyE2EService, getVariationRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<String> getVariation(OptimizelyE2EService optimizelyE2EService, GetVariationRequest getVariationRequest) {

        Variation variation = optimizelyE2EService.getOptimizelyManager().getOptimizely().getVariation(
                getVariationRequest.getExperimentKey(),
                getVariationRequest.getUserId(),
                getVariationRequest.getAttributes());
       String variationKey = variation != null ? variation.getKey() : null;

        return sendResponse(variationKey, optimizelyE2EService);
    }

}
