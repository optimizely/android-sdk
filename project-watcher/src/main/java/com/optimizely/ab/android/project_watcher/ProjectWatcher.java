package com.optimizely.ab.android.project_watcher;

import android.content.Context;

import java.util.concurrent.TimeUnit;

/**
 * Created by jdeffibaugh on 7/28/16 for Optimizely.
 *
 * Project Watcher interface
 */
public interface ProjectWatcher {
    void loadDataFile(Context context, OnDataFileLoadedListener onDataFileLoadedListener);

    void cancelDataFileLoad(Context context);

    void startWatching(Context context, TimeUnit timeUnit, long interval);

    void stopWatching(Context context);
}
