package com.optimizely.ab.android.test_app;

import android.app.Application;

import com.optimizely.ab.android.sdk.OptimizelyManager;

import java.util.concurrent.TimeUnit;

/**
 * Created by jdeffibaugh on 8/18/16 for Optimizely.
 */
public class MyApplication extends Application {

    private OptimizelyManager optimizelyManager;

    public OptimizelyManager getOptimizelyManager() {
        return optimizelyManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        optimizelyManager = OptimizelyManager.builder("6242822113")
                .withEventHandlerDispatchInterval(30, TimeUnit.SECONDS)
                .withDataFileDownloadInterval(30, TimeUnit.SECONDS)
                .build();
    }
}
