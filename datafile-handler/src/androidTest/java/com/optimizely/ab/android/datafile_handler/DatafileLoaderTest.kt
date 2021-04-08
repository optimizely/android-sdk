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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.optimizely.ab.android.shared.Cache
import com.optimizely.ab.android.shared.Client
import junit.framework.Assert
import org.json.JSONException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mockito
import org.slf4j.Logger
import java.io.IOException
import java.net.MalformedURLException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Tests for [DatafileLoader]
 */
@RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
@RunWith(AndroidJUnit4::class)
class DatafileLoaderTest {
    private var datafileCache: DatafileCache? = null
    private var datafileClient: DatafileClient? = null
    private var client: Client? = null
    private var logger: Logger? = null
    private var datafileLoadedListener: DatafileLoadedListener? = null
    var context = InstrumentationRegistry.getInstrumentation().targetContext
    @Before
    fun setup() {
        logger = Mockito.mock(Logger::class.java)
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        datafileCache = DatafileCache("1", Cache(targetContext, logger!!), logger!!)
        client = Mockito.mock(Client::class.java)
        datafileClient = DatafileClient(client!!, logger!!)
        datafileLoadedListener = Mockito.mock(DatafileLoadedListener::class.java)
    }

    @After
    fun tearDown() {
        datafileCache!!.delete()
    }

