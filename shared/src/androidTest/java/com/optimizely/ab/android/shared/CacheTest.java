package com.optimizely.ab.android.shared;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;

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
    public void saveLoadAndDelete() throws IOException {
        assertTrue(cache.save(FILE_NAME, "bar"));
        String data = cache.load(FILE_NAME);
        assertEquals("bar", data);
        assertTrue(cache.delete(FILE_NAME));
    }

    @Test
    public void deleteFileFail() {
        assertFalse(cache.delete(FILE_NAME));
    }
}
