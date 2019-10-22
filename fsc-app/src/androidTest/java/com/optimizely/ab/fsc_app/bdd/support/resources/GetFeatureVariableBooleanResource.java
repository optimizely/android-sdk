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

import com.optimizely.ab.fsc_app.bdd.models.apiparams.GetFeatureVariableParams;
import com.optimizely.ab.fsc_app.bdd.support.OptimizelyWrapper;
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

    @Override
    public BaseResponse parseToCallApi(OptimizelyWrapper optimizelyWrapper, Object desreailizeObject) {
        GetFeatureVariableParams getFeatureVariableParams = mapper.convertValue(desreailizeObject, GetFeatureVariableParams.class);
        return getFeatureVariableBoolean(optimizelyWrapper, getFeatureVariableParams);
    }

    private ListenerMethodResponse<Boolean> getFeatureVariableBoolean(OptimizelyWrapper optimizelyWrapper, GetFeatureVariableParams getFeatureVariableParams) {

        Boolean variableValue = optimizelyWrapper.getOptimizelyManager().getOptimizely().getFeatureVariableBoolean(
                getFeatureVariableParams.getFeatureFlagKey(),
                getFeatureVariableParams.getVariableKey(),
                getFeatureVariableParams.getUserId(),
                getFeatureVariableParams.getAttributes()
                );

        return sendResponse(variableValue, optimizelyWrapper);
    }

}
