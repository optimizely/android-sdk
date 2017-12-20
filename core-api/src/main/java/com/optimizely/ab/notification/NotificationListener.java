/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
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

import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Abstract class for Optimizely notification listeners.
 * <p>
 * We use an abstract class here instead of an interface for convenience of use and backwards
 * compatibility in the future. An interface would force consumers to implement every method defined
 * on it as well as update their application code with new method implementations every time new
 * methods are added to the interface in the SDK. An abstract classes allows consumers to override
 * just the methods they need.
 */
public abstract class NotificationListener {

    /**
     * Listener that is called after an event is tracked.
     *
     * @param eventKey the key of the tracked event
     * @param userId the ID of the user
     * @param attributes a map of attributes about the event
     * @param eventValue an integer to be aggregated for the event
     * @param logEvent the log event sent to the event dispatcher
     */
    @Deprecated
    public void onEventTracked(@Nonnull String eventKey,
                               @Nonnull String userId,
                               @Nonnull Map<String, String> attributes,
                               @CheckForNull Long eventValue,
                               @Nonnull LogEvent logEvent) {
    }

    /**
     * Listener that is called after an experiment has been activated.
     *
     * @param experiment the activated experiment
     * @param userId the id of the user
     * @param attributes a map of attributes about the user
     * @param variation the key of the variation that was bucketed
     */
    @Deprecated
    public void onExperimentActivated(@Nonnull Experiment experiment,
                                      @Nonnull String userId,
                                      @Nonnull Map<String, String> attributes,
                                      @Nonnull Variation variation) {
    }

    /**
     * This is the new method of notification.  Implementation classes such as {@link com.optimizely.ab.notification.ActivateNotification}
     * will implement this call and provide another method with the correct parameters
     * Notify called when a notification is triggered via the {@link com.optimizely.ab.notification.NotificationCenter}
     * @param args - variable argument list based on the type of notification.
     */
    public abstract void notify(Object... args);
}
