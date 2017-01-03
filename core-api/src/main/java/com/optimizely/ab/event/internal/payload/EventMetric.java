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
package com.optimizely.ab.event.internal.payload;

public class EventMetric {

    public static final String REVENUE_METRIC_TYPE = "revenue";

    private String name;
    private long value;

    public EventMetric() { }

    public EventMetric(String name, long value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }


    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EventMetric))
            return false;

        EventMetric otherEventMetric = (EventMetric)other;

        return name.equals(otherEventMetric.getName()) && value == otherEventMetric.getValue();
    }


    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (int) (value ^ (value >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "EventMetric{" +
                "name='" + name + '\'' +
                ", value=" + value +
                '}';
    }
}
