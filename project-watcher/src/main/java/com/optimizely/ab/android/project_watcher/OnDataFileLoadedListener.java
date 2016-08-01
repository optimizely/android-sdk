package com.optimizely.ab.android.project_watcher;

/**
 * Created by jdeffibaugh on 7/28/16 for Optimizely.
 *
 * Interface that is notified on data file loading by {@link ProjectWatcher}
 */
public interface OnDataFileLoadedListener {
    void onDataFileLoaded(String dataFile);
}
