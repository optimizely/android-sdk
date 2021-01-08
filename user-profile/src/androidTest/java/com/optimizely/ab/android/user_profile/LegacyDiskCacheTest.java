/****************************************************************************
 * Copyright 2021, Optimizely, Inc. and contributors                        *
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link UserProfileCache.LegacyDiskCache}
 */
@RunWith(AndroidJUnit4.class)
public class LegacyDiskCacheTest {

    // Runs tasks serially on the calling thread
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Cache cache;
    private Logger logger;
    private UserProfileCache.LegacyDiskCache legacyDiskCache;
    private String projectId;

    @Before
    public void setup() {
        logger = mock(Logger.class);
        cache = new Cache(InstrumentationRegistry.getInstrumentation().getTargetContext(), logger);
        projectId = "123";
        legacyDiskCache = new UserProfileCache.LegacyDiskCache(cache, executor, logger, projectId);
    }

    @After
    public void teardown() {
        cache.delete(legacyDiskCache.getFileName());
    }

    @Test
    public void testGetFileName() {
        assertEquals("optly-user-profile-123.json", legacyDiskCache.getFileName());
    }

    @Test
    public void testLoadWhenNoFile() throws JSONException {
        assertNull(legacyDiskCache.load());
        verify(logger).warn("Unable to load file {}.", legacyDiskCache.getFileName());
        verify(logger).info("Legacy user profile cache not found.");
    }

    @Test
    public void testLoadMalformedCache() throws JSONException {
        cache = mock(Cache.class);
        when(cache.load(legacyDiskCache.getFileName())).thenReturn("{?}");
        when(cache.delete(legacyDiskCache.getFileName())).thenReturn(true);
        legacyDiskCache = new UserProfileCache.LegacyDiskCache(cache, executor, logger, projectId);

        assertNull(legacyDiskCache.load());
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(logger).info("Deleted legacy user profile from disk.");
        verify(logger).warn(eq("Unable to parse legacy user profiles. Will delete legacy user profile cache file."),
                any(Exception.class));
    }

    @Test
    public void testDelete() throws JSONException {
        cache = mock(Cache.class);
        when(cache.delete(legacyDiskCache.getFileName())).thenReturn(true);
        legacyDiskCache = new UserProfileCache.LegacyDiskCache(cache, executor, logger, projectId);

        legacyDiskCache.delete();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(logger).info("Deleted legacy user profile from disk.");
    }

    @Test
    public void testDeleteFailed() throws JSONException {
        cache = mock(Cache.class);
        when(cache.delete(legacyDiskCache.getFileName())).thenReturn(false);
        legacyDiskCache = new UserProfileCache.LegacyDiskCache(cache, executor, logger, projectId);

        legacyDiskCache.delete();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(logger).warn("Unable to delete legacy user profile from disk.");
    }
}
