package com.optimizely.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.slf4j.Logger;

/**
 * Created by jdeffibaugh on 7/26/16 for Optimizely.
 * <p/>
 * Schedules {@link android.app.Service}es to run.
 */
public class ServiceScheduler {

    @NonNull private final AlarmManager alarmManager;
    @NonNull private final PendingIntentFactory pendingIntentFactory;
    @NonNull private final Logger logger;

    ServiceScheduler(@NonNull AlarmManager alarmManager, @NonNull PendingIntentFactory pendingIntentFactory, @NonNull Logger logger) {
        this.alarmManager = alarmManager;
        this.pendingIntentFactory = pendingIntentFactory;
        this.logger = logger;
    }

    public void schedule(Class clazz, long interval) {
        if (!Service.class.isAssignableFrom(clazz)) {
            logger.error("Tried to schedule {} which is not a Service", clazz);
            return;
        }

        if (interval < 1) {
            logger.error("Tried to schedule an interval less than 1");
            return;
        }

        if (!pendingIntentFactory.hasPendingIntent(clazz)) {

            PendingIntent pendingIntent = pendingIntentFactory.getPendingIntent(clazz);
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, interval, interval, pendingIntent);

            logger.info("Scheduled {}", clazz.getSimpleName());
        } else {
            logger.debug("Not scheduling {}. It's already scheduled", clazz.getSimpleName());
        }
    }

    public void unschedule(Class clazz) {
        if (!Service.class.isAssignableFrom(clazz)) {
            logger.error("Tried to unschedule {} which is not a Service", clazz);
            return;
        }

        PendingIntent pendingIntent = pendingIntentFactory.getPendingIntent(clazz);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        logger.info("Unscheduled {}", clazz.getSimpleName());
    }

    /**
     * Handles the complexities around PendingIntent flags
     *
     * We need to know if the PendingIntent has already been created to prevent pushing
     * the alarm back after the last event.
     *
     * Putting this in it's class makes mocking much easier when testing out {@link ServiceScheduler#schedule(Class, long)}
     */
    public static class PendingIntentFactory {

        private Context context;

        public PendingIntentFactory(Context context) {
            this.context = context;
        }

        public boolean hasPendingIntent(Class clazz) {
            // FLAG_NO_CREATE will cause null to be returned if this Intent hasn't been created yet.
            // It does matter if you send a new instance or not the equality check is done via
            // the data, action, and component of an Intent.  Ours will always match.
            return getPendingIntent(clazz, PendingIntent.FLAG_NO_CREATE) != null;
        }

        public PendingIntent getPendingIntent(Class clazz) {
            return getPendingIntent(clazz, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        private PendingIntent getPendingIntent(Class clazz, int flag) {
            return PendingIntent.getService(context, 0, new Intent(context, clazz), flag);
        }
    }
}
