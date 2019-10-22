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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.support.OptimizelyWrapper;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodArrayResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodResponse;

import java.util.List;
import java.util.Map;

import static com.optimizely.ab.fsc_app.bdd.optlyplugins.TestCompositeService.setListenerResponseCollection;

public abstract class BaseResource<TResponse> {

    protected ObjectMapper mapper;

    protected BaseResource() {
        mapper = new ObjectMapper();
    }

    public abstract BaseResponse parseToCallApi(OptimizelyWrapper optimizelyWrapper, Object desreailizeObject);

    ListenerMethodResponse sendResponse(TResponse result, OptimizelyWrapper optimizelyWrapper) {
        List<Map<String, Object>> responseCollection = setListenerResponseCollection(optimizelyWrapper);
        return new ListenerMethodResponse<TResponse>(result, responseCollection);
    }

    ListenerMethodArrayResponse sendArrayResponse(List<String> result, OptimizelyWrapper optimizelyWrapper) {
        List<Map<String, Object>> responseCollection = setListenerResponseCollection(optimizelyWrapper);
        return new ListenerMethodArrayResponse(result, responseCollection);
    }
}
