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

import com.fasterxml.jackson.databind.ObjectMapper;

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

public class JacksonSerializerTest {

    private JacksonSerializer serializer = new JacksonSerializer();
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void serializeImpression() throws IOException {
        Impression impression = generateImpression();
        // can't compare JSON strings since orders could vary so compare objects instead
        Impression actual = mapper.readValue(serializer.serialize(impression), Impression.class);
        Impression expected = mapper.readValue(generateImpressionJson(), Impression.class);

        assertThat(actual, is(expected));
    }

    @Test
    public void serializeImpressionWithSessionId() throws IOException {
        Impression impression = generateImpressionWithSessionId();
        // can't compare JSON strings since orders could vary so compare objects instead
        Impression actual = mapper.readValue(serializer.serialize(impression), Impression.class);
        Impression expected = mapper.readValue(generateImpressionWithSessionIdJson(), Impression.class);

        assertThat(actual, is(expected));
    }

    @Test
    public void serializeConversion() throws IOException {
        Conversion conversion = generateConversion();
        // can't compare JSON strings since orders could vary so compare objects instead
        Conversion actual = mapper.readValue(serializer.serialize(conversion), Conversion.class);
        Conversion expected = mapper.readValue(generateConversionJson(), Conversion.class);

        assertThat(actual, is(expected));
    }

    @Test
    public void serializeConversionWithSessionId() throws IOException {
        Conversion conversion = generateConversionWithSessionId();
        // can't compare JSON strings since orders could vary so compare objects instead
        Conversion actual = mapper.readValue(serializer.serialize(conversion), Conversion.class);
        Conversion expected = mapper.readValue(generateConversionWithSessionIdJson(), Conversion.class);

        assertThat(actual, is(expected));
    }
}

