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
package com.optimizely.ab.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

/**
 * Represents a live variable definition at the project level
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveVariable implements IdKeyMapped {

    public enum VariableStatus {
        @SerializedName("active")
        ACTIVE ("active"),

        @SerializedName("archived")
        ARCHIVED ("archived");

        private final String variableStatus;

        VariableStatus(String variableStatus) {
            this.variableStatus = variableStatus;
        }

        @JsonValue
        public String getVariableStatus() {
            return variableStatus;
        }

        public static VariableStatus fromString(String variableStatusString) {
            if (variableStatusString != null) {
                for (VariableStatus variableStatusEnum : VariableStatus.values()) {
                    if (variableStatusString.equals(variableStatusEnum.getVariableStatus())) {
                        return variableStatusEnum;
                    }
                }
            }

            return null;
        }
    }

    public enum VariableType {
        @SerializedName("boolean")
        BOOLEAN ("boolean"),

        @SerializedName("integer")
        INTEGER ("integer"),

        @SerializedName("string")
        STRING ("string"),

        @SerializedName("double")
        DOUBLE ("double");

        private final String variableType;

        VariableType(String variableType) {
            this.variableType = variableType;
        }

        @JsonValue
        public String getVariableType() {
            return variableType;
        }

        public static VariableType fromString(String variableTypeString) {
            if (variableTypeString != null) {
                for (VariableType variableTypeEnum : VariableType.values()) {
                    if (variableTypeString.equals(variableTypeEnum.getVariableType())) {
                        return variableTypeEnum;
                    }
                }
            }

            return null;
        }
    }

    private final String id;
    private final String key;
    private final String defaultValue;
    private final VariableType type;
    @Nullable private final VariableStatus status;

    @JsonCreator
    public LiveVariable(@JsonProperty("id") String id,
                        @JsonProperty("key") String key,
                        @JsonProperty("defaultValue") String defaultValue,
                        @JsonProperty("status") VariableStatus status,
                        @JsonProperty("type") VariableType type) {
        this.id = id;
        this.key = key;
        this.defaultValue = defaultValue;
        this.status = status;
        this.type = type;
    }

    public @Nullable VariableStatus getStatus() {
        return status;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public VariableType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "LiveVariable{" +
                "id='" + id + '\'' +
                ", key='" + key + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", type=" + type +
                ", status=" + status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LiveVariable variable = (LiveVariable) o;

        if (!id.equals(variable.id)) return false;
        if (!key.equals(variable.key)) return false;
        if (!defaultValue.equals(variable.defaultValue)) return false;
        if (type != variable.type) return false;
        return status == variable.status;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + key.hashCode();
        result = 31 * result + defaultValue.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + status.hashCode();
        return result;
    }
}
