/****************************************************************************
 * Copyright 2016-2021, Optimizely, Inc. and contributors                        *
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
import android.util.Pair
import com.optimizely.ab.android.shared.CountingIdlingResourceManager
import com.optimizely.ab.android.shared.OptlyStorage
import com.optimizely.ab.android.shared.ServiceScheduler
import org.slf4j.Logger
import java.net.MalformedURLException
import java.net.URL

/**
 * Dispatches [Event] instances sent to [EventIntentService]
 *
 * If sending events to the network fails they will be stored and dispatched again.
 *
 * This abstraction makes unit testing much simpler
 */
class EventDispatcher(private val context: Context, private val optlyStorage: OptlyStorage, private val eventDAO: EventDAO, private val eventClient: EventClient, private val serviceScheduler: ServiceScheduler, private val logger: Logger) {
    fun dispatch(url: String?, body: String?): Boolean {
        var dispatched = false
        var event: Event? = null
        try {
            event = Event(URL(url), body!!)
            // Send the event that triggered this run of the service for store it if sending fails
            dispatched = dispatch(event)
        } catch (e: MalformedURLException) {
            logger.error("Received a malformed URL in event handler service", e)
        } finally {
            eventDAO.closeDb()
        }
        return dispatched
    }

    @Deprecated("")
    fun dispatch(intent: Intent) {
        // Dispatch events still in storage
        var dispatched = dispatch()
        if (intent.hasExtra(EventIntentService.EXTRA_URL)) {
            try {
                val urlExtra = intent.getStringExtra(EventIntentService.EXTRA_URL)
                val requestBody = intent.getStringExtra(EventIntentService.EXTRA_REQUEST_BODY)
                val event = Event(URL(urlExtra), requestBody)
                // Send the event that triggered this run of the service for store it if sending fails
                dispatched = dispatch(event)
            } catch (e: MalformedURLException) {
                logger.error("Received a malformed URL in event handler service", e)
            } catch (e: Exception) {
                logger.warn("Failed to dispatch event.", e)
            }
        }
        try {
            if (!dispatched) {
                val interval = getInterval(intent)
                serviceScheduler.schedule(intent, interval)
                saveInterval(interval)
                logger.info("Scheduled events to be dispatched")
            } else {
                // Quit trying to dispatch events because their aren't any in storage
                serviceScheduler.unschedule(intent)
                logger.info("Unscheduled event dispatch")
            }
        } catch (e: Exception) {
            logger.warn("Failed to schedule event dispatch.", e)
        } finally {
            eventDAO.closeDb()
        }
    }

    // Either grab the interval for the first time from Intent or from storage
    // The extra won't exist if we are being restarted after a reboot or app update
    @Deprecated("")
    private fun getInterval(intent: Intent): Long {
        // We are either scheduling for the first time or rescheduling after our alarms were cancelled
        return intent.getLongExtra(EventIntentService.EXTRA_INTERVAL, -1)
    }

    @Deprecated("")
    private fun saveInterval(interval: Long) {
        optlyStorage.saveLong(EventIntentService.EXTRA_INTERVAL, interval)
    }

    /**
     * Dispatch all events in storage
     *
     * @return true if all events were dispatched, otherwise false
     */
    fun dispatch(): Boolean {
        val events = eventDAO.events
        val iterator = events.iterator()
        while (iterator.hasNext()) {
            val event = iterator.next()
            val eventWasSent = eventClient.sendEvent(event.second)
            if (eventWasSent) {
                val eventWasDeleted = eventDAO.removeEvent(event.first)
                if (!eventWasDeleted) {
                    logger.warn("Unable to delete an event from local storage that was sent to successfully")
                }
            }
        }
        return events.isEmpty()
    }

    /**
     * Send a single event
     *
     * @param event an [Event] instance to attempt to dispatch
     * @return true if event was sent to network, otherwise false if stored
     */
    private fun dispatch(event: Event): Boolean {
        val eventWasSent = eventClient.sendEvent(event)
        return if (eventWasSent) {
            CountingIdlingResourceManager.decrement()
            CountingIdlingResourceManager.recordEvent(Pair(event.uRL.toString(), event.requestBody))
            true
        } else {
            val eventWasStored = eventDAO.storeEvent(event)
            if (!eventWasStored) {
                logger.error("Unable to send or store event {}", event)
                // Return true since nothing was stored
                true
            } else {
                false
            }
        }
    }
}