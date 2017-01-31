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

public class LayerState {

    private String layerId;
    private String revision;
    private Decision decision;
    private boolean actionTriggered;

    public LayerState() { }

    public LayerState(String layerId, String revision, Decision decision, boolean actionTriggered) {
        this.layerId = layerId;
        this.revision = revision;
        this.decision = decision;
        this.actionTriggered = actionTriggered;
    }

    public String getLayerId() {
        return layerId;
    }

    public void setLayerId(String layerId) {
        this.layerId = layerId;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LayerState that = (LayerState) o;

        if (actionTriggered != that.actionTriggered) return false;
        if (!layerId.equals(that.layerId)) return false;
        if (!revision.equals(that.revision)) return false;
        return decision.equals(that.decision);

    }

    @Override
    public int hashCode() {
        int result = layerId.hashCode();
        result = 31 * result + revision.hashCode();
        result = 31 * result + decision.hashCode();
        result = 31 * result + (actionTriggered ? 1 : 0);
        return result;
    }
}
