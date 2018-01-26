/**
 *
 *    Copyright 2016-2018, Optimizely and contributors
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

import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.bucketing.DecisionService;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.Attribute;
import com.optimizely.ab.event.internal.payload.Decision;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.event.internal.payload.Event;
import com.optimizely.ab.event.internal.payload.Snapshot;
import com.optimizely.ab.event.internal.payload.Visitor;
import com.optimizely.ab.event.internal.serializer.DefaultJsonSerializer;
import com.optimizely.ab.event.internal.serializer.Serializer;
import com.optimizely.ab.internal.EventTagUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EventBuilder {
    private static final Logger logger = LoggerFactory.getLogger(EventBuilder.class);
    static final String ATTRIBUTE_KEY_FOR_BUCKETING_ATTRIBUTE = "optimizely_bucketing_id";
    static final String EVENT_ENDPOINT = "https://logx.optimizely.com/v1/events";
    static final String  ACTIVATE_EVENT_KEY = "campaign_activated";

    private Serializer serializer;
    @VisibleForTesting
    public final String clientVersion;
    @VisibleForTesting
    public final EventBatch.ClientEngine clientEngine;

    public EventBuilder() {
        this(EventBatch.ClientEngine.JAVA_SDK, BuildVersionInfo.VERSION);
    }

    public EventBuilder(EventBatch.ClientEngine clientEngine, String clientVersion) {
        this.clientEngine = clientEngine;
        this.clientVersion = clientVersion;
        this.serializer = DefaultJsonSerializer.getInstance();
    }


    public LogEvent createImpressionEvent(@Nonnull ProjectConfig projectConfig,
                                                   @Nonnull Experiment activatedExperiment,
                                                   @Nonnull Variation variation,
                                                   @Nonnull String userId,
                                                   @Nonnull Map<String, String> attributes) {

        Decision decision = new Decision(activatedExperiment.getLayerId(), activatedExperiment.getId(),
                variation.getId(), false);
        Event impressionEvent = new Event(System.currentTimeMillis(),UUID.randomUUID().toString(), activatedExperiment.getLayerId(),
                ACTIVATE_EVENT_KEY, null, null, null, ACTIVATE_EVENT_KEY, null);
        Snapshot snapshot = new Snapshot(Arrays.asList(decision), Arrays.asList(impressionEvent));

        Visitor visitor = new Visitor(userId, null, buildAttributeList(projectConfig, attributes), Arrays.asList(snapshot));
        List<Visitor> visitors = Arrays.asList(visitor);
        EventBatch eventBatch = new EventBatch(clientEngine.getClientEngineValue(), clientVersion, projectConfig.getAccountId(), visitors, projectConfig.getAnonymizeIP(), projectConfig.getProjectId(), projectConfig.getRevision());
        String payload = this.serializer.serialize(eventBatch);
        return new LogEvent(LogEvent.RequestMethod.POST, EVENT_ENDPOINT, Collections.<String, String>emptyMap(), payload);
    }

    public LogEvent createConversionEvent(@Nonnull ProjectConfig projectConfig,
                                                   @Nonnull Map<Experiment, Variation> experimentVariationMap,
                                                   @Nonnull String userId,
                                                   @Nonnull String eventId,
                                                   @Nonnull String eventName,
                                                   @Nonnull Map<String, String> attributes,
                                                   @Nonnull Map<String, ?> eventTags) {

        if (experimentVariationMap.isEmpty()) {
            return null;
        }

        ArrayList<Decision> decisions = new ArrayList<Decision>();
        for (Map.Entry<Experiment, Variation> entry : experimentVariationMap.entrySet()) {
            Decision decision = new Decision(entry.getKey().getLayerId(), entry.getKey().getId(), entry.getValue().getId(), false);
            decisions.add(decision);
        }

        EventType eventType = projectConfig.getEventNameMapping().get(eventName);

        Event conversionEvent = new Event(System.currentTimeMillis(),UUID.randomUUID().toString(), eventType.getId(),
                eventType.getKey(), null, EventTagUtils.getRevenueValue(eventTags), eventTags, eventType.getKey(), EventTagUtils.getNumericValue(eventTags));
        Snapshot snapshot = new Snapshot(decisions, Arrays.asList(conversionEvent));

        Visitor visitor = new Visitor(userId, null, buildAttributeList(projectConfig, attributes), Arrays.asList(snapshot));
        List<Visitor> visitors = Arrays.asList(visitor);
        EventBatch eventBatch = new EventBatch(clientEngine.getClientEngineValue(), clientVersion, projectConfig.getAccountId(), visitors, projectConfig.getAnonymizeIP(), projectConfig.getProjectId(), projectConfig.getRevision());
        String payload = this.serializer.serialize(eventBatch);
        return new LogEvent(LogEvent.RequestMethod.POST, EVENT_ENDPOINT, Collections.<String, String>emptyMap(), payload);
    }

    private List<Attribute> buildAttributeList(ProjectConfig projectConfig, Map<String, String> attributes) {
        List<Attribute> attributesList = new ArrayList<Attribute>();

        Map<String, com.optimizely.ab.config.Attribute> attributeMap = projectConfig.getAttributeKeyMapping();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            com.optimizely.ab.config.Attribute projectAttribute = attributeMap.get(entry.getKey());
            Attribute attribute = new Attribute((projectAttribute != null ? projectAttribute.getId() : null),
                    entry.getKey(), Attribute.CUSTOM_ATTRIBUTE_TYPE, entry.getValue());

            if (entry.getKey() == DecisionService.BUCKETING_ATTRIBUTE) {
                attribute = new Attribute(com.optimizely.ab.bucketing.DecisionService.BUCKETING_ATTRIBUTE,
                        ATTRIBUTE_KEY_FOR_BUCKETING_ATTRIBUTE, Attribute.CUSTOM_ATTRIBUTE_TYPE, entry.getValue());
            }

            attributesList.add(attribute);
        }

        return attributesList;
    }
}
