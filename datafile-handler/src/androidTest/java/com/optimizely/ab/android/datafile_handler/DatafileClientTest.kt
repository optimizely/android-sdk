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
package com.optimizely.ab.android.datafile_handler

import com.optimizely.ab.android.shared.Client
import com.optimizely.ab.android.shared.DatafileConfig
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentCaptor
import org.mockito.Matchers
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import org.slf4j.Logger
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

/**
 * Tests for [DatafileClient]
 */
@RunWith(JUnit4::class)
class DatafileClientTest {
    private var datafileClient: DatafileClient? = null
    private var logger: Logger? = null
    private var client: Client? = null
    private var urlConnection: HttpURLConnection? = null
    @Before
    fun setup() {
        client = Mockito.mock(Client::class.java)
        logger = Mockito.mock(Logger::class.java)
        datafileClient = DatafileClient(client!!, logger!!)
        urlConnection = Mockito.mock(HttpURLConnection::class.java)
    }

    @Test
    @Throws(IOException::class)
    fun request200() {
        val url = URL(DatafileConfig("1", null).url)
        Mockito.`when`(client!!.openConnection(url)).thenReturn(urlConnection)
        Mockito.`when`(urlConnection!!.responseCode).thenReturn(200)
        Mockito.`when`(client!!.readStream(urlConnection!!)).thenReturn("{}")
        datafileClient!!.request(url.toString())
        val captor1 = ArgumentCaptor.forClass(Client.Request::class.java)
        val captor2 = ArgumentCaptor.forClass(Int::class.java)
        val captor3 = ArgumentCaptor.forClass(Int::class.java)
        Mockito.verify(client)?.execute<Any>(captor1.capture() as Client.Request<Any>, captor2.capture(), captor3.capture())
        Assert.assertEquals(Integer.valueOf(DatafileClient.REQUEST_BACKOFF_TIMEOUT), captor2.value)
        Assert.assertEquals(Integer.valueOf(DatafileClient.REQUEST_RETRIES_POWER), captor3.value)
        val response = captor1.value.execute()
        junit.framework.Assert.assertTrue(String::class.java.isInstance(response))
        Assert.assertEquals("{}", response)
        Mockito.verify(logger)?.info("Requesting data file from {}", url)
        Mockito.verify(client)?.saveLastModified(urlConnection!!)
        Mockito.verify(client)?.readStream(urlConnection!!)
        Mockito.verify(urlConnection)?.disconnect()
    }

