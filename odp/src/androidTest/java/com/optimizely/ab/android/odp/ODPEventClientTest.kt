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
import com.optimizely.ab.android.shared.Client
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.*
import org.slf4j.Logger
import java.io.OutputStream
import java.net.HttpURLConnection

@RunWith(AndroidJUnit4::class)
class ODPEventClientTest {
    private val logger = mock(Logger::class.java)
    private val client = mock(Client::class.java)
    private val urlConnection = mock(HttpURLConnection::class.java)
    private val captor = ArgumentCaptor.forClass(Client.Request::class.java)
    private lateinit var eventClient: ODPEventClient

    private val apiKey = "valid-key"
    private var apiEndpoint = "http://valid-endpoint"
    private val payload = "valid-payload"

    @Before
    fun setUp() {
        eventClient = ODPEventClient(client, logger)
        `when`(client.openConnection(any())).thenReturn(urlConnection)
        `when`(urlConnection.outputStream).thenReturn(mock(OutputStream::class.java))
    }

    @Test
    fun dispatch_200() {
        `when`(urlConnection.responseCode).thenReturn(200)
        `when`(urlConnection.responseMessage).thenReturn("message")

        eventClient.dispatch(apiKey, apiEndpoint, payload)

        verify(client).execute(captor.capture(), eq(2), eq(3))
        val received = captor.value.execute() as Boolean

        assertTrue(received)
        verify(urlConnection).connectTimeout = 10*1000
        verify(urlConnection).readTimeout = 60*1000
        verify(urlConnection).setRequestProperty("x-api-key", apiKey)
        verify(urlConnection).disconnect()
    }

    @Test
    fun dispatch_400() {
        `when`(urlConnection.responseCode).thenReturn(400)
        `when`(urlConnection.responseMessage).thenReturn("message")

        eventClient.dispatch(apiKey, apiEndpoint, payload)

        verify(client).execute(captor.capture(), anyInt(), anyInt())
        val received = captor.value.execute() as Boolean

        assertFalse(received)
        verify(logger).error("ODP event send failed (Response code: 400, message)")
        verify(urlConnection).disconnect()
    }

    @Test
    fun dispatch_500() {
        `when`(urlConnection.responseCode).thenReturn(500)
        `when`(urlConnection.responseMessage).thenReturn("message")

        eventClient.dispatch(apiKey, apiEndpoint, payload)

        verify(client).execute(captor.capture(), anyInt(), anyInt())
        val received = captor.value.execute() as Boolean

        assertFalse(received)
        verify(logger).error("ODP event send failed (Response code: 500, message)")
        verify(urlConnection).disconnect()
    }

    @Test
    fun dispatch_connectionFailed() {
        `when`(urlConnection.responseCode).thenReturn(200)
        `when`(urlConnection.responseMessage).thenReturn("message")

        apiEndpoint = "invalid-url"
        eventClient.dispatch(apiKey, apiEndpoint, payload)

        verify(client).execute(captor.capture(), anyInt(), anyInt())
        val received = captor.value.execute() as Boolean

        assertFalse(received)
        verify(logger).error(contains("Error making request"), any())
    }

}