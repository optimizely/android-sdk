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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.UserAttribute;
import com.optimizely.ab.config.audience.NotCondition;
import com.optimizely.ab.config.audience.OrCondition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AudienceJacksonDeserializer extends JsonDeserializer<Audience> {

    @Override
    public Audience deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = parser.getCodec().readTree(parser);

        String id = node.get("id").textValue();
        String name = node.get("name").textValue();
        List<Object> rawObjectList = (List<Object>)mapper.readValue(node.get("conditions").textValue(), List.class);
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
                HashMap<String, String> conditionMap = (HashMap<String, String>)rawObjectList.get(i);
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

