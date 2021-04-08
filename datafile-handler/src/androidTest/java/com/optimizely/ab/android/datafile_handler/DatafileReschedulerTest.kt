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
package com.optimizely.ab.android.datafile_handler

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.optimizely.ab.android.shared.Cache
import com.optimizely.ab.android.shared.DatafileConfig
import junit.framework.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentCaptor
import org.mockito.Matchers
import org.mockito.Mockito
import org.slf4j.Logger

/**
 * Tests for [DatafileRescheduler]
 */
@RunWith(JUnit4::class)
@Ignore // Tests pass locally but not on travis
// probably starting too many services
class DatafileReschedulerTest {
    private var datafileRescheduler: DatafileRescheduler? = null
    private var logger: Logger? = null
    @Before
    fun setup() {
        datafileRescheduler = DatafileRescheduler()
        logger = Mockito.mock(Logger::class.java)
        datafileRescheduler!!.logger = logger
    }

    @Test
    fun receivingNullContext() {
        datafileRescheduler!!.onReceive(null, Mockito.mock(Intent::class.java))
        Mockito.verify(logger)?.warn("Received invalid broadcast to data file rescheduler")
    }

    @Test
    fun receivingNullIntent() {
        datafileRescheduler!!.onReceive(Mockito.mock(Context::class.java), null)
        Mockito.verify(logger)?.warn("Received invalid broadcast to data file rescheduler")
    }

    @Test
    fun receivedActionBootCompleted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Mockito.mock(Intent::class.java)
        Mockito.`when`(intent.action).thenReturn(Intent.ACTION_BOOT_COMPLETED)
        datafileRescheduler!!.onReceive(context, intent)
        Mockito.verify(logger)?.info("Received intent with action {}", Intent.ACTION_BOOT_COMPLETED)
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Test
    fun receivedActionMyPackageReplaced() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Mockito.mock(Intent::class.java)
        Mockito.`when`(intent.action).thenReturn(Intent.ACTION_MY_PACKAGE_REPLACED)
        datafileRescheduler!!.onReceive(context, intent)
        Mockito.verify(logger)?.info("Received intent with action {}", Intent.ACTION_MY_PACKAGE_REPLACED)
    }

    @Test
    fun dispatchingOneWithoutEnvironment() {
        val mockContext = Mockito.mock(Context::class.java)
        val cache = Cache(InstrumentationRegistry.getInstrumentation().targetContext, logger!!)
        val backgroundWatchersCache = BackgroundWatchersCache(cache, logger!!)
        backgroundWatchersCache.setIsWatching(DatafileConfig("1", null), true)
        val logger = Mockito.mock(Logger::class.java)
        val dispatcher = DatafileRescheduler.Dispatcher(mockContext, backgroundWatchersCache, logger)
        dispatcher.dispatch()
        val captor = ArgumentCaptor.forClass(Intent::class.java)
        Mockito.verify(mockContext).startService(captor.capture())
        Assert.assertEquals(DatafileConfig("1", null).toJSONString(), captor.value.getStringExtra(DatafileService.EXTRA_DATAFILE_CONFIG))
        Mockito.verify(logger).info("Rescheduled data file watching for project {}", "1")
        cache.delete(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME)
    }

    @Test
    fun dispatchingOneWithEnvironment() {
        val mockContext = Mockito.mock(Context::class.java)
        val cache = Cache(InstrumentationRegistry.getInstrumentation().targetContext, logger!!)
        val backgroundWatchersCache = BackgroundWatchersCache(cache, logger!!)
        backgroundWatchersCache.setIsWatching(DatafileConfig("1", "2"), true)
        val logger = Mockito.mock(Logger::class.java)
        val dispatcher = DatafileRescheduler.Dispatcher(mockContext, backgroundWatchersCache, logger)
        dispatcher.dispatch()
        val captor = ArgumentCaptor.forClass(Intent::class.java)
        Mockito.verify(mockContext).startService(captor.capture())
        Assert.assertEquals(DatafileConfig("1", "2").toJSONString(), captor.value.getStringExtra(DatafileService.EXTRA_DATAFILE_CONFIG))
        Mockito.verify(logger).info("Rescheduled data file watching for project {}", "2")
        cache.delete(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME)
    }

    @Test
    fun dispatchingManyWithoutEnvironment() {
        val mockContext = Mockito.mock(Context::class.java)
        val cache = Cache(InstrumentationRegistry.getInstrumentation().targetContext, logger!!)
        val backgroundWatchersCache = BackgroundWatchersCache(cache, logger!!)
        backgroundWatchersCache.setIsWatching(DatafileConfig("1", null), true)
        backgroundWatchersCache.setIsWatching(DatafileConfig("2", null), true)
        backgroundWatchersCache.setIsWatching(DatafileConfig("3", null), true)
        val logger = Mockito.mock(Logger::class.java)
        val dispatcher = DatafileRescheduler.Dispatcher(mockContext, backgroundWatchersCache, logger)
        dispatcher.dispatch()
        Mockito.verify(mockContext, Mockito.times(3)).startService(Matchers.any(Intent::class.java))
        cache.delete(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME)
    }

    @Test
    fun dispatchingManyWithEnvironment() {
        val mockContext = Mockito.mock(Context::class.java)
        val cache = Cache(InstrumentationRegistry.getInstrumentation().targetContext, logger!!)
        val backgroundWatchersCache = BackgroundWatchersCache(cache, logger!!)
        backgroundWatchersCache.setIsWatching(DatafileConfig("1", "1"), true)
        backgroundWatchersCache.setIsWatching(DatafileConfig("2", "1"), true)
        backgroundWatchersCache.setIsWatching(DatafileConfig("3", "1"), true)
        val logger = Mockito.mock(Logger::class.java)
        val dispatcher = DatafileRescheduler.Dispatcher(mockContext, backgroundWatchersCache, logger)
        dispatcher.dispatch()
        Mockito.verify(mockContext, Mockito.times(3)).startService(Matchers.any(Intent::class.java))
        cache.delete(BackgroundWatchersCache.BACKGROUND_WATCHERS_FILE_NAME)
    }
}