/****************************************************************************
 * Copyright 2016-2017, Optimizely, Inc. and contributors                   *
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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.slf4j.Logger
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * Tests for [CacheTest]
 */
@RunWith(AndroidJUnit4::class)
class CacheTest {
    private var cache: Cache? = null
    private var logger: Logger? = null
    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        logger = Mockito.mock(Logger::class.java)
        cache = Cache(context, logger)
    }

    @Test
    @Throws(IOException::class)
    fun testSaveExistsLoadAndDelete() {
        Assert.assertTrue(cache!!.save(FILENAME, "bar"))
        Assert.assertTrue(cache!!.exists(FILENAME))
        val data = cache!!.load(FILENAME)
        Assert.assertEquals("bar", data)
        Assert.assertTrue(cache!!.delete(FILENAME))
    }

    @Test
    fun testDeleteFail() {
        Assert.assertFalse(cache!!.delete(FILENAME))
    }

    @Test
    fun testExistsFalse() {
        Assert.assertFalse(cache!!.exists(FILENAME))
    }

    @Test
    @Throws(FileNotFoundException::class)
    fun testLoadFileNotFoundExceptionReturnsNull() {
        val context = Mockito.mock(Context::class.java)
        val cache = Cache(context, logger!!)
        Mockito.`when`(context.openFileInput(FILENAME)).thenThrow(FileNotFoundException())
        Assert.assertNull(cache.load(FILENAME))
        Mockito.verify(logger)?.warn("Unable to load file {}.", FILENAME)
    }

    @Test
    @Throws(IOException::class)
    fun testSaveFail() {
        val context = Mockito.mock(Context::class.java)
        val cache = Cache(context, logger!!)
        val fileOutputStream = Mockito.mock(FileOutputStream::class.java)
        val data = "{}"
        Mockito.doThrow(IOException()).`when`(fileOutputStream).write(data.toByteArray())
        Mockito.`when`(context.openFileOutput(FILENAME, Context.MODE_PRIVATE)).thenReturn(fileOutputStream)
        Assert.assertFalse(cache.save(FILENAME, data))
        Mockito.verify(logger)?.error("Error saving file {}.", FILENAME)
    }

    companion object {
        private const val FILENAME = "foo.txt"
    }
}