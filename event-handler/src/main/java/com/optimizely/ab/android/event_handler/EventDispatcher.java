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

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Pair;

import com.google.gson.Gson;
import com.optimizely.ab.android.shared.CountingIdlingResourceManager;
import com.optimizely.ab.android.shared.OptlyStorage;
import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.ab.event.internal.payload.batch.Attribute;
import com.optimizely.ab.event.internal.payload.batch.Batch;
import com.optimizely.ab.event.internal.payload.batch.Decision;
import com.optimizely.ab.event.internal.payload.batch.Snapshot;
import com.optimizely.ab.event.internal.payload.batch.Visitor;

import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    @NonNull private final Gson gson;

    EventDispatcher(@NonNull Context context, @NonNull OptlyStorage optlyStorage,
                    @NonNull EventDAO eventDAO, @NonNull EventClient eventClient,
                    @NonNull ServiceScheduler serviceScheduler, @NonNull Gson gson, @NonNull Logger logger) {
        this.context = context;
        this.optlyStorage = optlyStorage;
        this.eventDAO = eventDAO;
        this.eventClient = eventClient;
        this.serviceScheduler = serviceScheduler;
        this.gson = gson;
        this.logger = logger;
    }

    void store(@NonNull Intent intent) {
        if (intent.hasExtra(EventIntentService.EXTRA_URL)) {
            try {
                String urlExtra = intent.getStringExtra(EventIntentService.EXTRA_URL);
                String requestBody = intent.getStringExtra(EventIntentService.EXTRA_REQUEST_BODY);
                Event event = new Event(new URL(urlExtra), requestBody);
                // Store this event for merging and sending in a batch later
                store(event);
            } catch (Exception e) {
                logger.warn("Failed to store event.", e);
            }
        }

        eventDAO.closeDb();
    }

    void schedule(@NonNull Intent intent) {
        try {
            long interval = getInterval(intent);
            serviceScheduler.schedule(intent, interval);
            saveInterval(interval);
            logger.info("Scheduled events to be dispatched");
        } catch (Exception e) {
            logger.warn("Failed to schedule event dispatch.", e);
        }
    }

    // Either grab the interval for the first time from Intent or from storage
    // The extra won't exist if we are being restarted after a reboot or app update
    private long getInterval(@NonNull Intent intent) {
        long duration = intent.getLongExtra(EventIntentService.EXTRA_INTERVAL, -1);
        // We are either scheduling for the first time or rescheduling after our alarms were cancelled
        if (duration == -1) {
            // Use an hour for duration by default
            duration = optlyStorage.getLong(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR);
        }
        return duration;
    }

    private void saveInterval(long interval) {
        optlyStorage.saveLong(EventIntentService.EXTRA_INTERVAL, interval);
    }

    /**
     * Flush all events in storage
     */
    void flush() {
        List<Pair<Long, Event>> events = eventDAO.getEvents();

        for (List<Event> batches: partitionEventsByProject(events).values()) {
            Event mergedBatch = mergeBatches(batches);
            // send the merged batch
            boolean wasSent = eventClient.sendEvent(mergedBatch);
            if (wasSent) {
                for (Pair<Long, Event> event : events) {
                    // Remove everything if batch was successful
                    boolean wasDeleted = eventDAO.removeEvent(event.first);
                    if (!wasDeleted) {
                        logger.warn("Unable to delete an event from local storage that was sent to successfully");
                    }
                }
            }
        }
    }

    private Map<String, List<Event>> partitionEventsByProject(List<Pair<Long, Event>> events) {
        Map<String, List<Event>> projectIdToBatches = new HashMap<>();
        for (Pair<Long, Event> event : events) {
            Batch b = gson.fromJson(event.second.getRequestBody(), Batch.class);
            List<Event> projectBatches = new ArrayList<>();
            if (projectIdToBatches.containsKey(b.getProjectId())) {
                projectBatches = projectIdToBatches.get(b.getProjectId());
            }
            projectBatches.add(new Event(event.second.getURL(), gson.toJson(b, Batch.class)));
            projectIdToBatches.put(b.getProjectId(), projectBatches);
        }

        return projectIdToBatches;
    }

    Event mergeBatches(List<Event> batchEvents) {
        // TODO THIS IS ALMOST CERTAINLY WRONG
        URL url = null;
        Batch mergedBatch = null;
        for (Event batchEvent : batchEvents) {
            Batch batch = gson.fromJson(batchEvent.getRequestBody(), Batch.class);
            if (mergedBatch == null) {
                url = batchEvent.getURL();
                mergedBatch = batch;
            } else {
                for (Visitor visitor : batch.getVisitors()) {
                    for (Snapshot snapshot : visitor.getSnapshots()) {
                        Visitor mergedVisitor = mergedBatch.getVisitors().get(0);
                        Set<Decision> decisionSet = new HashSet<>(mergedVisitor.getSnapshots().get(0).getDecisions());
                        decisionSet.addAll(snapshot.getDecisions());
                        Set<com.optimizely.ab.event.internal.payload.batch.Event> eventSet
                                    = new HashSet<>(mergedVisitor.getSnapshots().get(0).getEvents());
                        eventSet.addAll(snapshot.getEvents());
                        Snapshot mergedSnapshot = mergedVisitor.getSnapshots().get(0);
                        mergedSnapshot.setDecisions(new ArrayList<>(decisionSet));
                        mergedSnapshot.setEvents(new ArrayList<>(eventSet));
                    }
                }
            }
        }

        return new Event(url, gson.toJson(mergedBatch, Batch.class));
    }


    /**
     * Store a singular batch event
     *
     * @param event an {@link Event} instance to store
     * @return true if event was stored, otherwise false
     */
    private boolean store(Event event) {
        boolean eventWasStored = eventDAO.storeEvent(event);
        if (!eventWasStored) {
            logger.error("Unable to store event {}", event);
            return true;
        } else {
            return false;
        }
    }
}
