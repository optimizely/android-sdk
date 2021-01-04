/****************************************************************************
 * Copyright 2016-2017, Optimizely, Inc. and contributors                   *
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Functionality common to all caches.  This is a simple cache class that takes a filename and saves string data
 * to that file.  It can then use that file name to load the data.
 *
 */
public class Cache {

    @NonNull private final Context context;
    @NonNull private final Logger logger;

    /**
     * Create new instance of {@link Cache}.
     *
     * @param context any {@link Context instance}
     * @param logger  a {@link Logger} instances
     */
    public Cache(@NonNull Context context, @NonNull Logger logger) {
        this.context = context;
        this.logger = logger;
    }

    /**
     * Delete the cache file.
     *
     * @param filename the path to the file
     * @return true if the file was deleted or false otherwise
     */
    public boolean delete(String filename) {
        return context.deleteFile(filename);
    }

    /**
     * Check if the cache file exists.
     *
     * @param filename the path to the file
     * @return true if the file exists or false otherwise
     */
    public boolean exists(String filename) {
        String[] files = context.fileList();
        return files != null && Arrays.asList(files).contains(filename);
    }

    /**
     * Load data from the cache file.
     *
     * @param filename the path to the file
     * @return the loaded cache file as String or null if the file cannot be loaded
     */
    @Nullable
    public String load(String filename) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = context.openFileInput(filename);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            logger.warn("Unable to load file {}.", filename);
            return null;
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (Exception e) {
                logger.warn("Unable to close file {}.", filename, e);
            }
        }
    }

    /**
     * Save data to the cache file and overwrite any existing data.
     *
     * @param filename the path to the file
     * @param data     the String data to write to the file
     * @return true if the file was saved
     */
    public boolean save(String filename, String data) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            fileOutputStream.write(data.getBytes());
            return true;
        } catch (Exception e) {
            logger.error("Error saving file {}.", filename);
            return false;
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (Exception e) {
                    logger.warn("Unable to close file {}.", filename, e);

                }
            }
        }
    }
}
