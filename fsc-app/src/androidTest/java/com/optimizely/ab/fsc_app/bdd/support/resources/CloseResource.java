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

import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodResponse;
import com.optimizely.ab.fsc_app.bdd.support.OptimizelyWrapper;

public class CloseResource extends BaseResource<Boolean> {
    private static CloseResource instance;

    private CloseResource() {
        super();
    }

    public static CloseResource getInstance() {
        if (instance == null) {
            instance = new CloseResource();
        }
        return instance;
    }

    @Override
    public BaseResponse parseToCallApi(OptimizelyWrapper optimizelyWrapper, Object desreailizeObject) {
        return close(optimizelyWrapper);
    }

    private ListenerMethodResponse<Boolean> close(OptimizelyWrapper optimizelyWrapper) {

        boolean success = true;
        try {
            optimizelyWrapper.getOptimizelyManager().getOptimizely().close();
        } catch (Exception e) {
            success = false;
            System.out.println(e.getMessage());
        } finally {
            return sendResponse(success, optimizelyWrapper);
        }
    }

}
