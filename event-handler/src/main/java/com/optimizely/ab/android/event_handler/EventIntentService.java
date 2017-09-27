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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.OptlyStorage;
import com.optimizely.ab.android.shared.ServiceScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Android {@link IntentService} that handles dispatching events to the Optimizely results servers.
 * <p>
 * Can be scheduled to run on interval.
 * <p>
 * Intents sent to this service are handled in order and on a background thread.  Think of it as a
 * worker queue.
 *
 */
public class EventIntentService extends IntentService {
    static final String EXTRA_URL = "com.optimizely.ab.android.EXTRA_URL";
    static final String EXTRA_REQUEST_BODY = "com.optimizely.ab.android.EXTRA_REQUEST_BODY";
    static final String EXTRA_INTERVAL = "com.optimizely.ab.android.EXTRA_INTERVAL";
    public static final Integer JOB_ID = 2112;

    Logger logger = LoggerFactory.getLogger(EventIntentService.class);
    @Nullable EventDispatcher eventDispatcher;

    public EventIntentService() {
        super("EventHandlerService");
    }

    /**
     * Create the event dispatcher {@link EventDispatcher}
     * @see IntentService#onCreate()
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onCreate() {
        super.onCreate();

        OptlyStorage optlyStorage = new OptlyStorage(this);
        EventClient eventClient = new EventClient(new Client(optlyStorage,
                LoggerFactory.getLogger(Client.class)), LoggerFactory.getLogger(EventClient.class));
        EventDAO eventDAO = EventDAO.getInstance(this, "1", LoggerFactory.getLogger(EventDAO.class));
        ServiceScheduler serviceScheduler = new ServiceScheduler(
                this,
                new ServiceScheduler.PendingIntentFactory(this),
                LoggerFactory.getLogger(ServiceScheduler.class));
        eventDispatcher = new EventDispatcher(this, optlyStorage, eventDAO, eventClient, serviceScheduler, LoggerFactory.getLogger(EventDispatcher.class));
    }

    /**
     * Dispatch event in intent.  This will also try to empty the event queue.
     * @see IntentService#onHandleIntent(Intent)
     */

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent == null) {
            logger.warn("Handled a null intent");
            return;
        }

        if (eventDispatcher != null) {
            logger.info("Handled intent");
            eventDispatcher.dispatch(intent);
        } else {
            logger.warn("Unable to create dependencies needed by intent handler");
        }

    }
}
