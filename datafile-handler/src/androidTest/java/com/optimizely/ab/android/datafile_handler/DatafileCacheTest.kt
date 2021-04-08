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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.optimizely.ab.android.shared.Cache
import junit.framework.Assert
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mockito
import org.slf4j.Logger
import java.io.IOException

/**
 * Tests for [DatafileCache]
 */
@RunWith(AndroidJUnit4::class)
class DatafileCacheTest {
    private var datafileCache: DatafileCache? = null
    private var logger: Logger? = null
    @Before
    fun setup() {
        logger = Mockito.mock(Logger::class.java)
        val cache = Cache(InstrumentationRegistry.getInstrumentation().targetContext, logger!!)
        datafileCache = DatafileCache("1", cache, logger!!)
    }

    @Test
    fun loadBeforeSaving() {
        Assert.assertNull(datafileCache!!.load())
    }

    @Test
    @Throws(JSONException::class)
    fun persistence() {
        Assert.assertTrue(datafileCache!!.save("{}"))
        val jsonObject = datafileCache!!.load()
        Assert.assertNotNull(jsonObject)
        val actual = jsonObject.toString()
        Assert.assertEquals(JSONObject("{}").toString(), actual)
        Assert.assertTrue(datafileCache!!.delete())
        Assert.assertNull(datafileCache!!.load())
    }

    @Test
    @Throws(IOException::class)
    fun loadJsonException() {
        val cache = Mockito.mock(Cache::class.java)
        val datafileCache = DatafileCache("1", cache, logger!!)
        Mockito.`when`(cache.load(datafileCache.fileName)).thenReturn("{")
        Assert.assertNull(datafileCache.load())
        Mockito.verify(logger)?.error(Matchers.contains("Unable to parse data file"), Matchers.any(JSONException::class.java))
    }
}