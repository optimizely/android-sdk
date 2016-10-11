/*
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
package com.optimizely.ab.android.shared;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;


/**
 * Created by jdeffibaugh on 8/1/16 for Optimizely.
 *
 * Tests for {@link OptlyStorage}
 */
@RunWith(AndroidJUnit4.class)
public class OptlyStorageTest {

    private OptlyStorage optlyStorage;

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getTargetContext();
        optlyStorage = new OptlyStorage(context);
    }

    @Test
    public void saveAndGetLong() {
        optlyStorage.saveLong("foo", 1);
        assertEquals(1L, optlyStorage.getLong("foo", 0L));
    }
}

