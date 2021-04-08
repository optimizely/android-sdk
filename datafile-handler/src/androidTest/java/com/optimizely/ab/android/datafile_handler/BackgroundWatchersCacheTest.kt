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

import androidx.test.platform.app.InstrumentationRegistry
import com.optimizely.ab.android.shared.Cache
import com.optimizely.ab.android.shared.DatafileConfig
import junit.framework.Assert
import junit.framework.Assert.assertFalse
import org.json.JSONException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.slf4j.Logger
import java.io.IOException


/**
 * Tests for [BackgroundWatchersCache]
 */
@RunWith(PowerMockRunner::class)
@PrepareForTest(Cache::class)
class BackgroundWatchersCacheTest {
    private var backgroundWatchersCache: BackgroundWatchersCache? = null
    private var cache: Cache? = null
    private var logger: Logger? = null
    @Before
    fun setup() {
        logger = PowerMockito.mock(Logger::class.java)
        cache = Cache(InstrumentationRegistry.getInstrumentation().targetContext, logger!!)
        backgroundWatchersCache = BackgroundWatchersCache(cache!!, logger!!)
    }

    @After
    fun tearDown() {
        cache!!.delete(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME)
    }

    @Test
    fun setIsWatchingEmptyString() {
        assertFalse(backgroundWatchersCache!!.setIsWatching(DatafileConfig("", null), false))
        verify(logger)?.error("Passed in an empty string for projectId")
    }

    @Test
    fun isWatchingEmptyString() {
        assertFalse(backgroundWatchersCache!!.isWatching(DatafileConfig("", null)))
        verify(logger)?.error("Passed in an empty string for projectId")
    }

    @Test
    fun setIsWatchingPersistsWithoutEnvironment() {
        Assert.assertTrue(backgroundWatchersCache!!.setIsWatching(DatafileConfig("1", null), true))
        Assert.assertTrue(backgroundWatchersCache!!.setIsWatching(DatafileConfig("2", null), true))
        Assert.assertTrue(backgroundWatchersCache!!.setIsWatching(DatafileConfig("3", null), false))
        Assert.assertTrue(backgroundWatchersCache!!.setIsWatching(DatafileConfig("1", null), false))
        Assert.assertFalse(backgroundWatchersCache!!.isWatching(DatafileConfig("1", null)))
        Assert.assertTrue(backgroundWatchersCache!!.isWatching(DatafileConfig("2", null)))
        Assert.assertFalse(backgroundWatchersCache!!.isWatching(DatafileConfig("3", null)))
        val watchingProjectIds = backgroundWatchersCache!!.watchingDatafileConfigs
        Assert.assertTrue(watchingProjectIds.contains(DatafileConfig("2", null)))
    }

    @Test
    fun setIsWatchingPersistsWithEnvironment() {
        Assert.assertTrue(backgroundWatchersCache!!.setIsWatching(DatafileConfig(null, "1-1"), true))
        Assert.assertTrue(backgroundWatchersCache!!.setIsWatching(DatafileConfig(null, "2-2"), true))
        Assert.assertTrue(backgroundWatchersCache!!.setIsWatching(DatafileConfig(null, "3-3"), false))
        Assert.assertTrue(backgroundWatchersCache!!.setIsWatching(DatafileConfig(null, "1-1"), false))
        Assert.assertFalse(backgroundWatchersCache!!.isWatching(DatafileConfig(null, "1-1")))
        Assert.assertTrue(backgroundWatchersCache!!.isWatching(DatafileConfig(null, "2-2")))
        Assert.assertFalse(backgroundWatchersCache!!.isWatching(DatafileConfig(null, "3-3")))
        val watchingProjectIds = backgroundWatchersCache!!.watchingDatafileConfigs
        Assert.assertTrue(watchingProjectIds.contains(DatafileConfig(null, "2-2")))
    }

    @Test
    @Throws(IOException::class)
    fun testExceptionHandling() {
        val cache = PowerMockito.mock(Cache::class.java)
        val backgroundWatchersCache = BackgroundWatchersCache(cache, logger!!)
        // Cause a JSONException to be thrown
        Mockito.`when`(cache.load(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME)).thenReturn("{")
        assertFalse(backgroundWatchersCache.setIsWatching(DatafileConfig("1", null), true))
        verify(logger)?.error(Matchers.contains("Unable to update watching state for project id"), Matchers.any(JSONException::class.java))
        assertFalse(backgroundWatchersCache.isWatching(DatafileConfig("1", null)))
        verify(logger)?.error(Matchers.contains("Unable check if project id is being watched"), Matchers.any(JSONException::class.java))
        val watchingProjectIds = backgroundWatchersCache.watchingDatafileConfigs
        Assert.assertTrue(watchingProjectIds.isEmpty())
        verify(logger)?.error(Matchers.contains("Unable to get watching project ids"), Matchers.any(JSONException::class.java))
    }

    @Test
    fun testLoadFileNotFound() {
        val cache = Mockito.mock(Cache::class.java)
        val backgroundWatchersCache = BackgroundWatchersCache(cache, logger!!)
        // Cause a JSONException to be thrown
        Mockito.`when`(cache.load(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME)).thenReturn(null)
        assertFalse(backgroundWatchersCache.setIsWatching(DatafileConfig("1", null), true))
        verify(logger)?.info("Creating background watchers file {}.", BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME)
    }
}