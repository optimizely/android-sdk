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
import com.optimizely.ab.android.user_profile.UserProfileCache.LegacyDiskCache
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Tests for [DefaultUserProfileService]
 */
@RunWith(AndroidJUnit4::class)
class DefaultUserProfileServiceTest {
    private var userProfileService: DefaultUserProfileService? = null
    private var cache: Cache? = null
    private var diskCache: DiskCache? = null
    private var executor: ExecutorService? = null
    private var logger: Logger? = null
    private var legacyDiskCache: LegacyDiskCache? = null
    private var memoryCache: HashMap<String, *>? = null
    private var projectId: String? = null
    private var userProfileCache: UserProfileCache? = null
    private var userId1: String? = null
    private var userId2: String? = null
    private var userProfileMap1: ConcurrentHashMap<String, *>? = null
    private var userProfileMap2: ConcurrentHashMap<String, *>? = null
    @Before
    fun setup() {
        logger = Mockito.mock(Logger::class.java)
        cache = Cache(InstrumentationRegistry.getInstrumentation().targetContext, logger!!)
        executor = Executors.newSingleThreadExecutor()
        projectId = "123"
        legacyDiskCache = LegacyDiskCache(cache!!, executor!!, logger!!, projectId!!)
        memoryCache =  HashMap<String, Any>()
        diskCache = DiskCache(cache!!, executor!!, logger!!, projectId!!)
        userProfileCache = UserProfileCache(diskCache!!, logger!!, memoryCache!! as HashMap<String, Map<String, Any>>, legacyDiskCache!!)
        userProfileService = DefaultUserProfileService(userProfileCache!!, logger!!)

        // Test data.
        userId1 = "user_1"
        val experimentBucketMap1: MutableMap<String, Map<String, String>> = ConcurrentHashMap()
        val decisionMap1: MutableMap<String, String> = ConcurrentHashMap()
        decisionMap1["variation_id"] = "var_1"
        experimentBucketMap1["exp_1"] = decisionMap1
        val decisionMap2: MutableMap<String, String> = ConcurrentHashMap()
        decisionMap2["variation_id"] = "var_2"
        experimentBucketMap1["exp_2"] = decisionMap2
        userProfileMap1 = ConcurrentHashMap<String, Any>()
        (userProfileMap1 as ConcurrentHashMap<String, Any>)["user_id"] = userId1!!
        (userProfileMap1 as ConcurrentHashMap<String, Any>)["experiment_bucket_map"] = experimentBucketMap1
        userId2 = "user_2"
        val experimentBucketMap2: MutableMap<String, Map<String, String>> = ConcurrentHashMap()
        val decisionMap3: MutableMap<String, String> = ConcurrentHashMap()
        decisionMap3["variation_id"] = "var_3"
        experimentBucketMap2["exp_1"] = decisionMap3
        val decisionMap4: MutableMap<String, String> = ConcurrentHashMap()
        decisionMap4["variation_id"] = "var_4"
        experimentBucketMap2["exp_2"] = decisionMap4
        userProfileMap2 = ConcurrentHashMap<String, Any>()
        (userProfileMap2 as ConcurrentHashMap<String, Any>)["user_id"] = userId2!!
        (userProfileMap2 as ConcurrentHashMap<String, Any>)["experiment_bucket_map"] = experimentBucketMap2
    }

    @After
    fun teardown() {
        cache!!.delete(diskCache!!.fileName)
    }

    @Test
    fun saveAndStartAndLookup() {
        userProfileService!!.save(userProfileMap1!!)
        userProfileService!!.save(userProfileMap2!!)
        try {
            executor!!.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Time out")
        }
        userProfileService!!.start()
        val userProfileMap = userProfileService!!.lookup(userId2!!)
        Assert.assertEquals(userId2, userProfileMap!!["user_id"])
        Assert.assertTrue(userProfileMap.containsKey("experiment_bucket_map"))
        val experimentBucketMap: Map<String, Map<String, String>>? = userProfileMap["experiment_bucket_map"] as ConcurrentHashMap<String, Map<String, String>>?
        Assert.assertEquals("var_3", experimentBucketMap!!["exp_1"]!!["variation_id"])
    }

    @Test
    fun remove() {
        userProfileService!!.save(userProfileMap1!!)
        userProfileService!!.save(userProfileMap2!!)
        try {
            executor!!.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Time out")
        }
        userProfileService!!.remove(userId1)
        Assert.assertNull(userProfileService!!.lookup(userId1!!))
        Assert.assertNotNull(userProfileService!!.lookup(userId2!!))
        val userProfileMap = userProfileService!!.lookup(userId2!!)
        Assert.assertEquals(userId2, userProfileMap!!["user_id"])
        Assert.assertTrue(userProfileMap.containsKey("experiment_bucket_map"))
        val experimentBucketMap: Map<String, Map<String, String>>? = userProfileMap["experiment_bucket_map"] as ConcurrentHashMap<String, Map<String, String>>?
        Assert.assertEquals("var_3", experimentBucketMap!!["exp_1"]!!["variation_id"])
    }

    @Test
    fun removeDecision() {
        userProfileService!!.save(userProfileMap1!!)
        userProfileService!!.save(userProfileMap2!!)
        try {
            executor!!.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Time out")
        }
        userProfileService!!.remove(userId2, "exp_2")
        Assert.assertNotNull(userProfileService!!.lookup(userId1!!))
        Assert.assertNotNull(userProfileService!!.lookup(userId2!!))
        val userProfileMap = userProfileService!!.lookup(userId2!!)
        Assert.assertEquals(userId2, userProfileMap!!["user_id"])
        Assert.assertTrue(userProfileMap.containsKey("experiment_bucket_map"))
        val experimentBucketMap: Map<String, Map<String, String>>? = userProfileMap["experiment_bucket_map"] as ConcurrentHashMap<String, Map<String, String>>?
        Assert.assertEquals("var_3", experimentBucketMap!!["exp_1"]!!["variation_id"])
        Assert.assertNull(experimentBucketMap["exp_2"])
    }
}