/****************************************************************************
 * Copyright 2016-2017, Optimizely, Inc. and contributors                   *
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
import org.slf4j.Logger
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.*

/**
 * Functionality common to all caches.  This is a simple cache class that takes a filename and saves string data
 * to that file.  It can then use that file name to load the data.
 *
 */
class Cache
/**
 * Create new instance of [Cache].
 *
 * @param context any [instance][Context]
 * @param logger  a [Logger] instances
 */(private val context: Context, private val logger: Logger) {
    /**
     * Delete the cache file.
     *
     * @param filename the path to the file
     * @return true if the file was deleted or false otherwise
     */
    fun delete(filename: String?): Boolean {
        return context.deleteFile(filename)
    }

    /**
     * Check if the cache file exists.
     *
     * @param filename the path to the file
     * @return true if the file exists or false otherwise
     */
    fun exists(filename: String): Boolean {
        val files = context.fileList()
        return files != null && Arrays.asList(*files).contains(filename)
    }

    /**
     * Load data from the cache file.
     *
     * @param filename the path to the file
     * @return the loaded cache file as String or null if the file cannot be loaded
     */
    fun load(filename: String?): String? {
        var fileInputStream: FileInputStream? = null
        return try {
            fileInputStream = context.openFileInput(filename)
            val inputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            val sb = StringBuilder()
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            sb.toString()
        } catch (e: Exception) {
            logger.warn("Unable to load file {}.", filename)
            null
        } finally {
            try {
                fileInputStream?.close()
            } catch (e: Exception) {
                logger.warn("Unable to close file {}.", filename, e)
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
    fun save(filename: String?, data: String): Boolean {
        var fileOutputStream: FileOutputStream? = null
        return try {
            fileOutputStream = context.openFileOutput(filename, Context.MODE_PRIVATE)
            fileOutputStream.write(data.toByteArray())
            true
        } catch (e: Exception) {
            logger.error("Error saving file {}.", filename)
            false
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close()
                } catch (e: Exception) {
                    logger.warn("Unable to close file {}.", filename, e)
                }
            }
        }
    }
}