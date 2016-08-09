package com.optimizely.ab.android.sdk;

import android.content.Context;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.bucketing.PersistentBucketer;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.android.event_handler.OptlyEventHandler;
import com.optimizely.persistent_bucketer.AndroidPersistentBucketer;

public class OptimizelyAndroid {

    public static Optimizely newInstance(Context context, String dataFile) {
        EventHandler eventHandler = OptlyEventHandler.getInstance(context);
        PersistentBucketer persistentBucketer = AndroidPersistentBucketer.newInstance(context);
        return Optimizely.builder(dataFile, eventHandler)
                .withPersistentBucketer(persistentBucketer)
                .build();
    }
}