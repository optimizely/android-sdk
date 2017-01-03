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
package com.optimizely.ab.config.audience;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Map;

/**
 * Represents a user attribute instance within an audience's conditions.
 */
@Immutable
public class UserAttribute implements Condition {

    private final String name;
    private final String type;
    private final String value;

    public UserAttribute(@Nonnull String name, @Nonnull String type, @Nonnull String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public boolean evaluate(Map<String, String> attributes) {
        String userAttributeValue = attributes.get(name);

        return value.equals(userAttributeValue);
    }

    @Override
    public String toString() {
        return "{name='" + name + "\'" +
               ", type='" + type + "\'" +
               ", value='" + value + "\'" +
               "}";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof UserAttribute))
            return false;

        UserAttribute otherConditionObj = (UserAttribute)other;

        return name.equals(otherConditionObj.getName()) && type.equals(otherConditionObj.getType())
            && value.equals(otherConditionObj.getValue());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        result = prime * result + type.hashCode();
        result = prime * result + value.hashCode();
        return result;
    }
}
