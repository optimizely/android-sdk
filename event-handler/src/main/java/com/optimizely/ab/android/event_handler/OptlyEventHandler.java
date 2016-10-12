/*
 * Copyright 2016, Optimizely
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.optimizely.ab.android.event_handler;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Reference implementation of {@link EventHandler} for Android.
 *
 * This is the main entry point to the Android Module
 */
public class OptlyEventHandler implements EventHandler {

    @NonNull private final Context context;
    Logger logger = LoggerFactory.getLogger(OptlyEventHandler.class);
    private long dispatchInterval = -1;


    private OptlyEventHandler(@NonNull Context context) {
        this.context = context;
    }

    /**
     * Gets a new instance
     *
     * @param context any valid Android {@link Context}
     */
    public static OptlyEventHandler getInstance(@NonNull Context context) {
        return new OptlyEventHandler(context);
    }

    /**
     * Sets event dispatch interval
     *
     * Events will only be scheduled to dispatch as long as events remain in storage.
     *
     * Events are put into storage when they fail to send over network.
     * @param dispatchInterval the interval in the provided {@link TimeUnit}
     * @param timeUnit a {@link TimeUnit}
     */
    public void setDispatchInterval(long dispatchInterval, TimeUnit timeUnit) {
        this.dispatchInterval = timeUnit.toMillis(dispatchInterval);
    }

    /**
     * @see EventHandler#dispatchEvent(LogEvent)
     */
    @Override
    public void dispatchEvent(LogEvent logEvent) {
        if (logEvent.getEndpointUrl() == null) {
            logger.error("Event dispatcher received a null url");
            return;
        }
        if (logEvent.getBody() == null) {
            logger.error("Event dispatcher received a null request body");
            return;
        }
        if (logEvent.getEndpointUrl().isEmpty()) {
            logger.error("Event dispatcher received an empty url");
        }

        Intent intent = new Intent(context, EventIntentService.class);
        intent.putExtra(EventIntentService.EXTRA_URL, logEvent.getEndpointUrl());
        intent.putExtra(EventIntentService.EXTRA_REQUEST_BODY, logEvent.getBody());
        if (dispatchInterval != -1) {
            intent.putExtra(EventIntentService.EXTRA_INTERVAL, dispatchInterval);
        }
        context.startService(intent);
        logger.info("Sent url {} to the event handler service", logEvent.getEndpointUrl());

    }
}
