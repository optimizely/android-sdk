/****************************************************************************
 * Copyright 2022, Optimizely, Inc. and contributors                   *
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

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class OptimizelyManagerEventHandlerTest {

    private String minDatafileWithEvent = "{\n" +
            "experiments: [ ],\n" +
            "version: \"2\",\n" +
            "audiences: [ ],\n" +
            "groups: [ ],\n" +
            "attributes: [ ],\n" +
            "projectId: \"123\",\n" +
            "accountId: \"6365361536\",\n" +
            "events: [{\"experimentIds\": [\"8509139139\"], \"id\": \"8505434668\", \"key\": \"test_event\"}],\n" +
            "revision: \"1\"\n" +
            "}";

    @Test
    public void eventClientNameAndVersion() throws Exception {
        EventHandler mockEventHandler = mock(EventHandler.class);

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("any-sdk-key")
                .withEventDispatchInterval(0, TimeUnit.SECONDS)
                .withEventHandler(mockEventHandler)
                .build(context);

        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, minDatafileWithEvent);
        optimizelyClient.track("test_event", "tester");

        ArgumentCaptor<LogEvent> argument = ArgumentCaptor.forClass(LogEvent.class);
        verify(mockEventHandler, timeout(5000)).dispatchEvent(argument.capture());
        assertEquals(argument.getValue().getEventBatch().getClientName(), "android-sdk");
        assertEquals(argument.getValue().getEventBatch().getClientVersion(), BuildConfig.CLIENT_VERSION);
    }

    @Test
    public void eventClientWithCustomNameAndVersion() throws Exception {
        EventHandler mockEventHandler = mock(EventHandler.class);

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
            .withSDKKey("any-sdk-key")
            .withEventDispatchInterval(0, TimeUnit.SECONDS)
            .withEventHandler(mockEventHandler)
            .withClientInfo("test-sdk", "test-version")
            .build(context);

        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, minDatafileWithEvent);
        optimizelyClient.track("test_event", "tester");

        ArgumentCaptor<LogEvent> argument = ArgumentCaptor.forClass(LogEvent.class);
        verify(mockEventHandler, timeout(5000)).dispatchEvent(argument.capture());
        assertEquals(argument.getValue().getEventBatch().getClientName(), "test-sdk");
        assertEquals(argument.getValue().getEventBatch().getClientVersion(), "test-version");
    }

}