    @Test
    @Throws(MalformedURLException::class, JSONException::class)
    fun loadFromCDNWhenNoCachedFile() {
        val executor = Executors.newSingleThreadExecutor()
        val datafileLoader = DatafileLoader(context, datafileClient!!, datafileCache!!, logger!!)
        Mockito.`when`(client!!.execute<Any>(Matchers.any(Client.Request::class.java) as Client.Request<Any>, Matchers.anyInt(), Matchers.anyInt())).thenReturn("{}")
        datafileLoader.getDatafile("1", datafileLoadedListener)
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail()
        }
        val cachedDatafile = datafileCache!!.load()
        Assert.assertNotNull(cachedDatafile)
        Assert.assertEquals("{}", cachedDatafile.toString())
        Mockito.verify(datafileLoadedListener, Mockito.atMost(1))?.onDatafileLoaded("{}")
    }

    @Test
    fun loadWhenCacheFileExistsAndCDNNotModified() {
        val executor = Executors.newSingleThreadExecutor()
        val datafileLoader = DatafileLoader(context, datafileClient!!, datafileCache!!, logger!!)
        datafileCache!!.save("{}")
        Mockito.`when`(client!!.execute<Any>(Matchers.any(Client.Request::class.java) as Client.Request<Any>, Matchers.anyInt(), Matchers.anyInt())).thenReturn("")
        datafileLoader.getDatafile("1", datafileLoadedListener)
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail()
        }
        val cachedDatafile = datafileCache!!.load()
        Assert.assertNotNull(cachedDatafile)
        Assert.assertEquals("{}", cachedDatafile.toString())
        Mockito.verify(datafileLoadedListener, Mockito.atMost(1))?.onDatafileLoaded("{}")
    }

    @Test
    fun noCacheAndLoadFromCDNFails() {
        val executor = Executors.newSingleThreadExecutor()
        val datafileLoader = DatafileLoader(context, datafileClient!!, datafileCache!!, logger!!)
        Mockito.`when`(client!!.execute<Any>(Matchers.any(Client.Request::class.java) as Client.Request<Any>, Matchers.anyInt(), Matchers.anyInt())).thenReturn(null)
        datafileLoader.getDatafile("1", datafileLoadedListener)
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail()
        }
        val cachedDatafile = datafileCache!!.load()
        Assert.assertNull(cachedDatafile)
        Mockito.verify(datafileLoadedListener, Mockito.atMost(1))?.onDatafileLoaded(null)
    }

    @Test
    @Throws(IOException::class)
    fun warningsAreLogged() {
        val executor = Executors.newSingleThreadExecutor()
        val cache = Mockito.mock(Cache::class.java)
        datafileCache = DatafileCache("warningsAreLogged", cache, logger!!)
        val datafileLoader = DatafileLoader(context, datafileClient!!, datafileCache!!, logger!!)
        Mockito.`when`(client!!.execute<Any>(Matchers.any(Client.Request::class.java) as Client.Request<Any>, Matchers.anyInt(), Matchers.anyInt())).thenReturn("{}")
        Mockito.`when`(cache.exists(datafileCache!!.fileName)).thenReturn(true)
        Mockito.`when`(cache.delete(datafileCache!!.fileName)).thenReturn(false)
        Mockito.`when`(cache.save(datafileCache!!.fileName, "{}")).thenReturn(false)
        datafileLoader.getDatafile("warningsAreLogged", datafileLoadedListener)
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail()
        }
        Mockito.verify(logger)?.warn("Unable to delete old datafile")
        Mockito.verify(logger)?.warn("Unable to save new datafile")
        Mockito.verify(datafileLoadedListener, Mockito.atMost(1))?.onDatafileLoaded("{}")
    }

    @Test
    @Throws(IOException::class)
    fun debugLogged() {
        val executor = Executors.newSingleThreadExecutor()
        val cache = Mockito.mock(Cache::class.java)
        datafileCache = DatafileCache("debugLogged", cache, logger!!)
        val datafileLoader = DatafileLoader(context, datafileClient!!, datafileCache!!, logger!!)
        Mockito.`when`(client!!.execute<Any>(Matchers.any(Client.Request::class.java) as Client.Request<Any>, Matchers.anyInt(), Matchers.anyInt())).thenReturn("{}")
        Mockito.`when`(cache.save(datafileCache!!.fileName, "{}")).thenReturn(true)
        Mockito.`when`(cache.exists(datafileCache!!.fileName)).thenReturn(true)
        Mockito.`when`(cache.load(datafileCache!!.fileName)).thenReturn("{}")
        datafileLoader.getDatafile("debugLogged", datafileLoadedListener)
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        datafileLoader.getDatafile("debugLogged", datafileLoadedListener)
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail()
        }
        Mockito.verify(logger)?.debug("Last download happened under 1 minute ago. Throttled to be at least 1 minute apart.")
        Mockito.verify(datafileLoadedListener, Mockito.atMost(2))?.onDatafileLoaded("{}")
        Mockito.verify(datafileLoadedListener, Mockito.atLeast(1))?.onDatafileLoaded("{}")
    }

    @Test
    @Throws(IOException::class)
    fun downloadAllowedNoCache() {
        val executor = Executors.newSingleThreadExecutor()
        val cache = Mockito.mock(Cache::class.java)
        datafileCache = DatafileCache("downloadAllowedNoCache", cache, logger!!)
        val datafileLoader = DatafileLoader(context, datafileClient!!, datafileCache!!, logger!!)
        Mockito.`when`(client!!.execute<Any>(Matchers.any(Client.Request::class.java) as Client.Request<Any>, Matchers.anyInt(), Matchers.anyInt())).thenReturn("{}")
        Mockito.`when`(cache.save(datafileCache!!.fileName, "{}")).thenReturn(false)
        Mockito.`when`(cache.exists(datafileCache!!.fileName)).thenReturn(false)
        Mockito.`when`(cache.load(datafileCache!!.fileName)).thenReturn("{}")
        datafileLoader.getDatafile("downloadAllowedNoCache", datafileLoadedListener)
        datafileLoader.getDatafile("downloadAllowedNoCache", datafileLoadedListener)
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail()
        }
        Mockito.verify(logger, Mockito.never())?.debug("Last download happened under 1 minute ago. Throttled to be at least 1 minute apart.")
        Mockito.verify(datafileLoadedListener, Mockito.atMost(2))?.onDatafileLoaded("{}")
        Mockito.verify(datafileLoadedListener, Mockito.atLeast(1))?.onDatafileLoaded("{}")
    }

    @Test
    @Throws(IOException::class)
    fun debugLoggedMultiThreaded() {
        val executor = Executors.newSingleThreadExecutor()
        val cache = Mockito.mock(Cache::class.java)
        datafileCache = DatafileCache("debugLoggedMultiThreaded", cache, logger!!)
        val datafileLoader = DatafileLoader(context, datafileClient!!, datafileCache!!, logger!!)
        Mockito.`when`(client!!.execute<Any>(Matchers.any(Client.Request::class.java) as Client.Request<Any>, Matchers.anyInt(), Matchers.anyInt())).thenReturn("{}")
        Mockito.`when`(cache.exists(datafileCache!!.fileName)).thenReturn(true)
        Mockito.`when`(cache.delete(datafileCache!!.fileName)).thenReturn(true)
        Mockito.`when`(cache.exists(datafileCache!!.fileName)).thenReturn(true)
        Mockito.`when`(cache.load(datafileCache!!.fileName)).thenReturn("{}")
        Mockito.`when`(cache.save(datafileCache!!.fileName, "{}")).thenReturn(true)
        datafileLoader.getDatafile("debugLoggedMultiThreaded", datafileLoadedListener)
        val r = Runnable { datafileLoader.getDatafile("debugLoggedMultiThreaded", datafileLoadedListener) }
        Thread(r).start()
        Thread(r).start()
        Thread(r).start()
        Thread(r).start()
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail()
        }
        Mockito.verify(datafileLoadedListener, Mockito.atMost(5))?.onDatafileLoaded("{}")
        Mockito.verify(datafileLoadedListener, Mockito.atLeast(1))?.onDatafileLoaded("{}")
    }

    private fun setTestDownloadFrequency(datafileLoader: DatafileLoader, value: Long) {
        try {
            val betweenDownloadsMilli = DatafileLoader::class.java.getDeclaredField("minTimeBetweenDownloadsMilli")
            betweenDownloadsMilli.isAccessible = true

            //Field modifiersField;
            //modifiersField = Field.class.getDeclaredField("modifiers");
            //modifiersField.setAccessible(true);
            //modifiersField.setInt(betweenDownloadsMilli, betweenDownloadsMilli.getModifiers() & ~Modifier.FINAL);
            betweenDownloadsMilli[null] = value
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    @Test
    @Throws(IOException::class)
    fun allowDoubleDownload() {
        val executor = Executors.newSingleThreadExecutor()
        val cache = Mockito.mock(Cache::class.java)
        datafileCache = DatafileCache("allowDoubleDownload", cache, logger!!)
        val datafileLoader = DatafileLoader(context, datafileClient!!, datafileCache!!, logger!!)

        // set download time to 1 second
        setTestDownloadFrequency(datafileLoader, 1000L)
        Mockito.`when`(client!!.execute<Any>(Matchers.any(Client.Request::class.java) as Client.Request<Any>, Matchers.anyInt(), Matchers.anyInt())).thenReturn("{}")
        datafileLoader.getDatafile("allowDoubleDownload", datafileLoadedListener)
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail()
        }
        datafileLoader.getDatafile("allowDoubleDownload", datafileLoadedListener)

        // reset back to normal.
        setTestDownloadFrequency(datafileLoader, 60 * 1000L)
        Mockito.verify(logger, Mockito.never())?.debug("Last download happened under 1 minute ago. Throttled to be at least 1 minute apart.")
        Mockito.verify(datafileLoadedListener, Mockito.atMost(2))?.onDatafileLoaded("{}")
        Mockito.verify(datafileLoadedListener, Mockito.atLeast(1))?.onDatafileLoaded("{}")
    }
}