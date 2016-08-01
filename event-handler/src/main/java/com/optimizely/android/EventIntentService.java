package com.optimizely.android;

import android.app.AlarmManager;
import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventIntentService extends IntentService {
    Logger logger = LoggerFactory.getLogger(EventIntentService.class);

    static final String EXTRA_URL = "com.optimizely.android.EXTRA_URL";
    static final String EXTRA_INTERVAL = "com.optimizely.android.EXTRA_INTERVAL";

    @Nullable EventDispatcher eventDispatcher;

    public EventIntentService() {
        super("EventHandlerService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        EventClient eventClient = new EventClient(LoggerFactory.getLogger(EventClient.class));
        EventDAO eventDAO = EventDAO.getInstance(this, LoggerFactory.getLogger(EventDAO.class));
        ServiceScheduler serviceScheduler = new ServiceScheduler(
                (AlarmManager) getSystemService(ALARM_SERVICE),
                new ServiceScheduler.PendingIntentFactory(this),
                LoggerFactory.getLogger(ServiceScheduler.class));
        OptlyStorage optlyStorage = new OptlyStorage(this);
        eventDispatcher = new EventDispatcher(optlyStorage, eventDAO, eventClient, serviceScheduler, LoggerFactory.getLogger(EventDispatcher.class));
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            logger.warn("Handled a null intent");
            return;
        }

        if (eventDispatcher != null) {
            eventDispatcher.dispatch(intent);
            logger.info("Handled intent");
        } else {
            logger.warn("Unable to create dependencies needed by intent handler");
        }
    }
}
