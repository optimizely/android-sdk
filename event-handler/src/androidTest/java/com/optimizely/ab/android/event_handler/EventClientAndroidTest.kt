/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                            *
 * *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.android.event_handler

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.optimizely.ab.android.shared.Client
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.slf4j.Logger
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tests [DefaultEventHandler]
 */
@RunWith(AndroidJUnit4::class)
class EventClientAndroidTest {
    @Mock
    var logger = Mockito.mock(Logger::class.java)

    @Mock
    var client = Mockito.mock(Client::class.java)
    private var urlConnection: HttpURLConnection? = null
    private var eventClient: EventClient? = null
    private var event: Event? = null
    @Before
    @Throws(IOException::class)
    fun setupEventClient() {
        urlConnection = Mockito.mock(HttpURLConnection::class.java)
        Mockito.`when`(urlConnection?.getOutputStream()).thenReturn(Mockito.mock(OutputStream::class.java))
        Mockito.`when`(urlConnection?.getInputStream()).thenReturn(Mockito.mock(InputStream::class.java))
        eventClient = EventClient(client, logger)
        val url = URL("http://www.foo.com")
        event = Event(url, "")
    }

    @Test
    fun testEventClient() {
        eventClient!!.sendEvent(event!!)
        Mockito.verify(logger).info("Successfully dispatched event: {}",
                event)
    }
}