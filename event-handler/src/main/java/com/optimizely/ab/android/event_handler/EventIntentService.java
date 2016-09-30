/**
 * Copyright 2016, Optimizely
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.android.event_handler;

import android.app.AlarmManager;
import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.optimizely.ab.android.shared.OptlyStorage;
import com.optimizely.ab.android.shared.ServiceScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventIntentService extends IntentService {
    static final String EXTRA_URL = "com.optimizely.ab.android.EXTRA_URL";
    static final String EXTRA_REQUEST_BODY = "com.optimizely.ab.andrdoid.EXTRA_REQUEST_BODY";
    static final String EXTRA_INTERVAL = "com.optimizely.ab.android.EXTRA_INTERVAL";
    Logger logger = LoggerFactory.getLogger(EventIntentService.class);
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
        eventDispatcher = new EventDispatcher(this, optlyStorage, eventDAO, eventClient, serviceScheduler, LoggerFactory.getLogger(EventDispatcher.class));
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
