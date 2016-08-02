package com.optimizely.ab.android.shared;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by jdeffibaugh on 7/26/16 for Optimizely.
 *
 * Wrapper for {@link SharedPreferences}
 */
public class OptlyStorage {

    static final String PREFS_NAME = "optly";

    @NonNull private Context context;

    public OptlyStorage(@NonNull Context context) {
        this.context = context;
    }

    public void saveLong(String key, long value) {
        getWritablePrefs().putLong(key, value ).apply();
    }

    public long getLong(String key, long defaultValue) {
        return getReadablePrefs().getLong(key, defaultValue);
    }

    private SharedPreferences.Editor getWritablePrefs() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
    }

    private SharedPreferences getReadablePrefs() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
