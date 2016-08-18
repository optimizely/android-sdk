package com.optimizely.ab.android.test_app;

import android.app.Application;

import com.optimizely.ab.android.sdk.OptimizelySDK;

import java.util.concurrent.TimeUnit;

/**
 * Created by jdeffibaugh on 8/18/16 for Optimizely.
 */
public class MyApplication extends Application {

    private OptimizelySDK optimizelySDK;

    public OptimizelySDK getOptimizelySDK() {
        return optimizelySDK;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        optimizelySDK = OptimizelySDK.builder("6820012714")
                .withEventHandlerDispatchInterval(1, TimeUnit.DAYS)
                .withDataFileDownloadInterval(1, TimeUnit.DAYS)
                .build();
    }
}
