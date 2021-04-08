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

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.optimizely.ab.android.shared.ServiceScheduler
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.runners.MockitoJUnitRunner
import org.slf4j.Logger

/**
 * Unit tests for [EventRescheduler]
 */
@RunWith(MockitoJUnitRunner::class)
@Ignore
class EventReschedulerTest {
    private var context: Context? = null
    private var intent: Intent? = null
    private var logger: Logger? = null
    private var rescheduler: EventRescheduler? = null
    @Before
    fun setupEventRescheduler() {
        context = Mockito.mock(Context::class.java)
        intent = Mockito.mock(Intent::class.java)
        logger = Mockito.mock(Logger::class.java)
        rescheduler = Mockito.mock(EventRescheduler::class.java)
        rescheduler = EventRescheduler()
        rescheduler!!.logger = logger
    }

    @Test
    fun onReceiveNullIntent() {
        rescheduler!!.onReceive(context!!, null)
        Mockito.verify(logger)?.warn("Received invalid broadcast to event rescheduler")
    }

    @Test
    fun onReceiveNullContext() {
        rescheduler!!.onReceive(null, intent!!)
        Mockito.verify(logger)?.warn("Received invalid broadcast to event rescheduler")
    }

    @Test
    fun onReceiveInvalidAction() {
        Mockito.`when`(intent!!.action).thenReturn("invalid")
        rescheduler!!.onReceive(context!!, intent!!)
        Mockito.verify(logger)?.warn("Received unsupported broadcast action to event rescheduler")
    }

    @Test
    fun onReceiveValidBootComplete() {
        Mockito.`when`(intent!!.action).thenReturn(Intent.ACTION_BOOT_COMPLETED)
        rescheduler!!.onReceive(context!!, intent!!)
        Mockito.verify(logger)?.info("Rescheduling event flushing if necessary")
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB_MR1)
    @Test
    fun onReceiveValidPackageReplaced() {
        Mockito.`when`(intent!!.action).thenReturn(Intent.ACTION_MY_PACKAGE_REPLACED)
        rescheduler!!.onReceive(context!!, intent!!)
        Mockito.verify(logger)?.info("Rescheduling event flushing if necessary")
    }

    @Test
    fun flushOnWifiConnectionIfScheduled() {
        val eventServiceIntent = Mockito.mock(Intent::class.java)
        val serviceScheduler = Mockito.mock(ServiceScheduler::class.java)
        Mockito.`when`(intent!!.action).thenReturn(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
        Mockito.`when`(intent!!.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)).thenReturn(true)
        Mockito.`when`(serviceScheduler.isScheduled(eventServiceIntent)).thenReturn(true)
        rescheduler!!.reschedule(context!!, intent!!, eventServiceIntent, serviceScheduler)
        Mockito.verify(context)?.startService(eventServiceIntent)
        Mockito.verify(logger)?.info("Preemptively flushing events since wifi became available")
    }
}