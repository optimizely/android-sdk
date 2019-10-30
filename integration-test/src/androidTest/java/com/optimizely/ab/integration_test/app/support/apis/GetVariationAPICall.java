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

package com.optimizely.ab.integration_test.app.support.apis;

import com.optimizely.ab.config.Variation;
import com.optimizely.ab.integration_test.app.support.OptimizelyWrapper;
import com.optimizely.ab.integration_test.app.models.apiparams.GetVariationParams;
import com.optimizely.ab.integration_test.app.models.responses.BaseResponse;
import com.optimizely.ab.integration_test.app.models.responses.ListenerMethodResponse;

public class GetVariationAPICall extends APICall<String> {

    public GetVariationAPICall() {
        super();
    }

    @Override
    public BaseResponse invokeAPI(OptimizelyWrapper optimizelyWrapper, Object desreailizeObject) {
        GetVariationParams getVariationParams = mapper.convertValue(desreailizeObject, GetVariationParams.class);
        return getVariation(optimizelyWrapper, getVariationParams);
    }

    private ListenerMethodResponse<String> getVariation(OptimizelyWrapper optimizelyWrapper, GetVariationParams getVariationParams) {

        Variation variation = optimizelyWrapper.getOptimizelyManager().getOptimizely().getVariation(
                getVariationParams.getExperimentKey(),
                getVariationParams.getUserId(),
                getVariationParams.getAttributes());
       String variationKey = variation != null ? variation.getKey() : null;

        return sendResponse(variationKey, optimizelyWrapper);
    }

}
