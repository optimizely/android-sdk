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
import com.optimizely.ab.fsc_app.bdd.models.requests.GetFeatureVariableStringRequest;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodResponse;

public class GetFeatureVariableStringResource extends BaseResource<String> {

    private static GetFeatureVariableStringResource instance;

    private GetFeatureVariableStringResource() {
        super();
    }

    public static GetFeatureVariableStringResource getInstance() {
        if (instance == null) {
            instance = new GetFeatureVariableStringResource();
        }
        return instance;
    }

    public BaseResponse convertToResourceCall(OptimizelyE2EService optimizelyE2EService, Object desreailizeObject) {
        GetFeatureVariableStringRequest getFeatureVariableStringRequest = mapper.convertValue(desreailizeObject, GetFeatureVariableStringRequest.class);
        ListenerMethodResponse<String> listenerMethodResponse = getFeatureVariableString(optimizelyE2EService, getFeatureVariableStringRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<String> getFeatureVariableString(OptimizelyE2EService optimizelyE2EService, GetFeatureVariableStringRequest getFeatureVariableStringRequest) {

        String variableValue = optimizelyE2EService.getOptimizelyManager().getOptimizely().getFeatureVariableString(
                getFeatureVariableStringRequest.getFeatureFlagKey(),
                getFeatureVariableStringRequest.getVariableKey(),
                getFeatureVariableStringRequest.getUserId(),
                getFeatureVariableStringRequest.getAttributes()
                );

        return sendResponse(variableValue, optimizelyE2EService);
    }

}
