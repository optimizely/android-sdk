package com.optimizely.ab.notification;

import com.optimizely.ab.event.LogEvent;

import javax.annotation.Nonnull;
import java.util.Map;

public interface TrackNotificationListenerInterface {
    /**
     * onTrack is called when a track event is triggered
     * @param eventKey - The event key that was triggered.
     * @param userId - user id passed into track.
     * @param attributes - filtered attributes list after passed into track
     * @param eventTags - event tags if any were passed in.
     * @param event - The event being recorded.
     */
    public void onTrack(@Nonnull String eventKey,
                                 @Nonnull String userId,
                                 @Nonnull Map<String, String> attributes,
                                 @Nonnull Map<String, ?>  eventTags,
                                 @Nonnull LogEvent event) ;

}
