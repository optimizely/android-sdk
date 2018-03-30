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
 * An interface class for Optimizely notification listeners.
 * <p>
 * We changed this from a abstract class to a interface to support lambdas moving forward in Java 8 and beyond.
 */
public interface NotificationListener {

    /**
     * This is the base method of notification.  Implementation classes such as {@link ActivateNotificationListener}
     * will implement this call and provide another method with the correct parameters
     * Notify called when a notification is triggered via the {@link com.optimizely.ab.notification.NotificationCenter}
     * @param args - variable argument list based on the type of notification.
     */
    public void notify(Object... args);
}
