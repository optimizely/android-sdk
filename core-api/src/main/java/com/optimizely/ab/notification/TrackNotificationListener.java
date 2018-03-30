/**
 *
 *    Copyright 2017, Optimizely and contributors
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

import java.util.Map;
import javax.annotation.Nonnull;

import com.optimizely.ab.event.LogEvent;

/**
 * This class handles the track event notification.
 */
public abstract class TrackNotificationListener implements NotificationListener, TrackNotificationListenerInterface {
    /**
     * Base notify called with var args.  This method parses the parameters and calls the abstract method.
     * @param args - variable argument list based on the type of notification.
     */
    @Override
    public final void notify(Object... args) {
        assert(args[0] instanceof String);
        String eventKey = (String) args[0];
        assert(args[1] instanceof String);
        String userId = (String) args[1];
        assert(args[2] instanceof java.util.Map);
        Map<String, String> attributes = (Map<String, String>) args[2];
        assert(args[3] instanceof java.util.Map);
        Map<String, ?> eventTags = (Map<String, ?>) args[3];
        assert(args[4] instanceof LogEvent);
        LogEvent logEvent = (LogEvent) args[4];

        onTrack(eventKey, userId,attributes,eventTags, logEvent);
    }

    /**
     * onTrack is called when a track event is triggered
     * @param eventKey - The event key that was triggered.
     * @param userId - user id passed into track.
     * @param attributes - filtered attributes list after passed into track
     * @param eventTags - event tags if any were passed in.
     * @param event - The event being recorded.
     */
    public abstract void onTrack(@Nonnull String eventKey,
                          @Nonnull String userId,
                          @Nonnull Map<String, String> attributes,
                          @Nonnull Map<String, ?>  eventTags,
                          @Nonnull LogEvent event) ;
}
