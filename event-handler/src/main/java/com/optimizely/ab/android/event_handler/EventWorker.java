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
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
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

    @NonNull
    @Override
    public Result doWork() {
        String url = getInputData().getString("url");
        String body = getEventBodyFromInputData();
        boolean dispatched = true;

        if (url != null && !url.isEmpty() && body != null && !body.isEmpty()) {
            dispatched = eventDispatcher.dispatch(url, body);
        }
        else {
            dispatched = eventDispatcher.dispatch();
        }

        return dispatched ? Result.success() : Result.retry();
    }

    public static Data getData(LogEvent event) {
        int length = event.getBody().length();

        // androidx.work.Data throws IllegalStateException if total data length is more than MAX_DATA_BYTES
        // compress larger body and uncompress it before dispatching. The compress rate is very high because of repeated data (20KB -> 1KB, 45KB -> 1.5KB).

        int maxSizeBeforeCompress = Data.MAX_DATA_BYTES - 1000;  // 1000 reserved for other meta data

        if (length < maxSizeBeforeCompress) {
            return dataForEvent(event);
        } else {
            return compressEvent(event);
        }
    }

    @VisibleForTesting
    public static Data compressEvent(LogEvent event) {
        String url = event.getEndpointUrl();
        String body = event.getBody();

        try {
            byte[] bodyArray = EventHandlerUtils.compress(body);
            return dataForCompressedEvent(url, bodyArray);
        } catch (Exception e) {
            return dataForEvent(url, body);
        }
    }

    @VisibleForTesting
    public static Data dataForEvent(LogEvent event) {
        return dataForEvent(event.getEndpointUrl(), event.getBody());
    }

    @VisibleForTesting
    public static Data dataForEvent(String url, String body) {
        return new Data.Builder()
                .putString("url", url)
                .putString("body", body)
                .build();
    }

    @VisibleForTesting
    public static Data dataForCompressedEvent(String url, byte[] bodyArray) {
        return new Data.Builder()
                .putString("url", url)
                .putByteArray("bodyArray", bodyArray)
                .build();
    }

    @VisibleForTesting
    @Nullable
    public String getEventBodyFromInputData() {
        Data inputData = getInputData();

        // check non-compressed data first

        String body = inputData.getString("body");
        if (body != null) return body;

        // check if data compressed

        byte[] byteArray = inputData.getByteArray("bodyArray");
        try {
            return EventHandlerUtils.uncompress(byteArray);
        } catch (Exception e) {
            return null;
        }
    }

}
