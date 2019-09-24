package com.optimizely.ab.fsc_app.bdd.support.listeners;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.fsc_app.bdd.support.responses.ListenerResponse;
import com.optimizely.ab.notification.ActivateNotificationListener;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

public class ActivateListener extends ActivateNotificationListener implements ListenerResponse {
    String experimentKey;
    String userId;
    Map<String, ?> attrib;
    String variationKey;

    @Override
    public void onActivate(@Nonnull Experiment experiment, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Variation variation, @Nonnull LogEvent logEvent) {
        experimentKey = experiment.getKey();
        this.userId = userId;
        attrib = attributes;
        variationKey = variation.getKey();
    }

    public Map getListenerObject() {
        Map<String, Object> listenerCallback = new HashMap<>();
        listenerCallback.put("experiment_key", experimentKey);
        listenerCallback.put("user_id", userId);
        listenerCallback.put("attributes", attrib);
        listenerCallback.put("variation_key", variationKey);

        return listenerCallback;
    }

}

