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

import com.optimizely.ab.android.event_handler.EventDAO.Companion.getInstance
import org.junit.runner.RunWith
import com.optimizely.ab.android.shared.OptlyStorage
import com.optimizely.ab.android.shared.ServiceScheduler
import androidx.annotation.RequiresApi
import android.os.Build
import org.junit.Before
import org.mockito.Mockito
import kotlin.Throws
import android.content.Intent
import android.app.AlarmManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.optimizely.ab.android.shared.Client
import org.junit.After
import org.junit.Test
import org.mockito.Matchers
import org.slf4j.Logger
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

/**
 * Tests [EventDispatcher]
 */
@RunWith(AndroidJUnit4::class)
class EventDispatcherTest {
    private var eventDispatcher: EventDispatcher? = null
    private var optlyStorage: OptlyStorage? = null
    private var eventDAO: EventDAO? = null
    private var eventClient: EventClient? = null
    private var serviceScheduler: ServiceScheduler? = null
    private var logger: Logger? = null
    private var context: Context? = null
    private var client: Client? = null
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        logger = Mockito.mock(Logger::class.java)
        client = Mockito.mock(Client::class.java)
        eventDAO = getInstance(context!!, "1", logger!!)
        eventClient = EventClient(client, logger)
        serviceScheduler = Mockito.mock(ServiceScheduler::class.java)
        optlyStorage = Mockito.mock(OptlyStorage::class.java)
        eventDispatcher = EventDispatcher(context!!, optlyStorage!!, eventDAO!!, eventClient!!, serviceScheduler!!, logger!!)
    }

    @After
    fun tearDown() {
        context!!.deleteDatabase(String.format(EventSQLiteOpenHelper.DB_NAME, "1"))
    }

    @Test
    @Throws(IOException::class)
    fun handleIntentSchedulesWhenEventsLeftInStorage() {
        val event1 = Event(URL("http://www.foo1.com"), "")
        val event2 = Event(URL("http://www.foo2.com"), "")
        val event3 = Event(URL("http://www.foo3.com"), "")
        eventDAO!!.storeEvent(event1)
        eventDAO!!.storeEvent(event2)
        eventDAO!!.storeEvent(event3)
        val goodConnection = goodConnection
        Mockito.`when`(client!!.openConnection(event1.uRL)).thenReturn(goodConnection)
        Mockito.`when`(client!!.openConnection(event2.uRL)).thenReturn(goodConnection)
        val badConnection = badConnection
        Mockito.`when`(client!!.openConnection(event3.uRL)).thenReturn(badConnection)
        val mockIntent = Mockito.mock(Intent::class.java)
        Mockito.`when`(mockIntent.getLongExtra(EventIntentService.EXTRA_INTERVAL, -1)).thenReturn(-1L)
        Mockito.`when`(optlyStorage!!.getLong(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR)).thenReturn(AlarmManager.INTERVAL_HOUR)
        eventDispatcher!!.dispatch(mockIntent)
        Mockito.verify(serviceScheduler)?.schedule(mockIntent, -1)
        Mockito.verify(optlyStorage)?.saveLong(EventIntentService.EXTRA_INTERVAL, -1)
        Mockito.verify(logger)?.info("Scheduled events to be dispatched")
    }

    @Test
    @Throws(IOException::class)
    fun handleIntentSchedulesWhenNewEventFailsToSend() {
        val event = Event(URL("http://www.foo.com"), "")
        val intent = Intent(context, EventIntentService::class.java)
        intent.putExtra(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR)
        intent.putExtra(EventIntentService.EXTRA_URL, event.uRL.toString())
        intent.putExtra(EventIntentService.EXTRA_REQUEST_BODY, event.requestBody)
        val badConnection = badConnection
        Mockito.`when`(client!!.openConnection(event.uRL)).thenReturn(badConnection)
        eventDispatcher!!.dispatch(intent)
        Mockito.verify(serviceScheduler)?.schedule(intent, AlarmManager.INTERVAL_HOUR)
        Mockito.verify(optlyStorage)?.saveLong(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR)
        Mockito.verify(logger)?.info("Scheduled events to be dispatched")
    }

    @Test
    fun unschedulesServiceWhenNoEventsToFlush() {
        val mockIntent = Mockito.mock(Intent::class.java)
        eventDispatcher!!.dispatch(mockIntent)
        Mockito.verify(serviceScheduler)?.unschedule(mockIntent)
        Mockito.verify(logger)?.info("Unscheduled event dispatch")
    }

    @Test
    @Throws(MalformedURLException::class)
    fun handleMalformedURL() {
        val url = "foo"
        val mockIntent = Mockito.mock(Intent::class.java)
        Mockito.`when`(mockIntent.hasExtra(EventIntentService.EXTRA_URL)).thenReturn(true)
        Mockito.`when`(mockIntent.getStringExtra(EventIntentService.EXTRA_URL)).thenReturn(url)
        eventDispatcher!!.dispatch(mockIntent)
        Mockito.verify(logger)?.error(Matchers.contains("Received a malformed URL in event handler service"), Matchers.any(MalformedURLException::class.java))
    }

    @get:Throws(IOException::class)
    private val goodConnection: HttpURLConnection
        private get() {
            val httpURLConnection = connection
            Mockito.`when`(httpURLConnection.responseCode).thenReturn(200)
            return httpURLConnection
        }

    @get:Throws(IOException::class)
    private val badConnection: HttpURLConnection
        private get() {
            val httpURLConnection = connection
            Mockito.`when`(httpURLConnection.responseCode).thenReturn(400)
            return httpURLConnection
        }

    @get:Throws(IOException::class)
    private val connection: HttpURLConnection
        private get() {
            val httpURLConnection = Mockito.mock(HttpURLConnection::class.java)
            val outputStream = Mockito.mock(OutputStream::class.java)
            Mockito.`when`(httpURLConnection.outputStream).thenReturn(outputStream)
            val inputStream = Mockito.mock(InputStream::class.java)
            Mockito.`when`(httpURLConnection.inputStream).thenReturn(inputStream)
            return httpURLConnection
        }
}