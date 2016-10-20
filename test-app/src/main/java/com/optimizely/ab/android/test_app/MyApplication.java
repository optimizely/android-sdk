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

        // This app is built against a real Optimizely project with real experiments set.  Automated
        // espresso tests are run against this project id.  Changing it will make the Optimizely
        // tests setup not work and the Espresso tests will fail.  Also, the project id passed here
        // must match the project id of the compiled in Optimizely data file in rest/raw/data_file.json.
        optimizelyManager = OptimizelyManager.builder("7664231436")
                .withEventHandlerDispatchInterval(30, TimeUnit.SECONDS)
                .withDataFileDownloadInterval(30, TimeUnit.SECONDS)
                .build();
    }
}
