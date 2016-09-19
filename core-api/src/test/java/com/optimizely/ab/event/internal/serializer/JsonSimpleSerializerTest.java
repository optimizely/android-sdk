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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.junit.Test;

import java.io.IOException;

import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversion;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpression;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversionJson;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpressionJson;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JsonSimpleSerializerTest {

    private JsonSimpleSerializer serializer = new JsonSimpleSerializer();
    private JSONParser parser = new JSONParser();

    @Test
    public void serializeImpression() throws IOException, ParseException {
        Impression impression = generateImpression();
        // can't compare JSON strings since orders could vary so compare JSONObjects instead
        JSONObject actual = (JSONObject)parser.parse(serializer.serialize(impression));
        JSONObject expected = (JSONObject)parser.parse(generateImpressionJson());

        assertThat(actual, is(expected));
    }

    @Test
    public void serializeConversion() throws IOException, ParseException {
        Conversion conversion = generateConversion();
        // can't compare JSON strings since orders could vary so compare JSONObjects instead
        JSONObject actual = (JSONObject)parser.parse(serializer.serialize(conversion));
        JSONObject expected = (JSONObject)parser.parse(generateConversionJson());

        assertThat(actual, is(expected));
    }
}
