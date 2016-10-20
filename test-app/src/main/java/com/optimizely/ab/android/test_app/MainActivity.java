/*
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.optimizely.ab.android.sdk.AndroidOptimizely;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.sdk.OptimizelyStartListener;
import com.optimizely.ab.config.Variation;

public class MainActivity extends AppCompatActivity {

    private OptimizelyManager optimizelyManager;
    private MyApplication myApplication;


    // The Idling Resource which will be null in production.
    @Nullable private CountingIdlingResource countingIdlingResource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button) findViewById(R.id.button_1);

        // This could also be done via DI framework such as Dagger
        myApplication = (MyApplication) getApplication();
        optimizelyManager = myApplication.getOptimizelyManager();

        // Load Optimizely from a compiled in data file
        final AndroidOptimizely optimizely = optimizelyManager.getOptimizely(this, R.raw.data_file);
        Variation variation = optimizely.activate("experiment_0", myApplication.getAnonUserId(), myApplication.getAttributes());
        if (variation != null) {
            if (variation.is("variation_1")) {
                button.setText(R.string.button_1_text_var_1);
            } else if (variation.is("variation_2")) {
                button.setText(R.string.button_1_text_var_2);
            }
        } else {
            button.setText(R.string.button_1_text_default);
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                optimizely.track("experiment_0", myApplication.getAnonUserId(), myApplication.getAttributes());

                v.getContext().getResources().getString(R.string.app_name);
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

        if (countingIdlingResource != null) {
            countingIdlingResource.increment();
        }
        optimizelyManager.start(this, new OptimizelyStartListener() {
            @Override
            public void onStart(AndroidOptimizely optimizely) {
                TextView textView1 = (TextView) findViewById(R.id.text_view_1);
                textView1.setVisibility(View.VISIBLE);
                Variation variation = optimizely.activate("experiment_1", myApplication.getAnonUserId());
                if (variation != null) {
                    if (variation.is("variation_1")) {
                        textView1.setText(R.string.text_view_1_var_1);
                    } else if (variation.is("variation_2")) {
                        textView1.setText(R.string.text_view_1_var_2);
                    }
                } else {
                    textView1.setText(R.string.text_view_1_default);
                }
                if (countingIdlingResource != null) {
                    countingIdlingResource.decrement();
                }
            }
        });
    }

    /**
     * Only called from test, creates and returns a new {@link CountingIdlingResource}.
     */
    @VisibleForTesting
    @NonNull
    public CountingIdlingResource getIdlingResource() {
        if (countingIdlingResource == null) {
            countingIdlingResource = new CountingIdlingResource("onOptimizelyStart", true);
        }
        return countingIdlingResource;
    }
}
