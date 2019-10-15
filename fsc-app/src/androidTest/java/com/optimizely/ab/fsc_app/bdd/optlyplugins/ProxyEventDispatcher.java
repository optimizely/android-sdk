package com.optimizely.ab.fsc_app.bdd.optlyplugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ProxyEventDispatcher implements EventHandler {

    private static Logger logger = LoggerFactory.getLogger(ProxyEventDispatcher.class);
    private static ArrayList<Map<String,Object>> dispatchedEvents = new ArrayList<>();

    public ProxyEventDispatcher(ArrayList<Map<String,Object>> dispatchedEvents){
        if(dispatchedEvents != null)
            this.dispatchedEvents = dispatchedEvents;
        else
            this.dispatchedEvents = new ArrayList<>();
    }

    @Override
    public void dispatchEvent(LogEvent logEvent) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> eventParams = mapper.readValue(logEvent.getBody(), Map.class);
        Map<String, Object> map = new HashMap<>();
        map.put("url", logEvent.getEndpointUrl());
        map.put("http_verb", logEvent.getRequestMethod());
        map.put("params", eventParams);
        dispatchedEvents.add(map);

        logger.debug("Called dispatchEvent with URL: {} and params: {}", logEvent.getEndpointUrl(),
                logEvent.getRequestParams());
    }

    public static ArrayList<Map<String, Object>> getDispatchedEvents() {
        return dispatchedEvents;
    }

}
