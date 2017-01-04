/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CacheTest}
 */
@RunWith(AndroidJUnit4.class)
public class CacheTest {

    private static final String FILE_NAME = "foo.txt";

    private Cache cache;

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getTargetContext();
        Logger logger = mock(Logger.class);
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

    @Test
    public void testLoadFileNotFoundExceptionReturnsNull() throws FileNotFoundException {
        Context context = mock(Context.class);
        Logger logger = mock(Logger.class);
        Cache cache = new Cache(context, logger);
        when(context.openFileInput(FILE_NAME)).thenThrow(new FileNotFoundException());
        assertNull(cache.load(FILE_NAME));
    }

    @Test
    public void testSaveFileNotFoundExceptionReturnsFalse() throws FileNotFoundException{
        Context context = mock(Context.class);
        Logger logger = mock(Logger.class);
        Cache cache = new Cache(context, logger);
        when(context.openFileInput(FILE_NAME)).thenThrow(new FileNotFoundException());
        assertFalse(cache.save(FILE_NAME, "{}"));
    }
}
