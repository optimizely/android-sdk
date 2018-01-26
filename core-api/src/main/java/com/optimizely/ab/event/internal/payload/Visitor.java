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

import java.util.List;

public class Visitor {
    @JsonProperty("visitor_id")
    String visitorId;
    @JsonProperty("session_id")
    String sessionId;
    List<Attribute> attributes;
    List<Snapshot> snapshots;

    public Visitor() {

    }

    public Visitor(String visitorId, String sessionId, List<Attribute> attributes, List<Snapshot> snapshots) {
        this.visitorId = visitorId;
        this.sessionId = sessionId;
        this.attributes = attributes;
        this.snapshots = snapshots;
    }

    public String getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public List<Snapshot> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<Snapshot> snapshots) {
        this.snapshots = snapshots;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Visitor visitor = (Visitor) o;

        if (!visitorId.equals(visitor.visitorId)) return false;
        if (sessionId != null ? !sessionId.equals(visitor.sessionId) : visitor.sessionId != null) return false;
        if (attributes != null ? !attributes.equals(visitor.attributes) : visitor.attributes != null) return false;
        return snapshots.equals(visitor.snapshots);
    }

    @Override
    public int hashCode() {
        int result = visitorId.hashCode();
        result = 31 * result + (sessionId != null ? sessionId.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 31 * result + snapshots.hashCode();
        return result;
    }
}
