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
import com.optimizely.ab.fsc_app.bdd.support.OptimizelyWrapper;
import com.optimizely.ab.fsc_app.bdd.models.apiparams.ActivateParams;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodResponse;

public class ActivateResource extends BaseResource<String> {
    private static ActivateResource instance;

    private ActivateResource() {
        super();
    }

    public static ActivateResource getInstance() {
        if (instance == null) {
            instance = new ActivateResource();
        }
        return instance;
    }

    @Override
    public BaseResponse parseToCallApi(OptimizelyWrapper optimizelyWrapper, Object desreailizeObject) {
        ActivateParams activateParams = mapper.convertValue(desreailizeObject, ActivateParams.class);
        return activate(optimizelyWrapper, activateParams);
    }

    private ListenerMethodResponse<Boolean> activate(OptimizelyWrapper optimizelyWrapper, ActivateParams activateParams) {

        Variation variation = optimizelyWrapper.getOptimizelyManager().getOptimizely().activate(
                activateParams.getExperimentKey(),
                activateParams.getUserId(),
                activateParams.getAttributes()
        );
        String variationKey = variation != null ? variation.getKey() : null;

        return sendResponse(variationKey, optimizelyWrapper);
    }

}
