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
package com.optimizely.ab.event.internal.serializer;

import com.google.gson.Gson;

import com.optimizely.ab.event.internal.payload.Conversion;
import com.optimizely.ab.event.internal.payload.Impression;

import org.junit.Test;

import java.io.IOException;

import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversion;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversionJson;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversionWithSessionId;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversionWithSessionIdJson;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpression;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpressionJson;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpressionWithSessionId;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpressionWithSessionIdJson;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class GsonSerializerTest {

    private GsonSerializer serializer = new GsonSerializer();
    private Gson gson = new Gson();

    @Test
    public void serializeImpression() throws IOException {
        Impression impression = generateImpression();
        // can't compare JSON strings since orders could vary so compare objects instead
        Impression actual = gson.fromJson(serializer.serialize(impression), Impression.class);
        Impression expected = gson.fromJson(generateImpressionJson(), Impression.class);

        assertThat(actual, is(expected));
    }

    @Test
    public void serializeImpressionWithSessionId() throws IOException {
        Impression impression = generateImpressionWithSessionId();
        // can't compare JSON strings since orders could vary so compare objects instead
        Impression actual = gson.fromJson(serializer.serialize(impression), Impression.class);
        Impression expected = gson.fromJson(generateImpressionWithSessionIdJson(), Impression.class);

        assertThat(actual, is(expected));
    }

    @Test
    public void serializeConversion() throws IOException {
        Conversion conversion = generateConversion();
        // can't compare JSON strings since orders could vary so compare objects instead
        Conversion actual = gson.fromJson(serializer.serialize(conversion), Conversion.class);
        Conversion expected = gson.fromJson(generateConversionJson(), Conversion.class);

        assertThat(actual, is(expected));
    }

    @Test
    public void serializeConversionWithSessionId() throws Exception {
        Conversion conversion = generateConversionWithSessionId();
        // can't compare JSON strings since orders could vary so compare objects instead
        Conversion actual = gson.fromJson(serializer.serialize(conversion), Conversion.class);
        Conversion expected = gson.fromJson(generateConversionWithSessionIdJson(), Conversion.class);

        assertThat(actual, is(expected));
    }
}
