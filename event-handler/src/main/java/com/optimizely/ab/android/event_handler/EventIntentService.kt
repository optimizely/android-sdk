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

import android.app.IntentService
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.optimizely.ab.android.event_handler.EventDAO
import com.optimizely.ab.android.event_handler.EventDAO.Companion.getInstance
import com.optimizely.ab.android.shared.Client
import com.optimizely.ab.android.shared.OptlyStorage
import com.optimizely.ab.android.shared.ServiceScheduler
import com.optimizely.ab.android.shared.ServiceScheduler.PendingIntentFactory
import org.slf4j.LoggerFactory

/**
 * Android [IntentService] that handles dispatching events to the Optimizely results servers.
 *
 *
 * Can be scheduled to run on interval.
 *
 *
 * Intents sent to this service are handled in order and on a background thread.  Think of it as a
 * worker queue.
 *
 */
@Deprecated("")
class EventIntentService : IntentService("EventHandlerService") {
    @JvmField
    var logger = LoggerFactory.getLogger(EventIntentService::class.java)
    @JvmField
    var eventDispatcher: EventDispatcher? = null

    /**
     * Create the event dispatcher [EventDispatcher]
     * @see IntentService.onCreate
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    override fun onCreate() {
        super.onCreate()
        val optlyStorage = OptlyStorage(this)
        val eventClient = EventClient(Client(optlyStorage,
                LoggerFactory.getLogger(Client::class.java)), LoggerFactory.getLogger(EventClient::class.java))
        val eventDAO = getInstance(this, "1", LoggerFactory.getLogger(EventDAO::class.java))
        val serviceScheduler = ServiceScheduler(
                this,
                PendingIntentFactory(this),
                LoggerFactory.getLogger(ServiceScheduler::class.java))
        eventDispatcher = EventDispatcher(this, optlyStorage, eventDAO, eventClient, serviceScheduler, LoggerFactory.getLogger(EventDispatcher::class.java))
    }

    /**
     * Dispatch event in intent.  This will also try to empty the event queue.
     * @see IntentService.onHandleIntent
     */
    public override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            logger.warn("Handled a null intent")
            return
        }
        if (eventDispatcher != null) {
            logger.info("Handled intent")
            eventDispatcher!!.dispatch(intent)
        } else {
            logger.warn("Unable to create dependencies needed by intent handler")
        }
    }

    companion object {
        const val EXTRA_URL = "com.optimizely.ab.android.EXTRA_URL"
        const val EXTRA_REQUEST_BODY = "com.optimizely.ab.android.EXTRA_REQUEST_BODY"
        const val EXTRA_INTERVAL = "com.optimizely.ab.android.EXTRA_INTERVAL"
        const val JOB_ID = 2112
    }
}