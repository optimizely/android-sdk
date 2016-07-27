package com.optimizely.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

/**
 * Created by jdeffibaugh on 7/26/16 for Optimizely.
 *
 * Schedules {@link EventIntentService} to run.
 */
public class EventScheduler {

    @NonNull private final Context context;
    @NonNull private final OptlyStorage optlyStorage;

    EventScheduler(@NonNull Context context, @NonNull OptlyStorage optlyStorage) {
        this.context = context;
        this.optlyStorage = optlyStorage;
    }

    public void schedule(Intent intent) {
        long duration = intent.getLongExtra(EventIntentService.EXTRA_DURATION, -1);
        // We are either scheduling for the first time or rescheduling after our alarms were cancelled
        if (duration == -1) {
            duration = optlyStorage.getLong(OptlyStorage.KEY_FLUSH_EVENTS_INTERVAl, AlarmManager.INTERVAL_HOUR);
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, 1, new Intent(context, EventIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        // Use inexact repeating so that the load on the server is more distributed
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, duration, duration, pendingIntent);

        // We might need to restart the flusher from a broadcast receiver that is triggered
        // after device reboots or the package is updated. We won't know the duration
        // from there so it must be persisted.
        optlyStorage.saveLong(OptlyStorage.KEY_FLUSH_EVENTS_INTERVAl, duration);
    }
}
