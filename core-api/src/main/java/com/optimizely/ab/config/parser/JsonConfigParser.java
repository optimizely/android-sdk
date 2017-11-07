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

import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Experiment.ExperimentStatus;
import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.config.Group;
import com.optimizely.ab.config.LiveVariable;
import com.optimizely.ab.config.LiveVariable.VariableStatus;
import com.optimizely.ab.config.LiveVariable.VariableType;
import com.optimizely.ab.config.LiveVariableUsageInstance;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Rollout;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.NotCondition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.config.audience.UserAttribute;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            int datafileVersion = Integer.parseInt(version);

            List<Experiment> experiments = parseExperiments(rootObject.getJSONArray("experiments"));

            List<Attribute> attributes;
            attributes = parseAttributes(rootObject.getJSONArray("attributes"));

            List<EventType> events = parseEvents(rootObject.getJSONArray("events"));
            List<Audience> audiences = parseAudiences(rootObject.getJSONArray("audiences"));
            List<Group> groups = parseGroups(rootObject.getJSONArray("groups"));

            boolean anonymizeIP = false;
            List<LiveVariable> liveVariables = null;
            if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V3.toString())) {
                liveVariables = parseLiveVariables(rootObject.getJSONArray("variables"));

                anonymizeIP = rootObject.getBoolean("anonymizeIP");
            }

            List<FeatureFlag> featureFlags = null;
            List<Rollout> rollouts = null;
            if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString())) {
                featureFlags = parseFeatureFlags(rootObject.getJSONArray("featureFlags"));
                rollouts = parseRollouts(rootObject.getJSONArray("rollouts"));
            }

            return new ProjectConfig(
                    accountId,
                    anonymizeIP,
                    projectId,
                    revision,
                    version,
                    attributes,
                    audiences,
                    events,
                    experiments,
                    featureFlags,
                    groups,
                    liveVariables,
                    rollouts
            );
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
            String status = experimentObject.isNull("status") ?
                    ExperimentStatus.NOT_STARTED.toString() : experimentObject.getString("status");
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

    private List<String> parseExperimentIds(JSONArray experimentIdsJson) {
        ArrayList<String> experimentIds = new ArrayList<String>(experimentIdsJson.length());

        for (Object experimentIdObj : experimentIdsJson) {
            experimentIds.add((String) experimentIdObj);
        }

        return experimentIds;
    }

    private List<FeatureFlag> parseFeatureFlags(JSONArray featureFlagJson) {
        List<FeatureFlag> featureFlags = new ArrayList<FeatureFlag>(featureFlagJson.length());

        for (Object obj : featureFlagJson) {
            JSONObject featureFlagObject = (JSONObject) obj;
            String id = featureFlagObject.getString("id");
            String key = featureFlagObject.getString("key");
            String layerId = featureFlagObject.getString("rolloutId");

            List<String> experimentIds = parseExperimentIds(featureFlagObject.getJSONArray("experimentIds"));

            List<LiveVariable> variables = parseLiveVariables(featureFlagObject.getJSONArray("variables"));

            featureFlags.add(new FeatureFlag(
                    id,
                    key,
                    layerId,
                    experimentIds,
                    variables
            ));
        }

        return featureFlags;
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
            List<String> experimentIds = parseExperimentIds(eventObject.getJSONArray("experimentIds"));

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
                String value = null;
                if (conditionMap.has("value")) {
                    value = conditionMap.getString("value");
                }
                conditions.add(new UserAttribute(
                        (String)conditionMap.get("name"),
                        (String)conditionMap.get("type"),
                               value
                ));
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
            VariableStatus status = null;
            if (liveVariableObject.has("status")) {
                status = VariableStatus.fromString(liveVariableObject.getString("status"));
            }

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

    private List<Rollout> parseRollouts(JSONArray rolloutsJson) {
        List<Rollout> rollouts = new ArrayList<Rollout>(rolloutsJson.length());

        for (Object obj : rolloutsJson) {
            JSONObject rolloutObject = (JSONObject) obj;
            String id = rolloutObject.getString("id");
            List<Experiment> experiments = parseExperiments(rolloutObject.getJSONArray("experiments"));

            rollouts.add(new Rollout(id, experiments));
        }

        return rollouts;
    }
}
