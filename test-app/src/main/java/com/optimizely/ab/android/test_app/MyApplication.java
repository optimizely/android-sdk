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

        // If the project doesn't compile because you are missing R.string.optly_project_id you neeed
        // to put `git_ignored_strings.xml` in test-app/src/main/res/values.  This file is git ignored.
        // It contains values that are unique for each developer.
        optimizelyManager = OptimizelyManager.builder(getResources().getString(R.string.optly_project_id))
                .withEventHandlerDispatchInterval(30, TimeUnit.SECONDS)
                .withDataFileDownloadInterval(30, TimeUnit.SECONDS)
                .build();
    }
}
