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
package com.optimizely.ab.android.user_profile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.optimizely.ab.android.shared.Cache
import com.optimizely.ab.android.user_profile.UserProfileCache.DiskCache
import junit.framework.Assert
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mockito
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Tests for [UserProfileCache.DiskCache]
 */
@RunWith(AndroidJUnit4::class)
class DiskCacheTest {
    // Runs tasks serially on the calling thread
    private val executor = Executors.newSingleThreadExecutor()
    private var cache: Cache? = null
    private var logger: Logger? = null
    private var diskCache: DiskCache? = null
    private var memoryCache: ConcurrentHashMap<String, Map<String, Any>>? = null
    private var projectId: String? = null
    private var userId: String? = null
    @Before
    fun setup() {
        logger = Mockito.mock(Logger::class.java)
        cache = Cache(InstrumentationRegistry.getInstrumentation().targetContext, logger!!)
        memoryCache = ConcurrentHashMap()
        projectId = "123"
        diskCache = DiskCache(cache!!, executor, logger!!, projectId!!)
        userId = "user_1"

        // Populate in-memory cache.
        val experimentBucketMap: MutableMap<String, Map<String, String>> = ConcurrentHashMap()
        val decisionMap1: MutableMap<String, String> = ConcurrentHashMap()
        decisionMap1["variation_id"] = "var_1"
        experimentBucketMap["exp_1"] = decisionMap1
        val decisionMap2: MutableMap<String, String> = ConcurrentHashMap()
        decisionMap2["variation_id"] = "var_2"
        experimentBucketMap["exp_2"] = decisionMap2
        val userProfileMap: MutableMap<String, Any> = ConcurrentHashMap()
        userProfileMap["user_id"] = userId!!
        userProfileMap["experiment_bucket_map"] = experimentBucketMap
        memoryCache!![userId!!] = userProfileMap
    }

    @After
    fun teardown() {
        cache!!.delete(diskCache!!.fileName)
    }

    @Test
    fun testGetFileName() {
        Assert.assertEquals("optly-user-profile-service-123.json", diskCache!!.fileName)
    }

    @Test
    @Throws(JSONException::class)
    fun testLoadWhenNoFile() {
        Assert.assertEquals(diskCache!!.load().toString(), JSONObject().toString())
        Mockito.verify(logger)?.warn("Unable to load file {}.", diskCache!!.fileName)
        Mockito.verify(logger)?.warn("Unable to load user profile cache from disk.")
    }

    @Test
    @Throws(JSONException::class)
    fun testLoadIOException() {
        cache = Mockito.mock(Cache::class.java)
        Mockito.`when`(cache?.load(diskCache!!.fileName)).thenReturn(null)
        diskCache = DiskCache(cache!!, executor, logger!!, projectId!!)
        Assert.assertEquals(JSONObject().toString(), diskCache!!.load().toString())
        Mockito.verify(logger)?.warn("Unable to load user profile cache from disk.")
    }

    @Test
    @Throws(JSONException::class)
    fun testSaveAndLoad() {
        diskCache!!.save(memoryCache!!)
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Timed out")
        }
        Mockito.verify(logger)?.info("Saved user profiles to disk.")
        val json = diskCache!!.load()
        Assert.assertTrue(json.has(userId))
        val userProfileJson = json.getJSONObject(userId)
        Assert.assertEquals(userId, userProfileJson.getString("user_id"))
        Assert.assertTrue(userProfileJson.has("experiment_bucket_map"))
        val experimentBucketMapJson = userProfileJson.getJSONObject("experiment_bucket_map")
        Assert.assertTrue(experimentBucketMapJson.has("exp_1"))
        val decisionMapJson1 = experimentBucketMapJson.getJSONObject("exp_1")
        Assert.assertEquals("var_1", decisionMapJson1.getString("variation_id"))
        Assert.assertTrue(experimentBucketMapJson.has("exp_2"))
        val decisionMapJson2 = experimentBucketMapJson.getJSONObject("exp_2")
        Assert.assertEquals("var_2", decisionMapJson2.getString("variation_id"))
    }

    @Test
    @Throws(JSONException::class)
    fun testSaveIOException() {
        cache = Mockito.mock(Cache::class.java)
        Mockito.`when`(cache?.save(diskCache!!.fileName, memoryCache.toString())).thenReturn(false)
        diskCache = DiskCache(cache!!, executor, logger!!, projectId!!)
        diskCache!!.save(memoryCache!!)
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Timed out")
        }
        Mockito.verify(logger)?.warn("Unable to save user profiles to disk.")
    }

    @Test
    @Throws(JSONException::class)
    fun testSaveInvalidMemoryCache() {
        memoryCache!!["user_2"] = ConcurrentHashMap()
        diskCache!!.save(memoryCache!!)
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Timed out")
        }
        Mockito.verify(logger)?.error(Matchers.eq("Unable to serialize user profiles to save to disk."), Matchers.any(Exception::class.java))
    }
}