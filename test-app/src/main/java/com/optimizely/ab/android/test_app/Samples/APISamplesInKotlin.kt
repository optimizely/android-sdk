/****************************************************************************
 * Copyright 2022, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.android.test_app

import android.content.Context
import android.util.Log
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption
import com.optimizely.ab.android.sdk.OptimizelyManager
import com.optimizely.ab.android.test_app.R
import com.optimizely.ab.android.sdk.OptimizelyStartListener
import com.optimizely.ab.android.sdk.OptimizelyClient
import com.optimizely.ab.OptimizelyUserContext
import com.optimizely.ab.optimizelydecision.OptimizelyDecision
import com.optimizely.ab.optimizelyjson.OptimizelyJSON
import com.optimizely.ab.config.Variation
import com.optimizely.ab.config.parser.JsonParseException
import com.optimizely.ab.notification.NotificationHandler
import com.optimizely.ab.optimizelyconfig.OptimizelyConfig
import com.optimizely.ab.optimizelyconfig.OptimizelyAttribute
import com.optimizely.ab.optimizelyconfig.OptimizelyAudience
import com.optimizely.ab.optimizelyconfig.OptimizelyEvent
import com.optimizely.ab.optimizelyconfig.OptimizelyFeature
import com.optimizely.ab.optimizelyconfig.OptimizelyExperiment
import com.optimizely.ab.optimizelyconfig.OptimizelyVariation
import com.optimizely.ab.optimizelyconfig.OptimizelyVariable
import com.optimizely.ab.notification.UpdateConfigNotification
import java.util.*
import java.util.concurrent.TimeUnit

object APISamplesInKotlin {
    fun samplesForDecide(context: Context?) {
        val defaultDecideOptions = Arrays.asList(OptimizelyDecideOption.DISABLE_DECISION_EVENT)
        val optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("FCnSegiEkRry9rhVMroit4")
                .withDefaultDecideOptions(defaultDecideOptions)
                .build(context)
        optimizelyManager.initialize(context!!, R.raw.datafile) { optimizelyClient: OptimizelyClient ->

            // createUserContext
            val userId = "user_123"
            val attributes: MutableMap<String, Any> = HashMap()
            attributes["is_logged_in"] = false
            attributes["app_version"] = "1.3.2"
            val user = optimizelyClient.createUserContext(userId, attributes)
            // attributes can be set in this way too
            user!!.setAttribute("location", "NY")

            // decide
            val options = Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS)
            val decision = user.decide("show_coupon", options)
            // or can be called without options
            //OptimizelyDecision decision = user.decide("show_coupon");
            val variationKey = decision.variationKey
            val enabled = decision.enabled
            val variables = decision.variables
            var vs: String? = null
            try {
                vs = variables.getValue("text_color", String::class.java)
                val a = variables.getValue("price", Int::class.java)!!
            } catch (e: JsonParseException) {
                e.printStackTrace()
            }
            val vb = variables.toMap()!!["discount"] as Int
            val ruleKey = decision.ruleKey
            val flagKey = decision.flagKey
            val userContext = decision.userContext
            val reasons = decision.reasons
            Log.d("Samples", "decision: $decision")
            Log.d("Samples", "items: $variationKey $enabled $vs $vb $ruleKey $flagKey $userContext $reasons")

            // decideForKeys
            val keys = Arrays.asList("show_coupon", "bg-feature")
            val decisionsMultiple = user.decideForKeys(keys)
            val decision1 = decisionsMultiple[keys[0]]
            val decision2 = decisionsMultiple[keys[1]]
            Log.d("Samples", "decisionsMultiple: $keys $decision1 $decision2")

            // decideAll
            val options2 = Arrays.asList(OptimizelyDecideOption.ENABLED_FLAGS_ONLY)
            val decisionsAll = user.decideAll(options2)
            val allKeys: Set<String> = decisionsAll.keys
            val allDecisions: Collection<OptimizelyDecision> = decisionsAll.values
            Log.d("Samples", "all keys: $allKeys")
            Log.d("Samples", "all decisions: $allDecisions")

            // trackEvent
            user.trackEvent("sample_conversion")
        }
    }

    fun samplesForInitialization(context: Context?) {
        var optimizelyManager: OptimizelyManager
        var optimizelyClient: OptimizelyClient
        var variation: Variation?

        // These are sample codes for synchronous and asynchronous SDK initializations with multiple options

        // [Synchronous]

        // [S1] Synchronous initialization
        //      1. SDK is initialized instantly with a cached (or bundled) datafile
        //      2. A new datafile can be downloaded in background and cached after the SDK is initialized.
        //         The cached datafile will be used only when the SDK re-starts in the next session.
        optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .build(context)
        optimizelyClient = optimizelyManager.initialize(context!!, R.raw.datafile)
        variation = optimizelyClient.activate("<Experiment_Key>", "<User_ID>")

        // [S2] Synchronous initialization
        //      1. SDK is initialized instantly with a cached (or bundled) datafile
        //      2. A new datafile can be downloaded in background and cached after the SDK is initialized.
        //         The cached datafile is used immediately to update the SDK project config.
        optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .build(context)
        optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile, true, true)
        variation = optimizelyClient.activate("<Experiment_Key>", "<User_ID>")

        // [S3] Synchronous initialization
        //      1. SDK is initialized instantly with a cached (or bundled) datafile
        //      2. A new datafile can be downloaded in background and cached after the SDK is initialized.
        //         The cached datafile is used immediately to update the SDK project config.
        //      3. Polling datafile periodically.
        //         The cached datafile is used immediately to update the SDK project config.
        optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .withDatafileDownloadInterval(TimeUnit.MINUTES.toSeconds(15))
                .build(context)
        optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile, true, true)
        variation = optimizelyClient.activate("<Experiment_Key>", "<User_ID>")

        // [Asynchronous]

        // [A1] Asynchronous initialization
        //      1. A datafile is downloaded from the server and the SDK is initialized with the datafile
        optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .build(context)
        optimizelyManager.initialize(context, null) { client: OptimizelyClient -> val variation2 = client.activate("<Experiment_Key>", "<User_ID>") }

        // [A2] Asynchronous initialization
        //      1. A datafile is downloaded from the server and the SDK is initialized with the datafile
        //      2. Polling datafile periodically.
        //         The cached datafile is used immediately to update the SDK project config.
        optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .withDatafileDownloadInterval(TimeUnit.MINUTES.toSeconds(15))
                .build(context)
        optimizelyManager.initialize(context, null) { client: OptimizelyClient -> val variation2 = client.activate("<Experiment_Key>", "<User_ID>") }
    }

    fun samplesForOptimizelyConfig(context: Context?) {
        val optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("FCnSegiEkRry9rhVMroit4")
                .build(context)
        optimizelyManager.initialize(context!!, R.raw.datafile) { optimizelyClient: OptimizelyClient ->
            val config = optimizelyClient.optimizelyConfig
            println("[OptimizelyConfig] revision = " + config!!.revision)
            println("[OptimizelyConfig] sdkKey = " + config.sdkKey)
            println("[OptimizelyConfig] environmentKey = " + config.environmentKey)
            println("[OptimizelyConfig] attributes:")
            for (attribute in config.attributes) {
                println("[OptimizelyAttribute]  -- (id, key) = " + attribute.id + ", " + attribute.key)
            }
            println("[OptimizelyConfig] audiences:")
            for (audience in config.audiences) {
                println("[OptimizelyAudience]  -- (id, name, conditions) = " + audience.id + ", " + audience.name + ", " + audience.conditions)
            }
            println("[OptimizelyConfig] events:")
            for (event in config.events) {
                println("[OptimizelyEvent]  -- (id, key, experimentIds) = " + event.id + ", " + event.key + ", " + Arrays.toString(event.experimentIds.toTypedArray()))
            }

            // all features
            for (flagKey in config.featuresMap.keys) {
                val flag = config.featuresMap[flagKey]
                for (experiment in flag!!.experimentRules) {
                    println("[OptimizelyExperiment]  -- Experiment Rule Key: " + experiment.key)
                    println("[OptimizelyExperiment]  -- Experiment Audiences: " + experiment.audiences)
                    val variationsMap = experiment.variationsMap
                    for (variationKey in variationsMap.keys) {
                        val variation = variationsMap[variationKey]
                        println("[OptimizelyVariation]    -- variation = { key: " + variationKey + ", id: " + variation!!.id + ", featureEnabled: " + variation.featureEnabled + " }")
                        // use variation data here...
                        val optimizelyVariableMap = variation.variablesMap
                        for (variableKey in optimizelyVariableMap.keys) {
                            val variable = optimizelyVariableMap[variableKey]
                            println("[OptimizelyVariable]      -- variable = key: " + variableKey + ", value: " + variable!!.value)
                            // use variable data here...
                        }
                    }
                }
                for (delivery in flag.deliveryRules) {
                    println("[OptimizelyExperiment]  -- Delivery Rule Key: " + delivery.key)
                    println("[OptimizelyExperiment]  -- Delivery Audiences: " + delivery.audiences)
                }
                val experimentsMap = flag.experimentsMap
                // feature flag experiments
                val experimentKeys: Set<String> = experimentsMap.keys

                // use experiments and other feature flag data here...
            }

            // listen to OPTIMIZELY_CONFIG_UPDATE to get updated data
            optimizelyClient.notificationCenter!!.addNotificationHandler(UpdateConfigNotification::class.java, NotificationHandler { handler: UpdateConfigNotification? -> val newConfig = optimizelyClient.optimizelyConfig })
        }
    }
}