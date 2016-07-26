package com.optimizely.android;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


public class EventHandlerService extends IntentService {
    private final Logger logger = LoggerFactory.getLogger(EventHandlerService.class);

    static final String EXTRA_STRING_URL = "com.optimizely.android.EXTRA_STRING_URL";

    @NonNull EventClient eventClient;

    public EventHandlerService() {
        super("EventHandlerService");
        eventClient = new EventClient(LoggerFactory.getLogger(EventClient.class));
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;

        EventDAO eventDAO = EventDAO.getInstance(this, LoggerFactory.getLogger(EventDAO.class));
        // Flush events still in storage
        boolean eventsWereStored = flushEvents(eventDAO);

        if (intent.hasExtra(EXTRA_STRING_URL)) {
            try {
                String eventExtra = intent.getStringExtra(EXTRA_STRING_URL);
                Event event = new Event(new URL(eventExtra));
                // Send the event that triggered this run of the service for store it if sending fails
                eventsWereStored = flushEvent(eventDAO, event);
            } catch (MalformedURLException e) {
                logger.error("Received a malformed event in event handler service", e);
            }
        }

        if (eventsWereStored) {
            schedule();
        }
    }

    /**
     * Flush all events in storage
     *
     * @param eventDAO used to access the DB
     * @return true if all events were flushed, otherwise false
     */
    boolean flushEvents(EventDAO eventDAO) {
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
     * @param eventDAO used to access the DB
     * @param event      the event to send
     * @return true if event was flushed, otherwise false
     */
    boolean flushEvent(EventDAO eventDAO, Event event) {
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

    void schedule() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(this, 1, new Intent(this, EventHandlerService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        // Use inexact repeating so that the load on the server is more distributed
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_HOUR, pendingIntent);
    }
}
