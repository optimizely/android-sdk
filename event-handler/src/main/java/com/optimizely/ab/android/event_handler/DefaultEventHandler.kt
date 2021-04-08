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
import com.optimizely.ab.android.shared.WorkerScheduler
import com.optimizely.ab.event.EventHandler
import com.optimizely.ab.event.LogEvent
import org.slf4j.LoggerFactory

/**
 * Reference implementation of [EventHandler] for Android.
 *
 *
 * This is the main entry point to the Android Module.  This event handler creates a service intent and starts it in the passed
 * in context.  The intent service will attempt to send any and all queued events.
 */
class DefaultEventHandler
/**
 * Private constructor
 *
 * @param context current context for service.
 */ private constructor(private val context: Context) : EventHandler {
    @JvmField
    var logger = LoggerFactory.getLogger(DefaultEventHandler::class.java)
    private var dispatchInterval: Long = -1

    /**
     * Sets event dispatch interval
     *
     *
     * Events will only be scheduled to dispatch as long as events remain in storage.
     *
     *
     * Events are put into storage when they fail to send over network.
     *
     * @param dispatchInterval the interval in seconds
     */
    fun setDispatchInterval(dispatchInterval: Long) {
        if (dispatchInterval <= 0) {
            this.dispatchInterval = -1
        } else {
            this.dispatchInterval = dispatchInterval
        }
    }

    /**
     * @see EventHandler.dispatchEvent
     */
    override fun dispatchEvent(logEvent: LogEvent) {
        if (logEvent.endpointUrl == null) {
            logger.error("Event dispatcher received a null url")
            return
        }
        if (logEvent.body == null) {
            logger.error("Event dispatcher received a null request body")
            return
        }
        if (logEvent.endpointUrl.isEmpty()) {
            logger.error("Event dispatcher received an empty url")
        }
        WorkerScheduler.startService(context, EventWorker.workerId, EventWorker::class.java,
                EventWorker.getData(logEvent), dispatchInterval)
        logger.info("Sent url {} to the event handler service", logEvent.endpointUrl)
    }

    companion object {
        /**
         * Gets a new instance
         *
         * @param context any valid Android [Context]
         * @return a new instance of [DefaultEventHandler]
         */
        @JvmStatic
        fun getInstance(context: Context): DefaultEventHandler {
            return DefaultEventHandler(context)
        }
    }
}