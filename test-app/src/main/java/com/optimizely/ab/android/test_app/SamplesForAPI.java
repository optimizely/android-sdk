/****************************************************************************
 * Copyright 2020, Optimizely, Inc. and contributors                   *
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

import android.content.Context;
import android.util.Log;

import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.sdk.OptimizelyStartListener;
import com.optimizely.ab.config.Variation;

import java.util.concurrent.TimeUnit;

public class SamplesForAPI {

    static public void samplesForInitialization(Context context) {
        OptimizelyManager optimizelyManager;
        OptimizelyClient optimizelyClient;
        Variation variation;

        // These are sample codes for synchronous and asynchronous SDK initializations with multiple options

        // [Synchronous]

        // [S1] Synchronous initialization
        //      1. SDK is initialized instantly with a cached (or bundled) datafile
        //      2. A new datafile can be downloaded in background and cached after the SDK is initialized.
        //         The cached datafile will be used only when the SDK re-starts in the next session.
        optimizelyManager =  OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .build(context);
        optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile);
        variation = optimizelyClient.activate("<Experiment_Key>", "<User_ID>");

        // [S2] Synchronous initialization
        //      1. SDK is initialized instantly with a cached (or bundled) datafile
        //      2. A new datafile can be downloaded in background and cached after the SDK is initialized.
        //         The cached datafile is used immediately to update the SDK project config.
        optimizelyManager =  OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .build(context);
        optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile, true, true);
        variation = optimizelyClient.activate("<Experiment_Key>", "<User_ID>");

        // [S3] Synchronous initialization
        //      1. SDK is initialized instantly with a cached (or bundled) datafile
        //      2. A new datafile can be downloaded in background and cached after the SDK is initialized.
        //         The cached datafile is used immediately to update the SDK project config.
        //      3. Polling datafile periodically.
        //         The cached datafile is used immediately to update the SDK project config.
        optimizelyManager =  OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .withDatafileDownloadInterval(TimeUnit.MINUTES.toSeconds(15))
                .build(context);
        optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile, true, true);
        variation = optimizelyClient.activate("<Experiment_Key>", "<User_ID>");

        // [Asynchronous]

        // [A1] Asynchronous initialization
        //      1. A datafile is downloaded from the server and the SDK is initialized with the datafile
        optimizelyManager =  OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .build(context);
        optimizelyManager.initialize(context, null, (OptimizelyClient client) -> {
            Variation variation2 = client.activate("<Experiment_Key>", "<User_ID>");
        });

        // [A2] Asynchronous initialization
        //      1. A datafile is downloaded from the server and the SDK is initialized with the datafile
        //      2. Polling datafile periodically.
        //         The cached datafile is used immediately to update the SDK project config.
        optimizelyManager =  OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .withDatafileDownloadInterval(TimeUnit.MINUTES.toSeconds(15))
                .build(context);
        optimizelyManager.initialize(context, null, (OptimizelyClient client) -> {
            Variation variation2 = client.activate("<Experiment_Key>", "<User_ID>");
        });

    }

}
