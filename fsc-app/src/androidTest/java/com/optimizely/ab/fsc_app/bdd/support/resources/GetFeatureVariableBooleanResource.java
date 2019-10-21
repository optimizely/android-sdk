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
import com.optimizely.ab.fsc_app.bdd.models.requests.GetFeatureVariableBooleanRequest;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodResponse;

public class GetFeatureVariableBooleanResource extends BaseResource<Boolean> {

    private static GetFeatureVariableBooleanResource instance;

    private GetFeatureVariableBooleanResource() {
        super();
    }

    public static GetFeatureVariableBooleanResource getInstance() {
        if (instance == null) {
            instance = new GetFeatureVariableBooleanResource();
        }
        return instance;
    }

    public BaseResponse convertToResourceCall(OptimizelyE2EService optimizelyE2EService, Object desreailizeObject) {
        GetFeatureVariableBooleanRequest getFeatureVariableBooleanRequest = mapper.convertValue(desreailizeObject, GetFeatureVariableBooleanRequest.class);
        ListenerMethodResponse<Boolean> listenerMethodResponse = getFeatureVariableBoolean(optimizelyE2EService, getFeatureVariableBooleanRequest);
        return listenerMethodResponse;
    }

    ListenerMethodResponse<Boolean> getFeatureVariableBoolean(OptimizelyE2EService optimizelyE2EService, GetFeatureVariableBooleanRequest getFeatureVariableBooleanRequest) {

        Boolean variableValue = optimizelyE2EService.getOptimizelyManager().getOptimizely().getFeatureVariableBoolean(
                getFeatureVariableBooleanRequest.getFeatureFlagKey(),
                getFeatureVariableBooleanRequest.getVariableKey(),
                getFeatureVariableBooleanRequest.getUserId(),
                getFeatureVariableBooleanRequest.getAttributes()
                );

        return sendResponse(variableValue, optimizelyE2EService);
    }

}
