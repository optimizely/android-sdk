/****************************************************************************
 * Copyright 2016-2017, Optimizely, Inc. and contributors                   *
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

package com.optimizely.ab.android.shared;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CacheTest}
 */
@RunWith(AndroidJUnit4.class)
public class CacheTest {

    private static final String FILENAME = "foo.txt";

    private Cache cache;
    private Logger logger;

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        logger = mock(Logger.class);
        cache = new Cache(context, logger);
    }

    @Test
    public void testSaveExistsLoadAndDelete() throws IOException {
        assertTrue(cache.save(FILENAME, "bar"));
        assertTrue(cache.exists(FILENAME));
        String data = cache.load(FILENAME);
        assertEquals("bar", data);
        assertTrue(cache.delete(FILENAME));
    }

    @Test
    public void testDeleteFail() {
        assertFalse(cache.delete(FILENAME));
    }

    @Test
    public void testExistsFalse() {
        assertFalse(cache.exists(FILENAME));
    }

    @Test
    public void testLoadFileNotFoundExceptionReturnsNull() throws FileNotFoundException {
        Context context = mock(Context.class);
        Cache cache = new Cache(context, logger);
        when(context.openFileInput(FILENAME)).thenThrow(new FileNotFoundException());
        assertNull(cache.load(FILENAME));
        verify(logger).warn("Unable to load file {}.", FILENAME);
    }

    @Test
    public void testSaveFail() throws IOException {
        Context context = mock(Context.class);
        Cache cache = new Cache(context, logger);
        FileOutputStream fileOutputStream = mock(FileOutputStream.class);

        String data = "{}";
        Mockito.doThrow(new IOException()).when(fileOutputStream).write(data.getBytes());
        when(context.openFileOutput(FILENAME, Context.MODE_PRIVATE)).thenReturn(fileOutputStream);
        assertFalse(cache.save(FILENAME, data));
        verify(logger).error("Error saving file {}.", FILENAME);
    }
}
