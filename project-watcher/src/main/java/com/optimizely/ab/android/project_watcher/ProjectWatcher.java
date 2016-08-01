package com.optimizely.ab.android.project_watcher;

import android.content.Context;

import java.util.concurrent.TimeUnit;

/**
 * Created by jdeffibaugh on 7/28/16 for Optimizely.
 *
 * Project Watcher interface
 */
public interface ProjectWatcher {
    void startWatching(Context context, OnDataFileLoadedListener onDataFileLoadedListener);

    void stopWatching(Context context);

    void startWatchingInBackground(Context context, TimeUnit timeUnit, long interval);

    void stopWatchingInBackground(Context context);
}
