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

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.optimizely.ab.android.shared.Client
import com.optimizely.ab.android.shared.EventHandlerUtils
import com.optimizely.ab.android.shared.OptlyStorage
import org.slf4j.LoggerFactory

class ODPEventWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public var eventClient = ODPEventClient(
        Client(OptlyStorage(context), LoggerFactory.getLogger(Client::class.java)),
        LoggerFactory.getLogger(ODPEventClient::class.java)
    )

    override fun doWork(): Result {
        val data = inputData
        val apiEndpoint = getApiEndpointFromInputData(data)
        val apiKey = getApiKeyFromInputData(data)
        val body = getEventBodyFromInputData(data)

        if (apiEndpoint == null || apiKey == null || body == null) {
            // malformed request (not retried).
            return Result.failure()
        }

        val status = eventClient.dispatch(apiKey, apiEndpoint, body) ?: false

        // NOTE: retries on failure is taken care in ODPEventClient
        return if (status) Result.success() else Result.failure()
    }

    @VisibleForTesting
    fun getEventBodyFromInputData(inputData: Data): String? {
        // check non-compressed data first
        val body = inputData.getString(KEY_ODP_EVENT_BODY)
        if (body != null) return body

        // check if data compressed
        val compressed = inputData.getString(KEY_ODP_EVENT_BODY_COMPRESSED) ?: return null
        return try {
            EventHandlerUtils.decompress(compressed)
        } catch (e: Exception) {
            null
        }
    }

    @VisibleForTesting
    fun getApiEndpointFromInputData(data: Data): String? {
        return data.getString(KEY_ODP_EVENT_APIENDPOINT)
    }

    @VisibleForTesting
    fun getApiKeyFromInputData(data: Data): String? {
        return data.getString(KEY_ODP_EVENT_APIKEY)
    }

    companion object {
        const val workerId = "ODPEventWorker"

        const val KEY_ODP_EVENT_APIENDPOINT = "apiEndpoint"
        const val KEY_ODP_EVENT_APIKEY = "apiKey"
        const val KEY_ODP_EVENT_BODY = "body"
        const val KEY_ODP_EVENT_BODY_COMPRESSED = "bodyCompressed"

        fun getData(apiKey: String, apiEndpoint: String, payload: String): Data {
            // androidx.work.Data throws IllegalStateException if total data length is more than MAX_DATA_BYTES
            // compress larger body and decompress it before dispatching. The compress rate is very high because of repeated data (20KB -> 1KB, 45KB -> 1.5KB).
            val maxSizeBeforeCompress = Data.MAX_DATA_BYTES - 1000 // 1000 reserved for other meta data

            val builder = Data.Builder()
                .putString(KEY_ODP_EVENT_APIENDPOINT, apiEndpoint)
                .putString(KEY_ODP_EVENT_APIKEY, apiKey)

            if (payload.length < maxSizeBeforeCompress) {
                builder.putString(KEY_ODP_EVENT_BODY, payload)
            } else {
                val compressed: String = EventHandlerUtils.compress(payload)
                builder.putString(KEY_ODP_EVENT_BODY_COMPRESSED, compressed)
            }

            return builder.build()
        }
    }
}
