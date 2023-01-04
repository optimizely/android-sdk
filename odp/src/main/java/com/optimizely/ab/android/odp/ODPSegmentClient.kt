// Copyright 2022, Optimizely, Inc. and contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.optimizely.ab.android.odp

import androidx.annotation.VisibleForTesting
import com.optimizely.ab.android.shared.Client
import org.slf4j.Logger
import java.net.HttpURLConnection
import java.net.URL

@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
open class ODPSegmentClient(private val client: Client, private val logger: Logger) {

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun fetchQualifiedSegments(
        apiKey: String,
        apiEndpoint: String,
        payload: String
    ): String? {

        val request: Client.Request<String> = Client.Request {
            var urlConnection: HttpURLConnection? = null
            try {
                val url = URL(apiEndpoint)
                urlConnection = client.openConnection(url)
                if (urlConnection == null) {
                    return@Request null
                }

                // set timeouts for releasing failed connections (default is 0 = no timeout).
                urlConnection.connectTimeout = CONNECTION_TIMEOUT
                urlConnection.readTimeout = READ_TIMEOUT

                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("x-api-key", apiKey)
                urlConnection.setRequestProperty("content-type", "application/json")

                urlConnection.doOutput = true
                val outputStream = urlConnection.outputStream
                outputStream.write(payload.toByteArray())
                outputStream.flush()
                outputStream.close()
                val status = urlConnection.responseCode
                if (status in 200..399) {
                    val json = client.readStream(urlConnection)
                    logger.debug("Successfully fetched segments: {}", json)
                    return@Request json
                } else {
                    logger.error("Unexpected response from event endpoint, status: $status")
                    return@Request null
                }
            } catch (e: Exception) {
                logger.error("Error making request", e)
                return@Request null
            } finally {
                if (urlConnection != null) {
                    try {
                        urlConnection.disconnect()
                    } catch (e: Exception) {
                        logger.error("Error closing connection", e)
                    }
                }
            }
        }

        return client.execute(request, REQUEST_BACKOFF_TIMEOUT, REQUEST_RETRIES_POWER)
    }

    companion object {
        // easy way to set the connection timeout
        var CONNECTION_TIMEOUT = 10 * 1000
        var READ_TIMEOUT = 60 * 1000

        // the numerical base for the exponential backoff
        const val REQUEST_BACKOFF_TIMEOUT = 2
        // power the number of retries (2 = retry once)
        const val REQUEST_RETRIES_POWER = 2
    }
}
