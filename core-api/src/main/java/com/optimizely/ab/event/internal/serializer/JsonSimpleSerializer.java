/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
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

import com.optimizely.ab.event.internal.payload.Attribute;
import com.optimizely.ab.event.internal.payload.Decision;
import com.optimizely.ab.event.internal.payload.EventBatch;

import com.optimizely.ab.event.internal.payload.Event;
import com.optimizely.ab.event.internal.payload.Snapshot;
import com.optimizely.ab.event.internal.payload.Visitor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
class JsonSimpleSerializer implements Serializer {

    public <T> String serialize(T payload) {
        JSONObject payloadJsonObj = serializeEventBatch((EventBatch)payload);

        return payloadJsonObj.toJSONString();
    }

    private JSONObject serializeEventBatch(EventBatch eventBatch) {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("account_id", eventBatch.getAccountId());
        jsonObject.put("visitors", serializeVisitors(eventBatch.getVisitors()));
        if (eventBatch.getAnonymizeIp() != null) jsonObject.put("anonymize_ip", eventBatch.getAnonymizeIp());
        if (eventBatch.getClientName() != null) jsonObject.put("client_name", eventBatch.getClientName());
        if (eventBatch.getClientVersion() != null) jsonObject.put("client_version", eventBatch.getClientVersion());
        if (eventBatch.getProjectId() != null) jsonObject.put("project_id", eventBatch.getProjectId());
        if (eventBatch.getRevision() != null) jsonObject.put("revision", eventBatch.getRevision());

        return jsonObject;

    }

    private JSONArray serializeVisitors(List<Visitor> visitors) {
        JSONArray jsonArray = new JSONArray();

        for (Visitor v: visitors) {
            jsonArray.add(serializeVisitor(v));
        }

        return jsonArray;
    }

    private JSONObject serializeVisitor(Visitor visitor) {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("visitor_id", visitor.getVisitorId());

        if (visitor.getSessionId() != null) jsonObject.put("session_id", visitor.getSessionId());

        if (visitor.getAttributes() != null) jsonObject.put("attributes", serializeFeatures(visitor.getAttributes()));

        jsonObject.put("snapshots", serializeSnapshots(visitor.getSnapshots()));

        return jsonObject;
    }

    private JSONArray serializeSnapshots(List<Snapshot> snapshots) {
        JSONArray jsonArray = new JSONArray();

        for (Snapshot snapshot : snapshots) {
            jsonArray.add(serializeSnapshot(snapshot));
        }

        return jsonArray;
    }

    private JSONObject serializeSnapshot(Snapshot snapshot) {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("decisions", serializeDecisions(snapshot.getDecisions()));
        jsonObject.put("events", serializeEvents(snapshot.getEvents()));

        return jsonObject;
    }

    private JSONArray serializeEvents(List<Event> events) {
        JSONArray jsonArray = new JSONArray();

        for (Event event : events) {
            jsonArray.add(serializeEvent(event));
        }

        return jsonArray;
    }

    private JSONObject serializeEvent(Event eventV3) {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("timestamp",eventV3.getTimestamp());
        jsonObject.put("uuid",eventV3.getUuid());
        jsonObject.put("key", eventV3.getKey());

        if (eventV3.getEntityId() != null)  jsonObject.put("entity_id",eventV3.getEntityId());
        if (eventV3.getQuantity() != null)  jsonObject.put("quantity",eventV3.getQuantity());
        if (eventV3.getRevenue() != null)   jsonObject.put("revenue",eventV3.getRevenue());
        if (eventV3.getTags() != null)  jsonObject.put("tags",serializeTags(eventV3.getTags()));
        if (eventV3.getType() != null)  jsonObject.put("type",eventV3.getType());
        if (eventV3.getValue() != null)  jsonObject.put("value",eventV3.getValue());

        return jsonObject;
    }

    private JSONArray serializeTags(Map<String, ?> tags) {
        JSONArray jsonArray = new JSONArray();
        for (Map.Entry<String, ?> entry : tags.entrySet()) {
            if (entry.getValue() != null) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(entry.getKey(), entry.getValue());
            }
        }

        return jsonArray;
    }

    private JSONObject serializeDecision(Decision decision) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("campaign_id", decision.getCampaignId());
        if (decision.getExperimentId() != null) jsonObject.put("experiment_id", decision.getExperimentId());
        if (decision.getVariationId() != null) jsonObject.put("variation_id", decision.getVariationId());
        jsonObject.put("is_campaign_holdback", decision.getIsCampaignHoldback());

        return jsonObject;
    }

    private JSONArray serializeFeatures(List<Attribute> features) {
        JSONArray jsonArray = new JSONArray();
        for (Attribute feature : features) {
            jsonArray.add(serializeFeature(feature));
        }

        return jsonArray;
    }

    private JSONObject serializeFeature(Attribute feature) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", feature.getType());
        jsonObject.put("value", feature.getValue());
        if (feature.getEntityId() != null) jsonObject.put("entity_id", feature.getEntityId());
        if (feature.getKey() != null) jsonObject.put("key", feature.getKey());

        return jsonObject;
    }

    private JSONArray serializeDecisions(List<Decision> layerStates) {
        JSONArray jsonArray = new JSONArray();
        for (Decision layerState : layerStates) {
            jsonArray.add(serializeDecision(layerState));
        }

        return jsonArray;
    }
}
