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

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.core.deps.guava.util.concurrent.ListeningExecutorService;
import android.support.test.espresso.core.deps.guava.util.concurrent.MoreExecutors;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.junit.After;
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
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AndroidUserExperimentRecord}
 */
@RunWith(AndroidJUnit4.class)
public class AndroidUserExperimentRecordTest {

    private AndroidUserExperimentRecord androidUserExperimentRecord;
    private UserExperimentRecordCache diskUserExperimentRecordCache;
    private Map<String, Map<String, String>> memoryUserExperimentRecordCache = new HashMap<>();
    private AndroidUserExperimentRecord.WriteThroughCacheTaskFactory writeThroughCacheFactory;
    private Logger logger;
    private ListeningExecutorService executor;
    private Cache cache;

    @Before
    public void setup() {
        logger = mock(Logger.class);
        cache = new Cache(InstrumentationRegistry.getTargetContext(), logger);
        diskUserExperimentRecordCache = new UserExperimentRecordCache("1", cache, logger);
        executor = MoreExecutors.newDirectExecutorService();
        writeThroughCacheFactory = new AndroidUserExperimentRecord.WriteThroughCacheTaskFactory(diskUserExperimentRecordCache, memoryUserExperimentRecordCache, executor, logger);
        androidUserExperimentRecord = new AndroidUserExperimentRecord(diskUserExperimentRecordCache,
                writeThroughCacheFactory, logger);
    }

    @After
    public void teardown() {
        cache.delete(diskUserExperimentRecordCache.getFileName());
    }

    @Test
    public void saveActivation() {
        String userId = "user1";
        String expKey = "exp1";
        String varKey = "var1";
        assertTrue(androidUserExperimentRecord.save(userId, expKey, varKey));
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Time out");
        }
        assertEquals(varKey, androidUserExperimentRecord.lookup(userId, expKey));
    }

    @Test
    public void saveActivationNullUserId() {
        assertFalse(androidUserExperimentRecord.save(null, "exp1", "var1"));
        verify(logger).error("Received null userId, unable to save activation");
    }

    @Test
    public void saveActivationNullExperimentKey() {
        assertFalse(androidUserExperimentRecord.save("foo", null, "var1"));
        verify(logger).error("Received null experiment key, unable to save activation");
    }

    @Test
    public void saveActivationNullVariationKey() {
        assertFalse(androidUserExperimentRecord.save("foo", "exp1", null));
        verify(logger).error("Received null variation key, unable to save activation");
    }

    @Test
    public void saveActivationEmptyUserId() {
        assertFalse(androidUserExperimentRecord.save("", "exp1", "var1"));
        verify(logger).error("Received empty user id, unable to save activation");
    }

    @Test
    public void saveActivationEmptyExperimentKey() {
        assertFalse(androidUserExperimentRecord.save("foo", "", "var1"));
        verify(logger).error("Received empty experiment key, unable to save activation");
    }

    @Test
    public void saveActivationEmptyVariationKey() {
        assertFalse(androidUserExperimentRecord.save("foo", "exp1", ""));
        verify(logger).error("Received empty variation key, unable to save activation");
    }

    @Test
    public void lookupActivationNullUserId() {
        assertNull(androidUserExperimentRecord.lookup(null, "exp1"));
        verify(logger).error("Received null user id, unable to lookup activation");
    }

    @Test
    public void lookupActivationNullExperimentKey() {
        assertNull(androidUserExperimentRecord.lookup("foo", null));
        verify(logger).error("Received null experiment key, unable to lookup activation");
    }

    @Test
    public void lookupActivationEmptyUserId() {
        assertNull(androidUserExperimentRecord.lookup("", "exp1"));
        verify(logger).error("Received empty user id, unable to lookup activation");
    }

    @Test
    public void lookupActivationEmptyExperimentKey() {
        assertNull(androidUserExperimentRecord.lookup("foo", ""));
        verify(logger).error("Received empty experiment key, unable to lookup activation");
    }

    @Test
    public void removeExistingActivation() {
        androidUserExperimentRecord.save("user1", "exp1", "var1");
        assertTrue(androidUserExperimentRecord.remove("user1", "exp1"));
        assertNull(androidUserExperimentRecord.lookup("user1", "exp1"));
    }

    @Test
    public void removeNonExistingActivation() {
        assertFalse(androidUserExperimentRecord.remove("user1", "exp1"));
    }

    @Test
    public void removeActivationNullUserId() {
        assertFalse(androidUserExperimentRecord.remove(null, "exp1"));
        verify(logger).error("Received null user id, unable to remove activation");
    }

    @Test
    public void removeActivationNullExperimentKey() {
        assertFalse(androidUserExperimentRecord.remove("foo", null));
        verify(logger).error("Received null experiment key, unable to remove activation");
    }

    @Test
    public void removeActivationEmptyUserId() {
        assertFalse(androidUserExperimentRecord.remove("", "exp1"));
        verify(logger).error("Received empty user id, unable to remove activation");
    }

    @Test
    public void removeActivationEmptyExperimentKey() {
        assertFalse(androidUserExperimentRecord.remove("foo", ""));
        verify(logger).error("Received empty experiment key, unable to remove activation");
    }

    @Test
    public void startHandlesJSONException() throws IOException {
        assertTrue(cache.save(diskUserExperimentRecordCache.getFileName(), "{"));
        androidUserExperimentRecord.start();
        verify(logger).error(eq("Unable to parse user experiment record cache"), any(JSONException.class));
    }

    @Test
    public void start() throws JSONException {
        androidUserExperimentRecord.start();
        androidUserExperimentRecord.save("user1", "exp1", "var1");

        Map<String, String> expKeyToVarKeyMap = new HashMap<>();
        expKeyToVarKeyMap.put("exp1", "var1");
        Map<String, Map<String, String>> recordMap = new HashMap<>();
        recordMap.put("user1", expKeyToVarKeyMap);

        assertEquals(recordMap, memoryUserExperimentRecordCache);
    }
}
