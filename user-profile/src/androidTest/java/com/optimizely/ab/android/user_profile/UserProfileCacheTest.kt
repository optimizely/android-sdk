/****************************************************************************
 * Copyright 2021, Optimizely, Inc. and contributors                   *
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
import com.optimizely.ab.android.user_profile.UserProfileCache.LegacyDiskCache
import junit.framework.Assert
import org.json.JSONException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Tests for [UserProfileCache]
 */
@RunWith(AndroidJUnit4::class)
class UserProfileCacheTest {
    // Runs tasks serially on the calling thread
    private val executor = Executors.newSingleThreadExecutor()
    private var logger: Logger? = null
    private var cache: Cache? = null
    private var diskCache: DiskCache? = null
    private var legacyDiskCache: LegacyDiskCache? = null
    private var memoryCache: HashMap<String, Map<String, Any>>? = null
    private var projectId: String? = null
    private var userProfileCache: UserProfileCache? = null
    private var userId1: String? = null
    private var userId2: String? = null
    private var userProfileMap1: ConcurrentHashMap<String, Any>? = null
    private var userProfileMap2: ConcurrentHashMap<String, Any>? = null
    @Before
    @Throws(JSONException::class)
    fun setup() {
        logger = Mockito.mock(Logger::class.java)
        projectId = "1"
        cache = Cache(InstrumentationRegistry.getInstrumentation().targetContext, logger)
        diskCache = DiskCache(cache!!, executor, logger!!, projectId!!)
        legacyDiskCache = LegacyDiskCache(cache!!, executor, logger!!, projectId!!)
        memoryCache = HashMap()
        userProfileCache = UserProfileCache(diskCache!!, logger!!, memoryCache!!, legacyDiskCache!!)

        // Test data.
        userId1 = "user_1"
        val experimentBucketMap1: MutableMap<String, Map<String, String>> = ConcurrentHashMap()
        val decisionMap1: MutableMap<String, String> = ConcurrentHashMap()
        decisionMap1["variation_id"] = "var_1"
        experimentBucketMap1["exp_1"] = decisionMap1
        val decisionMap2: MutableMap<String, String> = ConcurrentHashMap()
        decisionMap2["variation_id"] = "var_2"
        experimentBucketMap1["exp_2"] = decisionMap2
        userProfileMap1 = ConcurrentHashMap()
        userProfileMap1!!["user_id"] = userId1!!
        userProfileMap1!!["experiment_bucket_map"] = experimentBucketMap1
        userId2 = "user_2"
        val experimentBucketMap2: MutableMap<String, Map<String, String>> = ConcurrentHashMap()
        val decisionMap3: MutableMap<String, String> = ConcurrentHashMap()
        decisionMap3["variation_id"] = "var_3"
        experimentBucketMap2["exp_1"] = decisionMap3
        val decisionMap4: MutableMap<String, String> = ConcurrentHashMap()
        decisionMap4["variation_id"] = "var_4"
        experimentBucketMap2["exp_2"] = decisionMap4
        userProfileMap2 = ConcurrentHashMap()
        userProfileMap2!!["user_id"] = userId2!!
        userProfileMap2!!["experiment_bucket_map"] = experimentBucketMap2
    }

    @After
    fun teardown() {
        cache!!.delete(userProfileCache!!.diskCache.fileName)
    }

