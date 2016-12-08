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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * {@code org.json}-based config parser implementation.
 */
final class JsonConfigParser implements ConfigParser {

    @Override
    public ProjectConfig parseProjectConfig(@Nonnull String json) throws ConfigParseException {
        try {
            JSONObject rootObject = new JSONObject(json);

            String accountId = rootObject.getString("accountId");
            String projectId = rootObject.getString("projectId");
            String revision = rootObject.getString("revision");
            String version = rootObject.getString("version");

            List<Experiment> experiments = parseExperiments(rootObject.getJSONArray("experiments"));

            List<Attribute> attributes;
            if (version.equals(ProjectConfig.Version.V1.toString())) {
                attributes = parseAttributes(rootObject.getJSONArray("dimensions"));
            } else {
                attributes = parseAttributes(rootObject.getJSONArray("attributes"));
            }

            List<EventType> events = parseEvents(rootObject.getJSONArray("events"));
            List<Audience> audiences = parseAudiences(rootObject.getJSONArray("audiences"));
            List<Group> groups = parseGroups(rootObject.getJSONArray("groups"));

            List<LiveVariable> liveVariables = null;
            if (version.equals(ProjectConfig.Version.V3.toString())) {
                liveVariables = parseLiveVariables(rootObject.getJSONArray("variables"));
            }

            return new ProjectConfig(accountId, projectId, version, revision, groups, experiments, attributes, events,
                                     audiences, liveVariables);
        } catch (Exception e) {
            throw new ConfigParseException("Unable to parse datafile: " + json, e);
        }
    }

    //======== Helper methods ========//

    private List<Experiment> parseExperiments(JSONArray experimentJson) {
        return parseExperiments(experimentJson, "");
    }

    private List<Experiment> parseExperiments(JSONArray experimentJson, String groupId) {
        List<Experiment> experiments = new ArrayList<Experiment>(experimentJson.length());

        for (Object obj : experimentJson) {
            JSONObject experimentObject = (JSONObject)obj;
            String id = experimentObject.getString("id");
            String key = experimentObject.getString("key");
            String status = experimentObject.getString("status");
            String layerId = experimentObject.has("layerId") ? experimentObject.getString("layerId") : null;

            JSONArray audienceIdsJson = experimentObject.getJSONArray("audienceIds");
            List<String> audienceIds = new ArrayList<String>(audienceIdsJson.length());

            for (Object audienceIdObj : audienceIdsJson) {
                audienceIds.add((String)audienceIdObj);
            }

            // parse the child objects
            List<Variation> variations = parseVariations(experimentObject.getJSONArray("variations"));
            Map<String, String> userIdToVariationKeyMap =
                parseForcedVariations(experimentObject.getJSONObject("forcedVariations"));
            List<TrafficAllocation> trafficAllocations =
                parseTrafficAllocation(experimentObject.getJSONArray("trafficAllocation"));

            experiments.add(new Experiment(id, key, status, layerId, audienceIds, variations, userIdToVariationKeyMap,
                                           trafficAllocations, groupId));
        }

        return experiments;
    }

    private List<Variation> parseVariations(JSONArray variationJson) {
        List<Variation> variations = new ArrayList<Variation>(variationJson.length());

        for (Object obj : variationJson) {
            JSONObject variationObject = (JSONObject)obj;
            String id = variationObject.getString("id");
            String key = variationObject.getString("key");

            List<LiveVariableUsageInstance> liveVariableUsageInstances = null;
            if (variationObject.has("variables")) {
                liveVariableUsageInstances =
                        parseLiveVariableInstances(variationObject.getJSONArray("variables"));
            }

            variations.add(new Variation(id, key, liveVariableUsageInstances));
        }

        return variations;
    }

    private Map<String, String> parseForcedVariations(JSONObject forcedVariationJson) {
        Map<String, String> userIdToVariationKeyMap = new HashMap<String, String>();
        Set<String> userIdSet = forcedVariationJson.keySet();

        for (String userId : userIdSet) {
            userIdToVariationKeyMap.put(userId, forcedVariationJson.get(userId).toString());
        }

        return userIdToVariationKeyMap;
    }

