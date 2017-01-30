/**
 *
 *    Copyright 2016, Optimizely and contributors
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

import java.util.List;

public class Conversion extends Event {

    private String visitorId;
    private long timestamp;
    private String projectId;
    private String accountId;
    private List<Feature> userFeatures;
    private List<LayerState> layerStates;
    private String eventEntityId;
    private String eventName;
    private List<EventMetric> eventMetrics;
    private List<Feature> eventFeatures;
    private boolean isGlobalHoldback;
    private boolean anonymizeIP;
    private String sessionId;
    private String revision;

    public Conversion() { }

    public Conversion(String visitorId, long timestamp, String projectId, String accountId, List<Feature> userFeatures,
                      List<LayerState> layerStates, String eventEntityId, String eventName,
                      List<EventMetric> eventMetrics, List<Feature> eventFeatures, boolean isGlobalHoldback,
                      String revision, boolean anonymizeIP) {
        this(visitorId, timestamp, projectId, accountId, userFeatures, layerStates, eventEntityId, eventName,
             eventMetrics, eventFeatures, isGlobalHoldback, anonymizeIP, revision, null);
    }

    public Conversion(String visitorId, long timestamp, String projectId, String accountId, List<Feature> userFeatures,
                      List<LayerState> layerStates, String eventEntityId, String eventName,
                      List<EventMetric> eventMetrics, List<Feature> eventFeatures, boolean isGlobalHoldback,
                      boolean anonymizeIP, String revision, String sessionId) {
        this.visitorId = visitorId;
        this.timestamp = timestamp;
        this.projectId = projectId;
        this.accountId = accountId;
        this.userFeatures = userFeatures;
        this.layerStates = layerStates;
        this.eventEntityId = eventEntityId;
        this.eventName = eventName;
        this.eventMetrics = eventMetrics;
        this.eventFeatures = eventFeatures;
        this.isGlobalHoldback = isGlobalHoldback;
        this.anonymizeIP = anonymizeIP;
        this.revision = revision;
        this.sessionId = sessionId;
    }

    public String getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public List<Feature> getUserFeatures() {
        return userFeatures;
    }

    public void setUserFeatures(List<Feature> userFeatures) {
        this.userFeatures = userFeatures;
    }

    public List<LayerState> getLayerStates() {
        return layerStates;
    }

    public void setLayerStates(List<LayerState> layerStates) {
        this.layerStates = layerStates;
    }

    public String getEventEntityId() {
        return eventEntityId;
    }

    public void setEventEntityId(String eventEntityId) {
        this.eventEntityId = eventEntityId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public List<EventMetric> getEventMetrics() {
        return eventMetrics;
    }

    public void setEventMetrics(List<EventMetric> eventMetrics) {
        this.eventMetrics = eventMetrics;
    }

    public List<Feature> getEventFeatures() {
        return eventFeatures;
    }

    public void setEventFeatures(List<Feature> eventFeatures) {
        this.eventFeatures = eventFeatures;
    }

    public boolean getIsGlobalHoldback() {
        return isGlobalHoldback;
    }

    public void setIsGlobalHoldback(boolean globalHoldback) {
        this.isGlobalHoldback = globalHoldback;
    }

    public boolean getAnonymizeIP() { return anonymizeIP; }

    public void setAnonymizeIP(boolean anonymizeIP) { this.anonymizeIP = anonymizeIP; }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Conversion that = (Conversion) o;

        if (timestamp != that.timestamp) return false;
        if (isGlobalHoldback != that.isGlobalHoldback) return false;
        if (anonymizeIP != that.anonymizeIP) return false;
        if (!visitorId.equals(that.visitorId)) return false;
        if (!projectId.equals(that.projectId)) return false;
        if (!accountId.equals(that.accountId)) return false;
        if (!userFeatures.equals(that.userFeatures)) return false;
        if (!layerStates.equals(that.layerStates)) return false;
        if (!eventEntityId.equals(that.eventEntityId)) return false;
        if (!eventName.equals(that.eventName)) return false;
        if (!eventMetrics.equals(that.eventMetrics)) return false;
        if (!eventFeatures.equals(that.eventFeatures)) return false;
        if (sessionId != null ? !sessionId.equals(that.sessionId) : that.sessionId != null) return false;
        return revision.equals(that.revision);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + visitorId.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + projectId.hashCode();
        result = 31 * result + accountId.hashCode();
        result = 31 * result + userFeatures.hashCode();
        result = 31 * result + layerStates.hashCode();
        result = 31 * result + eventEntityId.hashCode();
        result = 31 * result + eventName.hashCode();
        result = 31 * result + eventMetrics.hashCode();
        result = 31 * result + eventFeatures.hashCode();
        result = 31 * result + (isGlobalHoldback ? 1 : 0);
        result = 31 * result + (anonymizeIP ? 1 : 0);
        result = 31 * result + (sessionId != null ? sessionId.hashCode() : 0);
        result = 31 * result + revision.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Conversion{" +
                "visitorId='" + visitorId + '\'' +
                ", timestamp=" + timestamp +
                ", projectId='" + projectId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", userFeatures=" + userFeatures +
                ", layerStates=" + layerStates +
                ", eventEntityId='" + eventEntityId + '\'' +
                ", eventName='" + eventName + '\'' +
                ", eventMetrics=" + eventMetrics +
                ", eventFeatures=" + eventFeatures +
                ", isGlobalHoldback=" + isGlobalHoldback +
                ", anonymizeIP=" + anonymizeIP +
                ", sessionId='" + sessionId + '\'' +
                ", revision='" + revision + '\'' +
                '}';
    }
}
