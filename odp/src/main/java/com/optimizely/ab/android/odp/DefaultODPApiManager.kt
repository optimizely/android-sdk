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
import com.optimizely.ab.android.shared.Client
import com.optimizely.ab.android.shared.OptlyStorage
import com.optimizely.ab.android.shared.WorkerScheduler
import org.slf4j.LoggerFactory


// TODO: remove when integrated with java-sdk (replace with one in java-core)
public interface ODPApiManager {
    fun fetchQualifiedSegments(apiKey: String, apiEndpoint: String, userKey: String, userValue: String, segmentsToCheck: Set<String>): String?
    fun sendEvents(apiKey: String, apiEndpoint: String, payload: String): Int
}

class DefaultODPApiManager(private val context: Context) : ODPApiManager {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public var segmentClient = ODPSegmentClient(
        Client(OptlyStorage(context), LoggerFactory.getLogger(Client::class.java)),
        LoggerFactory.getLogger(ODPSegmentClient::class.java)
    )
    private val logger = LoggerFactory.getLogger(DefaultODPApiManager::class.java)

    override fun fetchQualifiedSegments(
        apiKey: String,
        apiEndpoint: String,
        userKey: String,
        userValue: String,
        segmentsToCheck: Set<String>,
    ): String? {
        return segmentClient.fetchQualifiedSegments(
            apiKey,
            apiEndpoint,
            getSegmentsQueryPayload(userKey, userValue, segmentsToCheck)
        )
    }

    override fun sendEvents(apiKey: String, apiEndpoint: String, payload: String): Int {
        val inputData = ODPEventWorker.getData(apiKey, apiEndpoint, payload)
        WorkerScheduler.startService(
            context,
            ODPEventWorker.workerId,
            ODPEventWorker::class.java,
            inputData,
            0
        )

        logger.debug("Sent an ODP event ({}) to the event handler service: {}", payload, apiEndpoint)

        // return success to the caller - retries on failure will be taken care in WorkManager.
        return 200
    }

    // Helpers

    private fun getSegmentsQueryPayload(userKey: String, userValue: String, segmentsToCheck: Set<String>): String {
        val query = java.lang.String.format(
            "query(\$userId: String, \$audiences: [String]) {customer(%s: \$userId) {audiences(subset: \$audiences) {edges {node {name state}}}}}",
            userKey
        )

        val variables = java.lang.String.format(
            "{\"userId\": \"%s\", \"audiences\": [%s]}",
            userValue,
            getSegmentsStringForRequest(segmentsToCheck)
        )

        return String.format("{\"query\": \"%s\", \"variables\": %s}", query, variables)
    }

    private fun getSegmentsStringForRequest(segmentsList: Set<String?>): String {
        val segmentsString = StringBuilder()
        val segmentsListIterator = segmentsList.iterator()
        for (i in segmentsList.indices) {
            if (i > 0) {
                segmentsString.append(", ")
            }
            segmentsString.append("\"").append(segmentsListIterator.next()).append("\"")
        }
        return segmentsString.toString()
    }

}