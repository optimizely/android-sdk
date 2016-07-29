package com.optimizely.android;

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

    public void saveString(String key, String value) {
        getWritablePrefs().putString(key, value).apply();
    }

    public boolean getBoolean(String key) {
        return getReadablePrefs().getBoolean(key, false);
    }

    public void saveBoolean(String key, boolean value) {
        getWritablePrefs().putBoolean(key, value).apply();
    }

    @Nullable
    public String getString(String key) {
        getReadablePrefs().getString(key, null);
    }

    private SharedPreferences.Editor getWritablePrefs() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
    }

    private SharedPreferences getReadablePrefs() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
