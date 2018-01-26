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

public class Snapshot {
    List<Decision> decisions;
    List<Event> events;
    @JsonProperty("activation_timestamp")
    Long activationTimestamp;

    public Snapshot() {

    }

    public Snapshot(List<Decision> decisions, List<Event> events) {
        this.decisions = decisions;
        this.events = events;
        this.activationTimestamp = null;
    }

    public Long getActivationTimestamp() {
        return activationTimestamp;
    }

    public void setActivationTimestamp(Long activationTimestamp) {
        this.activationTimestamp = activationTimestamp;
    }

    public List<Decision> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<Decision> decisions) {
        this.decisions = decisions;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Snapshot snapshot = (Snapshot) o;

        if (activationTimestamp != null ?
                !activationTimestamp.equals(snapshot.activationTimestamp) :
                snapshot.activationTimestamp != null) return false;
        if (!decisions.equals(snapshot.decisions)) return false;
        return events.equals(snapshot.events);
    }

    @Override
    public int hashCode() {
        int result = decisions.hashCode();
        result = 31 * result + events.hashCode();
        if (activationTimestamp != null) {
            result = 31 * result + (int) (activationTimestamp ^ (activationTimestamp >>> 32));
        }
        return result;
    }
}
