/*
 *    Copyright 2017, Optimizely
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
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;

import com.google.gson.internal.LinkedTreeMap;

import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.NotCondition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.config.audience.UserAttribute;

import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.List;

public class AudienceGsonDeserializer implements JsonDeserializer<Audience> {

    @Override
    public Audience deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = json.getAsJsonObject();

        String id = jsonObject.get("id").getAsString();
        String name = jsonObject.get("name").getAsString();

        JsonElement conditionsElement = parser.parse(jsonObject.get("conditions").getAsString());
        List<Object> rawObjectList = gson.fromJson(conditionsElement, List.class);
        Condition conditions = parseConditions(rawObjectList);

        return new Audience(id, name, conditions);
    }

    private Condition parseConditions(List<Object> rawObjectList) {
        List<Condition> conditions = new ArrayList<Condition>();
        String operand = (String)rawObjectList.get(0);

        for (int i = 1; i < rawObjectList.size(); i++) {
            Object obj = rawObjectList.get(i);
            if (obj instanceof List) {
                List<Object> objectList = (List<Object>)rawObjectList.get(i);
                conditions.add(parseConditions(objectList));
            } else {
                LinkedTreeMap<String, String> conditionMap = (LinkedTreeMap<String, String>)rawObjectList.get(i);
                conditions.add(new UserAttribute(conditionMap.get("name"), conditionMap.get("type"),
                               conditionMap.get("value")));
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
}
