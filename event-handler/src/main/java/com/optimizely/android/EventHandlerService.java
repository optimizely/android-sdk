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

    static final String EXTRA_URL = "com.optimizely.android.EXTRA_URL";

    @NonNull EventClient eventClient;

    public EventHandlerService() {
        super("EventHandlerService");
        eventClient = new EventClient(LoggerFactory.getLogger(EventClient.class));
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;

        EventDAO eventDAO = EventDAO.getInstance(this);
        // Flush events still in storage
        boolean eventsWereStored = flushEvents(eventDAO);

        if (intent.hasExtra(EXTRA_URL)) {
            try {
                String urlExtra = intent.getStringExtra(EXTRA_URL);
                URL url = new URL(urlExtra);
                // Send the event that triggered this run of the service for store it if sending fails
                eventsWereStored = flushEvent(eventDAO, url);
            } catch (MalformedURLException e) {
                logger.error("Received a malformed URL in event handler service", e);
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
        List<Pair<Long, URL>> events = eventDAO.getEvents();
        while (events.iterator().hasNext()) {
            Pair<Long, URL> event = events.iterator().next();
            boolean eventWasSent = eventClient.sendEvent(new URLProxy(event.second));
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
     * @param url      the URL of the request for sending the event
     * @return true if event was flushed, otherwise false
     */
    boolean flushEvent(EventDAO eventDAO, URL url) {
        boolean eventWasSent = eventClient.sendEvent(new URLProxy(url));

        if (eventWasSent) {
            return true;
        } else {
            boolean eventWasStored = eventDAO.storeEvent(url);
            if (!eventWasStored) {
                logger.error("Unable to send or store event {}", url);
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
