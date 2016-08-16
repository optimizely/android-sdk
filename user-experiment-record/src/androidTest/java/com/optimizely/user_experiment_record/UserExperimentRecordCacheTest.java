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
        userExperimentRecordCache = new UserExperimentRecordCache(cache, logger);
    }

    @After
    public void teardown() {
        cache.delete(UserExperimentRecordCache.FILE_NAME);
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
        userExperimentRecordCache = new UserExperimentRecordCache(cache, logger);
        JSONObject expectedActivation = new JSONObject();
        JSONObject expectedExpIdToVarId = new JSONObject();
        expectedExpIdToVarId.put("exp1", "var1");
        expectedActivation.put("foo", expectedExpIdToVarId);
        when(cache.save(UserExperimentRecordCache.FILE_NAME, expectedActivation.toString())).thenThrow(ioException);
        assertFalse(userExperimentRecordCache.save("foo", "exp1", "var1"));
        verify(logger).error("Unable to save persistent bucketer cache", ioException);
    }

    @Test
    public void testRestoreIOException() throws IOException, JSONException {
        cache = mock(Cache.class);
        final IOException ioException = new IOException();
        userExperimentRecordCache = new UserExperimentRecordCache(cache, logger);
        when(cache.load(UserExperimentRecordCache.FILE_NAME)).thenThrow(ioException);
        assertEquals(userExperimentRecordCache.load().toString(), new JSONObject().toString());
        verify(logger).error("Unable to load persistent bucketer cache", ioException);
    }

    @Test
    public void testRestoreFileNotFoundException() throws IOException, JSONException {
        cache = mock(Cache.class);
        final FileNotFoundException fileNotFoundException = new FileNotFoundException();
        userExperimentRecordCache = new UserExperimentRecordCache(cache, logger);
        when(cache.load(UserExperimentRecordCache.FILE_NAME)).thenThrow(fileNotFoundException);
        assertEquals(userExperimentRecordCache.load().toString(), new JSONObject().toString());
        verify(logger).info("No persistent bucketer cache found");
    }
}
