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
package com.optimizely.ab.config.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Experiment.ExperimentStatus;
import com.optimizely.ab.config.LiveVariableUsageInstance;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class GsonHelpers {

    private static List<Variation> parseVariations(JsonArray variationJson, JsonDeserializationContext context) {
        List<Variation> variations = new ArrayList<Variation>(variationJson.size());
        for (Object obj : variationJson) {
            JsonObject variationObject = (JsonObject)obj;
            String id = variationObject.get("id").getAsString();
            String key = variationObject.get("key").getAsString();

            List<LiveVariableUsageInstance> variableUsageInstances = null;
            // this is an existence check rather than a version check since it's difficult to pass data
            // across deserializers.
            if (variationObject.has("variables")) {
                Type liveVariableUsageInstancesType = new TypeToken<List<LiveVariableUsageInstance>>() {}.getType();
                variableUsageInstances =
                        context.deserialize(variationObject.getAsJsonArray("variables"),
                                            liveVariableUsageInstancesType);
            }

            variations.add(new Variation(id, key, variableUsageInstances));
        }

        return variations;
    }

    private static Map<String, String> parseForcedVariations(JsonObject forcedVariationJson) {
        Map<String, String> userIdToVariationKeyMap = new HashMap<String, String>();
        Set<Map.Entry<String, JsonElement>> entrySet = forcedVariationJson.entrySet();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            userIdToVariationKeyMap.put(entry.getKey(), entry.getValue().getAsString());
        }

        return userIdToVariationKeyMap;
    }

    static List<TrafficAllocation> parseTrafficAllocation(JsonArray trafficAllocationJson) {
        List<TrafficAllocation> trafficAllocation = new ArrayList<TrafficAllocation>(trafficAllocationJson.size());

        for (Object obj : trafficAllocationJson) {
            JsonObject allocationObject = (JsonObject)obj;
            String entityId = allocationObject.get("entityId").getAsString();
            int endOfRange = allocationObject.get("endOfRange").getAsInt();

            trafficAllocation.add(new TrafficAllocation(entityId, endOfRange));
        }

        return trafficAllocation;
    }

    static Experiment parseExperiment(JsonObject experimentJson, String groupId, JsonDeserializationContext context) {
        String id = experimentJson.get("id").getAsString();
        String key = experimentJson.get("key").getAsString();
        JsonElement experimentStatusJson = experimentJson.get("status");
        String status = experimentStatusJson.isJsonNull() ?
                ExperimentStatus.NOT_STARTED.toString() : experimentStatusJson.getAsString();

        JsonElement layerIdJson = experimentJson.get("layerId");
        String layerId = layerIdJson == null ? null : layerIdJson.getAsString();

        JsonArray audienceIdsJson = experimentJson.getAsJsonArray("audienceIds");
        List<String> audienceIds = new ArrayList<String>(audienceIdsJson.size());
        for (JsonElement audienceIdObj : audienceIdsJson) {
            audienceIds.add(audienceIdObj.getAsString());
        }

        // parse the child objects
        List<Variation> variations = parseVariations(experimentJson.getAsJsonArray("variations"), context);
        Map<String, String> userIdToVariationKeyMap =
                parseForcedVariations(experimentJson.getAsJsonObject("forcedVariations"));
        List<TrafficAllocation> trafficAllocations =
                parseTrafficAllocation(experimentJson.getAsJsonArray("trafficAllocation"));

        return new Experiment(id, key, status, layerId, audienceIds, variations, userIdToVariationKeyMap,
                              trafficAllocations, groupId);
    }

    static Experiment parseExperiment(JsonObject experimentJson, JsonDeserializationContext context) {
        return parseExperiment(experimentJson, "", context);
    }
}
