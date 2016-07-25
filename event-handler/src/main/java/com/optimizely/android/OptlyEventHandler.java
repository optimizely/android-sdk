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
 */
public class OptlyEventHandler implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(OptlyEventHandler.class);

    @NonNull  private final Context context;

    public OptlyEventHandler(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public void dispatchEvent(String urlString, Map<String, String> params) {
        if (urlString == null) return;
        if (params == null) return;

        try {
            URL url = generateRequest(urlString, params);
            Intent intent = new Intent(context, EventHandlerService.class);
            intent.putExtra(EventHandlerService.EXTRA_URL, url.toString());
        } catch (MalformedURLException e) {
            logger.error("Received a malformed URL from optly core", e);
        }
    }

    URL generateRequest(@NonNull String url, @NonNull Map<String,String> params) throws MalformedURLException {
        url = url + "?";
        StringBuilder urlSb = new StringBuilder(url);
        for (Map.Entry<String, String> param : params.entrySet()) {
            urlSb.append(param.getKey());
            urlSb.append("=");
            urlSb.append(param.getValue());
            urlSb.append("&");
        }
        urlSb.deleteCharAt(urlSb.length() - 1);
        return new URL(urlSb.toString());
    }
}
