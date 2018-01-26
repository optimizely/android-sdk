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

public class Decision {
    @JsonProperty("campaign_id")
    String campaignId;
    @JsonProperty("experiment_id")
    String experimentId;
    @JsonProperty("variation_id")
    String variationId;
    @JsonProperty("is_campaign_holdback")
    boolean isCampaignHoldback;

    public Decision(String campaignId, String experimentId, String variationId, boolean isCampaignHoldback) {
        this.campaignId = campaignId;
        this.experimentId = experimentId;
        this.variationId = variationId;
        this.isCampaignHoldback = isCampaignHoldback;
    }

    public Decision() {

    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public String getVariationId() {
        return variationId;
    }

    public void setVariationId(String variationId) {
        this.variationId = variationId;
    }

    public boolean getIsCampaignHoldback() {
        return isCampaignHoldback;
    }

    public void setIsCampaignHoldback(boolean campaignHoldback) {
        isCampaignHoldback = campaignHoldback;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Decision that = (Decision) o;

        if (isCampaignHoldback != that.isCampaignHoldback) return false;
        if (!campaignId.equals(that.campaignId)) return false;
        if (!experimentId.equals(that.experimentId)) return false;
        return variationId.equals(that.variationId);
    }

    @Override
    public int hashCode() {
        int result = campaignId.hashCode();
        result = 31 * result + experimentId.hashCode();
        result = 31 * result + variationId.hashCode();
        result = 31 * result + (isCampaignHoldback ? 1 : 0);
        return result;
    }
}
