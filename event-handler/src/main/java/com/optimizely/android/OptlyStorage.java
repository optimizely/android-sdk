package com.optimizely.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

/**
 * Created by jdeffibaugh on 7/26/16 for Optimizely.
 *
 * Wrapper for {@link android.content.SharedPreferences}
 */
public class OptlyStorage {

    static final String PREFS_NAME = "optly";
    static final String KEY_FLUSH_EVENTS_INTERVAl = "flushEventsInterval";

    @NonNull private Context context;

    public OptlyStorage(@NonNull Context context) {
        this.context = context;
    }

    public void saveLong(String key, long value) {
        getWritablePrefs().putLong(KEY_FLUSH_EVENTS_INTERVAl, value ).apply();
    }

    public long getLong(String key, long defaultValue) {
        return getReadablePrefs().getLong(key, defaultValue);
    }

    private SharedPreferences.Editor getWritablePrefs() {
        return context.getSharedPreferences(KEY_FLUSH_EVENTS_INTERVAl, Context.MODE_PRIVATE).edit();
    }

    private SharedPreferences getReadablePrefs() {
        return context.getSharedPreferences(KEY_FLUSH_EVENTS_INTERVAl, Context.MODE_PRIVATE);
    }
}
