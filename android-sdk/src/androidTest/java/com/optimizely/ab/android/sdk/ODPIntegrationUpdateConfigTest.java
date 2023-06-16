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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.optimizely.ab.android.datafile_handler.DefaultDatafileHandler;
import com.optimizely.ab.android.event_handler.DefaultEventHandler;
import com.optimizely.ab.android.odp.DefaultODPApiManager;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.event.EventProcessor;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.odp.ODPApiManager;
import com.optimizely.ab.odp.ODPEventManager;
import com.optimizely.ab.odp.ODPManager;
import com.optimizely.ab.odp.ODPSegmentManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Set;

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

/**
 * Tests for Optimizely ODP Integration
 */
@RunWith(AndroidJUnit4.class)
public class ODPIntegrationUpdateConfigTest {

    private OptimizelyManager optimizelyManager;
    private ODPManager odpManager;
    private DefaultDatafileHandler datafileHandler;
    private NotificationCenter notificationCenter;
    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private String testSdkKey = "12345";

    private String emptyV4Core =
        "\"version\": \"4\"," +
        "\"sdkKey\": \"test-sdkKey\"," +
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
        "\"revision\": \"100\",";

    String integration1 = "\"integrations\":[{\"key\":\"odp\",\"host\":\"h-1\",\"publicKey\":\"p-1\"}]";
    String integration2 = "\"integrations\":[{\"key\":\"odp\",\"host\":\"h-2\",\"publicKey\":\"p-2\"}]";
    String odpDatafile1 = "{" + emptyV4Core + integration1 + "}";
    String odpDatafile2 = "{" + emptyV4Core + integration2 + "}";

    @Before
    public void setup() throws Exception {
        odpManager = mock(ODPManager.class);
        when(odpManager.getEventManager()).thenReturn(mock(ODPEventManager.class));

        datafileHandler = new DefaultDatafileHandler();
        notificationCenter = new NotificationCenter();

        optimizelyManager = new OptimizelyManager(
            null,
            testSdkKey,
            null,
            mock(Logger.class),
            3600L,
            datafileHandler,
            null,
            3600L,
            mock(DefaultEventHandler.class),
            mock(EventProcessor.class),
            null,
            notificationCenter,
            null,
            odpManager,
            "test-vuid",
            null,
            null);
    }

    @Test
    public void initializeSynchronous_updateODPConfig() {
        // NOTE: odpConfig is updated when Optimizely.java (java-sdk core) is initialized.
        //       Same for async-initialization, so need to repeat the same test (hard to test for async-init).

        optimizelyManager.initialize(context, odpDatafile1);
        verify(odpManager, times(1)).updateSettings(
            eq("h-1"),
            eq("p-1"),
            eq(Collections.emptySet()));

        // validate no other calls

        verify(odpManager, times(1)).updateSettings(
            anyString(),
            anyString(),
            any(Set.class));
    }

    @Test
    public void updateODPConfigWhenDatafileUpdatedByBackgroundPolling() throws InterruptedException {
        // NOTE: same logic for async-initialization, so no need to repeat for async

        boolean updateConfigOnBackgroundDatafile = true;
        optimizelyManager.initialize(context, odpDatafile1, true, updateConfigOnBackgroundDatafile);

        // datafile will be saved when a new datafile is downloaded by background polling
        datafileHandler.saveDatafile(context, new DatafileConfig(null, testSdkKey, null), odpDatafile2);
        Thread.sleep(1_000);  // need a delay for file-observer (update notification)

        // odpConfig updated on initialization

        verify(odpManager, times(1)).updateSettings(
            eq("h-1"),
            eq("p-1"),
            eq(Collections.emptySet()));

        // odpConfig updated on background polling

        verify(odpManager, times(1)).updateSettings(
            eq("h-2"),
            eq("p-2"),
            eq(Collections.emptySet()));

        // no other calls

        verify(odpManager, times(2)).updateSettings(
            anyString(),
            anyString(),
            any(Set.class));
    }

}
