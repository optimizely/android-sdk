/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
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
import android.content.Intent;
import android.support.annotation.NonNull;

import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reference implementation of {@link EventHandler} for Android.
 * <p>
 * This is the main entry point to the Android Module.  This event handler creates a service intent and starts it in the passed
 * in context.  The intent service will attempt to send any and all queued events.
 */
public class DefaultEventHandler implements EventHandler {

    @NonNull
    private final Context context;
    Logger logger = LoggerFactory.getLogger(DefaultEventHandler.class);
    private long dispatchInterval = -1;

    /**
     * Private constructor
     * @param context current context for service.
     */
    private DefaultEventHandler(@NonNull Context context) {
        this.context = context;
    }

    /**
     * Gets a new instance
     *
     * @param context any valid Android {@link Context}
     * @return a new instance of {@link DefaultEventHandler}
     */
    public static DefaultEventHandler getInstance(@NonNull Context context) {
        return new DefaultEventHandler(context);
    }

    /**
     * Sets event dispatch interval
     * <p>
     * Events will only be scheduled to dispatch as long as events remain in storage.
     * <p>
     * Events are put into storage when they fail to send over network.
     *
     * @param dispatchInterval the interval in seconds
     */
    public void setDispatchInterval(long dispatchInterval) {
        if (dispatchInterval <= 0) {
            this.dispatchInterval = -1;
        } else {
            this.dispatchInterval = dispatchInterval;
        }
    }

    /**
     * @see EventHandler#dispatchEvent(LogEvent)
     */
    @Override
    public void dispatchEvent(@NonNull LogEvent logEvent) {
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
        intent.putExtra(EventIntentService.EXTRA_INTERVAL, dispatchInterval);

        ServiceScheduler.startService(context, EventIntentService.JOB_ID, intent);

        logger.info("Sent url {} to the event handler service", logEvent.getEndpointUrl());
    }
}
