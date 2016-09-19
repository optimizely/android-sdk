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
package com.optimizely.ab.event.internal.serializer;

import com.optimizely.ab.event.internal.payload.Conversion;
import com.optimizely.ab.event.internal.payload.Impression;

import org.json.JSONObject;

import org.junit.Test;

import java.io.IOException;

import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversion;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpression;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversionJson;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpressionJson;

import static org.junit.Assert.assertTrue;

public class JsonSerializerTest {

    private JsonSerializer serializer = new JsonSerializer();

    @Test
    public void serializeImpression() throws IOException {
        Impression impression = generateImpression();
        // can't compare JSON strings since orders could vary so compare JSONObjects instead
        JSONObject actual = new JSONObject(serializer.serialize(impression));
        JSONObject expected = new JSONObject(generateImpressionJson());

        assertTrue(actual.similar(expected));
    }

    @Test
    public void serializeConversion() throws IOException {
        Conversion conversion = generateConversion();
        // can't compare JSON strings since orders could vary so compare JSONObjects instead
        JSONObject actual = new JSONObject(serializer.serialize(conversion));
        JSONObject expected = new JSONObject(generateConversionJson());

        assertTrue(actual.similar(expected));
    }
}
