package com.optimizely.ab.android.test_app;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.optimizely.ab.android.sdk.AndroidOptimizely;
import com.optimizely.ab.android.sdk.OptimizelyManager;

public class SecondaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        // Get Optimizely from the Intent that started this Activity
        final OptimizelyManager optimizelyManager = ((MyApplication) getApplication()).getOptimizelyManager();
        AndroidOptimizely optimizely = optimizelyManager.getOptimizely();

        // track conversion event
        optimizely.track("goal_1", "user_1");

        FragmentManager fragmentManager = getFragmentManager();
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

            if (optimizely != null) {
                optimizely.track("goal_2", "user_1");
            }
        }
    }
}
