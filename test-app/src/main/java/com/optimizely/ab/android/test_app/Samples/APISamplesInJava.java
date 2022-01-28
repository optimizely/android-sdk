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

package com.optimizely.ab.android.test_app.Samples;

import android.content.Context;
import android.util.Log;
import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.test_app.R;
import com.optimizely.ab.config.parser.JsonParseException;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class APISamplesInJava {

    static public void samplesAll(Context context) {
//        samplesForDecide(context);
//        samplesForInitialization(context);
//        samplesForOptimizelyConfig(context);
//        samplesForDoc_InitializeSDK(context);
//        samplesForDoc_GetClient(context);
//        samplesForDoc_DatafilePolling(context);
//        samplesForDoc_BundledDatafile(context);
//        samplesForDoc_ExampleUsage(context);
//        samplesForDoc_CreateUserContext(context);
//        samplesForDoc_DecideOptions(context);
//        samplesForDoc_Decide(context);
//        samplesForDoc_DecideAll(context);
//        samplesForDoc_DecideForKeys(context);
        samplesForDoc_TrackEvent(context);
    }

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
            // or can be called without options
            //OptimizelyDecision decision = user.decide("show_coupon");

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

            OptimizelyConfig config = optimizelyClient.getOptimizelyConfig();

            System.out.println("[OptimizelyConfig] revision = " + config.getRevision());
            System.out.println("[OptimizelyConfig] sdkKey = " + config.getSdkKey());
            System.out.println("[OptimizelyConfig] environmentKey = " + config.getEnvironmentKey());

            System.out.println("[OptimizelyConfig] attributes:");
            for (OptimizelyAttribute attribute : config.getAttributes()) {
                System.out.println("[OptimizelyAttribute]  -- (id, key) = " + attribute.getId() + ", " + attribute.getKey());
            }

            System.out.println("[OptimizelyConfig] audiences:");
            for (OptimizelyAudience audience : config.getAudiences()) {
                System.out.println("[OptimizelyAudience]  -- (id, name, conditions) = " + audience.getId() + ", " + audience.getName() + ", " + audience.getConditions());
            }

            System.out.println("[OptimizelyConfig] events:");
            for (OptimizelyEvent event : config.getEvents()) {
                System.out.println("[OptimizelyEvent]  -- (id, key, experimentIds) = " + event.getId() + ", " + event.getKey() + ", " + Arrays.toString(event.getExperimentIds().toArray()));
            }

            // all features
            for (String flagKey : config.getFeaturesMap().keySet()) {
                OptimizelyFeature flag = config.getFeaturesMap().get(flagKey);

                for (OptimizelyExperiment experiment : flag.getExperimentRules()) {
                    System.out.println("[OptimizelyExperiment]  -- Experiment Rule Key: " + experiment.getKey());
                    System.out.println("[OptimizelyExperiment]  -- Experiment Audiences: " + experiment.getAudiences());

                    Map<String, OptimizelyVariation> variationsMap = experiment.getVariationsMap();
                    for (String variationKey : variationsMap.keySet()) {
                        OptimizelyVariation variation = variationsMap.get(variationKey);
                        System.out.println("[OptimizelyVariation]    -- variation = { key: " + variationKey + ", id: " + variation.getId() + ", featureEnabled: " + variation.getFeatureEnabled() + " }");
                        // use variation data here...

                        Map<String, OptimizelyVariable> optimizelyVariableMap = variation.getVariablesMap();
                        for (String variableKey : optimizelyVariableMap.keySet()) {
                            OptimizelyVariable variable = optimizelyVariableMap.get(variableKey);
                            System.out.println("[OptimizelyVariable]      -- variable = key: " + variableKey + ", value: " + variable.getValue());
                            // use variable data here...

                        }
                    }

                }

                for (OptimizelyExperiment delivery : flag.getDeliveryRules()) {
                    System.out.println("[OptimizelyExperiment]  -- Delivery Rule Key: " + delivery.getKey());
                    System.out.println("[OptimizelyExperiment]  -- Delivery Audiences: " + delivery.getAudiences());
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
                .withSDKKey("SDK_KEY_HERE")
                .withDatafileDownloadInterval(15, TimeUnit.MINUTES)
                .withEventDispatchInterval(30, TimeUnit.SECONDS)
                .build(context);

        // Instantiate a client synchronously with a bundled datafile
        String datafile = "REPLACE_WITH_YOUR_DATAFILE";
        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, datafile);

        // Or, instantiate it asynchronously with a callback
        optimizelyManager.initialize(context, null, (OptimizelyClient client) -> {
            // flag decision
            OptimizelyUserContext user = client.createUserContext("USER_ID_HERE");
            OptimizelyDecision decision = user.decide("FLAG_KEY_HERE");
        });
    }

    static public void samplesForDoc_GetClient(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("SDK_KEY_HERE").build(context);

        // -- sample starts here

        OptimizelyClient optimizelyClient = optimizelyManager.getOptimizely();
    }

    static public void samplesForDoc_DatafilePolling(Context context) {
        // -- sample starts here

        // Poll every 15 minutes
        OptimizelyManager optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("SDK_KEY_HERE")
                .withDatafileDownloadInterval(15, TimeUnit.MINUTES)
                .build(context);

        OptimizelyClient optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile);
        optimizelyClient.getNotificationCenter().addNotificationHandler(UpdateConfigNotification.class, (UpdateConfigNotification notification) -> {
            System.out.println("got datafile change");
        });
    }

    static public void samplesForDoc_BundledDatafile(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("SDK_KEY_HERE").build(context);

        // -- sample starts here

        // Initialize Optimizely asynchronously with a datafile.
        //  If it is not able to download a new datafile, it will
        //  initialize an OptimizelyClient with the one provided.

        optimizelyManager.initialize(context, R.raw.datafile, (OptimizelyClient optimizelyClient) -> {
            OptimizelyUserContext user = optimizelyClient.createUserContext("USER_ID_HERE");
            OptimizelyDecision decision = user.decide("FLAG_KEY_HERE");
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
                .withSDKKey("SDK_KEY_HERE")
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
            System.out.println("decision error: " + reasons);
            return;
        }

        // execute code based on flag enabled state
        boolean enabled = decision.getEnabled();
        OptimizelyJSON variables = decision.getVariables();
        if (enabled) {
            String vs = null;
            try {
                vs = variables.getValue("sort_method", String.class);
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
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("SDK_KEY_HERE").build(context);
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
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("SDK_KEY_HERE").build(context);
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
        String vs = null;
        try {
            vs = variables.getValue("sort_method", String.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        }

        // Boolean variable value
        Boolean vb = (Boolean) variables.toMap().get("k_boolean");

        // flag key for which decision was made
        String flagKey = decision.getFlagKey();

        // user for which the decision was made
        OptimizelyUserContext userContext = decision.getUserContext();

        // reasons for the decision
        List<String> reasons = decision.getReasons();
    }

    static public void samplesForDoc_DecideAll(Context context) {
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("SDK_KEY_HERE").build(context);
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
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("SDK_KEY_HERE").build(context);
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
        OptimizelyManager optimizelyManager = OptimizelyManager.builder().withSDKKey("SDK_KEY_HERE").build(context);
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

}