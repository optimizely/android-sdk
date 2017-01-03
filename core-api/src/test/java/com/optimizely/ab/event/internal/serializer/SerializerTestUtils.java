/*
 *    Copyright 2017, Optimizely
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
package com.optimizely.ab.event.internal.serializer;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.optimizely.ab.event.internal.payload.Conversion;
import com.optimizely.ab.event.internal.payload.Decision;
import com.optimizely.ab.event.internal.payload.EventMetric;
import com.optimizely.ab.event.internal.payload.Feature;
import com.optimizely.ab.event.internal.payload.Impression;
import com.optimizely.ab.event.internal.payload.LayerState;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class SerializerTestUtils {

    private static final String visitorId = "testvisitor";
    private static final long timestamp = 12345L;
    private static final boolean isGlobalHoldback = false;
    private static final String projectId = "1";
    private static final String layerId = "2";
    private static final String accountId = "3";
    private static final String variationId = "4";
    private static final boolean isLayerHoldback = false;
    private static final String experimentId = "5";
    private static final Decision decision = new Decision(variationId, isLayerHoldback, experimentId);

    private static final String featureId = "6";
    private static final String featureName = "testfeature";
    private static final String featureType = "custom";
    private static final String featureValue = "testfeaturevalue";
    private static final boolean shouldIndex = true;
    private static final List<Feature> userFeatures = Collections.singletonList(
            new Feature(featureId, featureName, featureType, featureValue, shouldIndex));

    private static final boolean actionTriggered = true;
    private static final List<LayerState> layerStates =
            Collections.singletonList(new LayerState(layerId, decision, actionTriggered));

    private static final String eventEntityId = "7";
    private static final String eventName = "testevent";
    private static final String eventMetricName = EventMetric.REVENUE_METRIC_TYPE;
    private static final long eventMetricValue = 5000L;
    private static final List<EventMetric> eventMetrics = Collections.singletonList(
            new EventMetric(eventMetricName, eventMetricValue));
    private static final List<Feature> eventFeatures = Collections.emptyList();

    static Impression generateImpression() {
        Impression impression = new Impression();
        impression.setVisitorId(visitorId);
        impression.setTimestamp(timestamp);
        impression.setIsGlobalHoldback(isGlobalHoldback);
        impression.setProjectId(projectId);
        impression.setLayerId(layerId);
        impression.setAccountId(accountId);
        impression.setDecision(decision);
        impression.setUserFeatures(userFeatures);
        impression.setClientVersion("0.1.1");
        impression.setAnonymizeIP(true);

        return impression;
    }

    static Conversion generateConversion() {
        Conversion conversion = new Conversion();
        conversion.setVisitorId(visitorId);
        conversion.setTimestamp(timestamp);
        conversion.setProjectId(projectId);
        conversion.setAccountId(accountId);
        conversion.setUserFeatures(userFeatures);
        conversion.setLayerStates(layerStates);
        conversion.setEventEntityId(eventEntityId);
        conversion.setEventName(eventName);
        conversion.setEventMetrics(eventMetrics);
        conversion.setEventFeatures(eventFeatures);
        conversion.setIsGlobalHoldback(isGlobalHoldback);
        conversion.setClientVersion("0.1.1");
        conversion.setAnonymizeIP(true);

        return conversion;
    }

    static String generateImpressionJson() throws IOException {
        String impressionJson = Resources.toString(Resources.getResource("serializer/impression.json"), Charsets.UTF_8);
        return impressionJson.replaceAll("\\s+", "");
    }

    static String generateConversionJson() throws IOException {
        String conversionJson = Resources.toString(Resources.getResource("serializer/conversion.json"), Charsets.UTF_8);
        return conversionJson.replaceAll("\\s+", "");
    }
}
