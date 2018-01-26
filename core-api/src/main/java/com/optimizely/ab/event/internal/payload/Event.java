/**
 *
 *    Copyright 2018, Optimizely and contributors
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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Event {
    long timestamp;
    String uuid;
    @JsonProperty("entity_id")
    String entityId;
    String key;
    Number quantity;
    Number revenue;
    Map<String, ?> tags;
    String type;
    Number value;

    public Event() {

    }

    public Event(long timestamp, String uuid, String entityId, String key, Number quantity,
                 Number revenue, Map<String, ?> tags, String type, Number value) {
        this.timestamp = timestamp;
        this.uuid = uuid;
        this.entityId = entityId;
        this.key = key;
        this.quantity = quantity;
        this.revenue = revenue;
        this.tags = tags;
        this.type = type;
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Number getQuantity() {
        return quantity;
    }

    public void setQuantity(Number quantity) {
        this.quantity = quantity;
    }

    public Number getRevenue() {
        return revenue;
    }

    public void setRevenue(Number revenue) {
        this.revenue = revenue;
    }

    public Map<String, ?> getTags() {
        return tags;
    }

    public void setTags(Map<String, ?> tags) {
        this.tags = tags;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Event event = (Event) o;

        if (timestamp != event.timestamp) return false;
        if (!uuid.equals(event.uuid)) return false;
        if (entityId != null ? !entityId.equals(event.entityId) : event.entityId != null) return false;
        if (key != null ? !key.equals(event.key) : event.key != null) return false;
        if (quantity != null ? !quantity.equals(event.quantity) : event.quantity != null) return false;
        if (revenue != null ? !revenue.equals(event.revenue) : event.revenue != null) return false;
        if (tags != null ? !tags.equals(event.tags) : event.tags != null) return false;
        if (!type.equals(event.type)) return false;
        return value != null ? value.equals(event.value) : event.value == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + uuid.hashCode();
        result = 31 * result + (entityId != null ? entityId.hashCode() : 0);
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (quantity != null ? quantity.hashCode() : 0);
        result = 31 * result + (revenue != null ? revenue.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        result = 31 * result + type.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

}
