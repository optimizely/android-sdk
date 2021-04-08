/****************************************************************************
 * Copyright 2016-2017, Optimizely, Inc. and contributors                        *
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

import org.slf4j.Logger
import java.io.BufferedInputStream
import java.io.InputStream
import java.lang.Boolean
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Functionality common to all clients using http connections
 */
class Client
/**
 * Constructs a new Client instance
 *
 * @param optlyStorage an instance of [OptlyStorage]
 * @param logger       an instance of [Logger]
 */(private val optlyStorage: OptlyStorage, private val logger: Logger?) {
    /**
     * Opens [HttpURLConnection] from a [URL]
     *
     * @param url a [URL] instance
     * @return an open [HttpURLConnection]
     */
    fun openConnection(url: URL): HttpURLConnection? {
        try {
            return url.openConnection() as HttpURLConnection
        } catch (e: Exception) {
            logger?.warn("Error making request to {}.", url)
        }
        return null
    }

    /**
     * Adds a if-modified-since header to the open [URLConnection] if this value is
     * stored in [OptlyStorage].
     * @param urlConnection an open [URLConnection]
     */
    fun setIfModifiedSince(urlConnection: URLConnection) {
        if (urlConnection == null || urlConnection.url == null) {
            logger?.error("Invalid connection")
            return
        }
        val lastModified = optlyStorage.getLong(urlConnection.url.toString(), 0)
        if (lastModified > 0) {
            urlConnection.ifModifiedSince = lastModified
        }
    }

    /**
     * Retrieves the last-modified head from a [URLConnection] and saves it
     * in [OptlyStorage].
     * @param urlConnection a [URLConnection] instance
     */
    fun saveLastModified(urlConnection: URLConnection) {
        if (urlConnection == null || urlConnection.url == null) {
            logger?.error("Invalid connection")
            return
        }
        val lastModified = urlConnection.lastModified
        if (lastModified > 0) {
            optlyStorage.saveLong(urlConnection.url.toString(), urlConnection.lastModified)
        } else {
            logger?.warn("CDN response didn't have a last modified header")
        }
    }

    fun readStream(urlConnection: URLConnection): String? {
        var scanner: Scanner? = null
        return try {
            val `in`: InputStream = BufferedInputStream(urlConnection.getInputStream())
            scanner = Scanner(`in`).useDelimiter("\\A")
            if (scanner.hasNext()) scanner.next() else ""
        } catch (e: Exception) {
            logger?.warn("Error reading urlConnection stream.", e)
            null
        } finally {
            if (scanner != null) {
                // We assume that closing the scanner will close the associated input stream.
                try {
                    scanner.close()
                } catch (e: Exception) {
                    logger?.error("Problem with closing the scanner on a input stream", e)
                }
            }
        }
    }

    /**
     * Executes a request with exponential backoff
     * @param request the request executable, would be a lambda on Java 8
     * @param timeout the numerical base for the exponential backoff
     * @param power the number of retries
     * @param <T> the response type of the request
     * @return the response
    </T> */
    fun <T> execute(request: Request<*>, timeout: Int, power: Int): T? {
        var timeout = timeout
        val baseTimeout = timeout
        val maxTimeout = Math.pow(baseTimeout.toDouble(), power.toDouble()).toInt()
        var response: T? = null
        while (timeout <= maxTimeout) {
            try {
                response = request.execute() as T?
            } catch (e: Exception) {
                logger?.error("Request failed with error: ", e)
            }
            timeout = if (response == null || response === Boolean.FALSE) {
                try {
                    logger?.info("Request failed, waiting {} seconds to try again", timeout)
                    Thread.sleep(TimeUnit.MILLISECONDS.convert(timeout.toLong(), TimeUnit.SECONDS))
                } catch (e: InterruptedException) {
                    logger?.warn("Exponential backoff failed", e)
                    break
                }
                timeout * baseTimeout
            } else {
                break
            }
        }
        return response
    }

    /**
     * Bundles up a request allowing it's execution to be deferred
     * @param <T> The response type of the request
    </T> */
    interface Request<T> {
        fun execute(): T
    }

    companion object {
        val MAX_BACKOFF_TIMEOUT = Math.pow(2.0, 5.0).toInt()
    }
}