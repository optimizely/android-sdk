package com.optimizely.android;

import android.content.Context;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.event.EventHandler;

public class OptlyAndroid {

    public static Optimizely newInstance(Context context, String dataFile) {
        EventHandler eventHandler = OptlyEventHandler.getInstance(context);
        return Optimizely.builder(dataFile, eventHandler).build();
    }
}