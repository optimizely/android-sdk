/*
 *    Copyright 2017, Optimizely
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
package com.optimizely.ab.notification;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link NotificationBroadcaster}.
 */
public class NotificationBroadcasterTest {

    private NotificationBroadcaster notificationBroadcaster;
    private NotificationListener listener;
    private NotificationListener listener2;

    @Before
    public void initialize() {
        notificationBroadcaster = new NotificationBroadcaster();
        listener = mock(NotificationListener.class);
        listener2 = mock(NotificationListener.class);
    }

    /**
     * Verify that {@link NotificationBroadcaster#addListener(NotificationListener)} correctly adds
     * a new listener.
     */
    @Test
    public void addListener() throws Exception {
        notificationBroadcaster.addListener(listener);
        assertEquals("addListener did not add the listener",
                1, notificationBroadcaster.listeners.size());
    }

    /**
     * Verify that {@link NotificationBroadcaster#addListener(NotificationListener)} does not add a
     * listener that has already been added.
     */
    @Test
    public void addListenerAlreadyExists() throws Exception {
        notificationBroadcaster.addListener(listener);
        notificationBroadcaster.addListener(listener);
        assertEquals("addListener did not add the listener just once",
                1, notificationBroadcaster.listeners.size());
    }

    /**
     * Verify that {@link NotificationBroadcaster#removeListener(NotificationListener)} removes a
     * listener that has been added.
     */
    @Test
    public void removeListener() throws Exception {
        notificationBroadcaster.addListener(listener);
        notificationBroadcaster.removeListener(listener);
        assertEquals("removeListener did not remove the listener",
                0, notificationBroadcaster.listeners.size());
    }

    /**
     * Verify that {@link NotificationBroadcaster#removeListener(NotificationListener)} does not
     * remove a listener that was not added.
     */
    @Test
    public void removeListenerNotAdded() throws Exception {
        notificationBroadcaster.addListener(listener);
        notificationBroadcaster.removeListener(listener2);
        assertEquals("removeListener did not remove just the listener that was added",
                1, notificationBroadcaster.listeners.size());
    }

    /**
     * Verify that {@link NotificationBroadcaster#clearListeners()} removes all listeners.
     */
    @Test
    public void clearListeners() throws Exception {
        notificationBroadcaster.addListener(listener);
        notificationBroadcaster.addListener(listener2);
        notificationBroadcaster.clearListeners();
        assertEquals("clearListeners did not remove all listeners that were added",
                0, notificationBroadcaster.listeners.size());
    }

    /**
     * Verify that {@link NotificationBroadcaster#broadcastEventTracked(String, String, Map, Long, LogEvent)}
     * notifies all listeners.
     */
    @Test
    public void broadcastEventTracked() throws Exception {
        notificationBroadcaster.addListener(listener);
        notificationBroadcaster.addListener(listener2);

        String eventKey = "event1";
        String userId = "user1";
        Map<String, String> attributes = Collections.emptyMap();
        Long eventValue = 0L;
        LogEvent logEvent = mock(LogEvent.class);
        notificationBroadcaster.broadcastEventTracked(
                eventKey, userId, attributes, eventValue, logEvent);
        verify(listener).onEventTracked(eventKey, userId, attributes, eventValue, logEvent);
        verify(listener2).onEventTracked(eventKey, userId, attributes, eventValue, logEvent);
    }

    /**
     * Verify that {@link NotificationBroadcaster#broadcastExperimentActivated(Experiment, String, Map, Variation)}
     * notifies all listeners.
     */
    @Test
    public void broadcastExperimentActivated() throws Exception {
        notificationBroadcaster.addListener(listener);
        notificationBroadcaster.addListener(listener2);

        Experiment experiment = mock(Experiment.class);
        String userId = "user1";
        Map<String, String> attributes = Collections.emptyMap();
        Variation variation = mock(Variation.class);
        notificationBroadcaster.broadcastExperimentActivated(
                experiment, userId, attributes, variation);
        verify(listener).onExperimentActivated(experiment, userId, attributes, variation);
        verify(listener2).onExperimentActivated(experiment, userId, attributes, variation);
    }
}
