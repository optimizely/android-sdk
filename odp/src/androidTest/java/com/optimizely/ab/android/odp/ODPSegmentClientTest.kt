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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.optimizely.ab.android.shared.ClientForODPOnly
import java.io.OutputStream
import java.net.HttpURLConnection
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Matchers.contains
import org.mockito.Matchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.slf4j.Logger

@RunWith(AndroidJUnit4::class)
class ODPSegmentClientTest {
    private val logger = mock(Logger::class.java)
    private val client = mock(ClientForODPOnly::class.java)
    private val urlConnection = mock(HttpURLConnection::class.java)
    private val captor = ArgumentCaptor.forClass(ClientForODPOnly.Request::class.java)
    private lateinit var segmentClient: ODPSegmentClient

    private val apiKey = "valid-key"
    private var apiEndpoint = "http://valid-endpoint"
    private val payload = "valid-payload"
    private val response = "valid-response"

    @Before
    fun setUp() {
        segmentClient = ODPSegmentClient(client, logger)
        `when`(client.openConnection(any())).thenReturn(urlConnection)
        `when`(client.readStream(urlConnection)).thenReturn(response)
        `when`(urlConnection.outputStream).thenReturn(mock(OutputStream::class.java))
    }

    @Test
    fun fetchQualifiedSegments_200() {
        `when`(urlConnection.responseCode).thenReturn(200)

        segmentClient.fetchQualifiedSegments(apiKey, apiEndpoint, payload)

        verify(client).execute(captor.capture(), eq(0), eq(0))
        val received = captor.value.execute()

        assert(received == response)
        verify(urlConnection).connectTimeout = 10 * 1000
        verify(urlConnection).readTimeout = 60 * 1000
        verify(urlConnection).setRequestProperty("x-api-key", apiKey)
        verify(urlConnection).disconnect()
    }

//    @Test
//    fun fetchQualifiedSegments_400() {
//        `when`(urlConnection.responseCode).thenReturn(400)
//
//        segmentClient.fetchQualifiedSegments(apiKey, apiEndpoint, payload)
//
//        verify(client).execute(captor.capture(), eq(0), eq(0))
//        val received = captor.value.execute()
//
//        assertNull(received)
//        verify(logger).error("Unexpected response from ODP segment endpoint, status: 400")
//        verify(urlConnection).disconnect()
//    }

//    @Test
//    fun fetchQualifiedSegments_500() {
//        `when`(urlConnection.responseCode).thenReturn(500)
//
//        segmentClient.fetchQualifiedSegments(apiKey, apiEndpoint, payload)
//
//        verify(client).execute(captor.capture(), eq(0), eq(0))
//        val received = captor.value.execute()
//
//        assertNull(received)
//        verify(logger).error("Unexpected response from ODP segment endpoint, status: 500")
//        verify(urlConnection).disconnect()
//    }

//    @Test
//    fun fetchQualifiedSegments_connectionFailed() {
//        `when`(urlConnection.responseCode).thenReturn(200)
//
//        apiEndpoint = "invalid-url"
//        segmentClient.fetchQualifiedSegments(apiKey, apiEndpoint, payload)
//
//        verify(client).execute(captor.capture(), eq(0), eq(0))
//        val received = captor.value.execute()
//
//        assertNull(received)
//        verify(logger).error(contains("Error making ODP segment request"), any())
//    }
}
