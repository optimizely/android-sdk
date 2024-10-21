/****************************************************************************
 * Copyright 2023, Optimizely, Inc. and contributors                        *
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

package com.optimizely.ab.android.sdk;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.android.odp.DefaultODPApiManager;
import com.optimizely.ab.odp.ODPApiManager;
import com.optimizely.ab.odp.ODPEventManager;
import com.optimizely.ab.odp.ODPManager;
import com.optimizely.ab.android.odp.VuidManager;
import com.optimizely.ab.odp.ODPSegmentManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for Optimizely ODP Integration
 */
@RunWith(AndroidJUnit4.class)
public class ODPIntegrationTest {

    private OptimizelyManager optimizelyManager;
    private OptimizelyClient optimizelyClient;
    private ODPManager odpManager;
    private ODPEventManager odpEventManager;
    private ODPSegmentManager odpSegmentManager;
    private ODPApiManager odpApiManager;
    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private String testSdkKey = "12345";
    private String testUser = "test-user";
    private String testVuid = "vuid_123";   // must start with "vuid_" to be parsed properly in java-sdk core

    private String odpDatafile = "{" +
        "\"version\": \"4\"," +
        "\"rollouts\": []," +
        "\"anonymizeIP\": true," +
        "\"projectId\": \"10431130345\"," +
        "\"variables\": []," +
        "\"featureFlags\": []," +
        "\"experiments\": []," +
        "\"audiences\": []," +
        "\"groups\": []," +
        "\"attributes\": []," +
        "\"accountId\": \"10367498574\"," +
        "\"events\": []," +
        "\"revision\": \"100\"," +
        "\"typedAudiences\":[{\"id\": \"12\",\"conditions\": [\"or\",{\"value\": \"segment-1\",\"type\": \"third_party_dimension\",\"name\": \"odp.audiences\",\"match\": \"qualified\"}],\"name\": \"audience-1\"}]," +
        "\"integrations\":[{\"key\":\"odp\",\"host\":\"h-1\",\"publicKey\":\"p-1\"}]" +
        "}";

    @Before
    public void setup() throws Exception {
        odpApiManager = mock(DefaultODPApiManager.class);
        when(odpApiManager.sendEvents(anyString(), anyString(), anyString())).thenReturn(200);   // return success, otherwise retried 3 times.

        odpEventManager = new ODPEventManager(odpApiManager);
        odpSegmentManager = new ODPSegmentManager(odpApiManager);

        optimizelyManager = OptimizelyManager.builder()
            .withSDKKey(testSdkKey)
            .withVuidEnabled()
            .withODPEventManager(odpEventManager)
            .withODPSegmentManager(odpSegmentManager)
            .build(context);

        optimizelyManager.initialize(context, odpDatafile);
        optimizelyClient = optimizelyManager.getOptimizely();

    }

    @Test
    public void identifyOdpEventSentWhenUserContextCreated() throws InterruptedException {
        optimizelyClient.createUserContext(testUser);

        Thread.sleep(2000);  // wait for batch timeout (1sec)

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(odpApiManager, times(1)).sendEvents(eq("p-1"), eq("h-1/v3/events"), captor.capture());
        String eventStr = captor.getValue();

        // 2 events (client_initialized, identified) will be batched in a single sendEvents() call.
        JsonArray jsonArray = JsonParser.parseString(eventStr).getAsJsonArray();
        assertEquals(jsonArray.size(), 2);

        // "client_initialized" event (vuid only)
        JsonObject firstEvt = jsonArray.get(0).getAsJsonObject();
        JsonObject firstIdentifiers = firstEvt.get("identifiers").getAsJsonObject();
        JsonObject firstData = firstEvt.get("data").getAsJsonObject();

        // "identified" event (vuid + fs_user_id)
        JsonObject secondEvt = jsonArray.get(1).getAsJsonObject();
        JsonObject secondIdentifiers = secondEvt.get("identifiers").getAsJsonObject();

        assertEquals(firstEvt.get("action").getAsString(), "client_initialized");
        assertEquals(firstIdentifiers.size(), 1);
        assertTrue(VuidManager.isVuid(firstIdentifiers.get("vuid").getAsString()));

        assertEquals(secondEvt.get("action").getAsString(), "identified");
        assertEquals(secondIdentifiers.size(), 2);
        assertTrue(VuidManager.isVuid(secondIdentifiers.get("vuid").getAsString()));
        assertEquals(secondIdentifiers.get("fs_user_id").getAsString(), testUser);

        // validate that ODP event data includes correct values.
        assertEquals(firstData.size(), 8); // {idempotence_id, os, os_version, data_source_type, data_source_version, device_type, model, data_source}
        assertEquals(firstData.get("data_source").getAsString(), "android-sdk");
    }

    @Test
    public void identifyOdpEventSentWhenVuidUserContextCreated() throws InterruptedException {
        optimizelyClient.createUserContext();  // empty userId. vuid will be used.

        Thread.sleep(2000);  // wait for batch timeout (1sec)

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(odpApiManager, times(1)).sendEvents(eq("p-1"), eq("h-1/v3/events"), captor.capture());
        String eventStr = captor.getValue();

        // 2 events (client_initialized, identified) will be batched in a single sendEvents() call.
        JsonArray jsonArray = JsonParser.parseString(eventStr).getAsJsonArray();
        assertEquals(jsonArray.size(), 2);

        // "client_initialized" event (vuid only)
        JsonObject firstEvt = jsonArray.get(0).getAsJsonObject();
        JsonObject firstIdentifiers = firstEvt.get("identifiers").getAsJsonObject();

        // "identified" event (vuid only)
        JsonObject secondEvt = jsonArray.get(1).getAsJsonObject();
        JsonObject secondIdentifiers = secondEvt.get("identifiers").getAsJsonObject();

        assertEquals(firstEvt.get("action").getAsString(), "client_initialized");
        assertEquals(firstIdentifiers.size(), 1);
        assertTrue(VuidManager.isVuid(firstIdentifiers.get("vuid").getAsString()));

        assertEquals(secondEvt.get("action").getAsString(), "identified");
        assertEquals(secondIdentifiers.size(), 1);
        assertTrue(VuidManager.isVuid(secondIdentifiers.get("vuid").getAsString()));
    }

    @Test
    public void fetchQualifiedSegmentsWithUserContext() throws InterruptedException {
        OptimizelyUserContext user = optimizelyClient.createUserContext(testUser);

        Boolean status = user.fetchQualifiedSegments();

        verify(odpApiManager, times(1)).fetchQualifiedSegments(
            eq("p-1"),
            eq("h-1/v3/graphql"),
            eq("fs_user_id"),
            eq(testUser),
            eq(new HashSet<>(Arrays.asList("segment-1")))
        );
    }

    @Test
    public void fetchQualifiedSegmentsWithVuidUserContext() throws InterruptedException {
        OptimizelyUserContext user = optimizelyClient.createUserContext();  // empty userId. vuid will be used.

        Boolean status = user.fetchQualifiedSegments();

        verify(odpApiManager, times(1)).fetchQualifiedSegments(
            eq("p-1"),
            eq("h-1/v3/graphql"),
            eq("vuid"),
            eq(optimizelyClient.getVuid()),
            eq(new HashSet<>(Arrays.asList("segment-1")))
        );
    }

}
