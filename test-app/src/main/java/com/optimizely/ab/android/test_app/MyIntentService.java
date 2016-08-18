package com.optimizely.ab.android.test_app;

import android.app.IntentService;
import android.content.Intent;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.android.sdk.OptimizelySDK;
import com.optimizely.ab.android.sdk.ParcelableOptimizely;

public class MyIntentService extends IntentService {
    public MyIntentService() {
        super("MyIntentService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            // Get Optimizely from the Intent that started this Service
            final OptimizelySDK optimizelySDK = ((MyApplication) getApplication()).getOptimizelySDK();
            ParcelableOptimizely parcelableOptimizely = intent.getParcelableExtra("optly");
            Optimizely optimizely = optimizelySDK.unParcelOptimizely(parcelableOptimizely);

            optimizely.track("goal_3", "user1");
        }
    }
}
