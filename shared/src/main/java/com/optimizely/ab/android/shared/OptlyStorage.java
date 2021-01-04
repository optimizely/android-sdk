/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.android.shared;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

/**
 * Wrapper for {@link SharedPreferences}
 * @hide
 */
public class OptlyStorage {

    private static final String PREFS_NAME = "optly";

    @NonNull private Context context;

    /**
     * Creates a new instance of {@link OptlyStorage}
     *
     * @param context any instance of {@link Context}
     * @hide
     */
    public OptlyStorage(@NonNull Context context) {
        this.context = context;
    }

    /**
     * Save a long value
     *
     * @param key   a String key
     * @param value a long value
     * @hide
     */
    public void saveLong(String key, long value) {
        getWritablePrefs().putLong(key, value ).apply();
    }

    /**
     * Get a long value
     * @param key a String key
     * @param defaultValue the value to return if the key isn't stored
     * @return the long value
     * @hide
     */
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
