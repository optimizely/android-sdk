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

    public Conversion() { }

    public Conversion(String visitorId, long timestamp, String projectId, String accountId, List<Feature> userFeatures,
                      List<LayerState> layerStates, String eventEntityId, String eventName,
                      List<EventMetric> eventMetrics, List<Feature> eventFeatures, boolean isGlobalHoldback) {
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

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Conversion))
            return false;

        if (!super.equals(other))
            return false;

        Conversion otherConversion = (Conversion)other;

        return timestamp == otherConversion.timestamp &&
               isGlobalHoldback == otherConversion.isGlobalHoldback &&
               visitorId.equals(otherConversion.visitorId) &&
               projectId.equals(otherConversion.projectId) &&
               accountId.equals(otherConversion.accountId) &&
               userFeatures.equals(otherConversion.userFeatures) &&
               layerStates.equals(otherConversion.layerStates) &&
               eventEntityId.equals(otherConversion.eventEntityId) &&
               eventName.equals(otherConversion.eventName) &&
               eventMetrics.equals(otherConversion.eventMetrics) &&
               eventFeatures.equals(otherConversion.eventFeatures);
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
                ", clientEngine='" + clientEngine +
                ", clientVersion='" + clientVersion + '}';
    }
}
