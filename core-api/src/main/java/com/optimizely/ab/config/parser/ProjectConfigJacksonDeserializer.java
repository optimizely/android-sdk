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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.config.Group;
import com.optimizely.ab.config.LiveVariable;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.audience.Audience;

import java.io.IOException;
import java.util.List;

class ProjectConfigJacksonDeserializer extends JsonDeserializer<ProjectConfig> {

    @Override
    public ProjectConfig deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Audience.class, new AudienceJacksonDeserializer());
        module.addDeserializer(Group.class, new GroupJacksonDeserializer());
        mapper.registerModule(module);

        JsonNode node = parser.getCodec().readTree(parser);

        String accountId = node.get("accountId").textValue();
        String projectId = node.get("projectId").textValue();
        String revision = node.get("revision").textValue();
        String version = node.get("version").textValue();
        int datafileVersion = Integer.parseInt(version);

        List<Group> groups = mapper.readValue(node.get("groups").toString(), new TypeReference<List<Group>>() {});
        List<Experiment> experiments = mapper.readValue(node.get("experiments").toString(),
                                                        new TypeReference<List<Experiment>>() {});

        List<Attribute> attributes;
        attributes = mapper.readValue(node.get("attributes").toString(), new TypeReference<List<Attribute>>() {});

        List<EventType> events = mapper.readValue(node.get("events").toString(),
                                                  new TypeReference<List<EventType>>() {});
        List<Audience> audiences = mapper.readValue(node.get("audiences").toString(),
                                                    new TypeReference<List<Audience>>() {});

        boolean anonymizeIP = false;
        List<LiveVariable> liveVariables = null;
        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V3.toString())) {
            liveVariables = mapper.readValue(node.get("variables").toString(),
                                             new TypeReference<List<LiveVariable>>() {});
            anonymizeIP = node.get("anonymizeIP").asBoolean();
        }

        List<FeatureFlag> featureFlags = null;
        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString())) {
            featureFlags = mapper.readValue(node.get("featureFlags").toString(),
                   new TypeReference<List<FeatureFlag>>() {});
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
                liveVariables
        );
    }
}