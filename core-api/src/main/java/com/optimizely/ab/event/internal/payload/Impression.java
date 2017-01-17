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

public class Impression extends Event {

    private String visitorId;
    private long timestamp;
    private boolean isGlobalHoldback;
    private String projectId;
    private Decision decision;
    private String layerId;
    private String accountId;
    private List<Feature> userFeatures;
    private boolean anonymizeIP;

    public Impression() { }

    public Impression(String visitorId, long timestamp, boolean isGlobalHoldback, String projectId, Decision decision,
                      String layerId, String accountId, List<Feature> userFeatures, boolean anonymizeIP) {
        this.visitorId = visitorId;
        this.timestamp = timestamp;
        this.isGlobalHoldback = isGlobalHoldback;
        this.projectId = projectId;
        this.decision = decision;
        this.layerId = layerId;
        this.accountId = accountId;
        this.userFeatures = userFeatures;
        this.anonymizeIP = anonymizeIP;
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

    public boolean getIsGlobalHoldback() {
        return isGlobalHoldback;
    }

    public void setIsGlobalHoldback(boolean globalHoldback) {
        this.isGlobalHoldback = globalHoldback;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public String getLayerId() {
        return layerId;
    }

    public void setLayerId(String layerId) {
        this.layerId = layerId;
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

    public boolean getAnonymizeIP() {
        return anonymizeIP;
    }

    public void setAnonymizeIP(boolean anonymizeIP) {
        this.anonymizeIP = anonymizeIP;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Impression))
            return false;

        if (!super.equals(other))
            return false;

        Impression otherImpression = (Impression)other;

        return timestamp == otherImpression.timestamp &&
               isGlobalHoldback == otherImpression.isGlobalHoldback &&
               visitorId.equals(otherImpression.visitorId) &&
               projectId.equals(otherImpression.projectId) &&
               decision.equals(otherImpression.decision) &&
               layerId.equals(otherImpression.layerId) &&
               accountId.equals(otherImpression.accountId) &&
               userFeatures.equals(otherImpression.userFeatures);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + visitorId.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (isGlobalHoldback ? 1 : 0);
        result = 31 * result + projectId.hashCode();
        result = 31 * result + decision.hashCode();
        result = 31 * result + layerId.hashCode();
        result = 31 * result + accountId.hashCode();
        result = 31 * result + userFeatures.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Impression{" +
                "visitorId='" + visitorId + '\'' +
                ", timestamp=" + timestamp +
                ", isGlobalHoldback=" + isGlobalHoldback +
                ", anonymizeIP=" + anonymizeIP +
                ", projectId='" + projectId + '\'' +
                ", decision=" + decision +
                ", layerId='" + layerId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", userFeatures=" + userFeatures +
                ", clientEngine='" + clientEngine +
                ", clientVersion='" + clientVersion + '}';
    }
}
