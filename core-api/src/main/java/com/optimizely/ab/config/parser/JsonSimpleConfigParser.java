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

import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.NotCondition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.config.audience.UserAttribute;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Group;
import com.optimizely.ab.config.LiveVariable;
import com.optimizely.ab.config.LiveVariableUsageInstance;
import com.optimizely.ab.config.LiveVariable.VariableStatus;
import com.optimizely.ab.config.LiveVariable.VariableType;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * {@code json-simple}-based config parser implementation.
 */
final class JsonSimpleConfigParser implements ConfigParser {

    @Override
    public ProjectConfig parseProjectConfig(@Nonnull String json) throws ConfigParseException {
        try {
            JSONParser parser = new JSONParser();
            JSONObject rootObject = (JSONObject)parser.parse(json);

            String accountId = (String)rootObject.get("accountId");
            String projectId = (String)rootObject.get("projectId");
            String revision = (String)rootObject.get("revision");
            String version = (String)rootObject.get("version");

            List<Experiment> experiments = parseExperiments((JSONArray)rootObject.get("experiments"));

            List<Attribute> attributes;
            if (version.equals(ProjectConfig.Version.V1.toString())) {
                attributes = parseAttributes((JSONArray)rootObject.get("dimensions"));
            } else {
                attributes = parseAttributes((JSONArray)rootObject.get("attributes"));
            }

            List<EventType> events = parseEvents((JSONArray)rootObject.get("events"));
            List<Audience> audiences = parseAudiences((JSONArray)parser.parse(rootObject.get("audiences").toString()));
            List<Group> groups = parseGroups((JSONArray)rootObject.get("groups"));

            boolean anonymizeIP = false;
            List<LiveVariable> liveVariables = null;
            if (version.equals(ProjectConfig.Version.V3.toString())) {
                liveVariables = parseLiveVariables((JSONArray)rootObject.get("variables"));

                anonymizeIP = (Boolean)rootObject.get("anonymizeIP");
            }

            return new ProjectConfig(accountId, projectId, version, revision, groups, experiments, attributes, events,
                                     audiences, anonymizeIP, liveVariables);
        } catch (Exception e) {
            throw new ConfigParseException("Unable to parse datafile: " + json, e);
        }
    }

    //======== Helper methods ========//

    private List<Experiment> parseExperiments(JSONArray experimentJson) {
        return parseExperiments(experimentJson, "");
    }

    private List<Experiment> parseExperiments(JSONArray experimentJson, String groupId) {
        List<Experiment> experiments = new ArrayList<Experiment>(experimentJson.size());

        for (Object obj : experimentJson) {
            JSONObject experimentObject = (JSONObject)obj;
            String id = (String)experimentObject.get("id");
            String key = (String)experimentObject.get("key");
            String status = (String)experimentObject.get("status");
            Object layerIdObject = experimentObject.get("layerId");
            String layerId = layerIdObject == null ? null : (String)layerIdObject;

            JSONArray audienceIdsJson = (JSONArray)experimentObject.get("audienceIds");
            List<String> audienceIds = new ArrayList<String>(audienceIdsJson.size());

            for (Object audienceIdObj : audienceIdsJson) {
                audienceIds.add((String)audienceIdObj);
            }

            // parse the child objects
            List<Variation> variations = parseVariations((JSONArray)experimentObject.get("variations"));
            Map<String, String> userIdToVariationKeyMap =
                parseForcedVariations((JSONObject)experimentObject.get("forcedVariations"));
            List<TrafficAllocation> trafficAllocations =
                parseTrafficAllocation((JSONArray)experimentObject.get("trafficAllocation"));

            experiments.add(new Experiment(id, key, status, layerId, audienceIds, variations, userIdToVariationKeyMap,
                                           trafficAllocations, groupId));
        }

        return experiments;
    }

    private List<Variation> parseVariations(JSONArray variationJson) {
        List<Variation> variations = new ArrayList<Variation>(variationJson.size());

        for (Object obj : variationJson) {
            JSONObject variationObject = (JSONObject)obj;
            String id = (String)variationObject.get("id");
            String key = (String)variationObject.get("key");

            List<LiveVariableUsageInstance> liveVariableUsageInstances = null;
            if (variationObject.containsKey("variables")) {
                liveVariableUsageInstances = parseLiveVariableInstances((JSONArray)variationObject.get("variables"));
            }

            variations.add(new Variation(id, key, liveVariableUsageInstances));
        }

        return variations;
    }

    private Map<String, String> parseForcedVariations(JSONObject forcedVariationJson) {
        Map<String, String> userIdToVariationKeyMap = new HashMap<String, String>();
        for (Object obj : forcedVariationJson.entrySet()) {
            Map.Entry<String, String> entry = (Map.Entry<String, String>)obj;
            userIdToVariationKeyMap.put(entry.getKey(), entry.getValue());
        }

        return userIdToVariationKeyMap;
    }