    @Test
    @Throws(JSONException::class)
    fun testClear() {
        userProfileCache!!.save(userProfileMap1!!)
        Mockito.verify(logger)?.info("Saved user profile for {}.", userId1)
        userProfileCache!!.save(userProfileMap2!!)
        Mockito.verify(logger)?.info("Saved user profile for {}.", userId2)
        userProfileCache!!.clear()
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Time out")
        }
        userProfileCache!!.start()
        Assert.assertNull(userProfileCache!!.lookup(userId1))
        Assert.assertNull(userProfileCache!!.lookup(userId2))
    }

    @Test
    @Throws(JSONException::class)
    fun testLookupInvalidUserId() {
        userProfileCache!!.lookup(null)
        Mockito.verify(logger)?.error("Unable to lookup user profile because user ID was null.")
        userProfileCache!!.lookup("")
        Mockito.verify(logger)?.error("Unable to lookup user profile because user ID was empty.")
    }

    @Test
    @Throws(JSONException::class)
    fun testRemove() {
        userProfileCache!!.save(userProfileMap1!!)
        Mockito.verify(logger)?.info("Saved user profile for {}.", userId1)
        userProfileCache!!.save(userProfileMap2!!)
        Mockito.verify(logger)?.info("Saved user profile for {}.", userId2)
        userProfileCache!!.remove(userId1)
        // give cache a chance to save.  we should actually wait on the executor.
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        userProfileCache!!.start()
        Assert.assertNull(userProfileCache!!.lookup(userId1))
        Assert.assertNotNull(userProfileCache!!.lookup(userId2))
    }

    @Test
    @Throws(JSONException::class)
    fun testRemoveInvalidUserId() {
        userProfileCache!!.remove(null)
        Mockito.verify(logger)?.error("Unable to remove user profile because user ID was null.")
        userProfileCache!!.remove("")
        Mockito.verify(logger)?.error("Unable to remove user profile because user ID was empty.")
    }

    @Test
    @Throws(JSONException::class)
    fun testRemoveDecision() {
        userProfileCache!!.save(userProfileMap1!!)
        Mockito.verify(logger)?.info("Saved user profile for {}.", userId1)
        userProfileCache!!.save(userProfileMap2!!)
        Mockito.verify(logger)?.info("Saved user profile for {}.", userId2)
        userProfileCache!!.remove(userId1, "exp_1")
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        userProfileCache!!.start()
        val userProfileMap1 = userProfileCache!!.lookup(userId1)
        val experimentBucketMap1 = userProfileMap1!!["experiment_bucket_map"] as Map<String, Map<String, String>>?
        Assert.assertNull(experimentBucketMap1!!["exp_1"])
        Assert.assertNotNull(experimentBucketMap1["exp_2"])
        Assert.assertNotNull(userProfileCache!!.lookup(userId2))
    }

    @Test
    @Throws(JSONException::class)
    fun testRemoveDecisionInvalidUserIdAndExperimentId() {
        userProfileCache!!.remove(null, "1")
        Mockito.verify(logger)?.error("Unable to remove decision because user ID was null.")
        userProfileCache!!.remove("", "1")
        Mockito.verify(logger)?.error("Unable to remove decision because user ID was empty.")
        userProfileCache!!.remove("1", null)
        Mockito.verify(logger)?.error("Unable to remove decision because experiment ID was null.")
        userProfileCache!!.remove("1", "")
        Mockito.verify(logger)?.error("Unable to remove decision because experiment ID was empty.")
    }

    @Test
    @Throws(JSONException::class)
    fun testSaveAndLookup() {
        userProfileCache!!.save(userProfileMap1!!)
        Mockito.verify(logger)?.info("Saved user profile for {}.", userId1)
        userProfileCache!!.save(userProfileMap2!!)
        Mockito.verify(logger)?.info("Saved user profile for {}.", userId2)
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Time out")
        }
        Mockito.verify(logger, Mockito.times(2))?.info("Saved user profiles to disk.")
        userProfileCache!!.start()
        val userProfileMap1 = userProfileCache!!.lookup(userId1)
        Assert.assertNotNull(userProfileMap1)
        Assert.assertNotNull(userProfileMap1!!["user_id"])
        Assert.assertEquals(userId1, userProfileMap1["user_id"] as String?)
        val experimentBucketMap1: Map<String, Map<String, String>>? = userProfileMap1["experiment_bucket_map"] as ConcurrentHashMap<String, Map<String, String>>?
        Assert.assertEquals("var_1", experimentBucketMap1!!["exp_1"]!!["variation_id"])
        Assert.assertEquals("var_2", experimentBucketMap1["exp_2"]!!["variation_id"])
        val userProfileMap2 = userProfileCache!!.lookup(userId2)
        Assert.assertEquals(userId2, userProfileMap2!!["user_id"] as String?)
        val experimentBucketMap2: Map<String, Map<String, String>>? = userProfileMap2["experiment_bucket_map"] as ConcurrentHashMap<String, Map<String, String>>?
        Assert.assertEquals("var_3", experimentBucketMap2!!["exp_1"]!!["variation_id"])
        Assert.assertEquals("var_4", experimentBucketMap2["exp_2"]!!["variation_id"])
    }

    @Test
    @Throws(JSONException::class)
    fun testSaveInvalidUserId() {
        userProfileMap1!!.remove("user_id")
        userProfileCache!!.save(userProfileMap1!!)
        Mockito.verify(logger)?.error("Unable to save user profile because user ID was null.")
        userProfileMap1!!["user_id"] = ""
        userProfileCache!!.save(userProfileMap1!!)
        Mockito.verify(logger)?.error("Unable to save user profile because user ID was empty.")
    }
}