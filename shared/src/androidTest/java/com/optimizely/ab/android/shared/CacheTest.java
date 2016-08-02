package com.optimizely.ab.android.shared;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.io.FileNotFoundException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 8/1/16 for Optimizely.
 *
 * Tests for {@link CacheTest}
 */
@RunWith(AndroidJUnit4.class)
public class CacheTest {

    public static final String FILE_NAME = "foo.txt";

    Cache cache;
    Logger logger;
    Context context;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getTargetContext();
        logger = mock(Logger.class);
        cache = new Cache(context, logger);
    }

    @Test
    public void saveLoadAndDelete() {
        assertTrue(cache.save(FILE_NAME, "bar"));
        String data = cache.load(FILE_NAME);
        assertEquals("bar", data);
        assertTrue(cache.delete(FILE_NAME));
        data = cache.load(FILE_NAME);
        assertNull(data);
        verify(logger).error(contains("Error loading file"), any(FileNotFoundException.class));
    }

    @Test
    public void deleteFileFail() {
        assertFalse(cache.delete(FILE_NAME));
    }

    @Test
    public void saveIOException() throws FileNotFoundException {
        Context context = mock(Context.class);
        Cache cache = new Cache(context, logger);
        when(context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)).thenThrow(new FileNotFoundException());
        assertFalse(cache.save(FILE_NAME, "bar"));
        verify(logger).error(contains("Unable to save optly data file to cache"), any(FileNotFoundException.class));
    }

    @Test
    public void loadIOException() throws FileNotFoundException {
        Context context = mock(Context.class);
        Cache cache = new Cache(context, logger);
        when(context.openFileInput(FILE_NAME)).thenThrow(new FileNotFoundException());
        assertNull(cache.load(FILE_NAME));
        verify(logger).error(contains("Error loading file"), any(FileNotFoundException.class));
    }
}
