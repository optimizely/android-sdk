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

import android.app.AlarmManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import com.optimizely.ab.android.datafile_handler.DatafileService.LocalBinder
import com.optimizely.ab.android.shared.Cache
import com.optimizely.ab.android.shared.Client
import com.optimizely.ab.android.shared.DatafileConfig
import com.optimizely.ab.android.shared.ServiceScheduler
import com.optimizely.ab.android.shared.ServiceScheduler.PendingIntentFactory
import junit.framework.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Matchers
import org.mockito.Mockito
import org.slf4j.Logger
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Test for [DatafileService]
 */
// TODO These tests will pass individually but they fail when run as group
// Known bug https://code.google.com/p/android/issues/detail?id=180396
@Ignore
class DatafileServiceTest {
    private var executor: ExecutorService? = null

    @Rule
    val mServiceRule = ServiceTestRule()
    @Before
    fun setup() {
        executor = Executors.newSingleThreadExecutor()
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    @Ignore
    @Throws(TimeoutException::class)
    fun testBinding() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, DatafileService::class.java)
        var binder: IBinder? = null
        var it = 0
        while (mServiceRule.bindService(intent).also { binder = it } == null && it < MAX_ITERATION) {
            it++
        }
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val logger = Mockito.mock(Logger::class.java)
        val datafileCache = DatafileCache("1", Cache(targetContext, logger), logger)
        val client = Mockito.mock(Client::class.java)
        val datafileClient = DatafileClient(client, logger)
        val datafileLoadedListener = Mockito.mock(DatafileLoadedListener::class.java)
        val datafileService = (binder as LocalBinder?)!!.service
        val datafileLoader = DatafileLoader(targetContext, datafileClient, datafileCache, Mockito.mock(Logger::class.java))
        datafileService.getDatafile("1", datafileLoader, datafileLoadedListener)
        Assert.assertTrue(datafileService.isBound)
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    @Throws(TimeoutException::class)
    fun testValidStart() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, DatafileService::class.java)
        var binder: IBinder? = null
        var it = 0
        while (mServiceRule.bindService(intent).also { binder = it } == null && it < MAX_ITERATION) {
            it++
        }
        intent.putExtra(DatafileService.EXTRA_DATAFILE_CONFIG, DatafileConfig("1", null).toJSONString())
        val datafileService = (binder as LocalBinder?)!!.service
        val logger = Mockito.mock(Logger::class.java)
        datafileService.logger = logger
        val `val` = datafileService.onStartCommand(intent, 0, 0)
        Assert.assertEquals(`val`, Service.START_FLAG_REDELIVERY)
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    @Ignore
    @Throws(TimeoutException::class)
    fun testNullIntentStart() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, DatafileService::class.java)
        var binder: IBinder? = null
        var it = 0
        while (mServiceRule.bindService(intent).also { binder = it } == null && it < MAX_ITERATION) {
            it++
        }
        mServiceRule.bindService(intent)
        val datafileService = (binder as LocalBinder?)!!.service
        val logger = Mockito.mock(Logger::class.java)
        datafileService.logger = logger
        datafileService.onStartCommand(null, 0, 0)
        Mockito.verify(logger).warn("Data file service received a null intent")
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    @Ignore
    @Throws(TimeoutException::class)
    fun testNoProjectIdIntentStart() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, DatafileService::class.java)
        var binder: IBinder? = null
        var it = 0
        while (mServiceRule.bindService(intent).also { binder = it } == null && it < MAX_ITERATION) {
            it++
        }
        val datafileService = (binder as LocalBinder?)!!.service
        val logger = Mockito.mock(Logger::class.java)
        datafileService.logger = logger
        datafileService.onStartCommand(intent, 0, 0)
        Mockito.verify(logger).warn("Data file service received an intent with no project id extra")
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    @Ignore
    @Throws(TimeoutException::class)
    fun testUnbind() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, DatafileService::class.java)
        var binder: IBinder? = null
        var it = 0
        while (mServiceRule.bindService(intent).also { binder = it } == null && it < MAX_ITERATION) {
            it++
        }
        val datafileService = (binder as LocalBinder?)!!.service
        val logger = Mockito.mock(Logger::class.java)
        datafileService.logger = logger
        datafileService.onUnbind(intent)
        Mockito.verify(logger).info("All clients are unbound from data file service")
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    @Ignore
    fun testIntentExtraData() {
        val context = Mockito.mock(Context::class.java)
        Mockito.`when`(context.packageName).thenReturn("com.optly")
        val serviceScheduler = Mockito.mock(ServiceScheduler::class.java)
        val captor = ArgumentCaptor.forClass(Intent::class.java)
        val alarmManager = context
                .getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntentFactory = PendingIntentFactory(context)
        val intent = Intent(context, DatafileService::class.java)
        intent.putExtra(DatafileService.EXTRA_DATAFILE_CONFIG, DatafileConfig("1", null).toJSONString())
        serviceScheduler.schedule(intent, TimeUnit.HOURS.toMillis(1L))
        try {
            executor!!.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Timed out")
        }
        Mockito.verify(serviceScheduler).schedule(captor.capture(), Matchers.eq(TimeUnit.HOURS.toMillis(1L)))
        val intent2 = captor.value
        Assert.assertTrue(intent2.component!!.shortClassName.contains("DatafileService"))
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    fun testGetDatafileUrl() {
        // HARD-CODING link here to make sure we don't unintentionally mess up the datafile version
        // and url by accidentally changing those constants.
        // us to update this test.
        val datafileUrl = DatafileConfig("1", null).url
        Assert.assertEquals("https://cdn.optimizely.com/json/1.json", datafileUrl)
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    fun testGetDatafileEnvironmentUrl() {
        // HARD-CODING link here to make sure we don't unintentionally mess up the datafile version
        // and url by accidentally changing those constants.
        // us to update this test.
        val datafileUrl = DatafileConfig("1", "2").url
        Assert.assertEquals("https://cdn.optimizely.com/datafiles/2.json", datafileUrl)
    }

    companion object {
        private const val MAX_ITERATION = 100
    }
}