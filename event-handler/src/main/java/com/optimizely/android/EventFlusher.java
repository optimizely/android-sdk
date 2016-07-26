package com.optimizely.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Pair;

import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by jdeffibaugh on 7/25/16 for Optimizely.
 *
 * Flushes {@link Event} intents sent to {@link EventIntentService}
 *
 * This abstraction makes unit testing much simpler
 */
public class EventFlusher {

    @NonNull private final Context context;
    @NonNull private EventDAO eventDAO;
    @NonNull private final EventClient eventClient;
    @NonNull private final Logger logger;


    public EventFlusher(@NonNull Context context, @NonNull EventDAO eventDAO, @NonNull EventClient eventClient, @NonNull Logger logger) {
        this.context = context;
        this.eventDAO = eventDAO;
        this.eventClient = eventClient;
        this.logger = logger;
    }

    public void process(@NonNull Intent intent) {
        // Flush events still in storage
        boolean eventsWereStored = flushEvents();

        if (intent.hasExtra(EventIntentService.EXTRA_URL)) {
            try {
                String urlExtra = intent.getStringExtra(EventIntentService.EXTRA_URL);
                Event event = new Event(new URL(urlExtra));
                // Send the event that triggered this run of the service for store it if sending fails
                eventsWereStored = flushEvent(event);
            } catch (MalformedURLException e) {
                logger.error("Received a malformed URL in event handler service", e);
            }
        }

        if (eventsWereStored) {
            schedule();
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
        while (events.iterator().hasNext()) {
            Pair<Long, Event> event = events.iterator().next();
            boolean eventWasSent = eventClient.sendEvent(event.second);
            if (eventWasSent) {
                boolean eventWasDeleted = eventDAO.removeEvent(event.first);
                if (eventWasDeleted) {
                    events.iterator().remove();
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

    private void schedule() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, 1, new Intent(context, EventIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        // Use inexact repeating so that the load on the server is more distributed
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_HOUR, pendingIntent);
    }
}
