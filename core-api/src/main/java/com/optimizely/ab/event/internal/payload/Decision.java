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

public class Decision {

    private String variationId;
    private boolean isLayerHoldback;
    private String experimentId;

    public Decision() {}

    public Decision(String variationId, boolean isLayerHoldback, String experimentId) {
        this.variationId = variationId;
        this.isLayerHoldback = isLayerHoldback;
        this.experimentId = experimentId;
    }

    public String getVariationId() {
        return variationId;
    }

    public void setVariationId(String variationId) {
        this.variationId = variationId;
    }

    public boolean getIsLayerHoldback() {
        return isLayerHoldback;
    }

    public void setIsLayerHoldback(boolean layerHoldback) {
        this.isLayerHoldback = layerHoldback;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Decision))
            return false;

        Decision otherDecision = (Decision)other;

        return variationId.equals(otherDecision.getVariationId()) &&
               isLayerHoldback == otherDecision.getIsLayerHoldback() &&
               experimentId.equals(otherDecision.getExperimentId());
    }

    @Override
    public int hashCode() {
        int result = variationId.hashCode();
        result = 31 * result + (isLayerHoldback ? 1 : 0);
        result = 31 * result + experimentId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Decision{" +
                "variationId='" + variationId + '\'' +
                ", isLayerHoldback=" + isLayerHoldback +
                ", experimentId='" + experimentId + '\'' +
                '}';
    }
}
