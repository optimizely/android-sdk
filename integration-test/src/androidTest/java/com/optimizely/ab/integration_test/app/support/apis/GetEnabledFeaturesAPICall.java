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

import com.optimizely.ab.integration_test.app.models.apiparams.BaseParams;
import com.optimizely.ab.integration_test.app.support.OptimizelyWrapper;
import com.optimizely.ab.integration_test.app.models.responses.BaseResponse;
import com.optimizely.ab.integration_test.app.models.responses.ListenerMethodArrayResponse;

import java.util.List;

public class GetEnabledFeaturesAPICall extends APICall<String> {

    public GetEnabledFeaturesAPICall() {
        super();
    }

    @Override
    public BaseResponse invokeAPI(OptimizelyWrapper optimizelyWrapper, Object desreailizeObject) {
        BaseParams getEnabledFeaturesParams = mapper.convertValue(desreailizeObject, BaseParams.class);
        return getEnabledFeatures(optimizelyWrapper, getEnabledFeaturesParams);
    }

    private ListenerMethodArrayResponse getEnabledFeatures(OptimizelyWrapper optimizelyWrapper, BaseParams getEnabledFeaturesParams) {

        List<String> enabledFeatures = optimizelyWrapper.getOptimizelyManager().getOptimizely().getEnabledFeatures(
                    getEnabledFeaturesParams.getUserId(),
                    getEnabledFeaturesParams.getAttributes()
                );

        return sendArrayResponse(enabledFeatures, optimizelyWrapper);
    }

}
