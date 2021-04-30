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
package com.optimizely.ab.android.event_handler

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.optimizely.ab.android.event_handler.DefaultEventHandler.getInstance
import com.optimizely.ab.event.LogEvent
import com.optimizely.ab.event.internal.payload.EventBatch
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.slf4j.Logger
import java.net.MalformedURLException
import java.util.*

/**
 * Tests [DefaultEventHandler]
 */
@RunWith(AndroidJUnit4::class)
class DefaultEventHandlerAndroidTest {
    private var context: Context? = null
    private var logger: Logger? = null
    private var eventHandler: DefaultEventHandler? = null
    private val url = "http://www.foo.com"
    @Before
    fun setupEventHandler() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        logger = Mockito.mock(Logger::class.java)
        eventHandler = getInstance(context!!)
        eventHandler!!.logger = logger
        eventHandler!!.setDispatchInterval(60L)
    }

    @Test
    @Throws(MalformedURLException::class)
    fun dispatchEventSuccess() {
        eventHandler!!.dispatchEvent(LogEvent(LogEvent.RequestMethod.POST, url, HashMap(), EventBatch()))
        Mockito.verify(logger)?.info("Sent url {} to the event handler service", "http://www.foo.com")
    }

    @Test
    fun dispatchEmptyUrlString() {
        eventHandler!!.dispatchEvent(LogEvent(LogEvent.RequestMethod.POST, "", HashMap(), EventBatch()))
        Mockito.verify(logger)?.error("Event dispatcher received an empty url")
    }

    @Test
    fun dispatchEmptyParams() {
        eventHandler!!.dispatchEvent(LogEvent(LogEvent.RequestMethod.POST, url, HashMap(), EventBatch()))
        //verify(context).startService(any(Intent.class));
        Mockito.verify(logger)?.info("Sent url {} to the event handler service", "http://www.foo.com")
    }
}