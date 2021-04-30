/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
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

import com.optimizely.ab.android.shared.Client
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.slf4j.Logger
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Boolean
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tests [EventClient]
 */
@RunWith(PowerMockRunner::class)
@PrepareForTest(Client::class, Event::class)
class EventClientTest {
    @Mock
    var logger: Logger? = null

    @Mock
    var client: Client? = null
    private var urlConnection: HttpURLConnection? = null
    private var eventClient: EventClient? = null
    private var event: Event? = null
    @Before
    @Throws(IOException::class)
    fun setupEventClient() {
        urlConnection = Mockito.mock(HttpURLConnection::class.java)
        Mockito.`when`(urlConnection?.getOutputStream()).thenReturn(Mockito.mock(OutputStream::class.java))
        Mockito.`when`(urlConnection?.getInputStream()).thenReturn(Mockito.mock(InputStream::class.java))
        eventClient = EventClient(client!!, logger!!)
        val url = URL("http://www.foo.com")
        event = Event(url, "")
    }

    @Test
    @Throws(IOException::class)
    fun sendEvents200() {
        Mockito.`when`(client!!.openConnection(event!!.uRL)).thenReturn(urlConnection)
        Mockito.`when`(urlConnection!!.responseCode).thenReturn(200)
        val inputStream = Mockito.mock(InputStream::class.java)
        Mockito.`when`(urlConnection!!.inputStream).thenReturn(inputStream)
        eventClient!!.sendEvent(event!!)
        val captor1 = ArgumentCaptor.forClass(Client.Request::class.java)
        val captor2 = ArgumentCaptor.forClass(Int::class.java)
        val captor3 = ArgumentCaptor.forClass(Int::class.java)
        Mockito.verify(client)?.execute<Any>(captor1.capture() as? Client.Request<Any>, captor2.capture(), captor3.capture())
        Assert.assertEquals(Integer.valueOf(2), captor2.value)
        Assert.assertEquals(Integer.valueOf(5), captor3.value)
        val response = captor1.value.execute()
        Assert.assertEquals(Boolean.TRUE, response)
        Mockito.verify(logger)?.info("Dispatching event: {}", event)
    }

    @Test
    @Throws(IOException::class)
    fun sendEvents201() {
        Mockito.`when`(client!!.openConnection(event!!.uRL)).thenReturn(urlConnection)
        Mockito.`when`(urlConnection!!.responseCode).thenReturn(201)
        val inputStream = Mockito.mock(InputStream::class.java)
        Mockito.`when`(urlConnection!!.inputStream).thenReturn(inputStream)
        eventClient!!.sendEvent(event!!)
        val captor1 = ArgumentCaptor.forClass(Client.Request::class.java)
        val captor2 = ArgumentCaptor.forClass(Int::class.java)
        val captor3 = ArgumentCaptor.forClass(Int::class.java)
        Mockito.verify(client)?.execute<Any>(captor1.capture() as? Client.Request<Any>, captor2.capture(), captor3.capture())
        Assert.assertEquals(Integer.valueOf(2), captor2.value)
        Assert.assertEquals(Integer.valueOf(5), captor3.value)
        val response = captor1.value.execute()
        Assert.assertEquals(Boolean.TRUE, response)
        Mockito.verify(logger)?.info("Dispatching event: {}", event)
    }

    @Test
    @Throws(IOException::class)
    fun sendEvents300() {
        Mockito.`when`(client!!.openConnection(event!!.uRL)).thenReturn(urlConnection)
        Mockito.`when`(urlConnection!!.responseCode).thenReturn(300)
        val inputStream = Mockito.mock(InputStream::class.java)
        Mockito.`when`(urlConnection!!.inputStream).thenReturn(inputStream)
        eventClient!!.sendEvent(event!!)
        val captor1 = ArgumentCaptor.forClass(Client.Request::class.java)
        val captor2 = ArgumentCaptor.forClass(Int::class.java)
        val captor3 = ArgumentCaptor.forClass(Int::class.java)
        Mockito.verify(client)?.execute<Any>(captor1.capture() as? Client.Request<Any>, captor2.capture(), captor3.capture())
        Assert.assertEquals(Integer.valueOf(2), captor2.value)
        Assert.assertEquals(Integer.valueOf(5), captor3.value)
        val response = captor1.value.execute()
        Assert.assertEquals(Boolean.FALSE, response)
        Mockito.verify(logger)?.info("Dispatching event: {}", event)
        Mockito.verify(logger)?.error("Unexpected response from event endpoint, status: 300")
    }

    @Test
    @Throws(IOException::class)
    fun sendEventsIoExceptionGetInputStream() {
        Mockito.`when`(client!!.openConnection(event!!.uRL)).thenReturn(urlConnection)
        Mockito.`when`(urlConnection!!.responseCode).thenReturn(200)
        Mockito.`when`(urlConnection!!.inputStream).thenThrow(IOException::class.java)
        eventClient!!.sendEvent(event!!)
        val captor1 = ArgumentCaptor.forClass(Client.Request::class.java)
        val captor2 = ArgumentCaptor.forClass(Int::class.java)
        val captor3 = ArgumentCaptor.forClass(Int::class.java)
        Mockito.verify(client)?.execute<Any>(captor1.capture() as? Client.Request<Any>, captor2.capture(), captor3.capture())
        Assert.assertEquals(Integer.valueOf(2), captor2.value)
        Assert.assertEquals(Integer.valueOf(5), captor3.value)
        val response = captor1.value.execute()
        Assert.assertEquals(Boolean.FALSE, response)
        Mockito.verify(logger)?.info("Dispatching event: {}", event)
    }

    @Test
    @Throws(IOException::class)
    fun sendEventsIoExceptionOpenConnection() {
        PowerMockito.`when`(client!!.openConnection(event!!.uRL)).thenThrow(IOException::class.java)
        eventClient!!.sendEvent(event!!)
        val captor1 = ArgumentCaptor.forClass(Client.Request::class.java)
        val captor2 = ArgumentCaptor.forClass(Int::class.java)
        val captor3 = ArgumentCaptor.forClass(Int::class.java)
        Mockito.verify(client)?.execute<Any>(captor1.capture() as? Client.Request<Any>, captor2.capture(), captor3.capture())
        Assert.assertEquals(Integer.valueOf(2), captor2.value)
        Assert.assertEquals(Integer.valueOf(5), captor3.value)
        val response = captor1.value.execute()
        Assert.assertEquals(Boolean.FALSE, response)
        Mockito.verify(logger)?.info("Dispatching event: {}", event)
    }

    @Test
    fun convertsNullResponseToFalse() {
        val event = Mockito.mock(Event::class.java)
        Mockito.`when`(client!!.execute<Any>(Matchers.any(Client.Request::class.java) as? Client.Request<Any>, Matchers.eq(2), Matchers.eq(5))).thenReturn(null)
        Assert.assertFalse(eventClient!!.sendEvent(event))
    }
}