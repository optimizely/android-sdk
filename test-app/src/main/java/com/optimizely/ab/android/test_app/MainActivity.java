/**
 * Copyright 2016, Optimizely
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.android.test_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.optimizely.ab.android.sdk.AndroidOptimizely;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.sdk.OptimizelyStartListener;
import com.optimizely.ab.config.Variation;

public class MainActivity extends AppCompatActivity {

    private OptimizelyManager optimizelyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // This could also be done via DI framework such as Dagger
        optimizelyManager = ((MyApplication) getApplication()).getOptimizelyManager();

        // Load Optimizely from a compiled in data file
        AndroidOptimizely optimizely = optimizelyManager.getOptimizely(this, R.raw.data_file);
        Variation variation = optimizely.activate("experiment_0", "user_1");
        if (variation != null) {
            if (variation.is("variation_1")) {
                Toast.makeText(MainActivity.this, "Variation 1", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Default", Toast.LENGTH_LONG).show();
            }
        }

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), SecondaryActivity.class);
                startActivity(intent);

                intent.setClass(v.getContext(), MyIntentService.class);
                startService(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        optimizelyManager.start(this, new OptimizelyStartListener() {
            @Override
            public void onStart(AndroidOptimizely optimizely) {
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
    }
}
