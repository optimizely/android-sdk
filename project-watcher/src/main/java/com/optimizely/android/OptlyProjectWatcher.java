package com.optimizely.android;

import android.content.Context;
import android.support.annotation.Nullable;

/**
 * Created by jdeffibaugh on 7/28/16 for Optimizely.
 *
 * Handles loading the Optimizely data file
 */
public class OptlyProjectWatcher implements ProjectWatcher {

    private Context context;
    private OnDataFileLoadedListener onDataFileLoadedListener;

    public static ProjectWatcher newInstance(Context context, OnDataFileLoadedListener onDataFileLoadedListener) {
        return new OptlyProjectWatcher(context, onDataFileLoadedListener);
    }

    private OptlyProjectWatcher(Context context, OnDataFileLoadedListener onDataFileLoadedListener) {
        this.context = context;
        this.onDataFileLoadedListener = onDataFileLoadedListener;
    }

    @Override
    @Nullable
    public String getDataFile() {
        return null;
    }

    @Override
    public boolean hasDataFile() {
        return false;
    }
}
