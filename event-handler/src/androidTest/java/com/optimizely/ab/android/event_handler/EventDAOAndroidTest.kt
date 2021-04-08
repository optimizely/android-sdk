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
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.optimizely.ab.android.event_handler.EventDAO.Companion.getInstance
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.slf4j.Logger
import java.net.MalformedURLException
import java.net.URL

/**
 * Tests [EventDAO]
 */
@RunWith(AndroidJUnit4::class)
class EventDAOAndroidTest {
    private var eventDAO: EventDAO? = null
    private var logger: Logger? = null
    private var context: Context? = null
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Before
    fun setupEventDAO() {
        logger = Mockito.mock(Logger::class.java)
        context = InstrumentationRegistry.getInstrumentation().targetContext
        eventDAO = getInstance(context!!, "1", logger!!)
    }

    @After
    fun tearDownEventDAO() {
        Assert.assertTrue(context!!.deleteDatabase(String.format(EventSQLiteOpenHelper.DB_NAME, "1")))
    }

    @Test
    @Throws(MalformedURLException::class)
    fun storeEvent() {
        val event = Event(URL("http://www.foo.com"), "bar1=baz1")
        Assert.assertTrue(eventDAO!!.storeEvent(event))
        Mockito.verify(logger)?.info("Inserted {} into db", event)
    }

    @get:Throws(MalformedURLException::class)
    @get:Test
    val events: Unit
        get() {
            val event1 = Event(URL("http://www.foo1.com"), "bar1=baz1")
            val event2 = Event(URL("http://www.foo2.com"), "bar2=baz2")
            val event3 = Event(URL("http://www.foo3.com"), "bar3=baz3")
            Assert.assertTrue(eventDAO!!.storeEvent(event1))
            Assert.assertTrue(eventDAO!!.storeEvent(event2))
            Assert.assertTrue(eventDAO!!.storeEvent(event3))
            val events = eventDAO!!.events
            Assert.assertTrue(events.size == 3)
            val pair1 = events[0]
            val pair2 = events[1]
            val pair3 = events[2]
            Assert.assertEquals(1, pair1.first.toLong())
            Assert.assertEquals(2, pair2.first.toLong())
            Assert.assertEquals(3, pair3.first.toLong())
            Assert.assertEquals("http://www.foo1.com", pair1.second.uRL.toString())
            Assert.assertEquals("bar1=baz1", pair1.second.requestBody)
            Assert.assertEquals("http://www.foo2.com", pair2.second.uRL.toString())
            Assert.assertEquals("bar2=baz2", pair2.second.requestBody)
            Assert.assertEquals("http://www.foo3.com", pair3.second.uRL.toString())
            Assert.assertEquals("bar3=baz3", pair3.second.requestBody)
            Mockito.verify(logger)?.info("Got events from SQLite")
        }

    @Test
    @Throws(MalformedURLException::class)
    fun removeEventSuccess() {
        val event = Event(URL("http://www.foo.com"), "baz=baz")
        Assert.assertTrue(eventDAO!!.storeEvent(event))
        Assert.assertTrue(eventDAO!!.removeEvent(1))
        Mockito.verify(logger)?.info("Removed event with id {} from db", 1L)
    }

    @Test
    @Throws(MalformedURLException::class)
    fun removeEventInvalid() {
        val event = Event(URL("http://www.foo.com"), "baz=baz")
        Assert.assertTrue(eventDAO!!.storeEvent(event))
        Assert.assertFalse(eventDAO!!.removeEvent(2))
        Mockito.verify(logger)?.error("Tried to remove an event id {} that does not exist", 2L)
    }
}