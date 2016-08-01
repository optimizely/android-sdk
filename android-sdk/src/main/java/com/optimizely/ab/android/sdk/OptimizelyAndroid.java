package com.optimizely.ab.android.sdk;

import android.content.Context;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.android.event_handler.OptlyEventHandler;

public class OptimizelyAndroid {

    public static Optimizely newInstance(Context context, String dataFile) {
        EventHandler eventHandler = OptlyEventHandler.getInstance(context);
        return Optimizely.builder(dataFile, eventHandler).build();
    }
}