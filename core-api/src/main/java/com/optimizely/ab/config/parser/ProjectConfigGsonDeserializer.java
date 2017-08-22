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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.config.Group;
import com.optimizely.ab.config.LiveVariable;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Rollout;
import com.optimizely.ab.config.audience.Audience;

import java.lang.reflect.Type;
import java.util.List;

/**
 * GSON {@link ProjectConfig} deserializer to allow the constructor to be used.
 */
public class ProjectConfigGsonDeserializer implements JsonDeserializer<ProjectConfig> {

    @Override
    public ProjectConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        String accountId = jsonObject.get("accountId").getAsString();
        String projectId = jsonObject.get("projectId").getAsString();
        String revision = jsonObject.get("revision").getAsString();
        String version = jsonObject.get("version").getAsString();
        int datafileVersion = Integer.parseInt(version);

        // generic list type tokens
        Type groupsType = new TypeToken<List<Group>>() {}.getType();
        Type experimentsType = new TypeToken<List<Experiment>>() {}.getType();
        Type attributesType = new TypeToken<List<Attribute>>() {}.getType();
        Type eventsType = new TypeToken<List<EventType>>() {}.getType();
        Type audienceType = new TypeToken<List<Audience>>() {}.getType();

        List<Group> groups = context.deserialize(jsonObject.get("groups").getAsJsonArray(), groupsType);
        List<Experiment> experiments =
            context.deserialize(jsonObject.get("experiments").getAsJsonArray(), experimentsType);

        List<Attribute> attributes;
        attributes = context.deserialize(jsonObject.get("attributes"), attributesType);

        List<EventType> events =
            context.deserialize(jsonObject.get("events").getAsJsonArray(), eventsType);
        List<Audience> audiences =
            context.deserialize(jsonObject.get("audiences").getAsJsonArray(), audienceType);

        boolean anonymizeIP = false;
        // live variables should be null if using V2
        List<LiveVariable> liveVariables = null;
        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V3.toString())) {
            Type liveVariablesType = new TypeToken<List<LiveVariable>>() {}.getType();
            liveVariables = context.deserialize(jsonObject.getAsJsonArray("variables"), liveVariablesType);

            anonymizeIP = jsonObject.get("anonymizeIP").getAsBoolean();
        }

        List<FeatureFlag> featureFlags = null;
        List<Rollout> rollouts = null;
        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString())) {
            Type featureFlagsType = new TypeToken<List<FeatureFlag>>() {}.getType();
            featureFlags = context.deserialize(jsonObject.getAsJsonArray("featureFlags"), featureFlagsType);
            Type rolloutsType = new TypeToken<List<Rollout>>() {}.getType();
            rollouts = context.deserialize(jsonObject.get("rollouts").getAsJsonArray(), rolloutsType);
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
    }
}
