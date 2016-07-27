package com.optimizely.android;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.optimizely.ab.event.EventHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by jdeffibaugh on 7/21/16 for Optimizely.
 *
 * Reference implementation of {@link EventHandler} for Android.
 *
 * This is the main entry point to the Android Module
 */
public class OptlyEventHandler implements EventHandler {

    Logger logger = LoggerFactory.getLogger(OptlyEventHandler.class);

    @NonNull private final Context context;
    private long flushInterval = -1;

    /**
     * Constructs a new instance
     * @param context any valid Android {@link Context}
     */
    public OptlyEventHandler(@NonNull Context context) {
        this.context = context;
    }

    /**
     * Sets event flushing interval
     *
     * Events will only be scheduled to flush as long as events remain in storage.
     *
     * Events are put into storage when they fail to send over network.
     * @param timeUnit a {@link TimeUnit}
     * @param flushInterval the interval in the provided {@link TimeUnit}
     */
    private void setFlushInterval(TimeUnit timeUnit, long flushInterval) {
        this.flushInterval = timeUnit.toMillis(flushInterval);
    }

    @Override
    public void dispatchEvent(String url, Map<String, String> params) {
        if (url == null) {
            logger.error("Event dispatcher received a null url");
            return;
        }
        if (params == null) {
            logger.error("Event dispatcher received a null params map");
            return;
        }

        try {
            String event = generateRequest(url, params);
            Intent intent = new Intent(context, EventIntentService.class);
            intent.putExtra(EventIntentService.EXTRA_URL, event);
            if (flushInterval != -1) {
                intent.putExtra(EventIntentService.EXTRA_DURATION, flushInterval);
            }
            context.startService(intent);
            logger.info("Sent url {} to the event handler service", event);
        } catch (MalformedURLException e) {
            logger.error("Received a malformed url from optly core", e);
        }
    }

    private String generateRequest(@NonNull String url, @NonNull Map<String,String> params) throws MalformedURLException {
        url = url + "?";
        StringBuilder urlSb = new StringBuilder(url);
        for (Map.Entry<String, String> param : params.entrySet()) {
            urlSb.append(param.getKey());
            urlSb.append("=");
            urlSb.append(param.getValue());
            urlSb.append("&");
        }
        urlSb.deleteCharAt(urlSb.length() - 1);
        return urlSb.toString();
    }
}
