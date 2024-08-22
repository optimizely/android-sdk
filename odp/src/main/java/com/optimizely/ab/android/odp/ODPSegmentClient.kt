// Copyright 2022-2023, Optimizely, Inc. and contributors
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
import com.optimizely.ab.android.shared.ClientForODPOnly
import com.optimizely.ab.odp.parser.ResponseJsonParser
import com.optimizely.ab.odp.parser.ResponseJsonParserFactory
import org.slf4j.Logger
import java.net.HttpURLConnection
import java.net.URL

@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
open class ODPSegmentClient(private val client: ClientForODPOnly, private val logger: Logger) {

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun fetchQualifiedSegments(
        apiKey: String,
        apiEndpoint: String,
        payload: String
    ): List<String>? {

        val request: ClientForODPOnly.Request<String> = ClientForODPOnly.Request {
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
                    logger.debug("Successfully fetched ODP segments: {}", json)
                    return@Request json
                } else {
                    var errMsg = "Unexpected response from ODP segment endpoint, status: $status"
                    logger.error(errMsg)
                    return@Request null
                }
            } catch (e: Exception) {
                logger.error("Error making ODP segment request", e)
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
        val response = client.execute(request, REQUEST_BACKOFF_TIMEOUT, REQUEST_RETRIES_POWER)
        val parser: ResponseJsonParser = ResponseJsonParserFactory.getParser()
        try {
            return parser.parseQualifiedSegments(response)
        } catch (e: java.lang.Exception) {
            logger.error("Audience segments fetch failed (Error Parsing Response)")
            logger.debug(e.message)
        }
        return null
    }

    companion object {
        // configurable connection timeout in milliseconds
        var CONNECTION_TIMEOUT = 10 * 1000
        var READ_TIMEOUT = 60 * 1000

        // No retries on fetchQualifiedSegments() errors.
        // We want to return failure immediately to callers.

        // the numerical base for the exponential backoff
        const val REQUEST_BACKOFF_TIMEOUT = 0
        // power the number of retries
        const val REQUEST_RETRIES_POWER = 0
    }
}