    /**
     * testLastModified - This is a test to see if given two projects, the last modified for datafile download is project specific.
     * Two URLs url1 and url2 are both datafile urls, url1 is requested from the data client twice, while url2 is only asked for once.
     * The first time the last modified is 0 and the second time, if it is non-zero, then it is the current last modified and a 304 is returned.
     *
     * @throws IOException
     */
    @Test
    @Throws(IOException::class)
    fun testLastModified() {
        val url1 = URL(DatafileConfig("1", null).url)
        val url2 = URL(DatafileConfig("2", null).url)
        val urlConnection2 = Mockito.mock(HttpURLConnection::class.java)
        Mockito.`when`(urlConnection!!.url).thenReturn(url1)
        Mockito.`when`(urlConnection2.url).thenReturn(url2)
        Mockito.`when`(urlConnection!!.lastModified).thenReturn(0L)
        Mockito.`when`(urlConnection2.lastModified).thenReturn(0L)
        Mockito.`when`(client!!.openConnection(url1)).thenReturn(urlConnection)
        val answer = Answer { invocation ->
            val connection = invocation.mock as HttpURLConnection
            val url = connection.url
            if (url === url1) {
                if (connection.lastModified == 0L) {
                    Mockito.`when`(connection.lastModified).thenReturn(300L)
                    return@Answer 200
                } else {
                    Assert.assertEquals(connection.lastModified, 300L)
                    return@Answer 304
                }
            } else if (url === url2) {
                if (connection.lastModified == 0L) {
                    Mockito.`when`(connection.lastModified).thenReturn(200L)
                    return@Answer 200
                } else {
                    Assert.assertEquals(connection.lastModified, 200L)
                    return@Answer 304
                }
            }
            //Object[] arguments = invocation.getArguments();
            //String string = (String) arguments[0];
            0
        }
        Mockito.`when`(urlConnection!!.responseCode).thenAnswer(answer)
        Mockito.`when`(urlConnection2.responseCode).thenAnswer(answer)
        Mockito.`when`(client!!.openConnection(url2)).thenReturn(urlConnection2)
        Mockito.`when`(client!!.readStream(urlConnection!!)).thenReturn("{}")
        Mockito.`when`(client!!.readStream(urlConnection2)).thenReturn("{}")

        // first call returns the project file {}
        datafileClient!!.request(url1.toString())
        var captor1 = ArgumentCaptor.forClass(Client.Request::class.java)
        var captor2 = ArgumentCaptor.forClass(Int::class.java)
        var captor3 = ArgumentCaptor.forClass(Int::class.java)
        Mockito.verify(client)?.execute<Any>(captor1.capture() as Client.Request<Any>, captor2.capture()!!, captor3.capture()!!)
        Assert.assertEquals(Integer.valueOf(DatafileClient.REQUEST_BACKOFF_TIMEOUT), captor2.value)
        Assert.assertEquals(Integer.valueOf(DatafileClient.REQUEST_RETRIES_POWER), captor3.value)
        var response = captor1.value.execute()
        junit.framework.Assert.assertTrue(String::class.java.isInstance(response))
        Assert.assertEquals("{}", response)
        Mockito.verify(logger)?.info("Requesting data file from {}", url1)
        Mockito.verify(client)?.saveLastModified(urlConnection!!)
        Mockito.verify(client)?.readStream(urlConnection!!)
        Mockito.verify(urlConnection)?.disconnect()

        // second call returns 304 so the response is a empty string.
        datafileClient!!.request(url1.toString())
        captor1 = ArgumentCaptor.forClass(Client.Request::class.java)
        captor2 = ArgumentCaptor.forClass(Int::class.java)
        captor3 = ArgumentCaptor.forClass(Int::class.java)
        Mockito.verify(client, Mockito.times(2))?.execute<Any>(captor1.capture() as Client.Request<Any>, captor2.capture()!!, captor3.capture()!!)
        Assert.assertEquals(Integer.valueOf(DatafileClient.REQUEST_BACKOFF_TIMEOUT), captor2.value)
        Assert.assertEquals(Integer.valueOf(DatafileClient.REQUEST_RETRIES_POWER), captor3.value)
        response = captor1.value.execute()
        junit.framework.Assert.assertTrue(String::class.java.isInstance(response))
        Assert.assertEquals("", response)
        Mockito.verify(logger)?.info("Data file has not been modified on the cdn")
        Mockito.verify(urlConnection, Mockito.times(2))?.disconnect()
        datafileClient!!.request(url2.toString())
        captor1 = ArgumentCaptor.forClass(Client.Request::class.java)
        captor2 = ArgumentCaptor.forClass(Int::class.java)
        captor3 = ArgumentCaptor.forClass(Int::class.java)
        Mockito.verify(client, Mockito.times(3))?.execute<Any>(captor1.capture() as Client.Request<Any>, captor2.capture()!!, captor3.capture()!!)
        Assert.assertEquals(Integer.valueOf(DatafileClient.REQUEST_BACKOFF_TIMEOUT), captor2.value)
        Assert.assertEquals(Integer.valueOf(DatafileClient.REQUEST_RETRIES_POWER), captor3.value)
        response = captor1.value.execute()
        junit.framework.Assert.assertTrue(String::class.java.isInstance(response))
        Assert.assertEquals("{}", response)
        Mockito.verify(logger, Mockito.times(2))?.info("Requesting data file from {}", url1)
        Mockito.verify(client)?.saveLastModified(urlConnection2)
        Mockito.verify(client)?.readStream(urlConnection2)
        Mockito.verify(urlConnection2).disconnect()
    }

    @Test
    @Throws(IOException::class)
    fun request201() {
        val url = URL(DatafileConfig("1", null).url)
        Mockito.`when`(client!!.openConnection(url)).thenReturn(urlConnection)
        Mockito.`when`(urlConnection!!.responseCode).thenReturn(201)
        Mockito.`when`(client!!.readStream(urlConnection!!)).thenReturn("{}")
        datafileClient!!.request(url.toString())
        val captor1 = ArgumentCaptor.forClass(Client.Request::class.java)
        val captor2 = ArgumentCaptor.forClass(Int::class.java)
        val captor3 = ArgumentCaptor.forClass(Int::class.java)
        Mockito.verify(client)?.execute<Any>(captor1.capture() as Client.Request<Any>, captor2.capture(), captor3.capture())
        Assert.assertEquals(Integer.valueOf(DatafileClient.REQUEST_BACKOFF_TIMEOUT), captor2.value)
        Assert.assertEquals(Integer.valueOf(DatafileClient.REQUEST_RETRIES_POWER), captor3.value)
        val response = captor1.value.execute()
        junit.framework.Assert.assertTrue(String::class.java.isInstance(response))
        Assert.assertEquals("{}", response)
        Mockito.verify(logger)?.info("Requesting data file from {}", url)
        Mockito.verify(client)?.saveLastModified(urlConnection!!)
        Mockito.verify(client)?.readStream(urlConnection!!)
        Mockito.verify(urlConnection)?.disconnect()
    }

