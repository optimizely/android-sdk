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
import androidx.test.platform.app.InstrumentationRegistry;

import com.optimizely.ab.android.shared.Cache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultUserProfileService}
 */
@RunWith(AndroidJUnit4.class)
public class DefaultUserProfileServiceTest {

    private DefaultUserProfileService userProfileService;
    private Cache cache;
    private UserProfileCache.DiskCache diskCache;
    private ExecutorService executor;
    private Logger logger;
    private UserProfileCache.LegacyDiskCache legacyDiskCache;
    private Map<String, Map<String, Object>> memoryCache;
    private String projectId;
    private UserProfileCache userProfileCache;

    private String userId1;
    private String userId2;
    private Map<String, Object> userProfileMap1;
    private Map<String, Object> userProfileMap2;

    @Before
    public void setup() {
        logger = mock(Logger.class);
        cache = new Cache(InstrumentationRegistry.getInstrumentation().getTargetContext(), logger);
        executor =Executors.newSingleThreadExecutor();
        legacyDiskCache = new UserProfileCache.LegacyDiskCache(cache, executor, logger, projectId);
        memoryCache = new ConcurrentHashMap<>();
        projectId = "123";
        diskCache = new UserProfileCache.DiskCache(cache, executor, logger, projectId);
        userProfileCache = new UserProfileCache(diskCache, logger, memoryCache, legacyDiskCache);
        userProfileService = new DefaultUserProfileService(userProfileCache, logger);

        // Test data.
        userId1 = "user_1";
        Map<String, Map<String, String>> experimentBucketMap1 = new ConcurrentHashMap<>();
        Map<String, String> decisionMap1 = new ConcurrentHashMap<>();
        decisionMap1.put("variation_id", "var_1");
        experimentBucketMap1.put("exp_1", decisionMap1);
        Map<String, String> decisionMap2 = new ConcurrentHashMap<>();
        decisionMap2.put("variation_id", "var_2");
        experimentBucketMap1.put("exp_2", decisionMap2);
        userProfileMap1 = new ConcurrentHashMap<>();
        userProfileMap1.put("user_id", userId1);
        userProfileMap1.put("experiment_bucket_map", experimentBucketMap1);

        userId2 = "user_2";
        Map<String, Map<String, String>> experimentBucketMap2 = new ConcurrentHashMap<>();
        Map<String, String> decisionMap3 = new ConcurrentHashMap<>();
        decisionMap3.put("variation_id", "var_3");
        experimentBucketMap2.put("exp_1", decisionMap3);
        Map<String, String> decisionMap4 = new ConcurrentHashMap<>();
        decisionMap4.put("variation_id", "var_4");
        experimentBucketMap2.put("exp_2", decisionMap4);
        userProfileMap2 = new ConcurrentHashMap<>();
        userProfileMap2.put("user_id", userId2);
        userProfileMap2.put("experiment_bucket_map", experimentBucketMap2);    }

    @After
    public void teardown() {
        cache.delete(diskCache.getFileName());
    }

    @Test
    public void saveAndStartAndLookup() {
        userProfileService.save(userProfileMap1);
        userProfileService.save(userProfileMap2);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Time out");
        }
        userProfileService.start();

        Map<String, Object> userProfileMap = userProfileService.lookup(userId2);
        assertEquals(userId2, userProfileMap.get("user_id"));
        assertTrue(userProfileMap.containsKey("experiment_bucket_map"));
        Map<String, Map<String, String>> experimentBucketMap = (ConcurrentHashMap<String, Map<String, String>>)
                userProfileMap.get("experiment_bucket_map");
        assertEquals("var_3", experimentBucketMap.get("exp_1").get("variation_id"));
    }

    @Test
    public void remove() {
        userProfileService.save(userProfileMap1);
        userProfileService.save(userProfileMap2);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Time out");
        }
        userProfileService.remove(userId1);

        assertNull(userProfileService.lookup(userId1));
        assertNotNull(userProfileService.lookup(userId2));
        Map<String, Object> userProfileMap = userProfileService.lookup(userId2);
        assertEquals(userId2, userProfileMap.get("user_id"));
        assertTrue(userProfileMap.containsKey("experiment_bucket_map"));
        Map<String, Map<String, String>> experimentBucketMap = (ConcurrentHashMap<String, Map<String, String>>)
                userProfileMap.get("experiment_bucket_map");
        assertEquals("var_3", experimentBucketMap.get("exp_1").get("variation_id"));
    }

    @Test
    public void removeDecision() {
        userProfileService.save(userProfileMap1);
        userProfileService.save(userProfileMap2);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Time out");
        }
        userProfileService.remove(userId2, "exp_2");

        assertNotNull(userProfileService.lookup(userId1));
        assertNotNull(userProfileService.lookup(userId2));
        Map<String, Object> userProfileMap = userProfileService.lookup(userId2);
        assertEquals(userId2, userProfileMap.get("user_id"));
        assertTrue(userProfileMap.containsKey("experiment_bucket_map"));
        Map<String, Map<String, String>> experimentBucketMap = (ConcurrentHashMap<String, Map<String, String>>)
                userProfileMap.get("experiment_bucket_map");
        assertEquals("var_3", experimentBucketMap.get("exp_1").get("variation_id"));
        assertNull(experimentBucketMap.get("exp_2"));
    }
}
