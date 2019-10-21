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
import com.optimizely.ab.fsc_app.bdd.models.requests.GetEnabledFeaturesRequest;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodArrayResponse;

import java.util.List;

public class GetEnabledFeaturesResource extends BaseResource<String> {

    private static GetEnabledFeaturesResource instance;

    private GetEnabledFeaturesResource() {
        super();
    }

    public static GetEnabledFeaturesResource getInstance() {
        if (instance == null) {
            instance = new GetEnabledFeaturesResource();
        }
        return instance;
    }

    public BaseResponse convertToResourceCall(OptimizelyE2EService optimizelyE2EService, Object desreailizeObject) {
        GetEnabledFeaturesRequest getEnabledFeaturesRequest = mapper.convertValue(desreailizeObject, GetEnabledFeaturesRequest.class);
        ListenerMethodArrayResponse listenerMethodArrayResponse = getEnabledFeatures(optimizelyE2EService, getEnabledFeaturesRequest);
        return listenerMethodArrayResponse;
    }

    ListenerMethodArrayResponse getEnabledFeatures(OptimizelyE2EService optimizelyE2EService, GetEnabledFeaturesRequest getEnabledFeaturesRequest) {

        List<String> enabledFeatures = optimizelyE2EService.getOptimizelyManager().getOptimizely().getEnabledFeatures(
                    getEnabledFeaturesRequest.getUserId(),
                    getEnabledFeaturesRequest.getAttributes()
                );

        return sendArrayResponse(enabledFeatures, optimizelyE2EService);
    }

}
