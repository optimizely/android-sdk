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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class DefaultODPApiManagerTest {
    private val apiKey = "valid-key"
    private val apiEndpoint = "http://valid-endpoint"

    private var context: Context = ApplicationProvider.getApplicationContext()
    private val defaultODPApiManager = DefaultODPApiManager(context)

    @Test
    fun fetchQualifiedSegments() {
        val userKey = "fs_user_id"
        val userValue = "user123"
        val segmentsToCheck = setOf("a")

        val expRequestPayload = "{\"query\": \"query(\$userId: String, \$audiences: [String]) {customer(fs_user_id: \$userId) {audiences(subset: \$audiences) {edges {node {name state}}}}}\", \"variables\": {\"userId\": \"user123\", \"audiences\": [\"a\"]}}"

        val segmentClient = mock(ODPSegmentClient::class.java)
        defaultODPApiManager.segmentClient = segmentClient

        defaultODPApiManager.fetchQualifiedSegments(apiKey, apiEndpoint, userKey, userValue, segmentsToCheck)
        verify(segmentClient).fetchQualifiedSegments(apiKey, apiEndpoint, expRequestPayload)
    }

    @Test
    fun sendEvents() {
        val payload = "event-payload"
        val status = defaultODPApiManager.sendEvents(apiKey, apiEndpoint, payload)
        assertEquals(status, 200) // always return success to java-sdk
    }
}
