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
import com.optimizely.ab.android.user_profile.UserProfileCacheUtils.convertJSONObjectToMap
import com.optimizely.ab.android.user_profile.UserProfileCacheUtils.convertMapToJSONObject
import junit.framework.Assert
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentHashMap

/**
 * Tests for [UserProfileCacheUtils]
 */
@RunWith(AndroidJUnit4::class)
class UserProfileCacheUtilsTest {
    private val expId1 = "exp_1"
    private val expId2 = "exp_2"
    private var userId1: String? = null
    private var userId2: String? = null
    private var userProfilesJson: JSONObject? = null
    private var userProfilesMap: Map<String?, Map<String?, Any?>>? = null
    @Before
    @Throws(JSONException::class)
    fun setup() {
        // Test data.
        userId1 = "user_1"
        val experimentBucketMap1: MutableMap<String, Map<String, String>> = ConcurrentHashMap()
        val decisionMap1: MutableMap<String, String> = ConcurrentHashMap()
        decisionMap1["variation_id"] = "var_1"
        experimentBucketMap1[expId1] = decisionMap1
        val decisionMap2: MutableMap<String, String> = ConcurrentHashMap()
        decisionMap2["variation_id"] = "var_2"
        experimentBucketMap1[expId2] = decisionMap2
        val userProfileMap1: MutableMap<String?, Any?> = ConcurrentHashMap()
        userProfileMap1["user_id"] = userId1
        userProfileMap1["experiment_bucket_map"] = experimentBucketMap1
        userId2 = "user_2"
        val experimentBucketMap2: MutableMap<String, Map<String, String>> = ConcurrentHashMap()
        val decisionMap3: MutableMap<String, String> = ConcurrentHashMap()
        decisionMap3["variation_id"] = "var_3"
        experimentBucketMap2[expId1] = decisionMap3
        val decisionMap4: MutableMap<String, String> = ConcurrentHashMap()
        decisionMap4["variation_id"] = "var_4"
        experimentBucketMap2[expId2] = decisionMap4
        val userProfileMap2: MutableMap<String?, Any?> = ConcurrentHashMap()
        userProfileMap2["user_id"] = userId2
        userProfileMap2["experiment_bucket_map"] = experimentBucketMap2
        userProfilesMap = ConcurrentHashMap()
        (userProfilesMap as ConcurrentHashMap<String?, Map<String?, Any?>>)[userId1] = userProfileMap1
        (userProfilesMap as ConcurrentHashMap<String?, Map<String?, Any?>>)[userId2] = userProfileMap2
        userProfilesJson = JSONObject("{\"user_2\":{\"user_id\":\"user_2\"," +
                "\"experiment_bucket_map\":{\"exp_2\":{\"variation_id\":\"var_4\"}," +
                "\"exp_1\":{\"variation_id\":\"var_3\"}}},\"user_1\":{\"user_id\":\"user_1\"," +
                "\"experiment_bucket_map\":{\"exp_2\":{\"variation_id\":\"var_2\"}," +
                "\"exp_1\":{\"variation_id\":\"var_1\"}}}}")
    }

    @Test
    @Throws(Exception::class)
    fun testConvertJSONObjectToMap() {
        val userProfilesMap: Map<String, Map<String, Any>> = convertJSONObjectToMap(userProfilesJson!!)
        Assert.assertTrue(userProfilesMap.containsKey(userId1))
        Assert.assertNotNull(userProfilesMap[userId1])
        val userProfileMap1 = userProfilesMap[userId1]!!
        Assert.assertTrue(userProfileMap1.containsKey("user_id"))
        Assert.assertEquals(userId1, userProfileMap1["user_id"])
        Assert.assertTrue(userProfileMap1.containsKey("experiment_bucket_map"))
        val experimentBucketMap1 = userProfileMap1["experiment_bucket_map"] as Map<String, Map<String, String>>?
        Assert.assertNotNull(experimentBucketMap1)
        Assert.assertTrue(experimentBucketMap1!!.containsKey(expId1))
        val experimentMap1 = experimentBucketMap1[expId1]!!
        Assert.assertEquals("var_1", experimentMap1["variation_id"])
        Assert.assertTrue(experimentBucketMap1.containsKey(expId2))
        val experimentMap2 = experimentBucketMap1[expId2]!!
        Assert.assertEquals("var_2", experimentMap2["variation_id"])
        Assert.assertTrue(userProfilesMap.containsKey(userId2))
        Assert.assertNotNull(userProfilesMap[userId2])
        val userProfileMap2 = userProfilesMap[userId2]!!
        Assert.assertTrue(userProfileMap2.containsKey("user_id"))
        Assert.assertEquals(userId2, userProfileMap2["user_id"])
        Assert.assertTrue(userProfileMap2.containsKey("experiment_bucket_map"))
        val experimentBucketMap2 = userProfileMap2["experiment_bucket_map"] as Map<String, Map<String, String>>?
        Assert.assertNotNull(experimentBucketMap2)
        Assert.assertTrue(experimentBucketMap2!!.containsKey(expId1))
        val experimentMap3 = experimentBucketMap2[expId1]!!
        Assert.assertEquals("var_3", experimentMap3["variation_id"])
        Assert.assertTrue(experimentBucketMap2.containsKey(expId2))
        val experimentMap4 = experimentBucketMap2[expId2]!!
        Assert.assertEquals("var_4", experimentMap4["variation_id"])
    }

    @Test
    @Throws(Exception::class)
    fun convertMapToJSONObject() {
        Assert.assertEquals(userProfilesJson.toString(),
                convertMapToJSONObject(userProfilesMap as Map<String, Map<String, Any>>).toString())
    }
}