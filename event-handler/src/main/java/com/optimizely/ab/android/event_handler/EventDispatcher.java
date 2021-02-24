/****************************************************************************
 * Copyright 2016-2021, Optimizely, Inc. and contributors                        *
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
import android.util.Pair;

import androidx.annotation.NonNull;

import com.optimizely.ab.android.shared.CountingIdlingResourceManager;
import com.optimizely.ab.android.shared.OptlyStorage;
import com.optimizely.ab.android.shared.ServiceScheduler;

import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

/**
 * Dispatches {@link Event} instances sent to {@link EventIntentService}
 *
 * If sending events to the network fails they will be stored and dispatched again.
 *
 * This abstraction makes unit testing much simpler
 */
class EventDispatcher {

    @NonNull private final Context context;
    @NonNull private final ServiceScheduler serviceScheduler;
    @NonNull private final EventDAO eventDAO;
    @NonNull private final EventClient eventClient;
    @NonNull private final Logger logger;
    @NonNull private final OptlyStorage optlyStorage;

    EventDispatcher(@NonNull Context context, @NonNull OptlyStorage optlyStorage, @NonNull EventDAO eventDAO, @NonNull EventClient eventClient, @NonNull ServiceScheduler serviceScheduler, @NonNull Logger logger) {
        this.context = context;
        this.optlyStorage = optlyStorage;
        this.eventDAO = eventDAO;
        this.eventClient = eventClient;
        this.serviceScheduler = serviceScheduler;
        this.logger = logger;
    }

    boolean dispatch(String url, String body) {
        boolean dispatched = false;

        Event event = null;
        try {
            event = new Event(new URL(url), body);
            // Send the event that triggered this run of the service for store it if sending fails
            dispatched = dispatch(event);
        } catch (MalformedURLException e) {
            logger.error("Received a malformed URL in event handler service", e);
        }
        finally {
            eventDAO.closeDb();
        }

        return dispatched;
    }

    @Deprecated
    void dispatch(@NonNull Intent intent) {
        // Dispatch events still in storage
        boolean dispatched = dispatch();

        if (intent.hasExtra(EventIntentService.EXTRA_URL)) {
            try {
                String urlExtra = intent.getStringExtra(EventIntentService.EXTRA_URL);
                String requestBody = intent.getStringExtra(EventIntentService.EXTRA_REQUEST_BODY);
                Event event = new Event(new URL(urlExtra), requestBody);
                // Send the event that triggered this run of the service for store it if sending fails
                dispatched = dispatch(event);
            } catch (MalformedURLException e) {
                logger.error("Received a malformed URL in event handler service", e);
            } catch (Exception e) {
                logger.warn("Failed to dispatch event.", e);
            }
        }


        try {
            if (!dispatched) {
                long interval = getInterval(intent);
                serviceScheduler.schedule(intent, interval);
                saveInterval(interval);
                logger.info("Scheduled events to be dispatched");
            } else {
                // Quit trying to dispatch events because their aren't any in storage
                serviceScheduler.unschedule(intent);
                logger.info("Unscheduled event dispatch");
            }
        } catch (Exception e) {
            logger.warn("Failed to schedule event dispatch.", e);
        }
        finally {
            eventDAO.closeDb();
        }
    }

    // Either grab the interval for the first time from Intent or from storage
    // The extra won't exist if we are being restarted after a reboot or app update
    @Deprecated
    private long getInterval(@NonNull Intent intent) {
        long duration = intent.getLongExtra(EventIntentService.EXTRA_INTERVAL, -1);
        // We are either scheduling for the first time or rescheduling after our alarms were cancelled
        return duration;
    }

    @Deprecated
    private void saveInterval(long interval) {
        optlyStorage.saveLong(EventIntentService.EXTRA_INTERVAL, interval);
    }

    /**
     * Dispatch all events in storage
     *
     * @return true if all events were dispatched, otherwise false
     */
    protected boolean dispatch() {
        List<Pair<Long, Event>> events = eventDAO.getEvents();
        Iterator<Pair<Long,Event>> iterator = events.iterator();
        while (iterator.hasNext()) {
            Pair<Long, Event> event = iterator.next();
            boolean eventWasSent = eventClient.sendEvent(event.second);
            if (eventWasSent) {
                iterator.remove();
                boolean eventWasDeleted = eventDAO.removeEvent(event.first);
                if (!eventWasDeleted) {
                    logger.warn("Unable to delete an event from local storage that was sent to successfully");
                }
            }
        }

        return events.isEmpty();
    }

    /**
     * Send a single event
     *
     * @param event an {@link Event} instance to attempt to dispatch
     * @return true if event was sent to network, otherwise false if stored
     */
    private boolean dispatch(Event event) {
        boolean eventWasSent = eventClient.sendEvent(event);

        if (eventWasSent) {
            CountingIdlingResourceManager.decrement();
            CountingIdlingResourceManager.recordEvent(new Pair<>(event.getURL().toString(), event.getRequestBody()));
            return true;
        } else {
            boolean eventWasStored = eventDAO.storeEvent(event);
            if (!eventWasStored) {
                logger.error("Unable to send or store event {}", event);
                // Return true since nothing was stored
                return true;
            } else {
                return false;
            }
        }
    }
}
