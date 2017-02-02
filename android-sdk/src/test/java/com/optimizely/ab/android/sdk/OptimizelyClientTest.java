/****************************************************************************
 * Copyright 2016-2017, Optimizely, Inc. and contributors                   *
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

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.notification.NotificationListener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link OptimizelyClient}
 */
@RunWith(MockitoJUnitRunner.class)
public class OptimizelyClientTest {

    @Mock Logger logger;
    @Mock Optimizely optimizely;

    @Test
    public void testGoodActivation1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        optimizelyClient.activate("1", "1");
        verify(optimizely).activate("1", "1");
    }

    @Test
    public void testBadActivation1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.activate("1", "1");
        verify(logger).warn("Optimizely is not initialized, can't activate experiment {} " +
                "for user {}", "1", "1");
    }

    @Test
    public void testGoodActivation2() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.activate("1", "1", attributes);
        verify(optimizely).activate("1", "1", attributes);
    }

    @Test
    public void testBadActivation2() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.activate("1", "1", new HashMap<String, String>());
        verify(logger).warn("Optimizely is not initialized, can't activate experiment {} " +
                "for user {} with attributes", "1", "1");
    }


    @Test
    public void testGoodTrack1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        optimizelyClient.track("event1", "1");
        verify(optimizely).track("event1", "1");
    }

    @Test
    public void testBadTrack1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.track("event1", "1");
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}", "event1", "1");
    }

    @Test
    public void testGoodTrack2() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.track("event1", "1", attributes);
        verify(optimizely).track("event1", "1", attributes);
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
        optimizelyClient.track("event1", "1", 1L);
        verify(optimizely).track("event1", "1", 1L);
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
        optimizelyClient.track("event1", "1", attributes, 1L);
        verify(optimizely).track("event1", "1", attributes, 1L);
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
    public void testGoodGetVariation1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        optimizelyClient.getVariation("1", "1");
        verify(optimizely).getVariation("1", "1");
    }

    @Test
    public void testBadGetVariation1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.getVariation("1", "1");
        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                    "for user {}", "1", "1");
    }

    @Test
    public void testGoodGetVariation3() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.getVariation("1", "1", attributes);
        verify(optimizely).getVariation("1", "1", attributes);
    }

    @Test
    public void testBadGetVariation3() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.getVariation("1", "1", attributes);
        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {} with attributes", "1", "1");
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
        optimizelyClient.getVariableString("test_key", "userId",
                                           Collections.<String, String>emptyMap(), true);
        verify(optimizely).getVariableString("test_key", "userId",
                                             Collections.<String, String>emptyMap(), true);
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
        optimizelyClient.getVariableBoolean("test_key", "userId",
                                            Collections.<String, String>emptyMap(), true);
        verify(optimizely).getVariableBoolean("test_key", "userId",
                                              Collections.<String, String>emptyMap(), true);
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
        optimizelyClient.getVariableInteger("test_key", "userId",
                                            Collections.<String, String>emptyMap(), true);
        verify(optimizely).getVariableInteger("test_key", "userId",
                                              Collections.<String, String>emptyMap(), true);
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
        optimizelyClient.getVariableDouble("test_key", "userId",
                                           Collections.<String, String>emptyMap(), true);
        verify(optimizely).getVariableDouble("test_key", "userId",
                                             Collections.<String, String>emptyMap(), true);
    }

    @Test
    public void testBadGetVariableDouble() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.getVariableDouble("test_key", "userId",
                                           Collections.<String, String>emptyMap(), true);
        verify(logger).warn("Optimizely is not initialized, could not get live variable {} " +
                "for user {}", "test_key", "userId");
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
        verify(optimizely).addNotificationListener(listener);
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
    public void testGoodRemoveNotificationListener() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        NotificationListener listener = new NotificationListener() {
            @Override
            public void onExperimentActivated(Experiment experiment,
                                              String s,
                                              Map<String, String> map,
                                              Variation variation) {
            }
        };
        optimizelyClient.removeNotificationListener(listener);
        verify(optimizely).removeNotificationListener(listener);
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
        verify(optimizely).clearNotificationListeners();
    }

    @Test
    public void testBadClearNotificationListeners() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.clearNotificationListeners();
        verify(logger).warn("Optimizely is not initialized, could not clear notification listeners");
    }
}
