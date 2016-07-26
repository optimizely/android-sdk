package com.optimizely.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Created by jdeffibaugh on 7/26/16 for Optimizely.
 *
 * Schedules {@link EventIntentService} to run.
 */
public class EventScheduler {

    private Context context;

    EventScheduler(Context context) {
        this.context = context;
    }

    public void schedule() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, 1, new Intent(context, EventIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        // Use inexact repeating so that the load on the server is more distributed
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_HOUR, pendingIntent);
    }
}
