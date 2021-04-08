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
package com.optimizely.ab.android.shared

import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers
import org.mockito.Mockito
import org.mockito.internal.verification.VerificationModeFactory
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.net.URLConnection

/**
 * Tests for [com.optimizely.ab.android.shared.Client]
 */
@RunWith(AndroidJUnit4::class)
class ClientTest {
    private var client: Client? = null
    private lateinit var optlyStorage: OptlyStorage
    private var logger: Logger? = null
    @Before
    fun setup() {
        optlyStorage = Mockito.mock(OptlyStorage::class.java)
        logger = Mockito.mock(Logger::class.java)
        client = Client(optlyStorage, logger)
    }

    @Test
    fun setIfModifiedSinceHasValueInStorage() {
        var url: URL? = null
        try {
            url = URL("http://www.optimizely.com")
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        Mockito.`when`(optlyStorage!!.getLong(url.toString(), 0)).thenReturn(100L)
        val urlConnection = Mockito.mock(URLConnection::class.java)
        Mockito.`when`(urlConnection.url).thenReturn(url)
        client!!.setIfModifiedSince(urlConnection)
        Mockito.verify(urlConnection).ifModifiedSince = 100L
    }

    @Test
    fun saveLastModifiedNoHeader() {
        var url: URL? = null
        try {
            url = URL("http://www.optimizely.com")
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        val urlConnection = Mockito.mock(URLConnection::class.java)
        Mockito.`when`(urlConnection.url).thenReturn(url)
        Mockito.`when`(urlConnection.lastModified).thenReturn(0L)
        client!!.saveLastModified(urlConnection)
        Mockito.verify(logger)?.warn("CDN response didn't have a last modified header")
    }

    @Test
    fun saveLastModifiedWhenExists() {
        var url: URL? = null
        try {
            url = URL("http://www.optimizely.com")
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        val urlConnection = Mockito.mock(URLConnection::class.java)
        Mockito.`when`(urlConnection.lastModified).thenReturn(100L)
        Mockito.`when`(urlConnection.url).thenReturn(url)
        client!!.saveLastModified(urlConnection)
        Mockito.verify(optlyStorage).saveLong(url.toString(), 100L)
    }

    @Test
    @Throws(IOException::class)
    fun readStreamReturnsString() {
        val foo = "foo"
        val `is`: InputStream = ByteArrayInputStream(foo.toByteArray())
        val urlConnection = Mockito.mock(URLConnection::class.java)
        Mockito.`when`(urlConnection.getInputStream()).thenReturn(`is`)
        val readFoo = client!!.readStream(urlConnection)
        Assert.assertEquals(foo, readFoo)
    }

    @Test
    fun testExpBackoffSuccess() {
        val request = Mockito.mock(Client.Request::class.java)
        val expectedResponse = Any()
        Mockito.`when`(request.execute()).thenReturn(expectedResponse)
        val response = client!!.execute<Any>(request!!, 2, 4)
        Assert.assertEquals(expectedResponse, response)
        Mockito.verify(logger, Mockito.never())?.info(Matchers.eq("Request failed, waiting {} seconds to try again"), Matchers.any(Int::class.java))
    }

    @Test
    fun testExpBackoffFailure() {
        val request = Mockito.mock(Client.Request::class.java)
        Mockito.`when`(request.execute()).thenReturn(null)
        Assert.assertNull(client!!.execute<Any>(request!!, 2, 4))
        val captor = ArgumentCaptor.forClass(Int::class.java)
        Mockito.verify(logger, VerificationModeFactory.times(4))?.info(Matchers.eq("Request failed, waiting {} seconds to try again"), captor.capture())
        val timeouts = captor.allValues
        Assert.assertTrue(timeouts.contains(2))
        Assert.assertTrue(timeouts.contains(4))
        Assert.assertTrue(timeouts.contains(8))
        Assert.assertTrue(timeouts.contains(16))
    }
}