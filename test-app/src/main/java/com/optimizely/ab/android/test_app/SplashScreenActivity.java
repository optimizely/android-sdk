/****************************************************************************
 * Copyright 2017-2018, Optimizely, Inc. and contributors                        *
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
package com.optimizely.ab.android.test_app;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.optimizely.ab.android.event_handler.EventRescheduler;
import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.sdk.OptimizelyStartListener;
import com.optimizely.ab.android.shared.CountingIdlingResourceManager;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.notification.ActivateNotificationListener;
import com.optimizely.ab.notification.NotificationHandler;
import com.optimizely.ab.notification.TrackNotificationListenerInterface;
import com.optimizely.ab.notification.UpdateConfigNotification;

import java.util.Map;

public class SplashScreenActivity extends AppCompatActivity {

    // The Idling Resource which will be null in production.
    @Nullable
    private static CountingIdlingResourceManager countingIdlingResourceManager;

    private OptimizelyManager optimizelyManager;
    private MyApplication myApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // This could also be done via DI framework such as Dagger
        myApplication = (MyApplication) getApplication();
        optimizelyManager = myApplication.getOptimizelyManager();
    }

    @Override
    protected void onStart() {
        super.onStart();

        boolean INITIALIZE_ASYNCHRONOUSLY = false;

        // with the new Android O differences, you need to register the service for the intent filter you desire in code instead of
        // in the manifest.
        EventRescheduler eventRescheduler = new EventRescheduler();

        getApplicationContext().registerReceiver(eventRescheduler, new IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION));

        /** Example of using Cache datafile to initialize optimizely client, if file is not present
         in Cache it will be initialized from Raw.datafile.
         **/
        if (!INITIALIZE_ASYNCHRONOUSLY) {
               optimizelyManager.initialize(myApplication, R.raw.datafile);
               optimizelyManager.getOptimizely().getNotificationCenter().addActivateNotificationListener((Experiment experiment, String s,  Map<String, ?> map,  Variation variation,  LogEvent logEvent) -> {
                   System.out.println("got activation");
               });
               optimizelyManager.getOptimizely().getNotificationCenter().addTrackNotificationListener((String s, String s1, Map<String, ?> map, Map<String, ?> map1, LogEvent logEvent) -> {

                   System.out.println("got track");
               });
               optimizelyManager.getOptimizely().getNotificationCenter().addNotificationHandler(UpdateConfigNotification.class, (UpdateConfigNotification notification) -> {
                        System.out.println("got datafile change");
               });

               startVariation();
        } else {
            // Initialize Optimizely asynchronously
            optimizelyManager.initialize(this,R.raw.datafile, (OptimizelyClient optimizely) -> {
                startVariation();
            });
        }

    }

    /**
     * This method will start the user activity according to provided variation
     */
    private void startVariation(){
        // this is the control variation, it will show if we are not able to determine which variation to bucket the user into
        Intent intent = new Intent(myApplication.getBaseContext(), ActivationErrorActivity.class);

        // Activate user and start activity based on the variation we get.
        // You can pass in any string for the user ID. In this example we just use a convenience method to generate a random one.
        String userId = myApplication.getAnonUserId();
        Variation backgroundVariation = optimizelyManager.getOptimizely().activate("background_experiment", userId);

        // Utility method for verifying event dispatches in our automated tests
        CountingIdlingResourceManager.increment(); // increment for impression event

        // variation is nullable so we should check for null values
        if (backgroundVariation != null) {
            // Show activity based on the variation the user got bucketed into
            if (backgroundVariation.getKey().equals("variation_a")) {
                intent = new Intent(myApplication.getBaseContext(), VariationAActivity.class);
            } else if (backgroundVariation.getKey().equals("variation_b")) {
                intent = new Intent(myApplication.getBaseContext(), VariationBActivity.class);
            }
        }
        startActivity(intent);

        //call this method if you set an interval but want to now stop doing bakcground updates.
        //optimizelyManager.getDatafileHandler().stopBackgroundUpdates(myApplication.getApplicationContext(), optimizelyManager.getKey());

    }
}
