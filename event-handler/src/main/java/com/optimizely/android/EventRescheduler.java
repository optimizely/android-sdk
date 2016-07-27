package com.optimizely.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reschedules event flushing after package updates and reboots
 *
 * After the app is updated or the phone is rebooted the event flushing
 * jobs scheduled by {@link ServiceScheduler} are cancelled.
 *
 * This code is called by the Android Framework.  The Intent Filters are registered
 * AndroidManifest.xml.
 */
public class EventRescheduler extends BroadcastReceiver {

    Logger logger = LoggerFactory.getLogger(EventRescheduler.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        if ((context != null && intent != null) && (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED))) {
            intent = new Intent(context, EventIntentService.class);
            context.startService(intent);
            logger.info("Rescheduling event flushing if necessary");
        } else {
            logger.warn("Received invalid broadcast to event rescheduler");
        }
    }
}
