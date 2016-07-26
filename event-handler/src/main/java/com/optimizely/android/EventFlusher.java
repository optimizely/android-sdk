package com.optimizely.android;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Pair;

import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

/**
 * Created by jdeffibaugh on 7/25/16 for Optimizely.
 *
 * Flushes {@link Event} intents sent to {@link EventIntentService}
 *
 * This abstraction makes unit testing much simpler
 */
public class EventFlusher {

    @NonNull private final EventScheduler eventScheduler;
    @NonNull private final EventDAO eventDAO;
    @NonNull private final EventClient eventClient;
    @NonNull private final Logger logger;

    public EventFlusher(@NonNull EventDAO eventDAO, @NonNull EventClient eventClient, @NonNull EventScheduler eventScheduler, @NonNull Logger logger) {
        this.eventDAO = eventDAO;
        this.eventClient = eventClient;
        this.eventScheduler = eventScheduler;
        this.logger = logger;
    }

    public void flush(@NonNull Intent intent) {
        // Flush events still in storage
        boolean flushed = flushEvents();

        if (intent.hasExtra(EventIntentService.EXTRA_URL)) {
            try {
                String urlExtra = intent.getStringExtra(EventIntentService.EXTRA_URL);
                Event event = new Event(new URL(urlExtra));
                // Send the event that triggered this run of the service for store it if sending fails
                flushed = flushEvent(event);
            } catch (MalformedURLException e) {
                logger.error("Received a malformed URL in event handler service", e);
            }
        }

        if (!flushed) {
            eventScheduler.schedule();
            logger.info("Scheduled events to be flushed");
        }
    }

    /**
     * Flush all events in storage
     *
     * @return true if all events were flushed, otherwise false
     */
    private boolean flushEvents() {
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
     * Flush a single event
     *
     * @param event an {@link Event} instance to flush
     * @return true if event was flushed, otherwise false
     */
    private boolean flushEvent(Event event) {
        boolean eventWasSent = eventClient.sendEvent(event);

        if (eventWasSent) {
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
