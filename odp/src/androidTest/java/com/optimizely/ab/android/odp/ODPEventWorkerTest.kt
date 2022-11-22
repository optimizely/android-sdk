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
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestWorkerBuilder
import org.hamcrest.Matchers.`is`
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Tests [ODPEventWorker]
 */
@RunWith(AndroidJUnit4::class)
class ODPEventWorkerTest {
    private val apiKey = "valid-key"
    private var apiEndpoint = "http://valid-endpoint"
    private val smallBody = "valid-payload"
    private val largeBody = makeLongString(20000)

    private var context: Context = ApplicationProvider.getApplicationContext()
    private var executor: Executor = Executors.newSingleThreadExecutor()
    private var worker: ODPEventWorker = makeODPEventWorker(apiEndpoint, apiKey, smallBody)

    @Test
    fun dispatch_success() {
        val eventClient = mock(ODPEventClient::class.java)
        `when`(eventClient.dispatch(apiKey, apiEndpoint, smallBody)).thenReturn(true)
        worker.eventClient = eventClient

        val result = worker.doWork()
        assertThat(result, `is`(ListenableWorker.Result.success()))
        verify(eventClient).dispatch(apiKey, apiEndpoint, smallBody)
    }

    @Test
    fun dispatch_failure() {
        val eventClient = mock(ODPEventClient::class.java)
        `when`(eventClient.dispatch(apiKey, apiEndpoint, smallBody)).thenReturn(false)
        worker.eventClient = eventClient

        val result = worker.doWork()
        assertThat(result, `is`(ListenableWorker.Result.failure()))
        verify(eventClient).dispatch(apiKey, apiEndpoint, smallBody)
    }

    // Data

    @Test
    fun getDataForSmallEvent() {
        val data: Data = ODPEventWorker.getData(apiKey, apiEndpoint, smallBody)

        assertEquals(worker.getApiEndpointFromInputData(data), apiEndpoint)
        assertEquals(worker.getApiKeyFromInputData(data), apiKey)
        assertEquals(worker.getEventBodyFromInputData(data), smallBody)
        assertNotNull(data.getString("body"))
        assertNull(data.getString("bodyCompressed"))
        assertTrue(smallBody.length < 1000)
    }

    @Test
    fun getDataForCompressedEvent() {
        val data: Data = ODPEventWorker.getData(apiKey, apiEndpoint, largeBody)

        assertEquals(worker.getApiEndpointFromInputData(data), apiEndpoint)
        assertEquals(worker.getApiKeyFromInputData(data), apiKey)
        assertEquals(worker.getEventBodyFromInputData(data), largeBody)
        assertNull(data.getString("body"))
        assertNotNull(data.getString("bodyCompressed"))
        assertTrue(largeBody.length > 15000)
    }

    // Helpers

    private fun makeODPEventWorker(apiEndpoint: String, apiKey: String, payload: String): ODPEventWorker {
        val inputData: Data = ODPEventWorker.getData(apiKey, apiEndpoint, payload)
        return TestWorkerBuilder.from<ODPEventWorker>(context, ODPEventWorker::class.java, executor)
            .setInputData(inputData)
            .build()
    }

    private fun makeLongString(maxSize: Int): String {
        val builder = StringBuilder()
        val str = "random-string"
        var repeat = (maxSize / str.length) + 1
        for (i in 1..repeat) builder.append(str)
        return builder.toString()
    }

}
