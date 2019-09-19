package com.optimizely.ab.android.test_app.bdd.support.listeners;

import com.optimizely.ab.android.test_app.bdd.support.OptimizelyWrapper;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.notification.TrackNotificationListener;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

public class TrackListener extends TrackNotificationListener implements OptimizelyWrapper.ListenerResponse {
    String eventKey = null;
    String userId = null;
    Map<String, ?> attr = null;
    Map<String, ?> eventTags = null;

    @Override
    public void onTrack(@Nonnull String eventKey, @Nonnull String userId, @Nonnull Map<String, ?> attrib, @Nonnull Map<String, ?> eventTags, @Nonnull LogEvent logEvent) {
        this.eventKey = eventKey;
        this.userId = userId;
        this.attr = attrib;
        this.eventTags = eventTags;
    }

    public Map getListenerObject() {
        Map<String, Object> listenerCallback = new HashMap<>();
        listenerCallback.put("event_key", eventKey);
        listenerCallback.put("user_id", userId);
        listenerCallback.put("attributes", attr);
        listenerCallback.put("event_tags", eventTags);

        return listenerCallback;
    }

}
