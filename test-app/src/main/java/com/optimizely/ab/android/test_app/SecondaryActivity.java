package com.optimizely.ab.android.test_app;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.android.sdk.OptimizelySDK;

public class SecondaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        // Get Optimizely from the Intent that started this Activity
        final OptimizelySDK optimizelySDK = ((MyApplication) getApplication()).getOptimizelySDK();
        Optimizely optimizely = optimizelySDK.getOptimizely();

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
            final OptimizelySDK optimizelySDK = ((MyApplication) getActivity().getApplication()).getOptimizelySDK();
            Optimizely optimizely = optimizelySDK.getOptimizely();

            if (optimizely != null) {
                optimizely.track("goal_2", "user_1");
            }
        }
    }
}
