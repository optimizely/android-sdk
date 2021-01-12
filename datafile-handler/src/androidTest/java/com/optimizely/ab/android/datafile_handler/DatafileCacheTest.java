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
import androidx.test.ext.junit.runners.AndroidJUnit4;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;

/**
 * Tests for {@link DatafileCache}
 */
@RunWith(AndroidJUnit4.class)
public class DatafileCacheTest {

    private DatafileCache datafileCache;
    private Logger logger;

    @Before
    public void setup() {
        logger = mock(Logger.class);
        Cache cache = new Cache(InstrumentationRegistry.getInstrumentation().getTargetContext(), logger);
        datafileCache = new DatafileCache("1", cache, logger);
    }

    @Test
    public void loadBeforeSaving() {
        assertNull(datafileCache.load());
    }

    @Test
    public void persistence() throws JSONException {
        assertTrue(datafileCache.save("{}"));
        final JSONObject jsonObject = datafileCache.load();
        assertNotNull(jsonObject);
        final String actual = jsonObject.toString();
        assertEquals(new JSONObject("{}").toString(), actual);
        assertTrue(datafileCache.delete());
        assertNull(datafileCache.load());
    }

    @Test
    public void loadJsonException() throws IOException {
        Cache cache = mock(Cache.class);
        DatafileCache datafileCache = new DatafileCache("1", cache, logger);
        when(cache.load(datafileCache.getFileName())).thenReturn("{");
        assertNull(datafileCache.load());
        verify(logger).error(contains("Unable to parse data file"), any(JSONException.class));
    }
}
