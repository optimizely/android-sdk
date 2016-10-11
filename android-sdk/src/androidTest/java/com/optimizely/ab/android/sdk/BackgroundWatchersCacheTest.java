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
package com.optimizely.ab.android.sdk;

import android.support.test.InstrumentationRegistry;

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link BackgroundWatchersCache}
 */
@RunWith(JUnit4.class)
public class BackgroundWatchersCacheTest {

    private BackgroundWatchersCache backgroundWatchersCache;
    private Cache cache;
    private Logger logger;

    @Before
    public void setup() {
        logger = mock(Logger.class);
        cache = new Cache(InstrumentationRegistry.getTargetContext(), logger);
        backgroundWatchersCache = new BackgroundWatchersCache(cache, logger);
    }

    @After
    public void tearDown() {
        cache.delete(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME);
    }

    @Test
    public void setIsWatchingEmptyString() {
        assertFalse(backgroundWatchersCache.setIsWatching("", false));
        verify(logger).error("Passed in an empty string for projectId");
    }

    @Test
    public void isWatchingEmptyString() {
        assertFalse(backgroundWatchersCache.isWatching(""));
        verify(logger).error("Passed in an empty string for projectId");
    }

    @Test
    public void setIsWatchingPersists() {
        assertTrue(backgroundWatchersCache.setIsWatching("1", true));
        assertTrue(backgroundWatchersCache.setIsWatching("2", true));
        assertTrue(backgroundWatchersCache.setIsWatching("3", false));
        assertTrue(backgroundWatchersCache.setIsWatching("1", false));

        assertFalse(backgroundWatchersCache.isWatching("1"));
        assertTrue(backgroundWatchersCache.isWatching("2"));
        assertFalse(backgroundWatchersCache.isWatching("3"));

        List<String> watchingProjectIds = backgroundWatchersCache.getWatchingProjectIds();
        assertTrue(watchingProjectIds.contains("2"));
    }

    @Test
    public void testExceptionHandling() throws IOException {
        Cache cache = mock(Cache.class);
        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(cache, logger);
        // Cause a JSONException to be thrown
        when(cache.load(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME)).thenReturn("{");

        assertFalse(backgroundWatchersCache.setIsWatching("1", true));
        verify(logger).error(contains("Unable to update watching state for project id"), any(JSONException.class));

        assertFalse(backgroundWatchersCache.isWatching("1"));
        verify(logger).error(contains("Unable check if project id is being watched"), any(JSONException.class));

        List<String> watchingProjectIds = backgroundWatchersCache.getWatchingProjectIds();
        assertTrue(watchingProjectIds.isEmpty());
        verify(logger).error(contains("Unable to get watching project ids"), any(JSONException.class));
    }

    @Test
    public void testLoadFileNotFound() throws IOException {
        Cache cache = mock(Cache.class);
        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(cache, logger);
        // Cause a JSONException to be thrown
        when(cache.load(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME)).thenThrow(new FileNotFoundException());
        assertFalse(backgroundWatchersCache.setIsWatching("1", true));
        verify(logger).info("Creating background watchers file");
    }

    @Test
    public void testLoadIOException() throws IOException {
        Cache cache = mock(Cache.class);
        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(cache, logger);
        // Cause a JSONException to be thrown
        when(cache.load(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME)).thenThrow(new IOException());
        assertFalse(backgroundWatchersCache.setIsWatching("1", true));
        verify(logger).error(contains("Unable to load background watchers file"), any(IOException.class));
    }

    @Test
    public void testSaveIOException() throws IOException {
        Cache cache = mock(Cache.class);
        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(cache, logger);
        // Cause a JSONException to be thrown
        when(cache.load(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME)).thenReturn("{}");
        when(cache.save(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME, "{\"1\":true}")).thenThrow(new IOException());
        assertFalse(backgroundWatchersCache.setIsWatching("1", true));
        verify(logger).error(contains("Unable to save background watchers file"), any(IOException.class));
    }
}
