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
package com.optimizely.ab.config.audience;

import javax.annotation.concurrent.Immutable;
import javax.annotation.Nonnull;

import java.util.Map;

/**
 * Represents a 'Not' conditions condition operation.
 */
@Immutable
public class NotCondition implements Condition {

    private final Condition condition;

    public NotCondition(@Nonnull Condition condition) {
        this.condition = condition;
    }

    public Condition getCondition() {
        return condition;
    }

    public boolean evaluate(Map<String, String> attributes) {
        return !condition.evaluate(attributes);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();

        s.append("[not, ");
        s.append(condition);
        s.append("]");

        return s.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof NotCondition))
            return false;

        NotCondition otherNotCondition = (NotCondition)other;

        return condition.equals(otherNotCondition.getCondition());
    }

    @Override
    public int hashCode() {
        return condition.hashCode();
    }
}
