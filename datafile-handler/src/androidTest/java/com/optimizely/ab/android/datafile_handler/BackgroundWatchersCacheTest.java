/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
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

package com.optimizely.ab.android.datafile_handler;

import androidx.test.platform.app.InstrumentationRegistry;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.DatafileConfig;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;

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
        cache = new Cache(InstrumentationRegistry.getInstrumentation().getTargetContext(), logger);
        backgroundWatchersCache = new BackgroundWatchersCache(cache, logger);
    }

    @After
    public void tearDown() {
        cache.delete(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME);
    }

    @Test
    public void setIsWatchingEmptyString() {
        assertFalse(backgroundWatchersCache.setIsWatching(new DatafileConfig("", null), false));
        verify(logger).error("Passed in an empty string for projectId");
    }

    @Test
    public void isWatchingEmptyString() {
        assertFalse(backgroundWatchersCache.isWatching(new DatafileConfig("", null)));
        verify(logger).error("Passed in an empty string for projectId");
    }

    @Test
    public void setIsWatchingPersistsWithoutEnvironment() {
        assertTrue(backgroundWatchersCache.setIsWatching(new DatafileConfig("1", null), true));
        assertTrue(backgroundWatchersCache.setIsWatching(new DatafileConfig("2", null), true));
        assertTrue(backgroundWatchersCache.setIsWatching(new DatafileConfig("3", null), false));
        assertTrue(backgroundWatchersCache.setIsWatching(new DatafileConfig("1", null), false));

        assertFalse(backgroundWatchersCache.isWatching(new DatafileConfig("1", null)));
        assertTrue(backgroundWatchersCache.isWatching(new DatafileConfig("2", null)));
        assertFalse(backgroundWatchersCache.isWatching(new DatafileConfig("3", null)));

        List<DatafileConfig> watchingProjectIds = backgroundWatchersCache.getWatchingDatafileConfigs();
        assertTrue(watchingProjectIds.contains(new DatafileConfig("2", null)));
    }

    @Test
    public void setIsWatchingPersistsWithEnvironment() {
        assertTrue(backgroundWatchersCache.setIsWatching(new DatafileConfig(null, "1-1"), true));
        assertTrue(backgroundWatchersCache.setIsWatching(new DatafileConfig(null, "2-2"), true));
        assertTrue(backgroundWatchersCache.setIsWatching(new DatafileConfig(null, "3-3"), false));
        assertTrue(backgroundWatchersCache.setIsWatching(new DatafileConfig(null, "1-1"), false));

        assertFalse(backgroundWatchersCache.isWatching(new DatafileConfig(null, "1-1")));
        assertTrue(backgroundWatchersCache.isWatching(new DatafileConfig(null, "2-2")));
        assertFalse(backgroundWatchersCache.isWatching(new DatafileConfig(null, "3-3")));

        List<DatafileConfig> watchingProjectIds = backgroundWatchersCache.getWatchingDatafileConfigs();
        assertTrue(watchingProjectIds.contains(new DatafileConfig(null, "2-2")));
    }

    @Test
    public void testExceptionHandling() throws IOException {
        Cache cache = mock(Cache.class);
        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(cache, logger);
        // Cause a JSONException to be thrown
        when(cache.load(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME)).thenReturn("{");

        assertFalse(backgroundWatchersCache.setIsWatching(new DatafileConfig("1", null), true));
        verify(logger).error(contains("Unable to update watching state for project id"), any(JSONException.class));

        assertFalse(backgroundWatchersCache.isWatching(new DatafileConfig("1", null)));
        verify(logger).error(contains("Unable check if project id is being watched"), any(JSONException.class));

        List<DatafileConfig> watchingProjectIds = backgroundWatchersCache.getWatchingDatafileConfigs();
        assertTrue(watchingProjectIds.isEmpty());
        verify(logger).error(contains("Unable to get watching project ids"), any(JSONException.class));
    }

    @Test
    public void testLoadFileNotFound() {
        Cache cache = mock(Cache.class);
        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(cache, logger);
        // Cause a JSONException to be thrown
        when(cache.load(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME)).thenReturn(null);
        assertFalse(backgroundWatchersCache.setIsWatching(new DatafileConfig("1", null), true));
        verify(logger).info("Creating background watchers file {}.", BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME);
    }
}
