/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.android.test_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.sdk.OptimizelyStartListener;
import com.optimizely.ab.android.shared.CountingIdlingResourceManager;
import com.optimizely.ab.config.Variation;

public class MainActivity extends AppCompatActivity {

    // The Idling Resource which will be null in production.
    @Nullable private static CountingIdlingResourceManager countingIdlingResourceManager;
    private OptimizelyManager optimizelyManager;
    private MyApplication myApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button) findViewById(R.id.button_1);

        // This could also be done via DI framework such as Dagger
        myApplication = (MyApplication) getApplication();
        optimizelyManager = myApplication.getOptimizelyManager();

        // Load Optimizely from a compiled in data file
        final OptimizelyClient optimizely = optimizelyManager.initialize(this, R.raw.data_file);
        CountingIdlingResourceManager.increment(); // For impression event
        Variation variation = optimizely.activate("experiment_0", myApplication.getAnonUserId(), myApplication.getAttributes());
        if (variation != null) {
            if (variation.is("variation_1")) {
                button.setText(R.string.main_act_button_1_text_var_1);
            } else if (variation.is("variation_2")) {
                button.setText(R.string.main_act_button_1_text_var_2);
            }
        } else {
            button.setText(R.string.main_act_button_1_text_default);
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CountingIdlingResourceManager.increment();
                optimizely.track("experiment_0", myApplication.getAnonUserId(), myApplication.getAttributes());
                // For track event

                v.getContext().getResources().getString(R.string.app_name);
                Intent intent = new Intent(v.getContext(), SecondaryActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        CountingIdlingResourceManager.increment(); // For Optimizely starting
        optimizelyManager.initialize(this, new OptimizelyStartListener() {
            @Override
            public void onStart(OptimizelyClient optimizely) {
                CountingIdlingResourceManager.decrement(); // For Optimizely starting
                TextView textView1 = (TextView) findViewById(R.id.text_view_1);
                textView1.setVisibility(View.VISIBLE);
                CountingIdlingResourceManager.increment(); // For impression event
                Variation variation = optimizely.activate("experiment_1", myApplication.getAnonUserId());
                if (variation != null) {
                    if (variation.is("variation_1")) {
                        textView1.setText(R.string.main_act_text_view_1_var_1);
                    } else if (variation.is("variation_2")) {
                        textView1.setText(R.string.main_act_text_view_1_var_2);
                    }
                } else {
                    textView1.setText(R.string.main_act_text_view_1_default);
                }
            }
        });
    }
}
