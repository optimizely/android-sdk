/****************************************************************************
 * Copyright 2020, 2022-2023, Optimizely, Inc. and contributors             *
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

package com.optimizely.ab.android.test_app.Samples;

import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Path;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.OptimizelyDecisionContext;
import com.optimizely.ab.OptimizelyForcedDecision;
import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.android.event_handler.DefaultEventHandler;
import com.optimizely.ab.android.event_handler.EventRescheduler;
import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.sdk.cmab.CmabClientHelperAndroid;
import com.optimizely.ab.android.sdk.cmab.DefaultCmabClient;
import com.optimizely.ab.android.test_app.R;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.cmab.client.CmabClient;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.config.parser.JsonParseException;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.error.RaiseExceptionErrorHandler;
import com.optimizely.ab.event.BatchEventProcessor;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.notification.DecisionNotification;
import com.optimizely.ab.notification.UpdateConfigNotification;
import com.optimizely.ab.optimizelyconfig.OptimizelyAttribute;
import com.optimizely.ab.optimizelyconfig.OptimizelyAudience;
import com.optimizely.ab.optimizelyconfig.OptimizelyConfig;
import com.optimizely.ab.optimizelyconfig.OptimizelyEvent;
import com.optimizely.ab.optimizelyconfig.OptimizelyExperiment;
import com.optimizely.ab.optimizelyconfig.OptimizelyFeature;
import com.optimizely.ab.optimizelyconfig.OptimizelyVariable;
import com.optimizely.ab.optimizelyconfig.OptimizelyVariation;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;
import com.optimizely.ab.optimizelydecision.OptimizelyDecision;
import com.optimizely.ab.optimizelyjson.OptimizelyJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class APISamplesInJava {

    static public void samplesAll(Context context) {
        samplesForCmab(context);
        samplesForCmabConfig(context);
        samplesForCmabConfig_endpoint(context);

        samplesForDecide(context);
        samplesForInitialization(context);
        samplesForOptimizelyConfig(context);
        samplesForDoc_InitializeSDK(context);
        samplesForDoc_GetClient(context);
        samplesForDoc_DatafilePolling(context);
        samplesForDoc_BundledDatafile(context);
        samplesForDoc_ExampleUsage(context);
        samplesForDoc_CreateUserContext(context);
        samplesForDoc_DecideOptions(context);
        samplesForDoc_Decide(context);
        samplesForDoc_DecideAll(context);
        samplesForDoc_DecideForKeys(context);
        samplesForDoc_TrackEvent(context);
        samplesForDoc_OptimizelyJSON(context);
        samplesForDoc_CustomUserProfileService(context);
        samplesForDoc_EventBatchingDefault(context);
        samplesForDoc_EventBatchingAdvanced(context);
        samplesForDoc_ErrorHandler(context);
        samplesForDoc_AudienceAttributes(context);
        samplesForDoc_NotificatonListener(context);
        samplesForDoc_OlderVersions(context);
        samplesForDoc_ForcedDecision(context);
        samplesForDoc_ODP_async(context);
        samplesForDoc_ODP_sync(context);
    }


    static public void samplesForCmab(Context context) {
        List<OptimizelyDecideOption> options = Arrays.asList(
            OptimizelyDecideOption.INCLUDE_REASONS,
            OptimizelyDecideOption.IGNORE_USER_PROFILE_SERVICE,
            OptimizelyDecideOption.IGNORE_CMAB_CACHE
        );

        // we use cmab test project=4552646833471488 datafile for this testing

        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
            .withSDKKey("4ft9p1vSXYM5hLATwWdRc")
            .build(context);
        // we use raw datafile for this testing
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile_full);

        String userId = "user_123";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("country", "us");
        attributes.put("extra-1", 100);
        OptimizelyUserContext user = optimizelyClient.createUserContext(userId, attributes);

        String flagKey = "cmab-flag";

        // decide (decideSync)

        Log.d("Samples","=================================================================");
        Log.d("Samples","[CMAB] calling sync decision for cmab...");
        Log.d("Samples","=================================================================");
        OptimizelyDecision decision = user.decide(flagKey, options);

        Log.d("Samples","=================================================================");
        Log.d("Samples","[CMAB] sync decision for cmab: " + decision.toString());
        if (decision.getEnabled()) {
            Log.e("Samples","[ERROR] " + flagKey + " is expected to be NOT enabled for this user!");
        }
        Log.d("Samples","=================================================================");

        // decideAsync

        Log.d("Samples","=================================================================");
        Log.d("Samples","[CMAB] calling async decision for cmab...");
        Log.d("Samples","=================================================================");
        final CountDownLatch latch = new CountDownLatch(1);
        user.decideAsync(flagKey, options, (OptimizelyDecision optDecision) -> {
            Log.d("Samples","=================================================================");
            Log.d("Samples","[CMAB] async decision for cmab: " + optDecision.toString());
            if (!optDecision.getEnabled()) {
                Log.e("Samples","[ERROR] " + flagKey + " is expected to be enabled for this user!");
            }
            Log.d("Samples","=================================================================");
            latch.countDown();
        });

        try {
            latch.await(60, TimeUnit.SECONDS);
            Log.d("Samples", "[CMAB] Latch released. Async operation completed.");
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }

    }

    static public void samplesForCmabConfig(Context context) {
        List<OptimizelyDecideOption> options = Arrays.asList(
            OptimizelyDecideOption.INCLUDE_REASONS,
            OptimizelyDecideOption.IGNORE_USER_PROFILE_SERVICE,
            OptimizelyDecideOption.IGNORE_CMAB_CACHE
        );

        // we use cmab test project=4552646833471488 datafile for this testing

        CmabClient customCmabClient = new DefaultCmabClient(context);
        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
            .withSDKKey("4ft9p1vSXYM5hLATwWdRc")
            .withCmabCacheSize(50)
            .withCmabCacheTimeout(10, TimeUnit.SECONDS)
            .withCmabClient(customCmabClient)
            .build(context);
        // we use raw datafile for this testing
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile_full);

        String userId = "user_123";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("country", "us");
        attributes.put("extra-1", 100);
        OptimizelyUserContext user = optimizelyClient.createUserContext(userId, attributes);

        String flagKey = "cmab-flag";

        // decideAsync

        Log.d("Samples","=================================================================");
        Log.d("Samples","[CMAB] calling async decision for cmab with custom cmab cache...");
        Log.d("Samples","=================================================================");
        final CountDownLatch latch = new CountDownLatch(1);
        user.decideAsync(flagKey, options, (OptimizelyDecision optDecision) -> {
            Log.d("Samples","=================================================================");
            Log.d("Samples","[CMAB] async decision for cmab with custom cmab cache: " + optDecision.toString());
            if (!optDecision.getEnabled()) {
                Log.e("Samples","[ERROR] " + flagKey + " is expected to be enabled for this user!");
            }
            Log.d("Samples","=================================================================");
            latch.countDown();
        });

        try {
            latch.await(60, TimeUnit.SECONDS);
            Log.d("Samples", "[CMAB] Latch released. Async operation completed.");
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    static public void samplesForCmabConfig_endpoint(Context context) {
        List<OptimizelyDecideOption> options = Arrays.asList(
            OptimizelyDecideOption.INCLUDE_REASONS,
            OptimizelyDecideOption.IGNORE_USER_PROFILE_SERVICE,
            OptimizelyDecideOption.IGNORE_CMAB_CACHE
        );

        // custom Cmab Endpoint

        CmabClient customCmabClient = new DefaultCmabClient(context);
        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
            .withSDKKey("4ft9p1vSXYM5hLATwWdRc")
            .withCmabClient(customCmabClient)
            .build(context);
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile_full);

        String userId = "user_123";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("country", "us");
        attributes.put("extra-1", 100);
        OptimizelyUserContext user = optimizelyClient.createUserContext(userId, attributes);

        String flagKey = "cmab-flag";

        Log.d("Samples","=================================================================");
        Log.d("Samples","[CMAB] calling async decision for cmab with custom CmabClient...");
        Log.d("Samples","=================================================================");
        final CountDownLatch latch = new CountDownLatch(1);
        user.decideAsync(flagKey, options, (OptimizelyDecision optDecision) -> {
            Log.d("Samples","=================================================================");
            Log.d("Samples","[CMAB] async decision for cmab with custom CmabClient: " + optDecision.toString());
            if (!optDecision.getEnabled()) {
                Log.e("Samples","[ERROR] " + flagKey + " is expected to be enabled for this user!");
            }
            Log.d("Samples","=================================================================");
            latch.countDown();
        });

        try {
            latch.await(60, TimeUnit.SECONDS);
            Log.d("Samples", "[CMAB] Latch released. Async operation completed.");
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    static public void samplesForDecide(Context context) {
        // this default-options will be applied to all following decide calls.
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
            // or can be called without options
            //OptimizelyDecision decision = user.decide("show_coupon");

            String variationKey = decision.getVariationKey();
            boolean enabled = decision.getEnabled();
            OptimizelyJSON variables = decision.getVariables();
            String varStr = null;
            try {
                varStr = variables.getValue("text_color", String.class);
            } catch (JsonParseException e) {
                e.printStackTrace();
            }
            int varInt = (int) variables.toMap().get("discount");
            String ruleKey = decision.getRuleKey();
            String flagKey = decision.getFlagKey();
            OptimizelyUserContext userContext = decision.getUserContext();
            List<String> reasons = decision.getReasons();

            Log.d("Samples", "decision: " + decision.toString());
            Log.d("Samples", "items: " + variationKey + " " + String.valueOf(enabled) + " " + varStr + " " + String.valueOf(varInt) + " " + ruleKey + " " + flagKey + " " + userContext + " " + reasons);

            // decideForKeys

            List<String> keys = Arrays.asList("show_coupon", "bg-feature");
            Map<String, OptimizelyDecision> decisionsMultiple = user.decideForKeys(keys);
            OptimizelyDecision decision1 = decisionsMultiple.get(keys.get(0));
            OptimizelyDecision decision2 = decisionsMultiple.get(keys.get(1));
            Log.d("Samples", "decisionsMultiple: " + keys + " " + decision1.toString() + " " + decision2.toString());

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
        OptimizelyUserContext user;
        OptimizelyDecision decision;

        // Here are more sample codes for synchronous and asynchronous SDK initializations with multiple options

        // [Synchronous]

        // [S1] Synchronous initialization
        //      1. SDK is initialized instantly with a cached (or bundled) datafile
        //      2. A new datafile can be downloaded in background and cached after the SDK is initialized.
        //         The cached datafile will be used only when the SDK re-starts in the next session.
        optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .build(context);
        optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile);
        user = optimizelyClient.createUserContext("<User_ID>");
        decision = user.decide("<Flag_Key>");

        // [S2] Synchronous initialization
        //      1. SDK is initialized instantly with a cached (or bundled) datafile
        //      2. A new datafile can be downloaded in background and cached after the SDK is initialized.
        //         The cached datafile is used immediately to update the SDK project config.
        optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .build(context);
        optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile, true, true);
        user = optimizelyClient.createUserContext("<User_ID>");
        decision = user.decide("<Flag_Key>");

        // [S3] Synchronous initialization
        //      1. SDK is initialized instantly with a cached (or bundled) datafile
        //      2. A new datafile can be downloaded in background and cached after the SDK is initialized.
        //         The cached datafile is used immediately to update the SDK project config.
        //      3. Polling datafile periodically.
        //         The cached datafile is used immediately to update the SDK project config.
        optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .withDatafileDownloadInterval(15, TimeUnit.MINUTES)
                .build(context);
        optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile, true, true);
        user = optimizelyClient.createUserContext("<User_ID>");
        decision = user.decide("<Flag_Key>");

        // [Asynchronous]

        // [A1] Asynchronous initialization
        //      1. A datafile is downloaded from the server and the SDK is initialized with the datafile
        optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .build(context);
        optimizelyManager.initialize(context, null, (OptimizelyClient client) -> {
            OptimizelyUserContext userContext = client.createUserContext("<User_ID>");
            OptimizelyDecision optDecision = userContext.decide("<Flag_Key>");
        });

        // [A2] Asynchronous initialization
        //      1. A datafile is downloaded from the server and the SDK is initialized with the datafile
        //      2. Polling datafile periodically.
        //         The cached datafile is used immediately to update the SDK project config.
        optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .withDatafileDownloadInterval(15, TimeUnit.MINUTES)
                .build(context);
        optimizelyManager.initialize(context, null, (OptimizelyClient client) -> {
            OptimizelyUserContext userContext = client.createUserContext("<User_ID>");
            OptimizelyDecision optDecision = userContext.decide("<Flag_Key>");
        });
    }

    static public void samplesForOptimizelyConfig(Context context) {

        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("FCnSegiEkRry9rhVMroit4")
                .build(context);
        optimizelyManager.initialize(context, R.raw.datafile, optimizelyClient -> {

            // -- sample starts here

            OptimizelyConfig config = optimizelyClient.getOptimizelyConfig();

            Log.d("Optimizely", "[OptimizelyConfig] revision = " + config.getRevision());
            Log.d("Optimizely", "[OptimizelyConfig] sdkKey = " + config.getSdkKey());
            Log.d("Optimizely", "[OptimizelyConfig] environmentKey = " + config.getEnvironmentKey());

            Log.d("Optimizely", "[OptimizelyConfig] attributes:");
            for (OptimizelyAttribute attribute : config.getAttributes()) {
                Log.d("Optimizely", "[OptimizelyAttribute]  -- (id, key) = " + attribute.getId() + ", " + attribute.getKey());
            }

            Log.d("Optimizely", "[OptimizelyConfig] audiences:");
            for (OptimizelyAudience audience : config.getAudiences()) {
                Log.d("Optimizely", "[OptimizelyAudience]  -- (id, name, conditions) = " + audience.getId() + ", " + audience.getName() + ", " + audience.getConditions());
            }

            Log.d("Optimizely", "[OptimizelyConfig] events:");
            for (OptimizelyEvent event : config.getEvents()) {
                Log.d("Optimizely", "[OptimizelyEvent]  -- (id, key, experimentIds) = " + event.getId() + ", " + event.getKey() + ", " + Arrays.toString(event.getExperimentIds().toArray()));
            }

            // all features
            for (String flagKey : config.getFeaturesMap().keySet()) {
                OptimizelyFeature flag = config.getFeaturesMap().get(flagKey);

                for (OptimizelyExperiment experiment : flag.getExperimentRules()) {
                    Log.d("Optimizely", "[OptimizelyExperiment]  -- Experiment Rule Key: " + experiment.getKey());
                    Log.d("Optimizely", "[OptimizelyExperiment]  -- Experiment Audiences: " + experiment.getAudiences());

                    Map<String, OptimizelyVariation> variationsMap = experiment.getVariationsMap();
                    for (String variationKey : variationsMap.keySet()) {
                        OptimizelyVariation variation = variationsMap.get(variationKey);
                        Log.d("Optimizely", "[OptimizelyVariation]    -- variation = { key: " + variationKey + ", id: " + variation.getId() + ", featureEnabled: " + variation.getFeatureEnabled() + " }");
                        // use variation data here...

                        Map<String, OptimizelyVariable> optimizelyVariableMap = variation.getVariablesMap();
                        for (String variableKey : optimizelyVariableMap.keySet()) {
                            OptimizelyVariable variable = optimizelyVariableMap.get(variableKey);
                            Log.d("Optimizely", "[OptimizelyVariable]      -- variable = key: " + variableKey + ", value: " + variable.getValue());
                            // use variable data here...

                        }
                    }

                }

                for (OptimizelyExperiment delivery : flag.getDeliveryRules()) {
                    Log.d("Optimizely", "[OptimizelyExperiment]  -- Delivery Rule Key: " + delivery.getKey());
                    Log.d("Optimizely", "[OptimizelyExperiment]  -- Delivery Audiences: " + delivery.getAudiences());
                }

                // use experiments and other feature flag data here...

            }

            // listen to OPTIMIZELY_CONFIG_UPDATE to get updated data
            optimizelyClient.getNotificationCenter().addNotificationHandler(
                    UpdateConfigNotification.class,
                    handler -> {
                        OptimizelyConfig newConfig = optimizelyClient.getOptimizelyConfig();
                    });
        });
    }

    static public void samplesForDoc_InitializeSDK(Context context) {
        // -- sample starts here

        // Build a manager
        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .withDatafileDownloadInterval(15, TimeUnit.MINUTES)
                .withEventDispatchInterval(30, TimeUnit.SECONDS)
                .build(context);

        // Instantiate a client synchronously with a bundled datafile
        String datafile = "REPLACE_WITH_YOUR_DATAFILE";
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, datafile);

        // Or, instantiate it asynchronously with a callback
        optimizelyManager.initialize(context, null, (OptimizelyClient client) -> {
            // flag decision
            OptimizelyUserContext user = client.createUserContext("<User_ID>");
            OptimizelyDecision decision = user.decide("<Flag_Key>");
        });
    }

    static public void samplesForDoc_GetClient(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context);

        // -- sample starts here

        OptimizelyClient optimizelyClient = optimizelyManager.getOptimizely();
    }

    static public void samplesForDoc_DatafilePolling(Context context) {
        // -- sample starts here

        // Poll every 15 minutes
        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .withDatafileDownloadInterval(15, TimeUnit.MINUTES)
                .build(context);

        // datafile notifiation
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile);
        // -- sample starts here

        optimizelyClient.addUpdateConfigNotificationHandler(notification -> {
            Log.d("Optimizely", "got datafile change");
        });
    }

    static public void samplesForDoc_BundledDatafile(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context);

        // -- sample starts here

        // Initialize Optimizely asynchronously with a datafile.
        //  If it is not able to download a new datafile, it will
        //  initialize an OptimizelyClient with the one provided.

        optimizelyManager.initialize(context, R.raw.datafile, (OptimizelyClient optimizelyClient) -> {
            OptimizelyUserContext user = optimizelyClient.createUserContext("<User_ID>");
            OptimizelyDecision decision = user.decide("<Flag_Key>");
        });

        // Initialize Optimizely synchronously
        //  This will immediately instantiate and return an
        //  OptimizelyClient with the datafile that was passed in.
        //  It'll also download a new datafile from the CDN and
        //  persist it to local storage.
        //  The newly downloaded datafile will be used the next
        //  time the SDK is initialized.

        optimizelyManager.initialize(context, R.raw.datafile);
    }

    static public void samplesForDoc_ExampleUsage(Context context) {
        // Build a manager
        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .build(context);
        // Instantiate a client synchronously with a bundled datafile
        // copy datafile JSON from URL accessible in app>settings
        String datafile = "REPLACE_WITH_YOUR_DATAFILE";
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, datafile);

        // Create a user-context
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("logged_in", true);
        OptimizelyUserContext user = optimizelyClient.createUserContext("user123", attributes);

        // Call the decide method
        OptimizelyDecision decision = user.decide("product_sort");

        // did the decision fail with a critical error?
        String variationKey = decision.getVariationKey();
        if (variationKey == null) {
            List<String> reasons = decision.getReasons();
            Log.d("Optimizely", "decision error: " + reasons);
            return;
        }

        // execute code based on flag enabled state
        boolean enabled = decision.getEnabled();
        OptimizelyJSON variables = decision.getVariables();
        if (enabled) {
            String varStr = null;
            try {
                varStr = variables.getValue("sort_method", String.class);
            } catch (JsonParseException e) {
                e.printStackTrace();
            }
        }

        // or execute code based on flag variation:
        if (variationKey.equals("control")) {
            // Execute code for control variation
        } else if (variationKey.equals("treatment")) {
            // Execute code for treatment variation
        }

        // Track an event
        user.trackEvent("purchased");
    }

    static public void samplesForDoc_CreateUserContext(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context);
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile);

        // -- sample starts here

        // option 1: create a user, then set attributes

        OptimizelyUserContext user;
        user = optimizelyClient.createUserContext("user123");
        user.setAttribute("is_logged_in", false);
        user.setAttribute("app_version", "1.3.2");

        // option 2: pass attributes when creating the user

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("is_logged_in", false);
        attributes.put("app_version", "1.3.2");
        user = optimizelyClient.createUserContext("user123", attributes);
    }

    static public void samplesForDoc_DecideOptions(Context context) {
        // set global default decide options when initializing the client

        List<OptimizelyDecideOption> options;
        options = Arrays.asList(OptimizelyDecideOption.DISABLE_DECISION_EVENT);
        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("FCnSegiEkRry9rhVMroit4")
                .withDefaultDecideOptions(options)
                .build(context);

        // set additional options in a decide call

        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile);
        OptimizelyUserContext user = optimizelyClient.createUserContext("user123");
        options = Arrays.asList(OptimizelyDecideOption.DISABLE_DECISION_EVENT, OptimizelyDecideOption.DISABLE_DECISION_EVENT);

        Map<String, OptimizelyDecision> decisions = user.decideAll(options);
    }

    static public void samplesForDoc_Decide(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context);
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile);

        // -- sample starts here

        // create the user and decide which flag rule & variation they bucket into

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("logged_in", true);
        OptimizelyUserContext user = optimizelyClient.createUserContext("user123", attributes);

        OptimizelyDecision decision = user.decide("product_sort");

        // variation. if null, decision fail with a critical error
        String variationKey = decision.getVariationKey();

        // flag enabled state:
        boolean enabled = decision.getEnabled();

        // all variable values
        OptimizelyJSON variables = decision.getVariables();

        // String variable value
        String varStr = null;
        try {
            varStr = variables.getValue("sort_method", String.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        }

        // Boolean variable value
        Boolean varBool = (Boolean) variables.toMap().get("k_boolean");

        // flag key for which decision was made
        String flagKey = decision.getFlagKey();

        // user for which the decision was made
        OptimizelyUserContext userContext = decision.getUserContext();

        // reasons for the decision
        List<String> reasons = decision.getReasons();
    }

    static public void samplesForDoc_DecideAll(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context);
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("logged_in", true);
        OptimizelyUserContext user = optimizelyClient.createUserContext("user123", attributes);

        // -- sample starts here

        // make decisions for all active (unarchived) flags in the project for a user
        List<OptimizelyDecideOption> options = Arrays.asList(OptimizelyDecideOption.ENABLED_FLAGS_ONLY);
        Map<String, OptimizelyDecision> decisions = user.decideAll(options);

        Set<String> allKeys = decisions.keySet();
        OptimizelyDecision decisionForFlag1 = decisions.get("flag_1");
    }

    static public void samplesForDoc_DecideForKeys(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context);
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("logged_in", true);
        OptimizelyUserContext user = optimizelyClient.createUserContext("user123", attributes);

        // -- sample starts here

        // make decisions for specific flags
        List<String> keys = Arrays.asList("flag-1", "flag-2");
        Map<String, OptimizelyDecision> decisions = user.decideForKeys(keys);

        OptimizelyDecision decision1 = decisions.get("flag-1");
        OptimizelyDecision decision2 = decisions.get("flag-2");
    }

    static public void samplesForDoc_TrackEvent(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context);
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile);

        // -- sample starts here

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("logged_in", true);
        OptimizelyUserContext user = optimizelyClient.createUserContext("user123", attributes);

        Map<String, Object> tags = new HashMap<>();
        tags.put("category", "shoes");
        tags.put("purchase_count", 2);

        user.trackEvent("my_purchase_event_key", tags);
    }

    static public void samplesForDoc_OptimizelyJSON(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context);
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile);
        OptimizelyUserContext user = optimizelyClient.createUserContext("user123");
        OptimizelyDecision decision = user.decide("product_sort");
        OptimizelyJSON optlyJSON = decision.getVariables();

        // -- sample starts here

        //declare a schema object into which you want to unmarshal OptimizelyJson content:
        class SSub {
            String field;
        }

        class SObj {
            int field1;
            double field2;
            String field3;
            SSub field4;
        }

        try {
            //parse all json key/value pairs into your schema, sObj
            SObj robj = optlyJSON.getValue(null, SObj.class);

            //or, parse the specified key/value pair with an integer value
            Integer rint = optlyJSON.getValue("field1", Integer.class);

            //or, parse the specified key/value pair with a string value
            String rstr = optlyJSON.getValue("field4.field", String.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        }
    }

    static public void samplesForDoc_CustomUserProfileService(Context context) {
        // -- sample starts here

        class CustomUserProfileService implements UserProfileService {

            public Map<String, Object> lookup(String userId) throws Exception {
                return null;
            }

            public void save(Map<String, Object> userProfile) throws Exception {
            }

        }

        CustomUserProfileService customUserProfileService = new CustomUserProfileService();
        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .withUserProfileService(customUserProfileService)
                .build(context);
    }

    static public void samplesForDoc_EventDispatcher(Context context) {
        // -- sample starts here

        // Using an anonymous class here to implement the EventHandler interface.
        // Feel free to create an explicit class that implements the interface instead.
        EventHandler eventHandler = new EventHandler() {
            @Override
            public void dispatchEvent(LogEvent logEvent) throws Exception {
                // Send event to our log endpoint as documented in
                // https://developers.optimizely.com/x/events/api/index.html
            }
        };

        // Build a manager
        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .withEventDispatchInterval(60, TimeUnit.SECONDS)
                .withEventHandler(eventHandler)
                .build(context);


        // With the new Android O differences, you need to register the
        // service for the intent filter you desire in code instead of in the manifest.
        EventRescheduler eventRescheduler = new EventRescheduler();

        context.registerReceiver(eventRescheduler,
                new IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION));
    }

    static public void samplesForDoc_EventBatchingDefault(Context context) {
        // -- sample starts here

        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .build(context);
        OptimizelyClient optimizely = optimizelyManager.initialize(context, R.raw.datafile);
    }

    static public void samplesForDoc_EventBatchingAdvanced(Context context) {
        // -- sample starts here

        EventHandler eventHandler = DefaultEventHandler.getInstance(context);

        // Here we are using the builder options to set batch size
        // to 5 events and flush interval to a minute.
        BatchEventProcessor batchProcessor = BatchEventProcessor.builder()
                .withBatchSize(5)
                .withEventHandler(eventHandler)
                .withFlushInterval(TimeUnit.MINUTES.toMillis(1L))
                .build();

        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .withEventHandler(eventHandler)
                .withDatafileDownloadInterval(15, TimeUnit.MINUTES)
                .withEventProcessor(batchProcessor)
                .build(context);

        OptimizelyClient optimizely = optimizelyManager.initialize(context, R.raw.datafile);

        // log event
        // -- sample starts here

        optimizely.addLogEventNotificationHandler(logEvent -> {
            Log.d("Optimizely", "event dispatched: " + logEvent);
        });
    }

    static public void samplesForDoc_ErrorHandler(Context context) {
        // -- sample starts here

        // Error handler that raises exceptions
        ErrorHandler errorHandler = new RaiseExceptionErrorHandler();

        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .withErrorHandler(errorHandler)
                .withDatafileDownloadInterval(15, TimeUnit.MINUTES)
                .build(context);
    }

    static public void samplesForDoc_AudienceAttributes(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context);
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile);

        // -- sample starts here

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("device", "iPhone");
        attributes.put("lifetime", 24738388);
        attributes.put("is_logged_in", true);
        attributes.put("application_version", "4.3.0-beta");

        OptimizelyUserContext user = optimizelyClient.createUserContext("user123", attributes);
        OptimizelyDecision decision = user.decide("<Flag_Key>");
    }

    static public void samplesForDoc_NotificatonListener(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context);
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile);

        // -- sample starts here

        // Add Notification Listener (LogEvent)
        int notificationId = optimizelyClient.addLogEventNotificationHandler(logEvent -> {
            Log.d("Optimizely", "event dispatched: " + logEvent);
        });

        // Remove Notification Listener
        optimizelyClient.getNotificationCenter().removeNotificationListener(notificationId);

        // Remove all Notification Listeners
        optimizelyClient.getNotificationCenter().clearAllNotificationListeners();

        // Remove all Notification Listeners of a certain type
        optimizelyClient.getNotificationCenter().clearNotificationListeners(DecisionNotification.class);


        // notification listener types
        // -- sample starts here

        // SET UP DECISION NOTIFICATION LISTENER
        int notificationId1 = optimizelyClient.addDecisionNotificationHandler(notification -> {
            // Access type on decisionObject to get type of decision
            String decisionType = notification.getType();
            if (decisionType == "flag") {
                Map<String, ?> flagDecisionInfo = notification.getDecisionInfo();
                String flagKey = (String) flagDecisionInfo.get("flagKey");
                Boolean enabled = (Boolean) flagDecisionInfo.get("enabled");
                Boolean decisionEventDispatched = (Boolean) flagDecisionInfo.get("decisionEventDispatched");
                // Send data to analytics provider here
            }
        });

        // SET UP LOG EVENT NOTIFICATION LISTENER
        int notificationId2 = optimizelyClient.addLogEventNotificationHandler(notification -> {
            // process the logEvent object here (send to analytics provider, audit/inspect data)
        });

        // SET UP OPTIMIZELY CONFIG NOTIFICATION LISTENER
        int notificationId3 = optimizelyClient.addUpdateConfigNotificationHandler(notification -> {
            OptimizelyConfig optimizelyConfig = optimizelyClient.getOptimizelyConfig();
        });

        // SET UP TRACK LISTENER
        int notificationId4 = optimizelyClient.addTrackNotificationHandler(notification -> {
            // process the event here (send to analytics provider, audit/inspect data)
        });
    }

    static public void samplesForDoc_OlderVersions(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context);
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile);

        // -- sample starts here

        // Prereq for new methods: create a user
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("is_logged_in", true);
        OptimizelyUserContext user = optimizelyClient.createUserContext("user123", attributes);

        // Is Feature Enabled

        // old method
        boolean enabled = optimizelyClient.isFeatureEnabled("flag_1", "user123", attributes);
        // new method
        OptimizelyDecision decision = user.decide("flag_1");
        enabled = decision.getEnabled();

        // Activate & Get Variation

        // old method
        Variation variation = optimizelyClient.activate("experiment_1", "user123", attributes);
        // new method
        String variationKey = decision.getVariationKey();

        // Get All Feature Variables

        // old method
        OptimizelyJSON json = optimizelyClient.getAllFeatureVariables("flag_1", "user123", attributes);
        // new method
        json = decision.getVariables();

        // Get Enabled Features

        // old method
        List<String> enabledFlags = optimizelyClient.getEnabledFeatures("user123", attributes);
        // new method
        List<OptimizelyDecideOption> options = Arrays.asList(OptimizelyDecideOption.ENABLED_FLAGS_ONLY);
        Map<String, OptimizelyDecision> decisions = user.decideAll(options);
        Set<String> enabledFlagsSet = decisions.keySet();

        // Track

        // old method
        Map<String, Object> tags = new HashMap<>();
        attributes.put("purchase_count", 2);
        optimizelyClient.track("my_purchase_event_key", "user123", attributes, tags);
        // new method
        user.trackEvent("my_purchase_event_key", tags);
    }

    static public void samplesForDoc_ForcedDecision(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context);
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile);

        // -- sample starts here

        // Create the OptimizelyUserContext, passing in the UserId and Attributes
        OptimizelyUserContext user = optimizelyClient.createUserContext("user-id");

        OptimizelyDecisionContext flagContext = new OptimizelyDecisionContext("flag-1", null);
        OptimizelyDecisionContext flagAndABTestContext = new OptimizelyDecisionContext("flag-1", "exp-1");
        OptimizelyDecisionContext flagAndDeliveryRuleContext = new OptimizelyDecisionContext("flag-1", "delivery-1");
        OptimizelyForcedDecision variationAForcedDecision = new OptimizelyForcedDecision("variation-a");
        OptimizelyForcedDecision variationBForcedDecision = new OptimizelyForcedDecision("variation-b");
        OptimizelyForcedDecision variationOnForcedDecision = new OptimizelyForcedDecision("on");

        // set a forced decision for a flag
        Boolean success = user.setForcedDecision(flagContext, variationAForcedDecision);
        OptimizelyDecision decision = user.decide("flag-1");

        // set a forced decision for an ab-test rule
        success = user.setForcedDecision(flagAndABTestContext, variationBForcedDecision);
        decision = user.decide("flag-1");

        // set a forced variation for a delivery rule
        success = user.setForcedDecision(flagAndDeliveryRuleContext, variationOnForcedDecision);
        decision = user.decide("flag-1");

        // get forced variations
        OptimizelyForcedDecision forcedDecision = user.getForcedDecision(flagContext);
        Log.d("Optimizely", "[ForcedDecision] variationKey = " + forcedDecision.getVariationKey());

        // remove forced variations
        success = user.removeForcedDecision(flagAndABTestContext);
        success = user.removeAllForcedDecisions();
    }

    static public void samplesForDoc_ODP_async(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("VivZyCGPHY369D4z8T9yG").build(context);
        optimizelyManager.initialize(context, null, (OptimizelyClient client) -> {
            OptimizelyUserContext userContext = client.createUserContext("user_123");
            userContext.fetchQualifiedSegments((status) -> {
                Log.d("Optimizely", "[ODP] segments = " + userContext.getQualifiedSegments());
                OptimizelyDecision optDecision = userContext.decide("odp-flag-1");
                Log.d("Optimizely", "[ODP] decision = " + optDecision.toString());
            });
        });
    }

    static public void samplesForDoc_ODP_sync(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("VivZyCGPHY369D4z8T9yG").build(context);

        boolean returnInMainThread = false;

        optimizelyManager.initialize(context, null, returnInMainThread, (OptimizelyClient client) -> {
            OptimizelyUserContext userContext = client.createUserContext("user_123");
            userContext.fetchQualifiedSegments();

            Log.d("Optimizely", "[ODP] segments = " + userContext.getQualifiedSegments());
            OptimizelyDecision optDecision = userContext.decide("odp-flag-1");
            Log.d("Optimizely", "[ODP] decision = " + optDecision.toString());
        });
    }

}
