/**
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

import android.support.test.espresso.core.deps.guava.util.concurrent.ListeningExecutorService;
import android.support.test.espresso.core.deps.guava.util.concurrent.MoreExecutors;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

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
 * Created by jdeffibaugh on 8/15/16 for Optimizely.
 *
 * Tests for {@link com.optimizely.user_experiment_record.AndroidUserExperimentRecord.WriteThroughCacheTaskFactory}
 */
@RunWith(AndroidJUnit4.class)
public class WriteThroughCacheFactoryTest {

    AndroidUserExperimentRecord.WriteThroughCacheTaskFactory writeThroughCacheTaskFactory;
    UserExperimentRecordCache diskUserExperimentRecordCache;
    // Runs tasks serially on the calling thread
    ListeningExecutorService executor = MoreExecutors.newDirectExecutorService();
    Logger logger;

    @Before
    public void setup() {
        diskUserExperimentRecordCache = mock(UserExperimentRecordCache.class);
        logger = mock(Logger.class);
        writeThroughCacheTaskFactory =
                new AndroidUserExperimentRecord.WriteThroughCacheTaskFactory(diskUserExperimentRecordCache,
                        new HashMap<String, Map<String, String>>(), executor, logger);
    }

    @Test
    public void startWriteCacheTask() {
        when(diskUserExperimentRecordCache.save("user1", "exp1", "var1")).thenReturn(true);

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

        verify(diskUserExperimentRecordCache).save("user1", "exp1", "var1");
        verify(logger).info("Persisted user in variation {} for experiment {}.", "var1", "exp1");
    }

    @Test
    public void startWriteCacheTaskFail() {
        when(diskUserExperimentRecordCache.save("user1", "exp1", "var1")).thenReturn(false);

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

        verify(diskUserExperimentRecordCache).save("user1", "exp1", "var1");
        verify(logger).error("Failed to persist user in variation {} for experiment {}.", "var1", "exp1");
    }

    @Test
    public void startRemoveCacheTask() {
        when(diskUserExperimentRecordCache.remove("user1", "exp1")).thenReturn(true);

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
        verify(diskUserExperimentRecordCache).remove("user1", "exp1");
        assertFalse(writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache().get("user1").containsKey("exp1"));
        verify(logger).info("Removed experimentKey: {} variationKey: {} record for user: {} from disk", "exp1", "var1", "user1");
    }

    @Test
    public void startRemoveCacheTaskFail() {
        when(diskUserExperimentRecordCache.remove("user1", "exp1")).thenReturn(false);

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
        verify(diskUserExperimentRecordCache).remove("user1", "exp1");
        verify(logger).error("Restored experimentKey: {} variationKey: {} record for user: {} to memory", "exp1", "var1","user1");
        assertTrue(writeThroughCacheTaskFactory.getMemoryUserExperimentRecordCache().get("user1").containsKey("exp1"));
    }
}
