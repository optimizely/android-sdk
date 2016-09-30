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

import java.io.FileNotFoundException;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 8/8/16 for Optimizely.
 *
 * Tests for {@link UserExperimentRecordCache}
 */
@RunWith(AndroidJUnit4.class)
public class UserExperimentRecordCacheTest {

    UserExperimentRecordCache userExperimentRecordCache;
    Cache cache;
    Logger logger;

    @Before
    public void setup() {
        logger = mock(Logger.class);
        cache = new Cache(InstrumentationRegistry.getTargetContext(), logger);
        userExperimentRecordCache = new UserExperimentRecordCache("1", cache, logger);
    }

    @After
    public void teardown() {
        cache.delete(userExperimentRecordCache.getFileName());
    }

    @Test
    public void loadWhenNoFile() throws JSONException {
        assertEquals(userExperimentRecordCache.load().toString(), new JSONObject().toString());
    }

    @Test
    public void testSaveAndLoad() throws JSONException {
        assertTrue(userExperimentRecordCache.save("foo", "exp1", "var1"));
        JSONObject expectedActivation = new JSONObject();
        JSONObject expectedExpIdToVarId = new JSONObject();
        expectedExpIdToVarId.put("exp1", "var1");
        expectedActivation.put("foo", expectedExpIdToVarId);
        assertEquals(expectedActivation.toString(), userExperimentRecordCache.load().toString());
    }

    @Test
    public void testSaveIOException() throws IOException, JSONException {
        cache = mock(Cache.class);
        final IOException ioException = new IOException();
        userExperimentRecordCache = new UserExperimentRecordCache("1", cache, logger);
        JSONObject expectedActivation = new JSONObject();
        JSONObject expectedExpIdToVarId = new JSONObject();
        expectedExpIdToVarId.put("exp1", "var1");
        expectedActivation.put("foo", expectedExpIdToVarId);
        when(cache.save(userExperimentRecordCache.getFileName(), expectedActivation.toString())).thenThrow(ioException);
        assertFalse(userExperimentRecordCache.save("foo", "exp1", "var1"));
        verify(logger).error("Unable to save user experiment record cache", ioException);
    }

    @Test
    public void testRestoreIOException() throws IOException, JSONException {
        cache = mock(Cache.class);
        final IOException ioException = new IOException();
        userExperimentRecordCache = new UserExperimentRecordCache("1", cache, logger);
        when(cache.load(userExperimentRecordCache.getFileName())).thenThrow(ioException);
        assertEquals(userExperimentRecordCache.load().toString(), new JSONObject().toString());
        verify(logger).error("Unable to load user experiment record cache", ioException);
    }

    @Test
    public void testRestoreFileNotFoundException() throws IOException, JSONException {
        cache = mock(Cache.class);
        final FileNotFoundException fileNotFoundException = new FileNotFoundException();
        userExperimentRecordCache = new UserExperimentRecordCache("1", cache, logger);
        when(cache.load(userExperimentRecordCache.getFileName())).thenThrow(fileNotFoundException);
        assertEquals(userExperimentRecordCache.load().toString(), new JSONObject().toString());
        verify(logger).info("No user experiment record cache found");
    }
}
