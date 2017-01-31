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
    private String sessionId;
    private String revision;

    public Impression() { }

    public Impression(String visitorId, long timestamp, boolean isGlobalHoldback, String projectId, Decision decision,
                      String layerId, String accountId, List<Feature> userFeatures, boolean anonymizeIP,
                      String revision) {
        this(visitorId, timestamp, isGlobalHoldback, projectId, decision, layerId, accountId, userFeatures,
             anonymizeIP, revision, null);
    }

    public Impression(String visitorId, long timestamp, boolean isGlobalHoldback, String projectId, Decision decision,
                      String layerId, String accountId, List<Feature> userFeatures, boolean anonymizeIP,
                      String revision, String sessionId) {
        this.visitorId = visitorId;
        this.timestamp = timestamp;
        this.isGlobalHoldback = isGlobalHoldback;
        this.projectId = projectId;
        this.decision = decision;
        this.layerId = layerId;
        this.accountId = accountId;
        this.userFeatures = userFeatures;
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

        Impression that = (Impression) o;

        if (timestamp != that.timestamp) return false;
        if (isGlobalHoldback != that.isGlobalHoldback) return false;
        if (anonymizeIP != that.anonymizeIP) return false;
        if (!visitorId.equals(that.visitorId)) return false;
        if (!projectId.equals(that.projectId)) return false;
        if (!decision.equals(that.decision)) return false;
        if (!layerId.equals(that.layerId)) return false;
        if (!accountId.equals(that.accountId)) return false;
        if (!userFeatures.equals(that.userFeatures)) return false;
        if (sessionId != null ? !sessionId.equals(that.sessionId) : that.sessionId != null) return false;
        return revision.equals(that.revision);

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
        result = 31 * result + (anonymizeIP ? 1 : 0);
        result = 31 * result + (sessionId != null ? sessionId.hashCode() : 0);
        result = 31 * result + revision.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Impression{" +
                "visitorId='" + visitorId + '\'' +
                ", timestamp=" + timestamp +
                ", isGlobalHoldback=" + isGlobalHoldback +
                ", projectId='" + projectId + '\'' +
                ", decision=" + decision +
                ", layerId='" + layerId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", userFeatures=" + userFeatures +
                ", anonymizeIP=" + anonymizeIP +
                ", sessionId='" + sessionId + '\'' +
                ", revision='" + revision + '\'' +
                '}';
    }
}
