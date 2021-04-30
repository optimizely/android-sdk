/****************************************************************************
 * Copyright 2016-2021, Optimizely, Inc. and contributors                        *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                            *
 * *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.android.event_handler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import androidx.work.Data
import com.optimizely.ab.android.shared.ServiceScheduler
import com.optimizely.ab.android.shared.ServiceScheduler.PendingIntentFactory
import com.optimizely.ab.android.shared.WorkerScheduler
import org.slf4j.LoggerFactory

/**
 * Reschedules event flushing after package updates and reboots
 *
 *
 * After the app is updated or the phone is rebooted the event flushing
 * jobs scheduled by [ServiceScheduler] are cancelled.
 *
 *
 * This code is called by the Android Framework.  The Intent Filters are registered
 * AndroidManifest.xml.
 * <pre>
 * `<receiver android:name="com.optimizely.ab.android.event_handler.EventRescheduler" android:enabled="true" android:exported="false">
 * <intent-filter>
 * <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
 * <action android:name="android.intent.action.BOOT_COMPLETED" />
 * <action android:name="android.net.wifi.supplicant.CONNECTION_CHANGE" />
 * </intent-filter>
 * </receiver>
` *
</pre> *
 */
class EventRescheduler : BroadcastReceiver() {
    @JvmField
    var logger = LoggerFactory.getLogger(EventRescheduler::class.java)

    /**
     * Called when intent filter has kicked in.
     * @param context current context
     * @param intent broadcast intent received.  Try and reschedule.
     * @see BroadcastReceiver.onReceive
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            val serviceScheduler = ServiceScheduler(
                    context,
                    PendingIntentFactory(context),
                    LoggerFactory.getLogger(ServiceScheduler::class.java))
            val eventServiceIntent = Intent(context, EventIntentService::class.java)
            reschedule(context, intent, eventServiceIntent, serviceScheduler)
        } else {
            logger.warn("Received invalid broadcast to event rescheduler")
        }
    }

    /**
     * Actually reschedule the service
     * @param context current context
     * @param broadcastIntent broadcast intent (reboot, wifi change, reinstall)
     * @param eventServiceIntent event service intent
     * @param serviceScheduler scheduler for rescheduling.
     */
    fun reschedule(context: Context, broadcastIntent: Intent, eventServiceIntent: Intent, serviceScheduler: ServiceScheduler) {
        if (broadcastIntent.action == Intent.ACTION_BOOT_COMPLETED || broadcastIntent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            WorkerScheduler.startService(context, EventWorker.workerId, EventWorker::class.java, Data.EMPTY)
            logger.info("Rescheduling event flushing if necessary")
        } else if (broadcastIntent.action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
            val info = broadcastIntent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
            if (info != null && info.isConnected) {
                WorkerScheduler.startService(context, EventWorker.workerId, EventWorker::class.java, Data.EMPTY)
                logger.info("Preemptively flushing events since wifi became available")
            }
        } else {
            logger.warn("Received unsupported broadcast action to event rescheduler")
        }
    }
}