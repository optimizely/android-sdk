package com.optimizely.ab.android.test_app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.android.sdk.OptimizelyStartListener;
import com.optimizely.ab.android.sdk.OptimizelySDK;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OptimizelySDK.builder("6820012714")
                .withEventHandlerDispatchInterval(1, TimeUnit.DAYS)
                .withDataFileDownloadInterval(1, TimeUnit.DAYS)
                .build()
                .start(this, new OptimizelyStartListener() {
                    @Override
                    public void onStart(Optimizely optimizely) {
                        Variation variation = optimizely.activate("experiment_1", "user_1");

                        if (variation != null) {
                            if (variation.is("variation_1")) {
                                Toast.makeText(MainActivity.this, "Variation 1", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Default", Toast.LENGTH_LONG).show();
                        }

                        // track conversion event
                        optimizely.track("goal_1", "user_1");
                    }
                });
    }
}
