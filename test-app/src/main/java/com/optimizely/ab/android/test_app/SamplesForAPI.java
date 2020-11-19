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

import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.sdk.OptimizelyStartListener;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.config.parser.JsonParseException;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;
import com.optimizely.ab.optimizelydecision.OptimizelyDecision;
import com.optimizely.ab.optimizelyjson.OptimizelyJSON;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SamplesForAPI {

    static public void samplesForDecide(Context context) {
        List<OptimizelyDecideOption> defaultDecideOptions = Arrays.asList(OptimizelyDecideOption.DISABLE_DECISION_EVENT);

        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("FCnSegiEkRry9rhVMroit4")
                .withDefaultDecideOptions(defaultDecideOptions)
                .build(context);
        optimizelyManager.initialize(context, R.raw.datafile, optimizelyClient -> {

            // createUserContext

            String userId = "user_123";
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("is_logged_in", false);
            attributes.put("app_version", "1.3.2");
            OptimizelyUserContext user = optimizelyClient.createUserContext(userId, attributes);
            // attributes can be set in this way too
            user.setAttribute("location", "NY");

            // decide

            List<OptimizelyDecideOption> options = Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS);
            OptimizelyDecision decision = user.decide("show_coupon", options);

            String variationKey = decision.getVariationKey();
            boolean enabled = decision.getEnabled();
            OptimizelyJSON variables = decision.getVariables();
            String vs = null;
            try {
                vs = variables.getValue("text_color", String.class);
            } catch (JsonParseException e) {
                e.printStackTrace();
            }
            int vb = (int) variables.toMap().get("discount");
            String ruleKey = decision.getRuleKey();
            String flagKey = decision.getFlagKey();
            OptimizelyUserContext userContext = decision.getUserContext();
            List<String> reasons = decision.getReasons();

            Log.d("Samples", "decision: " + decision);
            Log.d("Samples", "items: " + variationKey + " " + String.valueOf(enabled) + " " + vs + " " + String.valueOf(vb) + " " + ruleKey + " " + flagKey + " " + userContext + " " + reasons);

            // decideForKeys

            List<String> keys = Arrays.asList("show_coupon", "bg-feature");
            Map<String, OptimizelyDecision> decisionsMultiple = user.decideForKeys(keys);

            OptimizelyDecision decision1 = decisionsMultiple.get(keys.get(0));
            OptimizelyDecision decision2 = decisionsMultiple.get(keys.get(1));
            Log.d("Samples", "decisionsMultiple: " + keys + " " + decision1 + " " + decision2);

            // decideAll

            List<OptimizelyDecideOption> options2 = Arrays.asList(OptimizelyDecideOption.ENABLED_FLAGS_ONLY);
            Map<String, OptimizelyDecision> decisionsAll = user.decideAll(options2);

            Set<String> allKeys = decisionsAll.keySet();
            Collection<OptimizelyDecision> allDecisions = decisionsAll.values();
            Log.d("Samples", "all keys: " + allKeys);
            Log.d("Samples", "all decisions: " + allDecisions);

            // trackEvent

            user.trackEvent("sample_conversion");
        });
    }

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
