/****************************************************************************
 * Copyright 2021, Optimizely, Inc. and contributors                   *
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
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.optimizely.ab.android.event_handler.EventDAO.Companion.getInstance
import com.optimizely.ab.android.shared.Client
import com.optimizely.ab.android.shared.OptlyStorage
import com.optimizely.ab.android.shared.ServiceScheduler
import com.optimizely.ab.android.shared.ServiceScheduler.PendingIntentFactory
import com.optimizely.ab.event.LogEvent
import org.slf4j.LoggerFactory

public class EventWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    var eventDispatcher: EventDispatcher
    override fun doWork(): Result {
        val url = inputData.getString("url")
        val body = inputData.getString("body")
        var dispatched = true
        dispatched = if (url != null && !url.isEmpty() && body != null && !body.isEmpty()) {
            eventDispatcher.dispatch(url, body)
        } else {
            eventDispatcher.dispatch()
        }
        return if (dispatched) Result.success() else Result.retry()
    }

    companion object {
        const val workerId = "EventWorker"
        fun getData(event: LogEvent): Data {
            return Data.Builder()
                    .putString("url", event.endpointUrl)
                    .putString("body", event.body)
                    .build()
        }
    }

    init {
        val optlyStorage = OptlyStorage(context)
        val eventClient = EventClient(Client(optlyStorage,
                LoggerFactory.getLogger(Client::class.java)), LoggerFactory.getLogger(EventClient::class.java))
        val eventDAO = getInstance(context, "1", LoggerFactory.getLogger(EventDAO::class.java))
        val serviceScheduler = ServiceScheduler(
                context,
                PendingIntentFactory(context),
                LoggerFactory.getLogger(ServiceScheduler::class.java))
        eventDispatcher = EventDispatcher(context, optlyStorage, eventDAO, eventClient, serviceScheduler, LoggerFactory.getLogger(EventDispatcher::class.java))
    }
}