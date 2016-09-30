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

import android.app.Application;

import com.optimizely.ab.android.sdk.OptimizelyManager;

import java.util.concurrent.TimeUnit;

public class MyApplication extends Application {

    private OptimizelyManager optimizelyManager;

    public OptimizelyManager getOptimizelyManager() {
        return optimizelyManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // If the project doesn't compile because you are missing R.string.optly_project_id you need
        // to put `git_ignored_strings.xml` in test-app/src/main/res/values.  This file is git ignored.
        // It contains values that are unique for each developer.
        optimizelyManager = OptimizelyManager.builder(getResources().getString(R.string.optly_project_id))
                .withEventHandlerDispatchInterval(30, TimeUnit.SECONDS)
                .withDataFileDownloadInterval(30, TimeUnit.SECONDS)
                .build();
    }
}
