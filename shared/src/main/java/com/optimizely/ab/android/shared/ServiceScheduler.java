/*
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
package com.optimizely.ab.android.shared;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.slf4j.Logger;

/**
 * Schedules {@link android.app.Service}es to run.
 */
// TODO Unit test coverage
public class ServiceScheduler {

    @NonNull private final AlarmManager alarmManager;
    @NonNull private final PendingIntentFactory pendingIntentFactory;
    @NonNull private final Logger logger;

    public ServiceScheduler(@NonNull AlarmManager alarmManager, @NonNull PendingIntentFactory pendingIntentFactory, @NonNull Logger logger) {
        this.alarmManager = alarmManager;
        this.pendingIntentFactory = pendingIntentFactory;
        this.logger = logger;
    }

    public void schedule(Intent intent, long interval) {
        if (interval < 1) {
            logger.error("Tried to schedule an interval less than 1");
            return;
        }

        if (!pendingIntentFactory.hasPendingIntent(intent)) {

            PendingIntent pendingIntent = pendingIntentFactory.getPendingIntent(intent);
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, interval, interval, pendingIntent);

            logger.info("Scheduled {}", intent.getComponent().toShortString());
        } else {
            logger.debug("Not scheduling {}. It's already scheduled", intent.getComponent().toShortString());
        }
    }

    public void unschedule(Intent intent) {
        PendingIntent pendingIntent = pendingIntentFactory.getPendingIntent(intent);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        logger.info("Unscheduled {}", intent.getComponent().toShortString());
    }

    /**
     * Handles the complexities around PendingIntent flags
     *
     * We need to know if the PendingIntent has already been created to prevent pushing
     * the alarm back after the last event.
     *
     * Putting this in it's class makes mocking much easier when testing out {@link ServiceScheduler#schedule(Intent, long)}
     */
    public static class PendingIntentFactory {

        private Context context;

        public PendingIntentFactory(Context context) {
            this.context = context;
        }

        public boolean hasPendingIntent(Intent intent) {
            // FLAG_NO_CREATE will cause null to be returned if this Intent hasn't been created yet.
            // It does matter if you send a new instance or not the equality check is done via
            // the data, action, and component of an Intent.  Ours will always match.
            return getPendingIntent(intent, PendingIntent.FLAG_NO_CREATE) != null;
        }

        public PendingIntent getPendingIntent(Intent intent) {
            return getPendingIntent(intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        private PendingIntent getPendingIntent(Intent intent, int flag) {
            return PendingIntent.getService(context, 0, intent, flag);
        }
    }
}
