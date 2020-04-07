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
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.optimizely.ab.android.sdk.OptimizelyStartListener;
import com.optimizely.ab.android.optimizely_debugger.OptimizelyDebugger;

public class VariationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String variation = intent.getStringExtra("variation");

        setContentView(R.layout.activity_variation);
        View view = (View)findViewById(R.id.activity_variation_activity);
        TextView textView = (TextView)findViewById(R.id.tv_variation_a_text_1);

        if(variation.equals("variation_a")) {
            view.setBackgroundResource(R.drawable.ic_background_varia);
            textView.setText("A");
        } else {
            view.setBackgroundResource(R.drawable.ic_background_varib_marina);
            textView.setText("B");
        }
    }

    public void openDebugger(View view) {
        OptimizelyDebugger.open(this, ((MyApplication)getApplication()).getOptimizelyManager());
    }
}
