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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.shared.CountingIdlingResourceManager;
import com.optimizely.ab.config.Variation;

public class SecondaryFragment extends Fragment {

    TextView textView1;
    Button button1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_secondary, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        textView1 = (TextView) view.findViewById(R.id.text_view_1);
        button1 = (Button) view.findViewById(R.id.button_1);

        final MyApplication myApplication = (MyApplication) getActivity().getApplication();
        final OptimizelyManager optimizelyManager = myApplication.getOptimizelyManager();

        OptimizelyClient optimizely = optimizelyManager.getOptimizely();
        CountingIdlingResourceManager.increment();
        Variation variation = optimizely.activate("experiment_2", myApplication.getAnonUserId());
        if (variation != null) {
            if (variation.is("variation_1")) {
                textView1.setText(R.string.secondary_frag_text_view_1_var_1);
            } else if (variation.is("variation_2")) {
                textView1.setText(R.string.secondary_frag_text_view_1_var_2);
            }
        } else {
            textView1.setText(R.string.secondary_frag_text_view_1_default);
        }

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final FragmentActivity activity = getActivity();
                Intent intent = new Intent(activity, NotificationService.class);
                activity.startService(intent);
            }
        });
    }
}
