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

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.shared.CountingIdlingResourceManager;

public class ConversionFragment extends Fragment {

    Button conversionButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_conversion, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        conversionButton = (Button) view.findViewById(R.id.btn_variation_conversion);

        final MyApplication myApplication = (MyApplication) getActivity().getApplication();
        final OptimizelyManager optimizelyManager = myApplication.getOptimizelyManager();

        conversionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userId = myApplication.getAnonUserId();

                OptimizelyClient optimizely = optimizelyManager.getOptimizely();
                optimizely.track("sample_conversion", userId);

                // Utility method for verifying event dispatches in our automated tests
                CountingIdlingResourceManager.increment(); // increment for conversion event

                Intent intent = new Intent(myApplication.getBaseContext(), EventConfirmationActivity.class);
                startActivity(intent);
            }
        });
    }
}
