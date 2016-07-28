package com.optimizely.android;

import android.support.annotation.Nullable;

/**
 * Created by jdeffibaugh on 7/28/16 for Optimizely.
 *
 * Project Watcher interface
 */
public interface ProjectWatcher {
    /**
     * Attempts to get a locally stored data file if it exists
     *
     * Calling this method will start the project watcher.  When a new version of
     * the data file is loaded it will be sent to {@link OnDataFileLoadedListener}
     * @return null if there is no cached data file and the data file if it exists
     */
    @Nullable String getDataFile();

    /**
     * Whether or not there is a cached data file
     *
     * Calling this method will not start project watching.
     * @return true if there is a cached data file otherwise false
     */
    boolean hasDataFile();
}
