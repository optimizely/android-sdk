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
package com.optimizely.ab.event.internal.payload;

public class Feature {

    public static final String CUSTOM_ATTRIBUTE_FEATURE_TYPE = "custom";

    private String id;
    private String name;
    private String type;
    private String value;
    private boolean shouldIndex;

    public Feature() { }

    public Feature(String id, String name, String type, String value, boolean shouldIndex) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.value = value;
        this.shouldIndex = shouldIndex;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean getShouldIndex() {
        return shouldIndex;
    }

    public void setShouldIndex(boolean shouldIndex) {
        this.shouldIndex = shouldIndex;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Feature))
            return false;

        Feature otherFeature = (Feature)other;

        return id.equals(otherFeature.getId()) &&
               name.equals(otherFeature.getName()) &&
               type.equals(otherFeature.getType()) &&
               value.equals(otherFeature.getValue()) &&
               shouldIndex == otherFeature.getShouldIndex();
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + (shouldIndex ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Feature{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", value='" + value + '\'' +
                ", shouldIndex=" + shouldIndex +
                '}';
    }
}
