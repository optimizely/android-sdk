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

package com.optimizely.ab.android.user_profile;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link UserProfileCache}
 */
@RunWith(AndroidJUnit4.class)
public class UserProfileCacheTest {

    private UserProfileCache userProfileCache;
    private Cache cache;
    private Logger logger;

    @Before
    public void setup() {
        logger = mock(Logger.class);
        cache = new Cache(InstrumentationRegistry.getTargetContext(), logger);
        userProfileCache = new UserProfileCache("1", cache, logger);
    }

    @After
    public void teardown() {
        cache.delete(userProfileCache.getFileName());
    }

    @Test
    public void loadWhenNoFile() throws JSONException {
        assertEquals(userProfileCache.load().toString(), new JSONObject().toString());
    }

    @Test
    public void testSaveAndLoad() throws JSONException {
        assertTrue(userProfileCache.save("foo", "exp1", "var1"));
        assertTrue(userProfileCache.save("foo", "exp2", "var2"));
        JSONObject expectedActivation = new JSONObject();
        JSONObject expectedExpIdToVarId = new JSONObject();
        expectedExpIdToVarId.put("exp1", "var1");
        expectedExpIdToVarId.put("exp2", "var2");
        expectedActivation.put("foo", expectedExpIdToVarId);
        assertEquals(expectedActivation.toString(), userProfileCache.load().toString());
    }

    @Test
    public void testSaveIOException() throws IOException, JSONException {
        cache = mock(Cache.class);
        userProfileCache = new UserProfileCache("1", cache, logger);
        JSONObject expectedActivation = new JSONObject();
        JSONObject expectedExpIdToVarId = new JSONObject();
        expectedExpIdToVarId.put("exp1", "var1");
        expectedActivation.put("foo", expectedExpIdToVarId);
        when(cache.save(userProfileCache.getFileName(), "{}")).thenReturn(false);
        assertFalse(userProfileCache.save("foo", "exp1", "var1"));
        verify(logger).warn("Unable to save user profile cache.");
    }

    @Test
    public void testRestoreIOException() throws JSONException {
        cache = mock(Cache.class);
        userProfileCache = new UserProfileCache("1", cache, logger);
        when(cache.load(userProfileCache.getFileName())).thenReturn(null);
        assertEquals(userProfileCache.load().toString(), new JSONObject().toString());
        verify(logger).warn("Unable to load user profile cache.");
    }
}
