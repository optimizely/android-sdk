/****************************************************************************
 * Copyright 2016-2017, Optimizely, Inc. and contributors                   *
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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;

import com.optimizely.ab.android.sdk.OptimizelyManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MyApplication extends Application {

    // Project ID owned by mobile-test@optimizely.com
    // if you'd like to configure your own experiment please check out https://developers.optimizely.com/x/solutions/sdks/getting-started/index.html?language=android&platform=mobile
    // to create your own project and experiment. Then just replace your project ID below.
    public static final String PROJECT_ID = "10554895220";
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
        // this is a convenience method that creates and persists an anonymous user id,
        // which we need to pass into the activate and track calls
        SharedPreferences sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);
        String id = sharedPreferences.getString("userId", null);
        if (id == null) {
            id = UUID.randomUUID().toString();

            // comment this out to get a brand new user id every time this function is called.
            // useful for incrementing results page count for QA purposes
            sharedPreferences.edit().putString("userId", id).apply();
        }
        return id;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // This app is built against a real Optimizely project with real experiments set.  Automated
        // espresso tests are run against this project id.  Changing it will make the Optimizely
        // must match the project id of the compiled in Optimizely data file in rest/raw/data_file.json.

         OptimizelyManager.Builder builder = OptimizelyManager.builder();
         optimizelyManager =  builder.withEventDispatchInterval(60L * 10L)
            .withDatafileDownloadInterval(30L)
            .withSDKKey("FCnSegiEkRry9rhVMroit4")
            .build(getApplicationContext());
    }
}
