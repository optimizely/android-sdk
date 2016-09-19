/**
 *
 *    Copyright 2016, Optimizely
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Group;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupJacksonDeserializer extends JsonDeserializer<Group> {

    @Override
    public Group deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = parser.getCodec().readTree(parser);

        String id = node.get("id").textValue();
        String policy = node.get("policy").textValue();
        List<TrafficAllocation> trafficAllocations = mapper.readValue(node.get("trafficAllocation").toString(),
                                                                      new TypeReference<List<TrafficAllocation>>(){});

        JsonNode groupExperimentsJson = node.get("experiments");
        List<Experiment> groupExperiments = new ArrayList<Experiment>();
        if (groupExperimentsJson.isArray()) {
            for (JsonNode groupExperimentJson : groupExperimentsJson) {
                groupExperiments.add(parseExperiment(groupExperimentJson, id));
            }
        }

        return new Group(id, policy, groupExperiments, trafficAllocations);
    }

    private Experiment parseExperiment(JsonNode experimentJson, String groupId) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        String id = experimentJson.get("id").textValue();
        String key = experimentJson.get("key").textValue();
        String status = experimentJson.get("status").textValue();
        JsonNode layerIdJson = experimentJson.get("layerId");
        String layerId = layerIdJson == null ? null : layerIdJson.textValue();
        List<String> audienceIds = mapper.readValue(experimentJson.get("audienceIds").toString(),
                                                    new TypeReference<List<String>>(){});
        List<Variation> variations = mapper.readValue(experimentJson.get("variations").toString(),
                                                      new TypeReference<List<Variation>>(){});
        List<TrafficAllocation> trafficAllocations = mapper.readValue(experimentJson.get("trafficAllocation").toString(),
                                                                      new TypeReference<List<TrafficAllocation>>(){});
        Map<String, String>  userIdToVariationKeyMap = mapper.readValue(
            experimentJson.get("forcedVariations").toString(), new TypeReference<Map<String, String>>(){});

        return new Experiment(id, key, status, layerId, audienceIds, variations, userIdToVariationKeyMap,
                              trafficAllocations, groupId);
    }

}