    @Test
    @Throws(IOException::class)
    fun request299() {
        val url = URL(DatafileConfig("1", null).url)
        Mockito.`when`(client!!.openConnection(url)).thenReturn(urlConnection)
        Mockito.`when`(urlConnection!!.responseCode).thenReturn(299)
        Mockito.`when`(client!!.readStream(urlConnection!!)).thenReturn("{}")
        datafileClient!!.request(url.toString())
        val captor1 = ArgumentCaptor.forClass(Client.Request::class.java)
        val captor2 = ArgumentCaptor.forClass(Int::class.java)
        val captor3 = ArgumentCaptor.forClass(Int::class.java)
        Mockito.verify(client)?.execute<Any>(captor1.capture() as Client.Request<Any>, captor2.capture(), captor3.capture())
        Assert.assertEquals(Integer.valueOf(DatafileClient.REQUEST_BACKOFF_TIMEOUT), captor2.value)
        Assert.assertEquals(Integer.valueOf(DatafileClient.REQUEST_RETRIES_POWER), captor3.value)
        val response = captor1.value.execute()
        junit.framework.Assert.assertTrue(String::class.java.isInstance(response))
        Assert.assertEquals("{}", response)
        Mockito.verify(logger)?.info("Requesting data file from {}", url)
        Mockito.verify(client)?.saveLastModified(urlConnection!!)
        Mockito.verify(client)?.readStream(urlConnection!!)
        Mockito.verify(urlConnection)?.disconnect()
    }

    @Test
    @Throws(IOException::class)
    fun request300() {
        val url = URL(DatafileConfig("1", null).url)
        Mockito.`when`(client!!.openConnection(url)).thenReturn(urlConnection)
        Mockito.`when`(urlConnection!!.responseCode).thenReturn(300)
        datafileClient!!.request(url.toString())
        val captor1 = ArgumentCaptor.forClass(Client.Request::class.java)
        val captor2 = ArgumentCaptor.forClass(Int::class.java)
        val captor3 = ArgumentCaptor.forClass(Int::class.java)
        Mockito.verify(client)?.execute<Any>(captor1.capture() as Client.Request<Any>, captor2.capture(), captor3.capture())
        Assert.assertEquals(Integer.valueOf(DatafileClient.REQUEST_BACKOFF_TIMEOUT), captor2.value)
        Assert.assertEquals(Integer.valueOf(DatafileClient.REQUEST_RETRIES_POWER), captor3.value)
        val response = captor1.value.execute()
        Assert.assertNull(response)
        Mockito.verify(logger)?.error("Unexpected response from data file cdn, status: {}", 300)
        Mockito.verify(urlConnection)?.disconnect()
    }

    @Test
    @Throws(IOException::class)
    fun handlesIOException() {
        val url = URL(DatafileConfig("1", null).url)
        Mockito.`when`(client!!.openConnection(url)).thenReturn(urlConnection)
        Mockito.`when`(urlConnection!!.responseCode).thenReturn(200)
        Mockito.doThrow(IOException()).`when`(urlConnection)?.connect()
        datafileClient!!.request(url.toString())
        val captor1 = ArgumentCaptor.forClass(Client.Request::class.java)
        val captor2 = ArgumentCaptor.forClass(Int::class.java)
        val captor3 = ArgumentCaptor.forClass(Int::class.java)
        Mockito.verify(client)?.execute<Any>(captor1.capture() as Client.Request<Any>, captor2.capture(), captor3.capture())
        Assert.assertEquals(Integer.valueOf(DatafileClient.REQUEST_BACKOFF_TIMEOUT), captor2.value)
        Assert.assertEquals(Integer.valueOf(DatafileClient.REQUEST_RETRIES_POWER), captor3.value)
        val response = captor1.value.execute()
        Assert.assertNull(response)
        Mockito.verify(logger)?.error(Matchers.contains("Error making request"), Matchers.any(IOException::class.java))
        Mockito.verify(urlConnection)?.disconnect()
        Mockito.verify(urlConnection)?.disconnect()
    }

    @Test
    @Throws(MalformedURLException::class)
    fun handlesNullResponse() {
        val url = URL(DatafileConfig("1", null).url)
        Mockito.`when`(client!!.execute<Any>(Matchers.any(), Matchers.eq(DatafileClient.REQUEST_BACKOFF_TIMEOUT), Matchers.eq(DatafileClient.REQUEST_RETRIES_POWER))).thenReturn(null)
        Assert.assertNull(datafileClient!!.request(url.toString()))
    }

    @Test
    @Throws(MalformedURLException::class)
    fun handlesEmptyStringResponse() {
        val url = URL(DatafileConfig("1", null).url)
        Mockito.`when`(client!!.execute<Any>(Matchers.any(Client.Request::class.java) as Client.Request<Any>, Matchers.eq(DatafileClient.REQUEST_BACKOFF_TIMEOUT), Matchers.eq(DatafileClient.REQUEST_RETRIES_POWER))).thenReturn("")
        Assert.assertEquals("", datafileClient!!.request(url.toString()))
    }
}