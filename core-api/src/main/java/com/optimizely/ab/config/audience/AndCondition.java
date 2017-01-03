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

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Represents an 'And' conditions condition operation.
 */
@Immutable
public class AndCondition implements Condition {

    private final List<Condition> conditions;

    public AndCondition(@Nonnull List<Condition> conditions) {
        this.conditions = conditions;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public boolean evaluate(Map<String, String> attributes) {
        for (Condition condition : conditions) {
            if (!condition.evaluate(attributes))
                return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("[and, ");
        for (int i = 0; i < conditions.size(); i++) {
            s.append(conditions.get(i));
            if (i < conditions.size() - 1)
                s.append(", ");
        }
        s.append("]");

        return s.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AndCondition))
            return false;

        AndCondition otherAndCondition = (AndCondition)other;

        return conditions.equals(otherAndCondition.getConditions());
    }

    @Override
    public int hashCode() {
        return conditions.hashCode();
    }
}

