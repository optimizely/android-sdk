/****************************************************************************
 * Copyright 2016-2019, Optimizely, Inc. and contributors                   *
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
import com.optimizely.ab.internal.ReservedEventKey;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.notification.ActivateNotificationListener;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.notification.TrackNotificationListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.exceptions.verification.junit.ArgumentsAreDifferent;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OptimizelyClient}
 */
@RunWith(MockitoJUnitRunner.class)
public class OptimizelyClientTest {

    @Mock Logger logger;
    @Mock Optimizely optimizely;
    @Mock NotificationCenter notificationCenter;

    @Before
    public void setup() {
        Field field = null;
        try {
            field = Optimizely.class.getDeclaredField("notificationCenter");
            // Mark the field as public so we can toy with it
            field.setAccessible(true);
// Get the Modifiers for the Fields
            Field modifiersField = Field.class.getDeclaredField("modifiers");
// Allow us to change the modifiers
            modifiersField.setAccessible(true);
            // Remove final modifier from field by blanking out the bit that says "FINAL" in the Modifiers
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
// Set new value
            field.set(optimizely, notificationCenter);

            when(optimizely.isValid()).thenReturn(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test(expected=ArgumentsAreDifferent.class)
    public void testGoodActivation1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        optimizelyClient.activate("1", "1");
        verify(optimizely).activate("1", "1");
    }

    @Test
    public void testBadActivation1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.activate("1", "1");
        verify(logger).warn("Optimizely is not initialized, could not activate experiment {} " +
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
        verify(logger).warn("Optimizely is not initialized, could not activate experiment {} " +
                "for user {} with attributes", "1", "1");
    }

    @Test(expected=ArgumentsAreDifferent.class)
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
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.track("event1", "1", attributes, Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L));
        Map<String, String> defaultAttributes = new HashMap<>();
        verify(optimizely).track("event1", "1", defaultAttributes, Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L));    }

    @Test
    public void testBadTrack4() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.track("event1", "1", attributes, Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L));
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}" +
                " with attributes and event tags", "event1", "1");
    }

    @Test
    public void testTrackWithEventTags() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        attributes.put("foo", "bar");
        final HashMap<String, Object> eventTags = new HashMap<>();
        eventTags.put("foo", 843);
        optimizelyClient.track("event1", "1", attributes, eventTags);
        verify(optimizely).track("event1", "1", attributes, eventTags);
    }

    @Test(expected=ArgumentsAreDifferent.class)
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
    public void testGoodGetProjectConfig() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        optimizelyClient.getProjectConfig();
        verify(optimizely).getProjectConfig();
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

    //======== Notification listeners ========//

    @Test
    public void testNewGoodAddNotificationCenterListener() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);

        ActivateNotificationListener listener = new ActivateNotificationListener() {
            @Override
            public void onActivate(Experiment experiment,String userId, Map<String, ?> attributes, Variation variation, LogEvent event) {

            }
        };

        int notificationId = optimizelyClient.getNotificationCenter().addNotificationListener(NotificationCenter.NotificationType.Activate, listener);
        verify(optimizely.notificationCenter).addNotificationListener(NotificationCenter.NotificationType.Activate, listener);
    }

    @Test
    public void testBadAddNotificationCenterListener() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        ActivateNotificationListener listener = new ActivateNotificationListener() {
            @Override
            public void onActivate(Experiment experiment, String userId, Map<String, ?> attributes, Variation variation, LogEvent event) {

            }
        };
        optimizelyClient.getNotificationCenter().addNotificationListener(NotificationCenter.NotificationType.Activate, listener);
        verify(optimizely.notificationCenter).addNotificationListener(NotificationCenter.NotificationType.Activate, listener);
    }

    @Test
    public void testGoodRemoveNotificationCenterListener() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        TrackNotificationListener listener = new TrackNotificationListener() {
            @Override
            public void onTrack( String eventKey, String userId, Map<String, ?> attributes, Map<String, ?> eventTags, LogEvent event) {

            }
        };
        int note = optimizelyClient.getNotificationCenter().addNotificationListener(NotificationCenter.NotificationType.Track, listener);
        optimizelyClient.getNotificationCenter().removeNotificationListener(note);
        verify(optimizely.notificationCenter).removeNotificationListener(note);
    }

    @Test
    public void testBadRemoveNotificationCenterListener() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        NotificationCenter notificationCenter = optimizelyClient.getNotificationCenter() != null ?
                optimizelyClient.getNotificationCenter() : new NotificationCenter();
        notificationCenter.removeNotificationListener(1);
        verify(logger).warn("Optimizely is not initialized, could not get the notification listener");
    }

    @Test
    public void testGoodClearNotificationCenterListeners() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        optimizelyClient.getNotificationCenter().clearAllNotificationListeners();
        verify(optimizely.notificationCenter).clearAllNotificationListeners();
    }

    @Test
    public void testBadClearNotificationCenterListeners() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        NotificationCenter notificationCenter = optimizelyClient.getNotificationCenter() != null ?
                optimizelyClient.getNotificationCenter() : new NotificationCenter();
        notificationCenter.clearAllNotificationListeners();
        verify(logger).warn("Optimizely is not initialized, could not get the notification listener");
    }
}
