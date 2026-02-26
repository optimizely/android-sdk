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

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.optimizely.ab.android.shared.Client
import com.optimizely.ab.android.shared.OptlyStorage
import com.optimizely.ab.cmab.client.CmabClient
import com.optimizely.ab.cmab.client.CmabFetchException
import com.optimizely.ab.cmab.client.CmabInvalidResponseException
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URL

@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
open class DefaultCmabClient : CmabClient {
    private val client: Client
    private val cmabClientHelper: CmabClientHelperAndroid
    private val logger = LoggerFactory.getLogger(DefaultCmabClient::class.java)

    @VisibleForTesting
    fun getCmabClientHelper(): CmabClientHelperAndroid {
        return cmabClientHelper
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, cmabClientHelper: CmabClientHelperAndroid?) {
        this.client =
            Client(OptlyStorage(context), LoggerFactory.getLogger(OptlyStorage::class.java))
        this.cmabClientHelper = cmabClientHelper ?: CmabClientHelperAndroid()
    }

    constructor(client: Client) : this(client, null)

    constructor(client: Client, cmabClientHelper: CmabClientHelperAndroid?) {
        this.client = client
        this.cmabClientHelper = cmabClientHelper ?: CmabClientHelperAndroid()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    override fun fetchDecision(
        ruleId: String?,
        userId: String?,
        attributes: Map<String?, Any?>?,
        cmabUuid: String?
    ): String? {
        val request: Client.Request<String?> = Client.Request {
            var urlConnection: HttpURLConnection? = null
            try {
                val apiEndpoint = String.format(cmabClientHelper.cmabPredictionEndpoint, ruleId)

                val requestBody: String =
                    cmabClientHelper.buildRequestJson(userId, ruleId, attributes, cmabUuid)

                logger.info("Fetching CMAB decision: {} with body: {}", apiEndpoint, requestBody)

                val url = URL(apiEndpoint)
                urlConnection = client.openConnection(url)
                if (urlConnection == null) {
                    val errorMessage = String.format(cmabClientHelper.cmabFetchFailed, "Failed to open connection")
                    logger.error(errorMessage)
                    throw CmabFetchException(errorMessage)
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

                    if (!cmabClientHelper.validateResponse(json)) {
                        logger.error(cmabClientHelper.invalidCmabFetchResponse)
                        throw CmabInvalidResponseException(cmabClientHelper.invalidCmabFetchResponse)
                    }

                    return@Request cmabClientHelper.parseVariationId(json)
                } else {
                    logger.debug("Failed to fetch CMAB decision for ruleId={} and userId={}: status={}", ruleId, userId, status)
                    val errorMessage: String = java.lang.String.format(
                        cmabClientHelper.cmabFetchFailed,
                        urlConnection.responseMessage
                    )
                    logger.error(errorMessage)
                    throw CmabFetchException(errorMessage)
                }
            } catch (e: CmabInvalidResponseException) {
                // Propagate validation exceptions as-is
                throw e
            } catch (e: CmabFetchException) {
                // Propagate fetch exceptions as-is
                throw e
            } catch (e: Exception) {
                logger.debug("Failed to fetch CMAB decision for ruleId={} and userId={}", ruleId, userId);
                val errorMessage: String =
                    java.lang.String.format(cmabClientHelper.cmabFetchFailed, e.message)
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
        return client.execute(request, REQUEST_BACKOFF_TIMEOUT, REQUEST_RETRIES_POWER)
    }

    companion object {
        // configurable connection timeout in milliseconds
        var CONNECTION_TIMEOUT = 10 * 1000
        var READ_TIMEOUT = 60 * 1000

        // cmab service retries twice with 1sec interval

        // the numerical base for the exponential backoff (1 second)
        const val REQUEST_BACKOFF_TIMEOUT = 1
        // retry once = 2 total attempts
        const val REQUEST_RETRIES_POWER = 1
    }
}
