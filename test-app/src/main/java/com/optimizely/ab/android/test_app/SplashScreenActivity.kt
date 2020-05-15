/****************************************************************************
 * Copyright 2017-2018, Optimizely, Inc. and contributors                        *
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
import android.support.v7.app.AppCompatActivity
import com.optimizely.ab.android.event_handler.EventRescheduler
import com.optimizely.ab.android.sdk.OptimizelyClient
import com.optimizely.ab.android.sdk.OptimizelyManager
import com.optimizely.ab.android.shared.CountingIdlingResourceManager
import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.Variation
import com.optimizely.ab.event.LogEvent
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

        // with the new Android O differences, you need to register the service for the intent filter you desire in code instead of
        // in the manifest.
        val eventRescheduler = EventRescheduler()
        applicationContext.registerReceiver(eventRescheduler, IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
        myApplication?.let {
            optimizelyManager?.initialize(it, null) { _ ->
                startVariation()
            }

        }
    }

    /**
     * This method will start the user activity according to provided variation
     */
    private fun startVariation() {
        // this is the control variation, it will show if we are not able to determine which variation to bucket the user into
        var intent = Intent(myApplication!!.baseContext, ActivationErrorActivity::class.java)

        // Activate user and start activity based on the variation we get.
        // You can pass in any string for the user ID. In this example we just use a convenience method to generate a random one.
        val userId = myApplication?.anonUserId ?: ""
        var backgroundVariation = optimizelyManager?.optimizely?.activate("background_experiment", "userId")

        val message = optimizelyManager?.optimizely?.getFeatureVariableString("join_banner", "banner", "userId")
        val bold = optimizelyManager?.optimizely?.getFeatureVariableBoolean("join_banner", "isBold", "userId")

        val enabled = optimizelyManager?.optimizely?.isFeatureEnabled("show_coupon", userId, myApplication?.attributes ?: HashMap<String,Any>()  )
        // variation is nullable so we should check for null values
        when (backgroundVariation?.key) {
            "variation_a" -> intent = Intent(myApplication!!.baseContext, VariationAActivity::class.java)
            "variation_b" -> intent = Intent(myApplication!!.baseContext, VariationBActivity::class.java)
        }
        enabled?.let {
            if (it) {
                intent.putExtra("show_coupon", true)
                val message = optimizelyManager?.optimizely?.getFeatureVariableString("show_coupon", "message", userId)
                var color = optimizelyManager?.optimizely?.getFeatureVariableString("my_feature", "string_variable", "userId")
            }
        }

        optimizelyManager?.optimizely?.notificationCenter?.addNotificationHandler(UpdateConfigNotification::class.java) {
            println("got datafile change")
            val currentActivity = myApplication?.currentActivity
            currentActivity?.setShowCoupon(optimizelyManager!!.optimizely.isFeatureEnabled("show_coupon", userId, myApplication!!.attributes))
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