/**
 *
 *    Copyright 2016, Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.event.internal.serializer;

import com.optimizely.ab.event.internal.payload.Conversion;
import com.optimizely.ab.event.internal.payload.Decision;
import com.optimizely.ab.event.internal.payload.EventMetric;
import com.optimizely.ab.event.internal.payload.Feature;
import com.optimizely.ab.event.internal.payload.Impression;
import com.optimizely.ab.event.internal.payload.LayerState;
import com.optimizely.ab.event.internal.payload.Event;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;

@SuppressWarnings("unchecked")
class JsonSimpleSerializer implements Serializer {

    public <T extends Event> String serialize(T payload) {
        JSONObject payloadJsonObj;
        if (payload instanceof Impression) {
            payloadJsonObj = serializeImpression((Impression)payload);
        } else {
            payloadJsonObj = serializeConversion((Conversion)payload);
        }

        return payloadJsonObj.toJSONString();
    }

    private JSONObject serializeImpression(Impression impression) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("visitorId", impression.getVisitorId());
        jsonObject.put("timestamp", impression.getTimestamp());
        jsonObject.put("isGlobalHoldback", impression.getIsGlobalHoldback());
        jsonObject.put("anonymizeIP", impression.getAnonymizeIP());
        jsonObject.put("projectId", impression.getProjectId());
        jsonObject.put("decision", serializeDecision(impression.getDecision()));
        jsonObject.put("layerId", impression.getLayerId());
        jsonObject.put("accountId", impression.getAccountId());
        jsonObject.put("userFeatures", serializeFeatures(impression.getUserFeatures()));
        jsonObject.put("clientEngine", impression.getClientEngine());
        jsonObject.put("clientVersion", impression.getClientVersion());
        jsonObject.put("revision", impression.getRevision());

        if (impression.getSessionId() != null) {
            jsonObject.put("sessionId", impression.getSessionId());
        }

        return jsonObject;
    }

    private JSONObject serializeConversion(Conversion conversion) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("visitorId", conversion.getVisitorId());
        jsonObject.put("timestamp", conversion.getTimestamp());
        jsonObject.put("projectId", conversion.getProjectId());
        jsonObject.put("accountId", conversion.getAccountId());
        jsonObject.put("userFeatures", serializeFeatures(conversion.getUserFeatures()));
        jsonObject.put("layerStates", serializeLayerStates(conversion.getLayerStates()));
        jsonObject.put("eventEntityId", conversion.getEventEntityId());
        jsonObject.put("eventName", conversion.getEventName());
        jsonObject.put("eventMetrics", serializeEventMetrics(conversion.getEventMetrics()));
        jsonObject.put("eventFeatures", serializeFeatures(conversion.getEventFeatures()));
        jsonObject.put("isGlobalHoldback", conversion.getIsGlobalHoldback());
        jsonObject.put("anonymizeIP", conversion.getAnonymizeIP());
        jsonObject.put("clientEngine", conversion.getClientEngine());
        jsonObject.put("clientVersion", conversion.getClientVersion());
        jsonObject.put("revision", conversion.getRevision());

        if (conversion.getSessionId() != null) {
            jsonObject.put("sessionId", conversion.getSessionId());
        }

        return jsonObject;
    }

    private JSONObject serializeDecision(Decision decision) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("variationId", decision.getVariationId());
        jsonObject.put("isLayerHoldback", decision.getIsLayerHoldback());
        jsonObject.put("experimentId", decision.getExperimentId());

        return jsonObject;
    }

    private JSONArray serializeFeatures(List<Feature> features) {
        JSONArray jsonArray = new JSONArray();
        for (Feature feature : features) {
            jsonArray.add(serializeFeature(feature));
        }

        return jsonArray;
    }

    private JSONObject serializeFeature(Feature feature) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", feature.getId());
        jsonObject.put("name", feature.getName());
        jsonObject.put("type", feature.getType());
        jsonObject.put("value", feature.getValue());
        jsonObject.put("shouldIndex", feature.getShouldIndex());

        return jsonObject;
    }

    private JSONArray serializeLayerStates(List<LayerState> layerStates) {
        JSONArray jsonArray = new JSONArray();
        for (LayerState layerState : layerStates) {
            jsonArray.add(serializeLayerState(layerState));
        }

        return jsonArray;
    }

    private JSONObject serializeLayerState(LayerState layerState) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("layerId", layerState.getLayerId());
        jsonObject.put("revision", layerState.getRevision());
        jsonObject.put("decision", serializeDecision(layerState.getDecision()));
        jsonObject.put("actionTriggered", layerState.getActionTriggered());

        return jsonObject;
    }

    private JSONArray serializeEventMetrics(List<EventMetric> eventMetrics) {
        JSONArray jsonArray = new JSONArray();
        for (EventMetric eventMetric : eventMetrics) {
            jsonArray.add(serializeEventMetric(eventMetric));
        }

        return jsonArray;
    }

    private JSONObject serializeEventMetric(EventMetric eventMetric) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", eventMetric.getName());
        jsonObject.put("value", eventMetric.getValue());

        return jsonObject;
    }
}
