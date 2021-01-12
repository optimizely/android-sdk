/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                            *
 * *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.android.shared

import android.content.Context
import android.content.SharedPreferences

/**
 * Wrapper for [SharedPreferences]
 * @hide
 */
class OptlyStorage
/**
 * Creates a new instance of [OptlyStorage]
 *
 * @param context any instance of [Context]
 * @hide
 */ constructor(private val context: Context) {
    /**
     * Save a long value
     *
     * @param key   a String key
     * @param value a long value
     * @hide
     */
    fun saveLong(key: String?, value: Long) {
        writablePrefs.putLong(key, value).apply()
    }

    /**
     * Get a long value
     * @param key a String key
     * @param defaultValue the value to return if the key isn't stored
     * @return the long value
     * @hide
     */
    fun getLong(key: String?, defaultValue: Long): Long {
        return readablePrefs.getLong(key, defaultValue)
    }

    private val writablePrefs: SharedPreferences.Editor
        private get() {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        }
    private val readablePrefs: SharedPreferences
        private get() {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

    companion object {
        private val PREFS_NAME: String = "optly"
    }
}