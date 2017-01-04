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

import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Manages Optimizely SDK notification listeners and broadcasts messages.
 */
public class NotificationBroadcaster {

    private static final Logger logger = LoggerFactory.getLogger(NotificationBroadcaster.class);

    @VisibleForTesting final HashSet<NotificationListener> listeners =
            new HashSet<NotificationListener>();

    /**
     * Add a listener if it does not exist already.
     *
     * @param listener listener to add
     */
    public void addListener(@Nonnull NotificationListener listener) {
        if (listeners.contains(listener)) {
            logger.debug("Notification listener was not added because it already existed");
            return;
        }

        listeners.add(listener);
        logger.debug("Notification listener was added");
    }

    /**
     * Remove a listener if it exists.
     *
     * @param listener listener to remove
     */
    public void removeListener(@Nonnull NotificationListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
            logger.debug("Notification listener was removed");
            return;
        }

        logger.debug("Notification listener was not removed because it did not exist");
    }

    /**
     * Remove all listeners.
     */
    public void clearListeners() {
        listeners.clear();
        logger.debug("Notification listeners were cleared");
    }

    /**
     * Notify listeners that an Optimizely event has been tracked.
     *
     * @param eventKey the key of the tracked event
     * @param userId the ID of the user
     * @param attributes a map of attributes about the event
     * @param eventValue an integer to be aggregated for the event
     * @param logEvent the log event sent to the event dispatcher
     */
    public void broadcastEventTracked(@Nonnull String eventKey,
                                      @Nonnull String userId,
                                      @Nonnull Map<String, String> attributes,
                                      @CheckForNull Long eventValue,
                                      @Nonnull LogEvent logEvent) {
        for (final NotificationListener iterListener : listeners) {
            iterListener.onEventTracked(eventKey, userId, attributes, eventValue, logEvent);
        }
    }

    /**
     * Notify listeners that an Optimizely experiment has been activated.
     *
     * @param experiment the key of the activated experiment
     * @param userId the id of the user
     * @param attributes a map of attributes about the user
     * @param variation the key of the variation that was bucketed
     */
    public void broadcastExperimentActivated(@Nonnull Experiment experiment,
                                             @Nonnull String userId,
                                             @Nonnull Map<String, String> attributes,
                                             @Nonnull Variation variation) {
        for (final NotificationListener iterListener : listeners) {
            iterListener.onExperimentActivated(experiment, userId, attributes, variation);
        }
    }
}
