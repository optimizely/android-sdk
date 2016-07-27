package com.optimizely.android;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventIntentService extends IntentService {
    Logger logger = LoggerFactory.getLogger(EventIntentService.class);

    static final String EXTRA_URL = "com.optimizely.android.EXTRA_URL";
    static final String EXTRA_DURATION = "com.optimizely.android.EXTRA_DURATION";

    @Nullable EventFlusher eventFlusher;

    public EventIntentService() {
        super("EventHandlerService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        EventClient eventClient = new EventClient(LoggerFactory.getLogger(EventClient.class));
        EventDAO eventDAO = EventDAO.getInstance(this, LoggerFactory.getLogger(EventDAO.class));
        OptlyStorage optlyStorage = new OptlyStorage(this);
        EventScheduler eventScheduler = new EventScheduler(this, optlyStorage);
        eventFlusher = new EventFlusher(eventDAO, eventClient, eventScheduler, LoggerFactory.getLogger(EventFlusher.class));
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            logger.warn("Handled a null intent");
            return;
        }

        if (eventFlusher != null) {
            eventFlusher.flush(intent);
            logger.info("Handled intent");
        } else {
            logger.warn("Unable to create dependencies needed by intent handler");
        }
    }
}
