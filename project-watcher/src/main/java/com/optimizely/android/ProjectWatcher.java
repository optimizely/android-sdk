package com.optimizely.android;

import android.content.Context;

/**
 * Created by jdeffibaugh on 7/28/16 for Optimizely.
 *
 * Project Watcher interface
 */
public interface ProjectWatcher {
    void startWatching(Context context, OnDataFileLoadedListener onDataFileLoadedListener);

    void stopWatching(Context context);
}
