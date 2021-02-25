/****************************************************************************
 * Copyright 2017-2020, Optimizely, Inc. and contributors                        *
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

import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.optimizely.ab.android.datafile_handler.DatafileRescheduler
import com.optimizely.ab.android.event_handler.EventRescheduler
import com.optimizely.ab.android.sdk.OptimizelyClient
import com.optimizely.ab.android.sdk.OptimizelyManager
import com.optimizely.ab.android.shared.CountingIdlingResourceManager
import com.optimizely.ab.notification.DecisionNotification
import com.optimizely.ab.notification.TrackNotification
import com.optimizely.ab.notification.UpdateConfigNotification

class SplashScreenActivity : AppCompatActivity() {
    private var optimizelyManager: OptimizelyManager? = null
    private var myApplication: MyApplication? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // This could also be done via DI framework such as Dagger
        myApplication = application as MyApplication
        optimizelyManager = myApplication!!.optimizelyManager
    }

    override fun onStart() {
        super.onStart()

        val INITIALIZE_ASYNCHRONOUSLY = true

        // with the new Android O differences, you need to register the service for the intent filter you desire in code instead of
        // in the manifest.
        val eventRescheduler = EventRescheduler()
        applicationContext.registerReceiver(eventRescheduler, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))

        if (INITIALIZE_ASYNCHRONOUSLY) {
            optimizelyManager!!.initialize(this, R.raw.datafile) { _ ->
                addNotificationListeners()
                startVariation()
            }
        } else {
            optimizelyManager!!.initialize(this, R.raw.datafile)
            addNotificationListeners()
            startVariation()
        }

    }

    private fun addNotificationListeners() {
        val optimizelyClient = optimizelyManager!!.optimizely

        // DECISION notification
        // - this callback is triggered with the decision type, associated decision information, user ID, and attributes.
        // - different APIs trigger different types of decisions: activate() -> "ab-test", isFeatureEnabled() -> "feature", ...
        optimizelyClient.addDecisionNotificationHandler { notification: DecisionNotification ->
            val type = notification.type
            val userId = notification.userId
            val attributes = notification.attributes
            val decisionInfo = notification.decisionInfo
            println("got decision notification: ($type)($userId)($attributes)($decisionInfo")
        }

        // Track notification
        // - this callback is triggered when a tracking event has been sent
        optimizelyClient.addTrackNotificationHandler { notification: TrackNotification ->
            val eventKey = notification.eventKey
            val userId = notification.userId
            val attributes = notification.attributes
            val eventTags = notification.eventTags
            println("got track notification: ($eventKey)($userId)($attributes)($eventTags")
        }

        // UpdateConfig notification
        // - this callback is triggered when a new datafile is downloaded and the SDK project configuration has been updated
        optimizelyClient.addUpdateConfigNotificationHandler { notification: UpdateConfigNotification? -> println("got datafile change notification:") }
    }

    /**
     * This method will start the user activity according to provided variation
     */
    private fun startVariation() {
        // this is the control variation, it will show if we are not able to determine which variation to bucket the user into
        var intent = Intent(this, ActivationErrorActivity::class.java)

        // Activate user and start activity based on the variation we get.
        // You can pass in any string for the user ID. In this example
        // we just use a convenience method to generate a random one.
        val userId = myApplication?.anonUserId ?: ""
        val attributes = myApplication?.attributes ?: HashMap<String, Any>()

        val v = optimizelyManager?.optimizely?.activate("background_experiment", userId, attributes)
        when (v?.key) {
            "variation_a" -> intent = Intent(this, VariationAActivity::class.java)
            "variation_b" -> intent = Intent(this, VariationBActivity::class.java)
        }

        when (optimizelyManager?.optimizely?.isFeatureEnabled("show_coupon", userId, attributes)) {
            true -> intent.putExtra("show_coupon", true)
        }

        optimizelyManager?.optimizely?.notificationCenter?.addNotificationHandler(UpdateConfigNotification::class.java) {
            myApplication?.currentActivity?.setShowCoupon(optimizelyManager?.optimizely?.isFeatureEnabled("show_coupon", userId, attributes))
        }

        startActivity(intent)

        //call this method if you set an interval but want to now stop doing bakcground updates.
        //optimizelyManager.getDatafileHandler().stopBackgroundUpdates(myApplication.getApplicationContext(), optimizelyManager.getKey());
    }

    companion object {
        // The Idling Resource which will be null in production.
        private val countingIdlingResourceManager: CountingIdlingResourceManager? = null
    }
}