/****************************************************************************
 * Copyright 2016-2021, Optimizely, Inc. and contributors                        *
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.work.Data;

import com.optimizely.ab.android.shared.WorkerScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reschedules event flushing after package updates and reboots
 * <p>
 * After the app is updated or the phone is rebooted the event flushing
 * jobs scheduled by {@link WorkerScheduler} are cancelled.
 * <p>
 * This code is called by the Android Framework.  The Intent Filters are registered
 * AndroidManifest.xml.
 * <pre>
 * {@code
 * <receiver android:name="com.optimizely.ab.android.event_handler.EventRescheduler" android:enabled="true" android:exported="false">
 *  <intent-filter>
 *      <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
 *      <action android:name="android.intent.action.BOOT_COMPLETED" />
 *      <action android:name="android.net.wifi.supplicant.CONNECTION_CHANGE" />
 *  </intent-filter>
 * </receiver>
 * }
 * </pre>
 */
public class EventRescheduler extends BroadcastReceiver {

    Logger logger = LoggerFactory.getLogger(EventRescheduler.class);

    /**
     * Called when intent filter has kicked in.
     * @param context current context
     * @param intent broadcast intent received.  Try and reschedule.
     * @see BroadcastReceiver#onReceive(Context, Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            logger.warn("Received invalid broadcast to event rescheduler");
            return;
        }

        try {
            reschedule(context, intent);
        } catch (Exception e) {
            // Rare exceptions (IllegalStateException: "WorkManager is not initialized properly...") with WorkerScheduler.startService(), probably related to a WorkManager start timing issue.
            // Gracefully handled here, and it's safe for those rare cases since event-dispatch service will be scheduled again on next events.
            logger.warn("WorkScheduler failed to reschedule an event service: " + e.getMessage());
        }
    }

    /**
     * Actually reschedule the service
     * @param context current context
     * @param broadcastIntent broadcast intent (reboot, wifi change, reinstall)
     */
    void reschedule(@NonNull Context context, @NonNull Intent broadcastIntent) {
        if (broadcastIntent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                broadcastIntent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
            WorkerScheduler.startService(context, EventWorker.workerId, EventWorker.class, Data.EMPTY, -1L);
            logger.info("Rescheduling event flushing if necessary");
        } else if (broadcastIntent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            NetworkInfo info = broadcastIntent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if(info != null && info.isConnected()) {
                WorkerScheduler.startService(context, EventWorker.workerId, EventWorker.class, Data.EMPTY, -1L);
                logger.info("Preemptively flushing events since wifi became available");
            }
        } else {
            logger.warn("Received unsupported broadcast action to event rescheduler");
        }
    }

}
