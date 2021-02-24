/****************************************************************************
 * Copyright 2021, Optimizely, Inc. and contributors                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/
package com.optimizely.ab.android.event_handler;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.OptlyStorage;
import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.ab.event.LogEvent;

import org.slf4j.LoggerFactory;

public class EventWorker extends Worker {
    public static final String workerId = "EventWorker";

    EventDispatcher eventDispatcher;

    public EventWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        OptlyStorage optlyStorage = new OptlyStorage(context);
        EventClient eventClient = new EventClient(new Client(optlyStorage,
                LoggerFactory.getLogger(Client.class)), LoggerFactory.getLogger(EventClient.class));
        EventDAO eventDAO = EventDAO.getInstance(context, "1", LoggerFactory.getLogger(EventDAO.class));
        ServiceScheduler serviceScheduler = new ServiceScheduler(
                context,
                new ServiceScheduler.PendingIntentFactory(context),
                LoggerFactory.getLogger(ServiceScheduler.class));
        eventDispatcher = new EventDispatcher(context, optlyStorage, eventDAO, eventClient, serviceScheduler, LoggerFactory.getLogger(EventDispatcher.class));

    }

    public static Data getData(LogEvent event) {
        return new Data.Builder()
                .putString("url", event.getEndpointUrl())
                .putString("body", event.getBody())
                .build();
    }

    @NonNull
    @Override
    public Result doWork() {
        String url = getInputData().getString("url");
        String body = getInputData().getString("body");
        boolean dispatched = true;

        if (url != null && !url.isEmpty() && body != null && !body.isEmpty()) {
            dispatched = eventDispatcher.dispatch(url, body);
        }
        else {
            dispatched = eventDispatcher.dispatch();
        }

        return dispatched ? Result.success() : Result.retry();
    }
}
