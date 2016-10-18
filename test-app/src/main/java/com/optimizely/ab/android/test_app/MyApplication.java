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
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.optimizely.ab.android.sdk.OptimizelyManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MyApplication extends Application {

    private OptimizelyManager optimizelyManager;

    public OptimizelyManager getOptimizelyManager() {
        return optimizelyManager;
    }
    public Map<String,String> getAttributes() {
        Map<String,String> attributes = new HashMap<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            attributes.put("locale", getResources().getConfiguration().getLocales().get(0).toString());
        } else {
            attributes.put("locale", getResources().getConfiguration().locale.toString());
        }
        return attributes;
    }
    @NonNull
    public String getAnonUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);
        String id = sharedPreferences.getString("userId", null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            sharedPreferences.edit().putString("userId", id).apply();
        }
        return id;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // If the project doesn't compile because you are missing R.string.optly_project_id you need
        // to put `git_ignored_strings.xml` in test-app/src/main/res/values.  This file is git ignored.
        // It contains values that are unique for each developer.
        final String projectId = getResources().getString(
                getResources().getIdentifier("optly_project_id", "string", getPackageName()));
        optimizelyManager = OptimizelyManager.builder(projectId)
                .withEventHandlerDispatchInterval(30, TimeUnit.SECONDS)
                .withDataFileDownloadInterval(30, TimeUnit.SECONDS)
                .build();
    }
}
