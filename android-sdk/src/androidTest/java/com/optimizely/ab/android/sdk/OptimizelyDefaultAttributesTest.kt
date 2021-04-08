/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
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
package com.optimizely.ab.android.sdk

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.optimizely.ab.android.sdk.OptimizelyDefaultAttributes.buildDefaultAttributesMap
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.slf4j.Logger

/**
 * Tests for [OptimizelyDefaultAttributes]
 */
@RunWith(AndroidJUnit4::class)
class OptimizelyDefaultAttributesTest {
    private var logger: Logger? = null
    @Before
    @Throws(Exception::class)
    fun setUp() {
        logger = Mockito.mock(Logger::class.java)
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
    }

    @Test
    @Throws(Exception::class)
    fun buildDefaultAttributesMap() {
        val context = Mockito.mock(Context::class.java)
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(context.applicationContext).thenReturn(appContext)
        Mockito.`when`(appContext.packageName).thenReturn("com.optly")
        val defaultAttributes = buildDefaultAttributesMap(context, logger!!)
        Assert.assertEquals(defaultAttributes.size.toLong(), 4)
    }
}