    private List<TrafficAllocation> parseTrafficAllocation(JSONArray trafficAllocationJson) {
        List<TrafficAllocation> trafficAllocation = new ArrayList<TrafficAllocation>(trafficAllocationJson.size());

        for (Object obj : trafficAllocationJson) {
            JSONObject allocationObject = (JSONObject)obj;
            String entityId = (String)allocationObject.get("entityId");
            long endOfRange = (Long)allocationObject.get("endOfRange");

            trafficAllocation.add(new TrafficAllocation(entityId, (int)endOfRange));
        }

        return trafficAllocation;
    }

    private List<Attribute> parseAttributes(JSONArray attributeJson) {
        List<Attribute> attributes = new ArrayList<Attribute>(attributeJson.size());

        for (Object obj : attributeJson) {
            JSONObject attributeObject = (JSONObject)obj;
            String id = (String)attributeObject.get("id");
            String key = (String)attributeObject.get("key");
            String segmentId = (String)attributeObject.get("segmentId");

            attributes.add(new Attribute(id, key, segmentId));
        }

        return attributes;
    }

    private List<EventType> parseEvents(JSONArray eventJson) {
        List<EventType> events = new ArrayList<EventType>(eventJson.size());

        for (Object obj : eventJson) {
            JSONObject eventObject = (JSONObject)obj;
            JSONArray experimentIdsJson = (JSONArray)eventObject.get("experimentIds");
            List<String> experimentIds = new ArrayList<String>(experimentIdsJson.size());

            for (Object experimentIdObj : experimentIdsJson) {
                experimentIds.add((String)experimentIdObj);
            }

            String id = (String)eventObject.get("id");
            String key = (String)eventObject.get("key");

            events.add(new EventType(id, key, experimentIds));
        }

        return events;
    }

    private List<Audience> parseAudiences(JSONArray audienceJson) throws ParseException {
        JSONParser parser = new JSONParser();
        List<Audience> audiences = new ArrayList<Audience>(audienceJson.size());

        for (Object obj : audienceJson) {
            JSONObject audienceObject = (JSONObject)obj;
            String id = (String)audienceObject.get("id");
            String key = (String)audienceObject.get("name");
            String conditionString = (String)audienceObject.get("conditions");

            JSONArray conditionJson = (JSONArray)parser.parse(conditionString);
            Condition conditions = parseConditions(conditionJson);
            audiences.add(new Audience(id, key, conditions));
        }

        return audiences;
    }

    private Condition parseConditions(JSONArray conditionJson) {
        List<Condition> conditions = new ArrayList<Condition>();
        String operand = (String)conditionJson.get(0);

        for (int i = 1; i < conditionJson.size(); i++) {
            Object obj = conditionJson.get(i);
            if (obj instanceof JSONArray) {
                conditions.add(parseConditions((JSONArray)conditionJson.get(i)));
            } else {
                JSONObject conditionMap = (JSONObject)obj;
                conditions.add(new UserAttribute((String)conditionMap.get("name"), (String)conditionMap.get("type"),
                               (String)conditionMap.get("value")));
            }
        }

        Condition condition;
        if (operand.equals("and")) {
            condition = new AndCondition(conditions);
        } else if (operand.equals("or")) {
            condition = new OrCondition(conditions);
        } else {
            condition = new NotCondition(conditions.get(0));
        }

        return condition;
    }

    private List<Group> parseGroups(JSONArray groupJson) {
        List<Group> groups = new ArrayList<Group>(groupJson.size());

        for (Object obj : groupJson) {
            JSONObject groupObject = (JSONObject)obj;
            String id = (String)groupObject.get("id");
            String policy = (String)groupObject.get("policy");
            List<Experiment> experiments = parseExperiments((JSONArray)groupObject.get("experiments"), id);
            List<TrafficAllocation> trafficAllocations =
                    parseTrafficAllocation((JSONArray)groupObject.get("trafficAllocation"));

            groups.add(new Group(id, policy, experiments, trafficAllocations));
        }

        return groups;
    }

    private List<LiveVariable> parseLiveVariables(JSONArray liveVariablesJson) {
        List<LiveVariable> liveVariables = new ArrayList<LiveVariable>(liveVariablesJson.size());

        for (Object obj : liveVariablesJson) {
            JSONObject liveVariableObject = (JSONObject)obj;
            String id = (String)liveVariableObject.get("id");
            String key = (String)liveVariableObject.get("key");
            String defaultValue = (String)liveVariableObject.get("defaultValue");
            VariableType type = VariableType.fromString((String)liveVariableObject.get("type"));
            VariableStatus status = VariableStatus.fromString((String)liveVariableObject.get("status"));

            liveVariables.add(new LiveVariable(id, key, defaultValue, status, type));
        }

        return liveVariables;
    }

    private List<LiveVariableUsageInstance> parseLiveVariableInstances(JSONArray liveVariableInstancesJson) {
        List<LiveVariableUsageInstance> liveVariableUsageInstances =
                new ArrayList<LiveVariableUsageInstance>(liveVariableInstancesJson.size());

        for (Object obj : liveVariableInstancesJson) {
            JSONObject liveVariableInstanceObject = (JSONObject)obj;
            String id = (String)liveVariableInstanceObject.get("id");
            String value = (String)liveVariableInstanceObject.get("value");

            liveVariableUsageInstances.add(new LiveVariableUsageInstance(id, value));
        }

        return liveVariableUsageInstances;
    }
}

