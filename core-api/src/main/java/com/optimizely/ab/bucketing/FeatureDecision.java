/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/
package com.optimizely.ab.bucketing;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;

import javax.annotation.Nullable;

public class FeatureDecision {
    /** The {@link Experiment} the Feature is associated with. */
    @Nullable public Experiment experiment;

    /** The {@link Variation} the user was bucketed into. */
    @Nullable public Variation variation;

    /** The source of the {@link Variation}. */
    @Nullable public DecisionSource decisionSource;

    public enum DecisionSource {
        EXPERIMENT,
        ROLLOUT
    }

    /**
     * Initialize a FeatureDecision object.
     * @param experiment The {@link Experiment} the Feature is associated with.
     * @param variation The {@link Variation} the user was bucketed into.
     * @param decisionSource The source of the variation.
     */
    public FeatureDecision(@Nullable Experiment experiment, @Nullable Variation variation,
                           @Nullable DecisionSource decisionSource) {
        this.experiment = experiment;
        this.variation = variation;
        this.decisionSource = decisionSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeatureDecision that = (FeatureDecision) o;

        if (variation != null ? !variation.equals(that.variation) : that.variation != null)
            return false;
        return decisionSource == that.decisionSource;
    }

    @Override
    public int hashCode() {
        int result = variation != null ? variation.hashCode() : 0;
        result = 31 * result + (decisionSource != null ? decisionSource.hashCode() : 0);
        return result;
    }
}
