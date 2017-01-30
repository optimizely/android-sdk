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
package com.optimizely.ab.event.internal;

import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import java.util.Map;

public abstract class EventBuilder {

    public LogEvent createImpressionEvent(@Nonnull ProjectConfig projectConfig,
                                          @Nonnull Experiment activatedExperiment,
                                          @Nonnull Variation variation,
                                          @Nonnull String userId,
                                          @Nonnull Map<String, String> attributes) {
        return createImpressionEvent(projectConfig, activatedExperiment, variation, userId, attributes, null);
    }

    public abstract LogEvent createImpressionEvent(@Nonnull ProjectConfig projectConfig,
                                                   @Nonnull Experiment activatedExperiment,
                                                   @Nonnull Variation variation,
                                                   @Nonnull String userId,
                                                   @Nonnull Map<String, String> attributes,
                                                   @CheckForNull String sessionId);

    public LogEvent createConversionEvent(@Nonnull ProjectConfig projectConfig,
                                          @Nonnull Bucketer bucketer,
                                          @Nonnull String userId,
                                          @Nonnull String eventId,
                                          @Nonnull String eventName,
                                          @Nonnull Map<String, String> attributes) {
        return createConversionEvent(projectConfig, bucketer, userId, eventId, eventName, attributes, null, null);
    }

    public LogEvent createConversionEvent(@Nonnull ProjectConfig projectConfig,
                                          @Nonnull Bucketer bucketer,
                                          @Nonnull String userId,
                                          @Nonnull String eventId,
                                          @Nonnull String eventName,
                                          @Nonnull Map<String, String> attributes,
                                          long eventValue) {
        return createConversionEvent(projectConfig, bucketer, userId, eventId, eventName, attributes, (Long)eventValue,
                                     null);
    }

    public abstract LogEvent createConversionEvent(@Nonnull ProjectConfig projectConfig,
                                                   @Nonnull Bucketer bucketer,
                                                   @Nonnull String userId,
                                                   @Nonnull String eventId,
                                                   @Nonnull String eventName,
                                                   @Nonnull Map<String, String> attributes,
                                                   @CheckForNull Long eventValue,
                                                   @CheckForNull String sessionId);
}
