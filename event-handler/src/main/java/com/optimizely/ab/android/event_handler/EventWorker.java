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

    @VisibleForTesting
    public EventDispatcher eventDispatcher;

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
        Data inputData = getInputData();
        String url = inputData.getString("url");
        String body = getEventBodyFromInputData(inputData);
        boolean dispatched = true;

        if (isEventValid(url, body)) {
            dispatched = eventDispatcher.dispatch(url, body);
        } else {
            dispatched = eventDispatcher.dispatch();
        }

        return dispatched ? Result.success() : Result.retry();
    }

    public static Data getData(LogEvent event) {
        String url = event.getEndpointUrl();
        String body = event.getBody();

        // androidx.work.Data throws IllegalStateException if total data length is more than MAX_DATA_BYTES
        // compress larger body and decompress it before dispatching. The compress rate is very high because of repeated data (20KB -> 1KB, 45KB -> 1.5KB).

        int maxSizeBeforeCompress = Data.MAX_DATA_BYTES - 1000;  // 1000 reserved for other meta data

        if (body.length() < maxSizeBeforeCompress) {
            return dataForEvent(url, body);
        } else {
            return compressEvent(url, body);
        }
    }

    @VisibleForTesting
    public static Data compressEvent(String url, String body) {
        try {
            String compressed = EventHandlerUtils.compress(body);
            return dataForCompressedEvent(url, compressed);
        } catch (Exception e) {
            return dataForEvent(url, body);
        }
    }

    @VisibleForTesting
    public static Data dataForEvent(String url, String body) {
        return new Data.Builder()
                .putString("url", url)
                .putString("body", body)
                .build();
    }

    @VisibleForTesting
    public static Data dataForCompressedEvent(String url, String compressed) {
        return new Data.Builder()
                .putString("url", url)
                .putString("bodyCompressed", compressed)
                .build();
    }

    @VisibleForTesting
    @Nullable
    public String getEventBodyFromInputData(Data inputData) {
        // check non-compressed data first

        String body = inputData.getString("body");
        if (body != null) return body;

        // check if data compressed

        String compressed = inputData.getString("bodyCompressed");
        try {
            return EventHandlerUtils.decompress(compressed);
        } catch (Exception e) {
            return null;
        }
    }

    @VisibleForTesting
    public boolean isEventValid(String url, String body) {
        return url != null && !url.isEmpty() && body != null && !body.isEmpty();
    }

}
