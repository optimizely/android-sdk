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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

/**
 * Functionality common to all caches
 * @hide
 */
public class Cache {

    @NonNull private final Context context;
    @NonNull private final Logger logger;

    /**
     * Create new instances of {@link Cache}
     *
     * @param context any {@link Context instance}
     * @param logger  a {@link Logger} instances
     * @hide
     */
    public Cache(@NonNull Context context, @NonNull Logger logger) {
        this.context = context;
        this.logger = logger;
    }

    /**
     * Load the cache file
     *
     * @param fileName the path to the file
     * @return the loaded cache file as String or null if the file cannot be loaded
     * @hide
     */
    @Nullable
    public String load(String fileName) {
        try {
            FileInputStream fis = context.openFileInput(fileName);
            InputStreamReader inputStreamReader = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            logger.warn("Unable to load file {}.", fileName);
            return null;
        }
    }

    /**
     * Delete the cache file
     * @param fileName the path to the file
     * @return true if the file was deleted or false otherwise
     * @hide
     */
    public boolean delete(String fileName) {
        return context.deleteFile(fileName);
    }

    /**
     * Check if the cache file exists
     * @param fileName the path to the file
     * @return true if the file exists or false otherwise
     * @hide
     */
    public boolean exists(String fileName) {
        String file = load(fileName);
        return file != null;
    }

    /**
     * Save the existing cache file with new data
     *
     * The original file will be overwritten.
     * @param fileName the path to the file
     * @param data the String data to write to the file
     * @return true if the file was saved
     * @hide
     */
    public boolean save(String fileName, String data) {
        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(data.getBytes());
            return true;
        } catch (Exception e) {
            logger.error("Error saving file {}.", fileName);
        }
        return false;
    }
}
