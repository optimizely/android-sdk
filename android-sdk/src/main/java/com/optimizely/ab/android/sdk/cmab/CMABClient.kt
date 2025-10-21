// Copyright 2025, Optimizely, Inc. and contributors
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

package com.optimizely.ab.android.sdk.cmab

import androidx.annotation.VisibleForTesting
import com.optimizely.ab.android.shared.Client
import org.slf4j.Logger
import java.net.HttpURLConnection
import java.net.URL

@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
open class CMABClient(private val client: Client, private val logger: Logger) {

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    fun fetchDecision(
        ruleId: String?,
        userId: String?,
        attributes: Map<String?, Any?>?,
        cmabUuid: String?
    ): String {
        val request: Client.Request<String> = Client.Request {
            var urlConnection: HttpURLConnection? = null
            try {
                val apiEndpoint = String.format(CmabClientHelper.CMAB_PREDICTION_ENDPOINT, ruleId)
                val requestBody: String =
                    CmabClientHelper.buildRequestJson(userId, ruleId, attributes, cmabUuid)

                val url = URL(apiEndpoint)
                urlConnection = client.openConnection(url)
                if (urlConnection == null) {
                    return@Request null
                }

                // set timeouts for releasing failed connections (default is 0 = no timeout).
                urlConnection.connectTimeout = CONNECTION_TIMEOUT
                urlConnection.readTimeout = READ_TIMEOUT

                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("content-type", "application/json")

                urlConnection.doOutput = true
                val outputStream = urlConnection.outputStream
                outputStream.write(requestBody.toByteArray())
                outputStream.flush()
                outputStream.close()
                val status = urlConnection.responseCode
                if (status in 200..399) {
                    val json = client.readStream(urlConnection)
                    logger.debug("Successfully fetched CMAB decision: {}", json)

                    if (!CmabClientHelper.validateResponse(json)) {
                        logger.error(CmabClientHelper.INVALID_CMAB_FETCH_RESPONSE)
                        throw CmabInvalidResponseException(CmabClientHelper.INVALID_CMAB_FETCH_RESPONSE)
                    }
                    return@Request CmabClientHelper.parseVariationId(json)
                } else {
                    val errorMessage: String = java.lang.String.format(
                        CmabClientHelper.CMAB_FETCH_FAILED,
                        statusLine.getReasonPhrase()
                    )
                    logger.error(errorMessage)
                    throw CmabFetchException(errorMessage)
                }
            } catch (e: Exception) {
                val errorMessage: String =
                    java.lang.String.format(CmabClientHelper.CMAB_FETCH_FAILED, e.message)
                logger.error(errorMessage)
                throw CmabFetchException(errorMessage)
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

        // the numerical base for the exponential backoff
        const val REQUEST_BACKOFF_TIMEOUT = 2
        // power the number of retries
        const val REQUEST_RETRIES_POWER = 3
    }
}
