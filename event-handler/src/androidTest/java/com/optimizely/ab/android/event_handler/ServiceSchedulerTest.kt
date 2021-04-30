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

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.optimizely.ab.android.shared.OptlyStorage
import com.optimizely.ab.android.shared.ServiceScheduler
import com.optimizely.ab.android.shared.ServiceScheduler.PendingIntentFactory
import junit.framework.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.slf4j.Logger

/**
 * Tests for [ServiceScheduler]
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
@Ignore
class ServiceSchedulerTest {
    private var optlyStorage: OptlyStorage? = null
    private var pendingIntentFactory: PendingIntentFactory? = null
    private var alarmManager: AlarmManager? = null
    private var jobScheduler: JobScheduler? = null
    private var logger: Logger? = null
    private var serviceScheduler: ServiceScheduler? = null
    private var context: Context? = null
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Before
    fun setup() {
        context = Mockito.mock(Context::class.java)
        optlyStorage = Mockito.mock(OptlyStorage::class.java)
        alarmManager = Mockito.mock(AlarmManager::class.java)
        jobScheduler = Mockito.mock(JobScheduler::class.java)
        Mockito.`when`(context?.getApplicationContext()).thenReturn(context)
        Mockito.`when`(context?.getSystemService(Context.ALARM_SERVICE)).thenReturn(alarmManager)
        Mockito.`when`(context?.getSystemService(Context.JOB_SCHEDULER_SERVICE)).thenReturn(jobScheduler)
        pendingIntentFactory = Mockito.mock(PendingIntentFactory::class.java)
        logger = Mockito.mock(Logger::class.java)
        serviceScheduler = ServiceScheduler(context!!, pendingIntentFactory!!, logger!!)
    }

    @Ignore
    @Test
    fun testScheduleWithNoDurationExtra() {
        val intent = Intent(context, EventIntentService::class.java)
        Mockito.`when`(pendingIntentFactory!!.hasPendingIntent(intent)).thenReturn(false)
        val pendingIntent = pendingIntent
        Mockito.`when`(pendingIntentFactory!!.getPendingIntent(intent)).thenReturn(pendingIntent)
        Mockito.`when`(optlyStorage!!.getLong(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR)).thenReturn(AlarmManager.INTERVAL_HOUR)
        serviceScheduler!!.schedule(intent, AlarmManager.INTERVAL_HOUR)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val jobInfoArgumentCaptor = ArgumentCaptor.forClass(JobInfo::class.java)
            Mockito.verify(jobScheduler)?.schedule(jobInfoArgumentCaptor.capture())
            Assert.assertEquals(jobInfoArgumentCaptor.value.intervalMillis, AlarmManager.INTERVAL_HOUR)
        } else {
            Mockito.verify(alarmManager)?.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_HOUR, pendingIntent)
        }
        Mockito.verify(logger)?.info("Scheduled {}", intent.component!!.toShortString())
        pendingIntent.cancel()
    }

    @Test
    @Ignore
    fun invalidDuration() {
        serviceScheduler!!.schedule(Intent(context, EventIntentService::class.java), 0)
        Mockito.verify(logger)?.error("Tried to schedule an interval less than 1")
    }

    @Test
    @Ignore
    fun testScheduleWithDurationExtra() {
        val intent = Intent(context, EventIntentService::class.java)
        Mockito.`when`(pendingIntentFactory!!.hasPendingIntent(intent)).thenReturn(false)
        val pendingIntent = pendingIntent
        Mockito.`when`(pendingIntentFactory!!.getPendingIntent(intent)).thenReturn(pendingIntent)
        Mockito.`when`(optlyStorage!!.getLong(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR)).thenReturn(AlarmManager.INTERVAL_HOUR)
        val duration = AlarmManager.INTERVAL_DAY
        intent.putExtra(EventIntentService.EXTRA_INTERVAL, duration)
        serviceScheduler!!.schedule(intent, duration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val jobInfoArgumentCaptor = ArgumentCaptor.forClass(JobInfo::class.java)
            Mockito.verify(jobScheduler)?.schedule(jobInfoArgumentCaptor.capture())
            Assert.assertEquals(jobInfoArgumentCaptor.value.intervalMillis, duration)
        } else {
            Mockito.verify(alarmManager)?.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, duration, duration, pendingIntent)
        }
        Mockito.verify(logger)?.info("Scheduled {}", intent.component!!.toShortString())
        pendingIntent.cancel()
    }

    @Test
    @Ignore
    fun testAlreadyScheduledAlarm() {
        val intent = Intent(context, EventIntentService::class.java)
        Mockito.`when`(pendingIntentFactory!!.hasPendingIntent(intent)).thenReturn(true)
        Mockito.`when`(pendingIntentFactory!!.getPendingIntent(intent)).thenReturn(pendingIntent)
        serviceScheduler!!.schedule(intent, AlarmManager.INTERVAL_HOUR)
        Mockito.verify(logger)?.info("Unscheduled {}", intent.component!!.toShortString())
    }

    @Test
    @Ignore
    fun arePendingIntentsForTheSameComponentEqual() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, EventIntentService::class.java)
        val pendingIntent1 = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val pendingIntent2 = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        Assert.assertEquals(pendingIntent1, pendingIntent2)
        pendingIntent1.cancel()
        pendingIntent2.cancel()
    }

    @Test
    @Ignore
    fun howDoesFlagNoCreateWorks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, EventIntentService::class.java)
        val pendingIntent1 = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE)
        Assert.assertNull(pendingIntent1)
        val pendingIntent2 = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val pendingIntent3 = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE)
        Assert.assertEquals(pendingIntent2, pendingIntent3)
        pendingIntent2.cancel()
        pendingIntent3.cancel()
    }

    @Test
    @Ignore
    fun hasPendingIntent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pendingIntentFactory = PendingIntentFactory(context)
        val intent = Intent(context, EventIntentService::class.java)
        Assert.assertFalse(pendingIntentFactory.hasPendingIntent(intent))
        val pendingIntent1 = pendingIntentFactory.getPendingIntent(intent)
        Assert.assertTrue(pendingIntentFactory.hasPendingIntent(intent))
        val pendingIntent2 = pendingIntentFactory.getPendingIntent(intent)
        Assert.assertEquals(pendingIntent1, pendingIntent2)
    }

    @Test
    @Ignore
    fun testCancel() {
        val pendingIntent = pendingIntent
        val intent = Intent(context, EventIntentService::class.java)
        Mockito.`when`(pendingIntentFactory!!.getPendingIntent(intent)).thenReturn(pendingIntent)
        serviceScheduler!!.unschedule(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Mockito.verify(jobScheduler)?.allPendingJobs
        } else {
            Mockito.verify(alarmManager)?.cancel(pendingIntent)
        }
        Mockito.verify(logger)?.info("Unscheduled {}", intent.component!!.toShortString())
    }

    // Mockito can't mock PendingIntent because it's final
    private val pendingIntent: PendingIntent
        private get() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            return PendingIntent.getService(context, 0, Intent(context, EventIntentService::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
        }
}