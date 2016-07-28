package com.optimizely.android;

import android.content.Context;

/**
 * Created by jdeffibaugh on 7/28/16 for Optimizely.
 *
 * Abstracts the actual data "file" {@link java.io.File}
 */
public class DataFileCache {

    private Context context;

    public DataFileCache(Context context) {
        this.context = context;
    }

    String load() {
        return "";
    }

    boolean delete() {
        return false;
    }

    boolean save(String dataFile) {
        return false;
    }
}
