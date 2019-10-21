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
import com.optimizely.ab.fsc_app.bdd.models.requests.ForcedVariationRequest;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodResponse;

public class ForcedVariationResource extends BaseResource<Object> {
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
        ListenerMethodResponse<Object> listenerMethodResponse = setForcedVariation(optimizelyE2EService, forcedVariationRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<Object> setForcedVariation(OptimizelyE2EService optimizelyE2EService, ForcedVariationRequest forcedVariationRequest) {

        Boolean forcedVariation = optimizelyE2EService.getOptimizelyManager().getOptimizely().setForcedVariation(
                forcedVariationRequest.getExperimentKey(),
                forcedVariationRequest.getUserId(),
                forcedVariationRequest.getForcedVariationKey()
        );

        if (!forcedVariation) {
            return sendResponse(forcedVariation, optimizelyE2EService);
        }

        Variation variation = optimizelyE2EService.getOptimizelyManager().getOptimizely().getForcedVariation(
                forcedVariationRequest.getExperimentKey(),
                forcedVariationRequest.getUserId());
        String variationKey = null;
        if(variation != null)
            variationKey = variation.getKey();

        return sendResponse(variationKey, optimizelyE2EService);
    }
}
