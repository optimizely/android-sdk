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
package com.optimizely.ab.android.shared

import android.app.IntentService
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.optimizely.ab.android.shared.ServiceScheduler
import com.optimizely.ab.android.shared.ServiceScheduler.PendingIntentFactory
import com.optimizely.ab.android.shared.ServiceSchedulerTest.MyIntent
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory

/**
 * Tests for [ServiceScheduler]
 */
@RunWith(AndroidJUnit4::class)
class ServiceSchedulerTest {
    private var context: Context? = null

    class MyIntent : IntentService("MyItentServiceTest") {
        override fun onHandleIntent(intent: Intent?) {}

        companion object {
            // if you want to schedule an intent for Android O or greater, the intent has to have the public
            // job id or it will not be scheduled.
            const val JOB_ID = 2112
        }
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testScheduler() {
        val pendingIntentFactory = PendingIntentFactory(context!!.applicationContext)
        val serviceScheduler = ServiceScheduler(context!!, pendingIntentFactory,
                LoggerFactory.getLogger(ServiceScheduler::class.java))
        val intent = Intent(context, MyIntent::class.java)
        serviceScheduler.schedule(intent, 30L)
        Assert.assertTrue(serviceScheduler.isScheduled(intent))
        serviceScheduler.unschedule(intent)
        Assert.assertFalse(serviceScheduler.isScheduled(intent))
    }
}