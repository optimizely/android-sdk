/****************************************************************************
 * Copyright 2021, Optimizely, Inc. and contributors                        *
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
package com.optimizely.ab.android.user_profile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.optimizely.ab.android.shared.Cache
import com.optimizely.ab.android.user_profile.UserProfileCache.LegacyDiskCache
import junit.framework.Assert
import org.json.JSONException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mockito
import org.slf4j.Logger
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Tests for [UserProfileCache.LegacyDiskCache]
 */
@RunWith(AndroidJUnit4::class)
class LegacyDiskCacheTest {
    // Runs tasks serially on the calling thread
    private val executor = Executors.newSingleThreadExecutor()
    private var cache: Cache? = null
    private var logger: Logger? = null
    private var legacyDiskCache: LegacyDiskCache? = null
    private var projectId: String? = null
    @Before
    fun setup() {
        logger = Mockito.mock(Logger::class.java)
        cache = Cache(InstrumentationRegistry.getInstrumentation().targetContext, logger!!)
        projectId = "123"
        legacyDiskCache = LegacyDiskCache(cache!!, executor, logger!!, projectId!!)
    }

    @After
    fun teardown() {
        cache!!.delete(legacyDiskCache!!.fileName)
    }

    @Test
    fun testGetFileName() {
        Assert.assertEquals("optly-user-profile-123.json", legacyDiskCache!!.fileName)
    }

    @Test
    @Throws(JSONException::class)
    fun testLoadWhenNoFile() {
        Assert.assertNull(legacyDiskCache!!.load())
        Mockito.verify(logger)?.warn("Unable to load file {}.", legacyDiskCache!!.fileName)
        Mockito.verify(logger)?.info("Legacy user profile cache not found.")
    }

    @Test
    @Throws(JSONException::class)
    fun testLoadMalformedCache() {
        cache = Mockito.mock(Cache::class.java)
        Mockito.`when`(cache?.load(legacyDiskCache!!.fileName)).thenReturn("{?}")
        Mockito.`when`(cache?.delete(legacyDiskCache!!.fileName)).thenReturn(true)
        legacyDiskCache = LegacyDiskCache(cache!!, executor, logger!!, projectId!!)
        Assert.assertNull(legacyDiskCache!!.load())
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Timed out")
        }
        Mockito.verify(logger)?.info("Deleted legacy user profile from disk.")
        Mockito.verify(logger)?.warn(Matchers.eq("Unable to parse legacy user profiles. Will delete legacy user profile cache file."),
                Matchers.any(Exception::class.java))
    }

    @Test
    @Throws(JSONException::class)
    fun testDelete() {
        cache = Mockito.mock(Cache::class.java)
        Mockito.`when`(cache?.delete(legacyDiskCache!!.fileName)).thenReturn(true)
        legacyDiskCache = LegacyDiskCache(cache!!, executor, logger!!, projectId!!)
        legacyDiskCache!!.delete()
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Timed out")
        }
        Mockito.verify(logger)?.info("Deleted legacy user profile from disk.")
    }

    @Test
    @Throws(JSONException::class)
    fun testDeleteFailed() {
        cache = Mockito.mock(Cache::class.java)
        Mockito.`when`(cache?.delete(legacyDiskCache!!.fileName)).thenReturn(false)
        legacyDiskCache = LegacyDiskCache(cache!!, executor, logger!!, projectId!!)
        legacyDiskCache!!.delete()
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Timed out")
        }
        Mockito.verify(logger)?.warn("Unable to delete legacy user profile from disk.")
    }
}