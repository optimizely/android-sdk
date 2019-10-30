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

package com.optimizely.ab.integration_test.app.optlyplugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ProxyEventDispatcher implements EventHandler {

    private static Logger logger = LoggerFactory.getLogger(ProxyEventDispatcher.class);
    private List<Map<String, Object>> dispatchedEvents;

    public ProxyEventDispatcher(List<Map<String, Object>> dispatchedEvents) {
        if (dispatchedEvents != null)
            this.dispatchedEvents = dispatchedEvents;
        else
            this.dispatchedEvents = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public void dispatchEvent(LogEvent logEvent) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> eventParams = mapper.readValue(logEvent.getBody(), Map.class);
        Map<String, Object> map = new HashMap<>();
        map.put("url", logEvent.getEndpointUrl());
        map.put("http_verb", logEvent.getRequestMethod());
        map.put("params", eventParams);
        dispatchedEvents.add(map);

        logger.debug("Called dispatchEvent with URL: {} and params: {}", logEvent.getEndpointUrl(),
                logEvent.getRequestParams());
    }

    public List<Map<String, Object>> getDispatchedEvents() {
        return dispatchedEvents;
    }

}
