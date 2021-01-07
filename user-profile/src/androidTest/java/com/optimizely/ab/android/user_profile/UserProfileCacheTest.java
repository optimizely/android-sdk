/****************************************************************************
 * Copyright 2021, Optimizely, Inc. and contributors                   *
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

import org.json.JSONException;
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
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link UserProfileCache}
 */
@RunWith(AndroidJUnit4.class)
public class UserProfileCacheTest {

    // Runs tasks serially on the calling thread
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Logger logger;
    private Cache cache;
    private UserProfileCache.DiskCache diskCache;
    private UserProfileCache.LegacyDiskCache legacyDiskCache;
    private Map<String, Map<String, Object>> memoryCache;
    private String projectId;
    private UserProfileCache userProfileCache;

    private String userId1;
    private String userId2;
    private Map<String, Object> userProfileMap1;
    private Map<String, Object> userProfileMap2;

    @Before
    public void setup() throws JSONException {
        logger = mock(Logger.class);
        projectId = "1";
        cache = new Cache(InstrumentationRegistry.getInstrumentation().getTargetContext(), logger);
        diskCache = new UserProfileCache.DiskCache(cache, executor, logger, projectId);
        legacyDiskCache = new UserProfileCache.LegacyDiskCache(cache, executor, logger, projectId);
        memoryCache = new ConcurrentHashMap<>();
        userProfileCache = new UserProfileCache(diskCache, logger, memoryCache, legacyDiskCache);

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
        userProfileMap2.put("experiment_bucket_map", experimentBucketMap2);
    }

    @After
    public void teardown() {
        cache.delete(userProfileCache.diskCache.getFileName());
    }

    @Test
    public void testClear() throws JSONException {
        userProfileCache.save(userProfileMap1);
        verify(logger).info("Saved user profile for {}.", userId1);
        userProfileCache.save(userProfileMap2);
        verify(logger).info("Saved user profile for {}.", userId2);

        userProfileCache.clear();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Time out");
        }
        userProfileCache.start();

        assertNull(userProfileCache.lookup(userId1));
        assertNull(userProfileCache.lookup(userId2));
    }

    @Test
    public void testLookupInvalidUserId() throws JSONException {
        userProfileCache.lookup(null);
        verify(logger).error("Unable to lookup user profile because user ID was null.");

        userProfileCache.lookup("");
        verify(logger).error("Unable to lookup user profile because user ID was empty.");
    }

    @Test
    public void testRemove() throws JSONException {
        userProfileCache.save(userProfileMap1);
        verify(logger).info("Saved user profile for {}.", userId1);
        userProfileCache.save(userProfileMap2);
        verify(logger).info("Saved user profile for {}.", userId2);

        userProfileCache.remove(userId1);
        // give cache a chance to save.  we should actually wait on the executor.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        userProfileCache.start();

        assertNull(userProfileCache.lookup(userId1));
        assertNotNull(userProfileCache.lookup(userId2));
    }

    @Test
    public void testRemoveInvalidUserId() throws JSONException {
        userProfileCache.remove(null);
        verify(logger).error("Unable to remove user profile because user ID was null.");

        userProfileCache.remove("");
        verify(logger).error("Unable to remove user profile because user ID was empty.");
    }

    @Test
    public void testRemoveDecision() throws JSONException {
        userProfileCache.save(userProfileMap1);
        verify(logger).info("Saved user profile for {}.", userId1);
        userProfileCache.save(userProfileMap2);
        verify(logger).info("Saved user profile for {}.", userId2);

        userProfileCache.remove(userId1, "exp_1");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        userProfileCache.start();

        Map<String, Object> userProfileMap1 = userProfileCache.lookup(userId1);
        Map<String, Map<String, String>> experimentBucketMap1 =
                (Map<String, Map<String, String>>) userProfileMap1.get("experiment_bucket_map");
        assertNull(experimentBucketMap1.get("exp_1"));
        assertNotNull(experimentBucketMap1.get("exp_2"));

        assertNotNull(userProfileCache.lookup(userId2));
    }

    @Test
    public void testRemoveDecisionInvalidUserIdAndExperimentId() throws JSONException {
        userProfileCache.remove(null, "1");
        verify(logger).error("Unable to remove decision because user ID was null.");

        userProfileCache.remove("", "1");
        verify(logger).error("Unable to remove decision because user ID was empty.");

        userProfileCache.remove("1", null);
        verify(logger).error("Unable to remove decision because experiment ID was null.");

        userProfileCache.remove("1", "");
        verify(logger).error("Unable to remove decision because experiment ID was empty.");
    }

    @Test
    public void testSaveAndLookup() throws JSONException {
        userProfileCache.save(userProfileMap1);
        verify(logger).info("Saved user profile for {}.", userId1);
        userProfileCache.save(userProfileMap2);
        verify(logger).info("Saved user profile for {}.", userId2);

        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Time out");
        }
        verify(logger, times(2)).info("Saved user profiles to disk.");
        userProfileCache.start();

        Map<String, Object> userProfileMap1 = userProfileCache.lookup(userId1);
        assertNotNull(userProfileMap1);
        assertNotNull(userProfileMap1.get("user_id"));
        assertEquals(userId1, (String) userProfileMap1.get("user_id"));
        Map<String, Map<String, String>> experimentBucketMap1 = (ConcurrentHashMap<String, Map<String, String>>)
                userProfileMap1.get("experiment_bucket_map");
        assertEquals("var_1", experimentBucketMap1.get("exp_1").get("variation_id"));
        assertEquals("var_2", experimentBucketMap1.get("exp_2").get("variation_id"));

        Map<String, Object> userProfileMap2 = userProfileCache.lookup(userId2);
        assertEquals(userId2, (String) userProfileMap2.get("user_id"));
        Map<String, Map<String, String>> experimentBucketMap2 = (ConcurrentHashMap<String, Map<String, String>>)
                userProfileMap2.get("experiment_bucket_map");
        assertEquals("var_3", experimentBucketMap2.get("exp_1").get("variation_id"));
        assertEquals("var_4", experimentBucketMap2.get("exp_2").get("variation_id"));
    }

    @Test
    public void testSaveInvalidUserId() throws JSONException {
        userProfileMap1.remove("user_id");
        userProfileCache.save(userProfileMap1);
        verify(logger).error("Unable to save user profile because user ID was null.");

        userProfileMap1.put("user_id", "");
        userProfileCache.save(userProfileMap1);
        verify(logger).error("Unable to save user profile because user ID was empty.");
    }
}
