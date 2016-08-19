package com.optimizely.ab.android.test_app;

import android.app.IntentService;
import android.content.Intent;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.android.sdk.OptimizelyManager;

public class MyIntentService extends IntentService {
    public MyIntentService() {
        super("MyIntentService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            // Get Optimizely from the Intent that started this Service
            final OptimizelyManager optimizelyManager = ((MyApplication) getApplication()).getOptimizelyManager();
            Optimizely optimizely = optimizelyManager.getOptimizely();
            if (optimizely != null) {
                optimizely.track("goal_3", "user_1");
            }
        }
    }
}
