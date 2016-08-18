package com.optimizely.ab.android.test_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.android.sdk.OptimizelySDK;
import com.optimizely.ab.android.sdk.OptimizelyStartListener;
import com.optimizely.ab.config.Variation;

public class MainActivity extends AppCompatActivity {

    @Nullable private Optimizely optimizely;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // This could also be done via DI framework such as Dagger
        final OptimizelySDK optimizelySDK = ((MyApplication) getApplication()).getOptimizelySDK();

        optimizelySDK.start(this, new OptimizelyStartListener() {
            @Override
            public void onStart(Optimizely optimizely) {
                MainActivity.this.optimizely = optimizely;

                Variation variation = optimizely.activate("experiment_1", "user_1");

                if (variation != null) {
                    if (variation.is("variation_1")) {
                        Toast.makeText(MainActivity.this, "Variation 1", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Default", Toast.LENGTH_LONG).show();
                }


            }
        });

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), SecondaryActivity.class);
                // Try and pass Optimizely through the Intent system
                if (optimizely != null) { // If null Optimizely didn't load before the click
                    intent.putExtra("optly", optimizelySDK.parcelOptimizely(optimizely));
                }
                startActivity(intent);
            }
        });
    }
}
