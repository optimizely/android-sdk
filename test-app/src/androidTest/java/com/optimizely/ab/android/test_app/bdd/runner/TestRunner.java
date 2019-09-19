package com.optimizely.ab.android.test_app.bdd.runner;

import android.os.Bundle;
import android.support.test.runner.MonitoringInstrumentation;


import com.optimizely.ab.android.test_app.BuildConfig;

import cucumber.api.CucumberOptions;
import cucumber.api.android.CucumberInstrumentationCore;

@CucumberOptions(
        features = "features",
        glue = "com.optimizely.ab.android.test_app.bdd.support")
public class TestRunner extends MonitoringInstrumentation {

    private final CucumberInstrumentationCore instrumentationCore = new CucumberInstrumentationCore(this);

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);

        String tags = BuildConfig.TEST_TAGS;
        if (!tags.isEmpty()) {
            arguments.putString("tags", tags.replaceAll(",", "--").replaceAll("\\s",""));
        }
        instrumentationCore.create(arguments);
        start();
    }

    @Override
    public void onStart() {
        super.onStart();

        waitForIdleSync();
        instrumentationCore.start();
    }
}
