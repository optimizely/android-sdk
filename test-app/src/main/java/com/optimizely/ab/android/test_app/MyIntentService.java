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

import android.app.IntentService;
import android.content.Intent;

import com.optimizely.ab.android.sdk.AndroidOptimizely;
import com.optimizely.ab.android.sdk.OptimizelyManager;

public class MyIntentService extends IntentService {
    public MyIntentService() {
        super("MyIntentService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            // Get Optimizely from the Intent that started this Service
            final OptimizelyManager optimizelyManager = ((MyApplication) getApplication()).getOptimizelyManager();
            AndroidOptimizely optimizely = optimizelyManager.getOptimizely();
            if (optimizely != null) {
                optimizely.track("goal_3", "user_1");
            }
        }
    }
}
