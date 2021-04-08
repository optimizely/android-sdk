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
package com.optimizely.ab.android.event_handler

import android.content.Intent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.runners.MockitoJUnitRunner
import org.slf4j.Logger

/**
 * Unit tests for [EventIntentService]
 */
@RunWith(MockitoJUnitRunner::class)
class EventIntentServiceTest {
    private var service: EventIntentService? = null
    private var logger: Logger? = null
    @Before
    fun setup() {
        service = EventIntentService()
        logger = Mockito.mock(Logger::class.java)
        service!!.logger = logger
    }

    @Test
    fun testHandleNullIntent() {
        service!!.onHandleIntent(null)
        Mockito.verify(logger)?.warn("Handled a null intent")
    }

    @Test
    fun testHandleNullIntentHandler() {
        val intent = Mockito.mock(Intent::class.java)
        service!!.onHandleIntent(intent)
        Mockito.verify(logger)?.warn("Unable to create dependencies needed by intent handler")
    }

    @Test
    fun forwardsToIntentHandler() {
        val intent = Mockito.mock(Intent::class.java)
        val eventDispatcher = Mockito.mock(EventDispatcher::class.java)
        service!!.eventDispatcher = eventDispatcher
        service!!.onHandleIntent(intent)
        Mockito.verify(eventDispatcher).dispatch(intent)
        Mockito.verify(logger)?.info("Handled intent")
    }
}