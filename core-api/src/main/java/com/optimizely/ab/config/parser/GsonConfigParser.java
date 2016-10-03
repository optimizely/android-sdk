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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Group;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.ProjectConfig;

import javax.annotation.Nonnull;

/**
 * {@link Gson}-based config parser implementation.
 */
final class GsonConfigParser implements ConfigParser {

    @Override
    public ProjectConfig parseProjectConfig(@Nonnull String json) throws ConfigParseException {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(ProjectConfig.class, new ProjectConfigGsonDeserializer())
            .registerTypeAdapter(Audience.class, new AudienceGsonDeserializer())
            .registerTypeAdapter(Group.class, new GroupGsonDeserializer())
            .registerTypeAdapter(Experiment.class, new ExperimentGsonDeserializer())
            .create();

        try {
            return gson.fromJson(json, ProjectConfig.class);
        } catch (JsonParseException e) {
            throw new ConfigParseException("Unable to parse datafile: " + json, e);
        }
    }
}
