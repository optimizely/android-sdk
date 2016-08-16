package com.optimizely.ab.android.project_watcher;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 8/2/16 for Optimizely.
 *
 * Tests for {@link DataFileCache}
 */
@RunWith(AndroidJUnit4.class)
public class DataFileCacheTest {

    DataFileCache dataFileCache;
    Cache cache;
    Logger logger;

    @Before
    public void setup() {
        logger = mock(Logger.class);
        cache = new Cache(InstrumentationRegistry.getTargetContext(), logger);
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
        Cache cache = mock(Cache.class);
        DataFileCache dataFileCache = new DataFileCache("1", cache, logger);
        when(cache.load(dataFileCache.getFileName())).thenReturn("{");
        assertNull(dataFileCache.load());
        verify(logger).error(contains("Unable to parse data file"), any(JSONException.class));
    }

    @Test
    public void testLoadFileNotFound() throws IOException {
        Cache cache = mock(Cache.class);
        DataFileCache dataFileCache = new DataFileCache("1", cache, logger);
        when(cache.load(dataFileCache.getFileName())).thenThrow(new FileNotFoundException());
        assertNull(dataFileCache.load());
        verify(logger).info(contains("No data file found"));
    }

    @Test
    public void testLoadIoException() throws IOException {
        Cache cache = mock(Cache.class);
        DataFileCache dataFileCache = new DataFileCache("1", cache, logger);
        when(cache.load(dataFileCache.getFileName())).thenThrow(new IOException());
        assertNull(dataFileCache.load());
        verify(logger).error(contains("Unable to load data file"), any(IOException.class));
    }

    @Test
    public void testSaveIOException() throws IOException {
        Cache cache = mock(Cache.class);
        DataFileCache dataFileCache = new DataFileCache("1", cache, logger);
        when(cache.save(dataFileCache.getFileName(), "")).thenThrow(new IOException());
        assertFalse(dataFileCache.save(""));
        verify(logger).error(contains("Unable to save data file"), any(IOException.class));
    }
}
