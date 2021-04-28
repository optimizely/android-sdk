/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                            *
 * *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.android.shared

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.RunWith
import junit.framework.Assert
import org.junit.Before
import org.junit.Test

/**
 * Tests for [OptlyStorage]
 */
@RunWith(AndroidJUnit4::class)
class OptlyStorageTest {
    private var optlyStorage: OptlyStorage? = null
    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        optlyStorage = OptlyStorage(context)
    }

    @Test
    fun saveAndGetLong() {
        optlyStorage!!.saveLong("foo", 1)
        Assert.assertEquals(1L, optlyStorage!!.getLong("foo", 0L))
    }
}