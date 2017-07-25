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

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;

import com.optimizely.ab.android.shared.ServiceScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static android.content.Context.ALARM_SERVICE;

/**
 * Reschedules event flushing after package updates and reboots
 * <p>
 * After the app is updated or the phone is rebooted the event flushing
 * jobs scheduled by {@link ServiceScheduler} are cancelled.
 * <p>
 * This code is called by the Android Framework.  The Intent Filters are registered
 * AndroidManifest.xml.
 *
 * @hide
 */
public class EventRescheduler extends BroadcastReceiver {

    Logger logger = LoggerFactory.getLogger(EventRescheduler.class);

    /**
     * @hide
     * @see BroadcastReceiver#onReceive(Context, Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context != null && intent != null) {
            ServiceScheduler serviceScheduler = new ServiceScheduler(
                    (AlarmManager) context.getSystemService(ALARM_SERVICE),
                    new ServiceScheduler.PendingIntentFactory(context),
                    LoggerFactory.getLogger(ServiceScheduler.class));
            Intent eventServiceIntent = new Intent(context, EventIntentService.class);
            reschedule(context, intent, eventServiceIntent, serviceScheduler);
        } else {
            logger.warn("Received invalid broadcast to event rescheduler");
        }
    }

    void reschedule(@NonNull Context context, @NonNull Intent broadcastIntent, @NonNull Intent eventServiceIntent, @NonNull ServiceScheduler serviceScheduler) {
        if (broadcastIntent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                broadcastIntent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
            context.startService(eventServiceIntent);
            logger.info("Rescheduling event flushing if necessary");
        } else if (broadcastIntent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo networkInfo = broadcastIntent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo.getState() == NetworkInfo.State.CONNECTING) {

                if (serviceScheduler.isScheduled(eventServiceIntent)) {
                    // If we get wifi and the event flushing service is scheduled preemptively
                    // flush events before the next interval occurs.  If sending fails even
                    // with wifi the service will be rescheduled on the interval.
                    // Wifi connection state changes all the time and starting services is expensive
                    // so it's important to only do this if we have stored events.
                    context.startService(eventServiceIntent);
                    logger.info("Preemptively flushing events since wifi became available");
                }
            }
        } else {
            logger.warn("Received unsupported broadcast action to event rescheduler");
        }
    }
}
