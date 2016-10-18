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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.optimizely.ab.android.sdk.AndroidOptimizely;
import com.optimizely.ab.android.sdk.OptimizelyManager;

public class SecondaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        // Get Optimizely from the Intent that started this Activity
        final MyApplication myApplication = (MyApplication) getApplication();
        final OptimizelyManager optimizelyManager = myApplication.getOptimizelyManager();
        AndroidOptimizely optimizely = optimizelyManager.getOptimizely();

        // track conversion event
        optimizely.track("experiment_1", myApplication.getAnonUserId());

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        SecondaryFragment secondaryFragment = new SecondaryFragment();
        Bundle arguments = new Bundle();
        secondaryFragment.setArguments(arguments);

        fragmentTransaction.add(secondaryFragment, "frag");

        fragmentTransaction.commit();
    }

    public static class SecondaryFragment extends Fragment {

        @Override
        public void onStart() {
            super.onStart();
            final OptimizelyManager optimizelyManager = ((MyApplication) getActivity().getApplication()).getOptimizelyManager();
            AndroidOptimizely optimizely = optimizelyManager.getOptimizely();
//            optimizely.track("goal_2", "user_1");
        }
    }
}
