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

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DataFileCache}
 */
@RunWith(AndroidJUnit4.class)
public class DataFileCacheTest {

    private DataFileCache dataFileCache;
    private Logger logger;

    @Before
    public void setup() {
        logger = Mockito.mock(Logger.class);
        Cache cache = new Cache(InstrumentationRegistry.getTargetContext(), logger);
        dataFileCache = new DataFileCache("1", cache, logger);
    }

    @Test
    public void loadBeforeSaving() {
        assertNull(dataFileCache.load());
    }

    @Test
    public void persistence() throws JSONException {
        assertTrue(dataFileCache.save("{}"));
        final JSONObject jsonObject = dataFileCache.load();
        assertNotNull(jsonObject);
        final String actual = jsonObject.toString();
        assertEquals(new JSONObject("{}").toString(), actual);
        assertTrue(dataFileCache.delete());
        assertNull(dataFileCache.load());
    }

    @Test
    public void loadJsonException() throws IOException {
        Cache cache = Mockito.mock(Cache.class);
        DataFileCache dataFileCache = new DataFileCache("1", cache, logger);
        Mockito.when(cache.load(dataFileCache.getFileName())).thenReturn("{");
        assertNull(dataFileCache.load());
        Mockito.verify(logger).error(Matchers.contains("Unable to parse data file"), Matchers.any(JSONException.class));
    }
}
