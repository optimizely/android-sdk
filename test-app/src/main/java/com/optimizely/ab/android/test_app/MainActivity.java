package com.optimizely.ab.android.test_app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.android.project_watcher.OptimizelyStartedListener;
import com.optimizely.ab.android.project_watcher.OptimizelyAndroid;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements OptimizelyStartedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // After the data file is downloaded and other initialization happens off the UI thread the callback will be hit
        OptimizelyAndroid optimizelyAndroid = OptimizelyAndroid.start("6820012714", getApplication(), this);
        // sync the data file even when the app isn't open, this method doesn't hit the callback
        // If the alarm has been scheduled already it won't be rescheduled even if the interval
        // is different.  The alarm must be unregistered first via optimizelyAndroid.cancelDataFileLoad().
        optimizelyAndroid.syncDataFile(TimeUnit.DAYS, 1);
    }

    @Override
    public void onOptimizelyStarted(Optimizely optimizely) {
        // activate user in the experiment
        Variation variation = optimizely.activate("exp1", "user1");

        if (variation != null) {
            if (variation.is("var1")) {
                Toast.makeText(this, "Variation 1", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Default", Toast.LENGTH_LONG).show();
        }

        // track conversion event
        optimizely.track("goal1", "user1");
    }
}
