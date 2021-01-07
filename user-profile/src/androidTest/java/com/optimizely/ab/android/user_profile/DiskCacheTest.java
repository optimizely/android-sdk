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

import org.json.JSONException;
import org.json.JSONObject;
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
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link UserProfileCache.DiskCache}
 */
@RunWith(AndroidJUnit4.class)
public class DiskCacheTest {

    // Runs tasks serially on the calling thread
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Cache cache;
    private Logger logger;
    private UserProfileCache.DiskCache diskCache;
    private Map<String, Map<String, Object>> memoryCache;
    private String projectId;
    private String userId;

    @Before
    public void setup() {
        logger = mock(Logger.class);
        cache = new Cache(InstrumentationRegistry.getInstrumentation().getTargetContext(), logger);
        memoryCache = new ConcurrentHashMap<>();
        projectId = "123";
        diskCache = new UserProfileCache.DiskCache(cache, executor, logger, projectId);
        userId = "user_1";

        // Populate in-memory cache.
        Map<String, Map<String, String>> experimentBucketMap = new ConcurrentHashMap<>();
        Map<String, String> decisionMap1 = new ConcurrentHashMap<>();
        decisionMap1.put("variation_id", "var_1");
        experimentBucketMap.put("exp_1", decisionMap1);
        Map<String, String> decisionMap2 = new ConcurrentHashMap<>();
        decisionMap2.put("variation_id", "var_2");
        experimentBucketMap.put("exp_2", decisionMap2);
        Map<String, Object> userProfileMap = new ConcurrentHashMap<>();
        userProfileMap.put("user_id", userId);
        userProfileMap.put("experiment_bucket_map", experimentBucketMap);
        memoryCache.put(userId, userProfileMap);
    }

    @After
    public void teardown() {
        cache.delete(diskCache.getFileName());
    }

    @Test
    public void testGetFileName() {
        assertEquals("optly-user-profile-service-123.json", diskCache.getFileName());
    }

    @Test
    public void testLoadWhenNoFile() throws JSONException {
        assertEquals(diskCache.load().toString(), new JSONObject().toString());
        verify(logger).warn("Unable to load file {}.", diskCache.getFileName());
        verify(logger).warn("Unable to load user profile cache from disk.");
    }

    @Test
    public void testLoadIOException() throws JSONException {
        cache = mock(Cache.class);
        when(cache.load(diskCache.getFileName())).thenReturn(null);
        diskCache = new UserProfileCache.DiskCache(cache, executor, logger, projectId);
        assertEquals(new JSONObject().toString(), diskCache.load().toString());
        verify(logger).warn("Unable to load user profile cache from disk.");
    }

    @Test
    public void testSaveAndLoad() throws JSONException {
        diskCache.save(memoryCache);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(logger).info("Saved user profiles to disk.");

        JSONObject json = diskCache.load();
        assertTrue(json.has(userId));
        JSONObject userProfileJson = json.getJSONObject(userId);
        assertEquals(userId, userProfileJson.getString("user_id"));
        assertTrue(userProfileJson.has("experiment_bucket_map"));
        JSONObject experimentBucketMapJson = userProfileJson.getJSONObject("experiment_bucket_map");
        assertTrue(experimentBucketMapJson.has("exp_1"));
        JSONObject decisionMapJson1 = experimentBucketMapJson.getJSONObject("exp_1");
        assertEquals("var_1", decisionMapJson1.getString("variation_id"));
        assertTrue(experimentBucketMapJson.has("exp_2"));
        JSONObject decisionMapJson2 = experimentBucketMapJson.getJSONObject("exp_2");
        assertEquals("var_2", decisionMapJson2.getString("variation_id"));
    }

    @Test
    public void testSaveIOException() throws JSONException {
        cache = mock(Cache.class);
        when(cache.save(diskCache.getFileName(), memoryCache.toString())).thenReturn(false);
        diskCache = new UserProfileCache.DiskCache(cache, executor, logger, projectId);

        diskCache.save(memoryCache);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(logger).warn("Unable to save user profiles to disk.");
    }

    @Test
    public void testSaveInvalidMemoryCache() throws JSONException {
        memoryCache.put("user_2", new ConcurrentHashMap<String, Object>());
        diskCache.save(memoryCache);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(logger).error(eq("Unable to serialize user profiles to save to disk."), any(Exception.class));
    }
}
