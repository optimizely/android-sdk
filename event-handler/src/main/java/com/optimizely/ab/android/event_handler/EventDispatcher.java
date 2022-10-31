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
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.optimizely.ab.android.shared.CountingIdlingResourceManager;
import com.optimizely.ab.android.shared.OptlyStorage;

import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

/**
 * Dispatches {@link Event} instances.
 *
 * If sending events to the network fails they will be stored and dispatched again.
 *
 * This abstraction makes unit testing much simpler
 */
@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
public class EventDispatcher {

    @NonNull private final Context context;
    @NonNull private final EventDAO eventDAO;
    @NonNull private final EventClient eventClient;
    @NonNull private final Logger logger;
    @NonNull private final OptlyStorage optlyStorage;

    EventDispatcher(@NonNull Context context, @NonNull OptlyStorage optlyStorage, @NonNull EventDAO eventDAO, @NonNull EventClient eventClient, @NonNull Logger logger) {
        this.context = context;
        this.optlyStorage = optlyStorage;
        this.eventDAO = eventDAO;
        this.eventClient = eventClient;
        this.logger = logger;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public boolean dispatch(String url, String body) {
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
