/*
 * Copyright 2016, Optimizely
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.user_experiment_record;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.core.deps.guava.util.concurrent.ListeningExecutorService;
import android.support.test.espresso.core.deps.guava.util.concurrent.MoreExecutors;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link com.optimizely.user_experiment_record.AndroidUserExperimentRecord.WriteThroughCacheTaskFactory}
 */
@RunWith(AndroidJUnit4.class)
public class WriteThroughCacheFactoryTest {

    // Runs tasks serially on the calling thread
    private ListeningExecutorService executor = MoreExecutors.newDirectExecutorService();
    private Logger logger;

    @Before
    public void setup() {
        logger = mock(Logger.class);
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void startWriteCacheTask() throws JSONException {
        Cache cache = new Cache(InstrumentationRegistry.getTargetContext(), logger);
        UserExperimentRecordCache diskUserExperimentRecordCache = new UserExperimentRecordCache("1", cache, logger);
        AndroidUserExperimentRecord.WriteThroughCacheTaskFactory writeThroughCacheTaskFactory =
                new AndroidUserExperimentRecord.WriteThroughCacheTaskFactory(diskUserExperimentRecordCache,
                        new HashMap<String, Map<String, String>>(), executor, logger);

        writeThroughCacheTaskFactory.startWriteCacheTask("user1", "exp1", "var1");
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(logger).info("Updated in memory user experiment record");
        assertTrue(writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache().containsKey("user1"));
        Map<String, String> activation = writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache().get("user1");
        assertTrue(activation.containsKey("exp1"));
        assertEquals("var1", activation.get("exp1"));

        final JSONObject json = diskUserExperimentRecordCache.load();
        assertTrue(json.has("user1"));
        final JSONObject user1 = json.getJSONObject("user1");
        assertTrue(user1.has("exp1"));
        assertEquals("var1", user1.getString("exp1"));
        verify(logger).info("Persisted user in variation {} for experiment {}.", "var1", "exp1");

        cache.delete(diskUserExperimentRecordCache.getFileName());
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void startWriteCacheTaskFail() throws JSONException, IOException {
        Cache cache = mock(Cache.class);
        UserExperimentRecordCache diskUserExperimentRecordCache = new UserExperimentRecordCache("1", cache, logger);
        AndroidUserExperimentRecord.WriteThroughCacheTaskFactory writeThroughCacheTaskFactory =
                new AndroidUserExperimentRecord.WriteThroughCacheTaskFactory(diskUserExperimentRecordCache,
                        new HashMap<String, Map<String, String>>(), executor, logger);

        JSONObject json = getJsonObject();

        when(cache.save(diskUserExperimentRecordCache.getFileName(), json.toString())).thenThrow(new IOException());

        writeThroughCacheTaskFactory.startWriteCacheTask("user1", "exp1", "var1");
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(logger).info("Updated in memory user experiment record");
        assertTrue(writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache().containsKey("user1"));
        Map<String, String> activation = writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache().get("user1");
        assertFalse(activation.containsKey("exp1"));

        verify(logger).error("Failed to persist user in variation {} for experiment {}.", "var1", "exp1");
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void startRemoveCacheTask() throws JSONException, IOException {
        Cache cache = new Cache(InstrumentationRegistry.getTargetContext(), logger);
        UserExperimentRecordCache diskUserExperimentRecordCache = new UserExperimentRecordCache("1", cache, logger);
        AndroidUserExperimentRecord.WriteThroughCacheTaskFactory writeThroughCacheTaskFactory =
                new AndroidUserExperimentRecord.WriteThroughCacheTaskFactory(diskUserExperimentRecordCache,
                        new HashMap<String, Map<String, String>>(), executor, logger);

        diskUserExperimentRecordCache.save("user1", "exp1", "var1");

        Map<String, String> activation = new HashMap<>();
        activation.put("exp1", "var1");
        writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache().put("user1", activation);

        writeThroughCacheTaskFactory.startRemoveCacheTask("user1", "exp1", "var1");
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(logger).info("Removed experimentKey: {} variationKey: {} record for user: {} from memory", "exp1", "var1", "user1");
        JSONObject json = diskUserExperimentRecordCache.load();
        assertTrue(json.has("user1"));
        json = json.getJSONObject("user1");
        assertFalse(json.has("exp1"));
        assertFalse(writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache().get("user1").containsKey("exp1"));
        verify(logger).info("Removed experimentKey: {} variationKey: {} record for user: {} from disk", "exp1", "var1", "user1");
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void startRemoveCacheTaskFail() throws JSONException, IOException {
        Cache cache = mock(Cache.class);
        UserExperimentRecordCache diskUserExperimentRecordCache = new UserExperimentRecordCache("1", cache, logger);
        AndroidUserExperimentRecord.WriteThroughCacheTaskFactory writeThroughCacheTaskFactory =
                new AndroidUserExperimentRecord.WriteThroughCacheTaskFactory(diskUserExperimentRecordCache,
                        new HashMap<String, Map<String, String>>(), executor, logger);

        when(cache.save(diskUserExperimentRecordCache.getFileName(), getJsonObject().toString())).thenReturn(false);

        Map<String, String> activation = new HashMap<>();
        activation.put("exp1", "var1");
        writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache().put("user1", activation);
        writeThroughCacheTaskFactory.startRemoveCacheTask("user1", "exp1", "var1");
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(logger).info("Removed experimentKey: {} variationKey: {} record for user: {} from memory", "exp1", "var1", "user1");
        verify(logger).error("Restored experimentKey: {} variationKey: {} record for user: {} to memory", "exp1", "var1","user1");
        assertTrue(writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache().get("user1").containsKey("exp1"));
    }

    @NonNull
    private JSONObject getJsonObject() throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject user1 = new JSONObject();
        user1.put("exp1", "var1");
        json.put("user1", user1);
        return json;
    }
}
