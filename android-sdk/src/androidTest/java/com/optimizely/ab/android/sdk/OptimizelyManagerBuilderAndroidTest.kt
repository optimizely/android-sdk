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

import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.optimizely.ab.OptimizelyRuntimeException
import com.optimizely.ab.android.datafile_handler.DefaultDatafileHandler
import com.optimizely.ab.android.event_handler.DefaultEventHandler
import com.optimizely.ab.android.sdk.OptimizelyManager.Companion.builder
import com.optimizely.ab.android.shared.DatafileConfig
import com.optimizely.ab.android.user_profile.DefaultUserProfileService
import com.optimizely.ab.error.ErrorHandler
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.slf4j.Logger
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class OptimizelyManagerBuilderAndroidTest {
    private val logger = Mockito.mock(Logger::class.java)
    private val testProjectId = "7595190003"
    private val minDatafile = "{\"groups\": [], \"projectId\": \"8504447126\", \"variables\": [{\"defaultValue\": \"true\", \"type\": \"boolean\", \"id\": \"8516291943\", \"key\": \"test_variable\"}], \"version\": \"3\", \"experiments\": [{\"status\": \"Running\", \"key\": \"android_experiment_key\", \"layerId\": \"8499056327\", \"trafficAllocation\": [{\"entityId\": \"8509854340\", \"endOfRange\": 5000}, {\"entityId\": \"8505434669\", \"endOfRange\": 10000}], \"audienceIds\": [], \"variations\": [{\"variables\": [], \"id\": \"8509854340\", \"key\": \"var_1\"}, {\"variables\": [], \"id\": \"8505434669\", \"key\": \"var_2\"}], \"forcedVariations\": {}, \"id\": \"8509139139\"}], \"audiences\": [], \"anonymizeIP\": true, \"attributes\": [], \"revision\": \"7\", \"events\": [{\"experimentIds\": [\"8509139139\"], \"id\": \"8505434668\", \"key\": \"test_event\"}], \"accountId\": \"8362480420\"}"
    @Before
    @Throws(Exception::class)
    fun setUp() {
    }

    @Test
    fun testBuilderWith() {
        val errorHandler: ErrorHandler = object : ErrorHandler {
            @Throws(Exception::class)
            override fun <T : OptimizelyRuntimeException?> handleError(exception: T) {
                logger.error("Inside error handler", exception)
            }
        }
        val manager = builder(testProjectId).withUserProfileService(DefaultUserProfileService.newInstance(testProjectId, InstrumentationRegistry.getInstrumentation().targetContext))
                .withDatafileDownloadInterval(30L)
                .withEventDispatchInterval(30L)
                .withDatafileHandler(DefaultDatafileHandler())
                .withErrorHandler(errorHandler)
                .withEventHandler(DefaultEventHandler.getInstance(InstrumentationRegistry.getInstrumentation().targetContext))
                .withLogger(logger).build(InstrumentationRegistry.getInstrumentation().targetContext)
        Assert.assertNotNull(manager)
        Assert.assertNotNull(manager!!.datafileHandler)
        Assert.assertNotNull(manager.userProfileService)
        Assert.assertNotNull(manager.getErrorHandler(InstrumentationRegistry.getInstrumentation().targetContext))
        Assert.assertNotNull(manager.getEventHandler(InstrumentationRegistry.getInstrumentation().targetContext))
    }

    @Test
    fun testBuilderWithOutDatafileConfig() {
        val errorHandler: ErrorHandler = object : ErrorHandler {
            @Throws(Exception::class)
            override fun <T : OptimizelyRuntimeException?> handleError(exception: T) {
                logger.error("Inside error handler", exception)
            }
        }
        val manager = builder().withUserProfileService(DefaultUserProfileService.newInstance(testProjectId, InstrumentationRegistry.getInstrumentation().targetContext))
                .withDatafileDownloadInterval(30L, TimeUnit.MINUTES)
                .withEventDispatchInterval(30L, TimeUnit.MINUTES)
                .withDatafileHandler(DefaultDatafileHandler())
                .withErrorHandler(errorHandler)
                .withEventHandler(DefaultEventHandler.getInstance(InstrumentationRegistry.getInstrumentation().targetContext))
                .withLogger(logger).build(InstrumentationRegistry.getInstrumentation().targetContext)
        org.junit.Assert.assertNull(manager)
    }

    @Test
    fun testBuilderWithOutDatafileConfigWithSdkKey() {
        val errorHandler: ErrorHandler = object : ErrorHandler {
            @Throws(Exception::class)
            override fun <T : OptimizelyRuntimeException?> handleError(exception: T) {
                logger.error("Inside error handler", exception)
            }
        }
        val manager = builder().withUserProfileService(DefaultUserProfileService.newInstance(testProjectId, InstrumentationRegistry.getInstrumentation().targetContext))
                .withDatafileDownloadInterval(30L, TimeUnit.MINUTES)
                .withEventDispatchInterval(30L, TimeUnit.MINUTES)
                .withDatafileHandler(DefaultDatafileHandler())
                .withErrorHandler(errorHandler)
                .withSDKKey("sdkKey7")
                .withEventHandler(DefaultEventHandler.getInstance(InstrumentationRegistry.getInstrumentation().targetContext))
                .withLogger(logger).build(InstrumentationRegistry.getInstrumentation().targetContext)
        Assert.assertNotNull(manager)
        Assert.assertNotNull(manager!!.datafileHandler)
        Assert.assertNotNull(manager.userProfileService)
        Assert.assertNotNull(manager.getEventHandler(InstrumentationRegistry.getInstrumentation().targetContext))
        manager.stop(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @Test
    fun testBuilderWithDatafileConfig() {
        val errorHandler: ErrorHandler = object : ErrorHandler {
            @Throws(Exception::class)
            override fun <T : OptimizelyRuntimeException?> handleError(exception: T) {
                logger.error("Inside error handler", exception)
            }
        }
        val manager = builder().withUserProfileService(DefaultUserProfileService.newInstance(testProjectId, InstrumentationRegistry.getInstrumentation().targetContext))
                .withDatafileDownloadInterval(30L, TimeUnit.MINUTES)
                .withEventDispatchInterval(30L, TimeUnit.MINUTES)
                .withDatafileHandler(DefaultDatafileHandler())
                .withErrorHandler(errorHandler)
                .withDatafileConfig(DatafileConfig(null, "sdkKey7"))
                .withEventHandler(DefaultEventHandler.getInstance(InstrumentationRegistry.getInstrumentation().targetContext))
                .withLogger(logger).build(InstrumentationRegistry.getInstrumentation().targetContext)
        Assert.assertNotNull(manager)
        Assert.assertNotNull(manager!!.datafileHandler)
        Assert.assertNotNull(manager.userProfileService)
        Assert.assertNotNull(manager.getEventHandler(InstrumentationRegistry.getInstrumentation().targetContext))
        manager.stop(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @Test
    fun testBuilderWithOut() {
        val manager = builder(testProjectId).build(InstrumentationRegistry.getInstrumentation().targetContext)
        Assert.assertNotNull(manager)
        Assert.assertNotNull(manager!!.datafileHandler)
        Assert.assertNotNull(manager.userProfileService)
        Assert.assertNotNull(manager.getEventHandler(InstrumentationRegistry.getInstrumentation().targetContext))
    }
}