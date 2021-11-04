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
import com.optimizely.ab.android.shared.WorkerScheduler;
import com.optimizely.ab.event.LogEvent;

import org.slf4j.LoggerFactory;

public class EventWorker extends Worker {
    public static final String workerId = "EventWorker";

    public static final String KEY_EVENT_URL = "url";
    public static final String KEY_EVENT_BODY = "body";
    public static final String KEY_EVENT_BODY_COMPRESSED = "bodyCompressed";
    public static final String KEY_EVENT_RETRY_INTERVAL = "retryInterval";

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
        String url = getUrlFromInputData(inputData);
        String body = getEventBodyFromInputData(inputData);
        long interval = getRetryIntervalFromInputData(inputData);

        boolean dispatched = true;

        if (isEventValid(url, body)) {
            dispatched = eventDispatcher.dispatch(url, body);
        } else {
            dispatched = eventDispatcher.dispatch();
        }

        if (interval > 0) {
            return dispatched ? Result.success() : Result.retry();
        } else {
            return Result.success();
        }
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
    public static Data getData(LogEvent event, Long retryInterval) {
        Data data = getData(event);
        if (retryInterval > 0) {
            data = new Data.Builder()
                    .putAll(data)
                    .putLong(KEY_EVENT_RETRY_INTERVAL, retryInterval)
                    .build();
        }

        return data;
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
                .putString(KEY_EVENT_URL, url)
                .putString(KEY_EVENT_BODY, body)
                .build();
    }

    @VisibleForTesting
    public static Data dataForCompressedEvent(String url, String compressed) {
        return new Data.Builder()
                .putString(KEY_EVENT_URL, url)
                .putString(KEY_EVENT_BODY_COMPRESSED, compressed)
                .build();
    }

    @VisibleForTesting
    @Nullable
    public String getEventBodyFromInputData(Data inputData) {
        // check non-compressed data first

        String body = inputData.getString(KEY_EVENT_BODY);
        if (body != null) return body;

        // check if data compressed

        String compressed = inputData.getString(KEY_EVENT_BODY_COMPRESSED);
        try {
            return EventHandlerUtils.decompress(compressed);
        } catch (Exception e) {
            return null;
        }
    }

    @VisibleForTesting
    public String getUrlFromInputData(Data data) {
        return data.getString(KEY_EVENT_URL);
    }

    @VisibleForTesting
    public long getRetryIntervalFromInputData(Data data) {
        return data.getLong(KEY_EVENT_RETRY_INTERVAL, -1);
    }

    @VisibleForTesting
    public boolean isEventValid(String url, String body) {
        return url != null && !url.isEmpty() && body != null && !body.isEmpty();
    }

}
