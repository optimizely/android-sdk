package com.optimizely.ab.android.test_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.android.sdk.OptimizelySDK;
import com.optimizely.ab.android.sdk.ParcelableOptimizely;

public class SecondaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        // Get Optimizely from the Intent that started this Activity
        final OptimizelySDK optimizelySDK = ((MyApplication) getApplication()).getOptimizelySDK();
        Intent intent = getIntent();
        ParcelableOptimizely parcelableOptimizely = intent.getParcelableExtra("optly");
        Optimizely optimizely = optimizelySDK.unParcelOptimizely(parcelableOptimizely);

        // track conversion event
        optimizely.track("goal_1", "user_1");
    }

}
