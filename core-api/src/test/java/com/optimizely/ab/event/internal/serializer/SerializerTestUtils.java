/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
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
import com.optimizely.ab.event.internal.payload.Attribute;
import com.optimizely.ab.event.internal.payload.Decision;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.event.internal.payload.Event;
import com.optimizely.ab.event.internal.payload.Snapshot;
import com.optimizely.ab.event.internal.payload.Visitor;

import java.io.IOException;
import java.util.Arrays;
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
    private static final String sessionId = "sessionid";
    private static final String revision = "1";
    private static final Decision decision = new Decision(layerId, experimentId, variationId, isLayerHoldback);

    private static final String featureId = "6";
    private static final String featureName = "testfeature";
    private static final String featureType = "custom";
    private static final String featureValue = "testfeaturevalue";
    private static final boolean shouldIndex = true;
    private static final List<Attribute> userFeatures = Collections.singletonList(
            new Attribute(featureId, featureName, featureType, featureValue));

    private static final boolean actionTriggered = true;

    private static final String eventEntityId = "7";
    private static final String eventName = "testevent";
    private static final String eventMetricName = "revenue";
    private static final long eventMetricValue = 5000L;

    private static final List<Event> events = Collections.singletonList(new Event(timestamp,
            "uuid", eventEntityId, eventName, null, 5000L, null, eventName, null));

    static EventBatch generateImpression() {
        Snapshot snapshot = new Snapshot(Arrays.asList(decision), events);

        Visitor vistor = new Visitor(visitorId, null, userFeatures, Arrays.asList(snapshot));
        EventBatch impression = new EventBatch(accountId, Arrays.asList(vistor), false, projectId,revision );
        impression.setProjectId(projectId);
        impression.setAccountId(accountId);
        impression.setClientVersion("0.1.1");
        impression.setAnonymizeIp(true);
        impression.setRevision(revision);

        return impression;
    }

    static EventBatch generateImpressionWithSessionId() {
        EventBatch impression = generateImpression();
        impression.getVisitors().get(0).setSessionId(sessionId);

        return impression;
    }

    static EventBatch generateConversion() {
        EventBatch conversion = generateImpression();
        conversion.setClientVersion("0.1.1");
        conversion.setAnonymizeIp(true);
        conversion.setRevision(revision);

        return conversion;
    }

    static EventBatch generateConversionWithSessionId() {
        EventBatch conversion = generateConversion();
        conversion.getVisitors().get(0).setSessionId(sessionId);

        return conversion;
    }

    static String generateImpressionJson() throws IOException {
        String impressionJson = Resources.toString(Resources.getResource("serializer/impression.json"), Charsets.UTF_8);
        return impressionJson.replaceAll("\\s+", "");
    }

    static String generateImpressionWithSessionIdJson() throws IOException {
        String impressionJson = Resources.toString(Resources.getResource("serializer/impression-session-id.json"),
                                                   Charsets.UTF_8);
        return impressionJson.replaceAll("\\s+", "");
    }

    static String generateConversionJson() throws IOException {
        String conversionJson = Resources.toString(Resources.getResource("serializer/conversion.json"), Charsets.UTF_8);
        return conversionJson.replaceAll("\\s+", "");
    }

    static String generateConversionWithSessionIdJson() throws IOException {
        String conversionJson = Resources.toString(Resources.getResource("serializer/conversion-session-id.json"),
                                                   Charsets.UTF_8);
        return conversionJson.replaceAll("\\s+", "");
    }
}
