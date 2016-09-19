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

public class LayerState {

    private String layerId;
    private Decision decision;
    private boolean actionTriggered;

    public LayerState() { }

    public LayerState(String layerId, Decision decision, boolean actionTriggered) {
        this.layerId = layerId;
        this.decision = decision;
        this.actionTriggered = actionTriggered;
    }

    public String getLayerId() {
        return layerId;
    }

    public void setLayerId(String layerId) {
        this.layerId = layerId;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public boolean getActionTriggered() {
        return actionTriggered;
    }

    public void setActionTriggered(boolean actionTriggered) {
        this.actionTriggered = actionTriggered;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof LayerState))
            return false;

        LayerState otherLayerState = (LayerState)other;

        return layerId.equals(otherLayerState.getLayerId()) &&
               decision.equals(otherLayerState.getDecision()) &&
               actionTriggered == otherLayerState.getActionTriggered();
    }

    @Override
    public int hashCode() {
        int result = layerId != null ? layerId.hashCode() : 0;
        result = 31 * result + (decision != null ? decision.hashCode() : 0);
        result = 31 * result + (actionTriggered ? 1 : 0);
        return result;
    }
}
