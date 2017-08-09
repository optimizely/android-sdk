/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
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

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.core.deps.guava.util.concurrent.ListeningExecutorService;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.android.event_handler.DefaultEventHandler;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.internal.payload.Event;
import com.optimizely.ab.notification.NotificationListener;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.exceptions.verification.junit.ArgumentsAreDifferent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class OptimizelyClientTest {
    private Logger logger = mock(Logger.class);
    private Optimizely optimizely;

    private String testProjectId = "7595190003";

    private String minDatafile = "{\"groups\": [], \"projectId\": \"8504447126\", \"variables\": [{\"defaultValue\": \"true\", \"type\": \"boolean\", \"id\": \"8516291943\", \"key\": \"test_variable\"}], \"version\": \"3\", \"experiments\": [{\"status\": \"Running\", \"key\": \"android_experiment_key\", \"layerId\": \"8499056327\", \"trafficAllocation\": [{\"entityId\": \"8509854340\", \"endOfRange\": 5000}, {\"entityId\": \"8505434669\", \"endOfRange\": 10000}], \"audienceIds\": [], \"variations\": [{\"variables\": [], \"id\": \"8509854340\", \"key\": \"var_1\"}, {\"variables\": [], \"id\": \"8505434669\", \"key\": \"var_2\"}], \"forcedVariations\": {}, \"id\": \"8509139139\"}], \"audiences\": [], \"anonymizeIP\": true, \"attributes\": [], \"revision\": \"7\", \"events\": [{\"experimentIds\": [\"8509139139\"], \"id\": \"8505434668\", \"key\": \"test_event\"}], \"accountId\": \"8362480420\"}";

    @Before
    public void setUp() throws Exception {
        optimizely = Optimizely.builder(minDatafile, DefaultEventHandler.getInstance(InstrumentationRegistry.getTargetContext())).build();
    }

    @Test
    public void testGoodActivation1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        Variation v = optimizelyClient.activate("android_experiment_key", "1");
        assertNotNull(v);

    }

    @Test
    public void testGoodActivation2() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        Variation v = optimizelyClient.activate("android_experiment_key", "1", attributes);
        assertNotNull(v);
    }

    @Test
    public void testBadActivation2() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.activate("1", "1", new HashMap<String, String>());
        verify(logger).warn("Optimizely is not initialized, could not activate experiment {} " +
                "for user {} with attributes", "1", "1");
    }


    @Test
    public void testGoodTrack1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        optimizelyClient.track("test_event", "1");
        verifyZeroInteractions(logger);
    }

    @Test
    public void testBadTrack1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.track("test_event", "1");
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}", "test_event", "1");
    }

    @Test
    public void testGoodTrack2() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.track("test_event", "1", attributes);
        verifyZeroInteractions(logger);
    }

    @Test
    public void testBadTrack2() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.track("event1", "1", attributes);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with attributes", "event1", "1");
    }

    @Test
    public void testGoodTrack3() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        optimizelyClient.track("test_event", "1", 1L);
        verifyZeroInteractions(logger);
    }

    @Test
    public void testBadTrack3() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.track("event1", "1", 1L);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with value {}", "event1", "1", 1L);
    }

    @Test
    public void testGoodTrack4() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.track("test_event", "1", attributes, 1L);
        verifyZeroInteractions(logger);
    }

    @Test
    public void testBadTrack4() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.track("event1", "1", attributes, 1L);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with value {} and attributes", "event1", "1", 1L);
    }

    @Test
    public void testTrackWithEventTags() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        attributes.put("foo", "bar");
        final HashMap<String, Object> eventTags = new HashMap<>();
        eventTags.put("foo", 843);
        optimizelyClient.track("test_event", "1", attributes, eventTags);
        verifyZeroInteractions(logger);
    }

    @Test
    public void testGoodGetVariation1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        Variation v = optimizelyClient.getVariation("android_experiment_key", "1");
        assertNotNull(v);
    }

    @Test
    public void testBadGetVariation1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.getVariation("android_experiment_key", "1");
        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {}", "android_experiment_key", "1");
    }

    @Test
    public void testGoodGetVariation3() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.getVariation("android_experiment_key", "1", attributes);
        verifyZeroInteractions(logger);
    }

    @Test
    public void testBadGetVariation3() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.getVariation("android_experiment_key", "1", attributes);
        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {} with attributes", "android_experiment_key", "1");
    }

    @Test
    public void testGoodGetProjectConfig() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        ProjectConfig config = optimizelyClient.getProjectConfig();
        assertNotNull(config);
    }

    @Test
    public void testBadGetProjectConfig() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.getProjectConfig();
        verify(logger).warn("Optimizely is not initialized, could not get project config");
    }

    @Test
    public void testIsValid() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        assertTrue(optimizelyClient.isValid());
    }

    @Test
    public void testIsInvalid() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        assertFalse(optimizelyClient.isValid());
    }

    @Test
    public void testGoodGetVariableString() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        String v = optimizelyClient.getVariableString("test_variable", "userId",
                Collections.<String, String>emptyMap(), true);
        assertEquals(v, "true");
    }

    @Test
    public void testBadGetVariableString() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.getVariableString("test_key", "userId",
                Collections.<String, String>emptyMap(), true);
        verify(logger).warn("Optimizely is not initialized, could not get live variable {} " +
                "for user {}", "test_key", "userId");
    }

    @Test
    public void testGoodGetVariableBoolean() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        Boolean b = optimizelyClient.getVariableBoolean("test_variable", "userId",
                Collections.<String, String>emptyMap(), true);
        assertNotNull(b);
    }

    @Test
    public void testBadGetVariableBoolean() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.getVariableBoolean("test_key", "userId",
                Collections.<String, String>emptyMap(), true);
        verify(logger).warn("Optimizely is not initialized, could not get live variable {} " +
                "for user {}", "test_key", "userId");
    }

    @Test
    public void testGoodGetVariableInteger() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        Integer i = optimizelyClient.getVariableInteger("test_variable", "userId",
                Collections.<String, String>emptyMap(), true);
        assertNull(i);
    }

    @Test
    public void testBadGetVariableInteger() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.getVariableInteger("test_key", "userId",
                Collections.<String, String>emptyMap(), true);
        verify(logger).warn("Optimizely is not initialized, could not get live variable {} " +
                "for user {}", "test_key", "userId");
    }

    @Test
    public void testGoodGetVariableDouble() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        Double v = optimizelyClient.getVariableDouble("test_variable", "userId",
                Collections.<String, String>emptyMap(), true);
        assertNull(v);
    }

    @Test
    public void testBadGetVariableDouble() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.getVariableDouble("test_key", "userId",
                Collections.<String, String>emptyMap(), true);
        verify(logger).warn("Optimizely is not initialized, could not get live variable {} " +
                "for user {}", "test_key", "userId");
    }

    @Test
    public void testDefaultAttributes() {
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);
        when(appContext.getPackageName()).thenReturn("com.optly");

        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.setDefaultAttributes(OptimizelyDefaultAttributes.buildDefaultAttributesMap(context, logger));

        Map<String, String> map = optimizelyClient.getDefaultAttributes();
        Assert.assertEquals(map.size(), 4);
    }

    //======== Notification listeners ========//

    @Test
    public void testGoodAddNotificationListener() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        NotificationListener listener = new NotificationListener() {
            @Override
            public void onExperimentActivated(Experiment experiment,
                                              String s,
                                              Map<String, String> map,
                                              Variation variation) {
            }
        };
        optimizelyClient.addNotificationListener(listener);
        optimizelyClient.removeNotificationListener(listener);
        verifyZeroInteractions(logger);
    }

    @Test
    public void testBadAddNotificationListener() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        NotificationListener listener = new NotificationListener() {
            @Override
            public void onExperimentActivated(Experiment experiment,
                                              String s,
                                              Map<String, String> map,
                                              Variation variation) {
            }
        };
        optimizelyClient.addNotificationListener(listener);
        verify(logger).warn("Optimizely is not initialized, could not add notification listener");
    }

    @Test
    public void testBadRemoveNotificationListener() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        NotificationListener listener = new NotificationListener() {
            @Override
            public void onExperimentActivated(Experiment experiment,
                                              String s,
                                              Map<String, String> map,
                                              Variation variation) {
            }
        };
        optimizelyClient.removeNotificationListener(listener);
        verify(logger).warn("Optimizely is not initialized, could not remove notification listener");
    }

    @Test
    public void testGoodClearNotificationListeners() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        optimizelyClient.clearNotificationListeners();
        verifyZeroInteractions(logger);
    }

    @Test
    public void testBadClearNotificationListeners() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.clearNotificationListeners();
        verify(logger).warn("Optimizely is not initialized, could not clear notification listeners");
    }
}
