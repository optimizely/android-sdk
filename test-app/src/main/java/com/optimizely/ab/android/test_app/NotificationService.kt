/****************************************************************************
 * Copyright 2016-2017, Optimizely, Inc. and contributors                   *
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
package com.optimizely.ab.android.test_app

import android.app.IntentService
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.optimizely.ab.android.shared.CountingIdlingResourceManager

class NotificationService : IntentService("NotificationService") {
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            showNotification()

            // Get Optimizely from the Intent that started this Service
            val myApplication = application as MyApplication
            val optimizelyManager = myApplication.optimizelyManager
            val optimizely = optimizelyManager!!.optimizely
            CountingIdlingResourceManager.increment()
            optimizely.track("experiment_2", myApplication.anonUserId)
        }
    }

    private fun showNotification() {
        val mBuilder = NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.ic_notification_clear_all)
                .setContentTitle(getString(R.string.notification_service_notification_title))
                .setContentText(getString(R.string.notification_service_notification_text))
        // @TODO(mng): Add notification back when we add more complex scenario
        // Creates an explicit intent for an Activity in your app
//        Intent resultIntent = new Intent(this, MainActivity.class);
//
//        // The stack builder object will contain an artificial back stack for the
//        // started Activity.
//        // This ensures that navigating backward from the Activity leads out of
//        // your application to the Home screen.
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//        // Adds the back stack for the Intent (but not the Intent itself)
//        stackBuilder.addParentStack(SecondaryActivity.class);
//        // Adds the Intent that starts the Activity to the top of the stack
//        stackBuilder.addNextIntent(resultIntent);
//        PendingIntent resultPendingIntent =
//                stackBuilder.getPendingIntent(
//                        0,
//                        PendingIntent.FLAG_UPDATE_CURRENT
//                );
//        mBuilder.setContentIntent(resultPendingIntent);
//        NotificationManager mNotificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        // mId allows you to update the notification later on.
//        mNotificationManager.notify(0, mBuilder.build());
    }
}