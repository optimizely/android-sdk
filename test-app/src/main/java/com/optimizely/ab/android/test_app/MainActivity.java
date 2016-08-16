package com.optimizely.ab.android.test_app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.android.project_watcher.OptimizelyStartedListener;
import com.optimizely.ab.android.project_watcher.OptimizelyAndroid;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OptimizelyAndroid optimizelyAndroid = OptimizelyAndroid.start("6820012714", getApplication(), new OptimizelyStartedListener() {
            @Override
            public void onOptimizelyStarted(Optimizely optimizely) {
                Variation variation = optimizely.activate("exp1", "user1");

                if (variation != null) {
                    if (variation.is("var1")) {
                        Toast.makeText(MainActivity.this, "Variation 1", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Default", Toast.LENGTH_LONG).show();
                }

                // track conversion event
                optimizely.track("goal1", "user1");
            }
        });

        optimizelyAndroid.syncDataFile(TimeUnit.DAYS, 1);
    }
}
