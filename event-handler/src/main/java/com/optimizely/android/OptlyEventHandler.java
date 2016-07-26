package com.optimizely.android;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.optimizely.ab.event.EventHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Created by jdeffibaugh on 7/21/16 for Optimizely.
 *
 * Reference implementation of {@link EventHandler} for Android.
 *
 * This is the main entry point to the Android Module
 */
public class OptlyEventHandler implements EventHandler {

    Logger logger = LoggerFactory.getLogger(OptlyEventHandler.class);

    @NonNull  private final Context context;

    public OptlyEventHandler(@NonNull Context context) {
        this.context = context;
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
            Event event = generateRequest(url, params);
            Intent intent = new Intent(context, EventHandlerService.class);
            intent.putExtra(EventHandlerService.EXTRA_STRING_URL, event.toString());
            context.startService(intent);
            logger.info("Sent url {} to the event handler service", event);
        } catch (MalformedURLException e) {
            logger.error("Received a malformed url from optly core", e);
        }
    }

    private Event generateRequest(@NonNull String url, @NonNull Map<String,String> params) throws MalformedURLException {
        url = url + "?";
        StringBuilder urlSb = new StringBuilder(url);
        for (Map.Entry<String, String> param : params.entrySet()) {
            urlSb.append(param.getKey());
            urlSb.append("=");
            urlSb.append(param.getValue());
            urlSb.append("&");
        }
        urlSb.deleteCharAt(urlSb.length() - 1);
        return new Event(new URL(urlSb.toString()));
    }
}
