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
import com.optimizely.ab.android.datafile_handler.DatafileHandler
import com.optimizely.ab.android.sdk.OptimizelyManager.Companion.builder
import com.optimizely.ab.android.shared.DatafileConfig
import com.optimizely.ab.bucketing.UserProfileService
import com.optimizely.ab.error.ErrorHandler
import com.optimizely.ab.event.BatchEventProcessor
import com.optimizely.ab.event.EventHandler
import com.optimizely.ab.event.EventProcessor
import com.optimizely.ab.notification.NotificationCenter
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.slf4j.Logger
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(PowerMockRunner::class)
@PrepareForTest(OptimizelyManager::class, BatchEventProcessor::class)
class OptimizelyManagerIntervalTest {
    private val logger: Logger? = null

    // DatafileDownloadInterval
    @Test
    @Throws(Exception::class)
    fun testBuildWithDatafileDownloadInterval() {
        PowerMockito.whenNew(OptimizelyManager::class.java).withAnyArguments().thenReturn(Mockito.mock(OptimizelyManager::class.java))
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(appContext.applicationContext).thenReturn(appContext)
        val goodNumber: Long = 27
        val manager = builder("1")
                .withLogger(logger)
                .withDatafileDownloadInterval(goodNumber, TimeUnit.MINUTES)
                .build(appContext)
        PowerMockito.verifyNew(OptimizelyManager::class.java).withArguments(Matchers.anyString(),
                Matchers.anyString(),
                Matchers.any(DatafileConfig::class.java),
                Matchers.any(Logger::class.java),
                Matchers.eq(goodNumber * 60L),  // seconds
                Matchers.any(DatafileHandler::class.java),
                Matchers.any(ErrorHandler::class.java),
                Matchers.anyLong(),
                Matchers.any(EventHandler::class.java),
                Matchers.any(EventProcessor::class.java),
                Matchers.any(UserProfileService::class.java),
                Matchers.any(NotificationCenter::class.java),
                Matchers.any(ArrayList<OptimizelyDecideOption>()::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun testBuildWithDatafileDownloadIntervalDeprecated() {
        PowerMockito.whenNew(OptimizelyManager::class.java).withAnyArguments().thenReturn(Mockito.mock(OptimizelyManager::class.java))
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(appContext.applicationContext).thenReturn(appContext)
        val goodNumber = 1234L
        val manager = builder("1")
                .withLogger(logger)
                .withDatafileDownloadInterval(goodNumber) // deprecated
                .build(appContext)
        PowerMockito.verifyNew(OptimizelyManager::class.java).withArguments(Matchers.anyString(),
                Matchers.anyString(),
                Matchers.any(DatafileConfig::class.java),
                Matchers.any(Logger::class.java),
                Matchers.eq(goodNumber),  // seconds
                Matchers.any(DatafileHandler::class.java),
                Matchers.any(ErrorHandler::class.java),
                Matchers.anyLong(),
                Matchers.any(EventHandler::class.java),
                Matchers.any(EventProcessor::class.java),
                Matchers.any(UserProfileService::class.java),
                Matchers.any(NotificationCenter::class.java),
                Matchers.any(ArrayList<OptimizelyDecideOption>()::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun testBuildWithEventDispatchInterval() {
        PowerMockito.whenNew(OptimizelyManager::class.java).withAnyArguments().thenReturn(Mockito.mock(OptimizelyManager::class.java))
        PowerMockito.whenNew(BatchEventProcessor::class.java).withAnyArguments().thenReturn(Mockito.mock(BatchEventProcessor::class.java))
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(appContext.applicationContext).thenReturn(appContext)
        val goodNumber = 100L
        val manager = builder("1")
                .withLogger(logger)
                .withEventDispatchInterval(goodNumber, TimeUnit.SECONDS)
                .build(appContext)
        PowerMockito.verifyNew(BatchEventProcessor::class.java).withArguments(Matchers.any(BlockingQueue::class.java),
                Matchers.any(EventHandler::class.java),
                Matchers.anyInt(),
                Matchers.eq(goodNumber * 1000L),  // milliseconds
                Matchers.anyLong(),
                Matchers.any(ExecutorService::class.java),
                Matchers.any(NotificationCenter::class.java),
                Matchers.any(Any::class.java))
        PowerMockito.verifyNew(OptimizelyManager::class.java).withArguments(Matchers.anyString(),
                Matchers.anyString(),
                Matchers.any(DatafileConfig::class.java),
                Matchers.any(Logger::class.java),
                Matchers.anyLong(),
                Matchers.any(DatafileHandler::class.java),
                Matchers.any(ErrorHandler::class.java),
                Matchers.eq(-1L),  // milliseconds
                Matchers.any(EventHandler::class.java),
                Matchers.any(EventProcessor::class.java),
                Matchers.any(UserProfileService::class.java),
                Matchers.any(NotificationCenter::class.java),
                Matchers.any(ArrayList<OptimizelyDecideOption>()::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun testBuildWithEventDispatchRetryInterval() {
        PowerMockito.whenNew(OptimizelyManager::class.java).withAnyArguments().thenReturn(Mockito.mock(OptimizelyManager::class.java))
        PowerMockito.whenNew(BatchEventProcessor::class.java).withAnyArguments().thenReturn(Mockito.mock(BatchEventProcessor::class.java))
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(appContext.applicationContext).thenReturn(appContext)
        val goodNumber = 100L
        val defaultEventFlushInterval = 30L
        val manager = builder("1")
                .withLogger(logger)
                .withEventDispatchRetryInterval(goodNumber, TimeUnit.MINUTES)
                .build(appContext)
        PowerMockito.verifyNew(BatchEventProcessor::class.java).withArguments(Matchers.any(BlockingQueue::class.java),
                Matchers.any(EventHandler::class.java),
                Matchers.anyInt(),
                Matchers.eq(defaultEventFlushInterval * 1000L),  // milliseconds
                Matchers.anyLong(),
                Matchers.any(ExecutorService::class.java),
                Matchers.any(NotificationCenter::class.java),
                Matchers.any(Any::class.java))
        PowerMockito.verifyNew(OptimizelyManager::class.java).withArguments(Matchers.anyString(),
                Matchers.anyString(),
                Matchers.any(DatafileConfig::class.java),
                Matchers.any(Logger::class.java),
                Matchers.anyLong(),
                Matchers.any(DatafileHandler::class.java),
                Matchers.any(ErrorHandler::class.java),
                Matchers.eq(goodNumber * 1000L * 60L),  // milliseconds
                Matchers.any(EventHandler::class.java),
                Matchers.any(EventProcessor::class.java),
                Matchers.any(UserProfileService::class.java),
                Matchers.any(NotificationCenter::class.java),
                Matchers.any(ArrayList<OptimizelyDecideOption>()::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun testBuildWithEventDispatchIntervalDeprecated() {
        PowerMockito.whenNew(OptimizelyManager::class.java).withAnyArguments().thenReturn(Mockito.mock(OptimizelyManager::class.java))
        PowerMockito.whenNew(BatchEventProcessor::class.java).withAnyArguments().thenReturn(Mockito.mock(BatchEventProcessor::class.java))
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(appContext.applicationContext).thenReturn(appContext)
        val goodNumber = 1234L
        val manager = builder("1")
                .withLogger(logger)
                .withEventDispatchInterval(goodNumber) // deprecated
                .build(appContext)
        PowerMockito.verifyNew(BatchEventProcessor::class.java).withArguments(Matchers.any(BlockingQueue::class.java),
                Matchers.any(EventHandler::class.java),
                Matchers.anyInt(),
                Matchers.eq(goodNumber),  // milliseconds
                Matchers.anyLong(),
                Matchers.any(ExecutorService::class.java),
                Matchers.any(NotificationCenter::class.java),
                Matchers.any(Any::class.java))
        PowerMockito.verifyNew(OptimizelyManager::class.java).withArguments(Matchers.anyString(),
                Matchers.anyString(),
                Matchers.any(DatafileConfig::class.java),
                Matchers.any(Logger::class.java),
                Matchers.anyLong(),
                Matchers.any(DatafileHandler::class.java),
                Matchers.any(ErrorHandler::class.java),
                Matchers.eq(goodNumber),  // milliseconds
                Matchers.any(EventHandler::class.java),
                Matchers.any(EventProcessor::class.java),
                Matchers.any(UserProfileService::class.java),
                Matchers.any(NotificationCenter::class.java),
                Matchers.any(ArrayList<OptimizelyDecideOption>()::class.java))
    }
}