    private List<TrafficAllocation> parseTrafficAllocation(JSONArray trafficAllocationJson) {
        List<TrafficAllocation> trafficAllocation = new ArrayList<TrafficAllocation>(trafficAllocationJson.length());

        for (Object obj : trafficAllocationJson) {
            JSONObject allocationObject = (JSONObject)obj;
            String entityId = allocationObject.getString("entityId");
            int endOfRange = allocationObject.getInt("endOfRange");

            trafficAllocation.add(new TrafficAllocation(entityId, endOfRange));
        }

        return trafficAllocation;
    }

    private List<Attribute> parseAttributes(JSONArray attributeJson) {
        List<Attribute> attributes = new ArrayList<Attribute>(attributeJson.length());

        for (Object obj : attributeJson) {
            JSONObject attributeObject = (JSONObject)obj;
            String id = attributeObject.getString("id");
            String key = attributeObject.getString("key");

            attributes.add(new Attribute(id, key, attributeObject.optString("segmentId", null)));
        }

        return attributes;
    }

    private List<EventType> parseEvents(JSONArray eventJson) {
        List<EventType> events = new ArrayList<EventType>(eventJson.length());

        for (Object obj : eventJson) {
            JSONObject eventObject = (JSONObject)obj;
            JSONArray experimentIdsJson = eventObject.getJSONArray("experimentIds");
            List<String> experimentIds = new ArrayList<String>(experimentIdsJson.length());

            for (Object experimentIdObj : experimentIdsJson) {
                experimentIds.add((String)experimentIdObj);
            }

            String id = eventObject.getString("id");
            String key = eventObject.getString("key");

            events.add(new EventType(id, key, experimentIds));
        }

        return events;
    }

    private List<Audience> parseAudiences(JSONArray audienceJson) {
        List<Audience> audiences = new ArrayList<Audience>(audienceJson.length());

        for (Object obj : audienceJson) {
            JSONObject audienceObject = (JSONObject)obj;
            String id = audienceObject.getString("id");
            String key = audienceObject.getString("name");
            String conditionString = audienceObject.getString("conditions");

            JSONArray conditionJson = new JSONArray(conditionString);
            Condition conditions = parseConditions(conditionJson);
            audiences.add(new Audience(id, key, conditions));
        }

        return audiences;
    }

    private Condition parseConditions(JSONArray conditionJson) {
        List<Condition> conditions = new ArrayList<Condition>();
        String operand = (String)conditionJson.get(0);

        for (int i = 1; i < conditionJson.length(); i++) {
            Object obj = conditionJson.get(i);
            if (obj instanceof JSONArray) {
                conditions.add(parseConditions(conditionJson.getJSONArray(i)));
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
        List<Group> groups = new ArrayList<Group>(groupJson.length());

        for (Object obj : groupJson) {
            JSONObject groupObject = (JSONObject)obj;
            String id = groupObject.getString("id");
            String policy = groupObject.getString("policy");
            List<Experiment> experiments = parseExperiments(groupObject.getJSONArray("experiments"), id);
            List<TrafficAllocation> trafficAllocations =
                parseTrafficAllocation(groupObject.getJSONArray("trafficAllocation"));

            groups.add(new Group(id, policy, experiments, trafficAllocations));
        }

        return groups;
    }

    private List<LiveVariable> parseLiveVariables(JSONArray liveVariablesJson) {
        List<LiveVariable> liveVariables = new ArrayList<LiveVariable>(liveVariablesJson.length());

        for (Object obj : liveVariablesJson) {
            JSONObject liveVariableObject = (JSONObject)obj;
            String id = liveVariableObject.getString("id");
            String key = liveVariableObject.getString("key");
            String defaultValue = liveVariableObject.getString("defaultValue");
            VariableType type = VariableType.fromString(liveVariableObject.getString("type"));
            VariableStatus status = VariableStatus.fromString(liveVariableObject.getString("status"));

            liveVariables.add(new LiveVariable(id, key, defaultValue, status, type));
        }

        return liveVariables;
    }

    private List<LiveVariableUsageInstance> parseLiveVariableInstances(JSONArray liveVariableInstancesJson) {
        List<LiveVariableUsageInstance> liveVariableUsageInstances = new ArrayList<LiveVariableUsageInstance>(liveVariableInstancesJson.length());

        for (Object obj : liveVariableInstancesJson) {
            JSONObject liveVariableInstanceObject = (JSONObject)obj;
            String id = liveVariableInstanceObject.getString("id");
            String value = liveVariableInstanceObject.getString("value");

            liveVariableUsageInstances.add(new LiveVariableUsageInstance(id, value));
        }

        return liveVariableUsageInstances;
    }
}
