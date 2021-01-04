/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.android.user_profile;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for {@link UserProfileCacheUtils}
 */
@RunWith(AndroidJUnit4.class)
public class UserProfileCacheUtilsTest {

    private String expId1 = "exp_1";
    private String expId2 = "exp_2";
    private String userId1;
    private String userId2;
    private JSONObject userProfilesJson;
    private Map<String, Map<String, Object>> userProfilesMap;

    @Before
    public void setup() throws JSONException {
        // Test data.
        userId1 = "user_1";
        Map<String, Map<String, String>> experimentBucketMap1 = new ConcurrentHashMap<>();
        Map<String, String> decisionMap1 = new ConcurrentHashMap<>();
        decisionMap1.put("variation_id", "var_1");
        experimentBucketMap1.put(expId1, decisionMap1);
        Map<String, String> decisionMap2 = new ConcurrentHashMap<>();
        decisionMap2.put("variation_id", "var_2");
        experimentBucketMap1.put(expId2, decisionMap2);
        Map<String, Object> userProfileMap1 = new ConcurrentHashMap<>();
        userProfileMap1.put("user_id", userId1);
        userProfileMap1.put("experiment_bucket_map", experimentBucketMap1);

        userId2 = "user_2";
        Map<String, Map<String, String>> experimentBucketMap2 = new ConcurrentHashMap<>();
        Map<String, String> decisionMap3 = new ConcurrentHashMap<>();
        decisionMap3.put("variation_id", "var_3");
        experimentBucketMap2.put(expId1, decisionMap3);
        Map<String, String> decisionMap4 = new ConcurrentHashMap<>();
        decisionMap4.put("variation_id", "var_4");
        experimentBucketMap2.put(expId2, decisionMap4);
        Map<String, Object> userProfileMap2 = new ConcurrentHashMap<>();
        userProfileMap2.put("user_id", userId2);
        userProfileMap2.put("experiment_bucket_map", experimentBucketMap2);

        userProfilesMap = new ConcurrentHashMap<>();
        userProfilesMap.put(userId1, userProfileMap1);
        userProfilesMap.put(userId2, userProfileMap2);

        userProfilesJson = new JSONObject("{\"user_2\":{\"user_id\":\"user_2\"," +
                "\"experiment_bucket_map\":{\"exp_2\":{\"variation_id\":\"var_4\"}," +
                "\"exp_1\":{\"variation_id\":\"var_3\"}}},\"user_1\":{\"user_id\":\"user_1\"," +
                "\"experiment_bucket_map\":{\"exp_2\":{\"variation_id\":\"var_2\"}," +
                "\"exp_1\":{\"variation_id\":\"var_1\"}}}}");
    }

    @Test
    public void testConvertJSONObjectToMap() throws Exception {
        Map<String, Map<String, Object>> userProfilesMap =
                UserProfileCacheUtils.convertJSONObjectToMap(userProfilesJson);

        assertTrue(userProfilesMap.containsKey(userId1));
        assertNotNull(userProfilesMap.get(userId1));
        Map<String, Object> userProfileMap1 = userProfilesMap.get(userId1);
        assertTrue(userProfileMap1.containsKey("user_id"));
        assertEquals(userId1, userProfileMap1.get("user_id"));
        assertTrue(userProfileMap1.containsKey("experiment_bucket_map"));
        Map<String, Map<String, String>> experimentBucketMap1 =
                (Map<String, Map<String, String>>) userProfileMap1.get("experiment_bucket_map");
        assertNotNull(experimentBucketMap1);
        assertTrue(experimentBucketMap1.containsKey(expId1));
        Map<String, String> experimentMap1 = experimentBucketMap1.get(expId1);
        assertEquals("var_1", experimentMap1.get("variation_id"));
        assertTrue(experimentBucketMap1.containsKey(expId2));
        Map<String, String> experimentMap2 = experimentBucketMap1.get(expId2);
        assertEquals("var_2", experimentMap2.get("variation_id"));

        assertTrue(userProfilesMap.containsKey(userId2));
        assertNotNull(userProfilesMap.get(userId2));
        Map<String, Object> userProfileMap2 = userProfilesMap.get(userId2);
        assertTrue(userProfileMap2.containsKey("user_id"));
        assertEquals(userId2, userProfileMap2.get("user_id"));
        assertTrue(userProfileMap2.containsKey("experiment_bucket_map"));
        Map<String, Map<String, String>> experimentBucketMap2 =
                (Map<String, Map<String, String>>) userProfileMap2.get("experiment_bucket_map");
        assertNotNull(experimentBucketMap2);
        assertTrue(experimentBucketMap2.containsKey(expId1));
        Map<String, String> experimentMap3 = experimentBucketMap2.get(expId1);
        assertEquals("var_3", experimentMap3.get("variation_id"));
        assertTrue(experimentBucketMap2.containsKey(expId2));
        Map<String, String> experimentMap4 = experimentBucketMap2.get(expId2);
        assertEquals("var_4", experimentMap4.get("variation_id"));
    }

    @Test
    public void convertMapToJSONObject() throws Exception {
        assertEquals(userProfilesJson.toString(),
                UserProfileCacheUtils.convertMapToJSONObject(userProfilesMap).toString());
    }
}
