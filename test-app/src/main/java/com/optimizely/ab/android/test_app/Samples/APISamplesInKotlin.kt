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
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.optimizely.ab.OptimizelyUserContext
import com.optimizely.ab.android.event_handler.EventRescheduler
import com.optimizely.ab.android.sdk.OptimizelyClient
import com.optimizely.ab.android.sdk.OptimizelyManager
import com.optimizely.ab.bucketing.UserProfileService
import com.optimizely.ab.config.parser.JsonParseException
import com.optimizely.ab.event.EventHandler
import com.optimizely.ab.event.LogEvent
import com.optimizely.ab.notification.NotificationHandler
import com.optimizely.ab.notification.UpdateConfigNotification
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption
import com.optimizely.ab.optimizelydecision.OptimizelyDecision
import com.optimizely.ab.optimizelyjson.OptimizelyJSON
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

object APISamplesInKotlin {

    fun samplesAll(context: Context) {
//        samplesForDecide(context)
//        samplesForInitialization(context)
//        samplesForOptimizelyConfig(context)
//        samplesForDoc_InitializeSDK(context)
//        samplesForDoc_GetClient(context)
//        samplesForDoc_DatafilePolling(context)
//        samplesForDoc_BundledDatafile(context)
//        samplesForDoc_ExampleUsage(context)
//        samplesForDoc_CreateUserContext(context)
//        samplesForDoc_DecideOptions(context)
//        samplesForDoc_Decide(context)
//        samplesForDoc_DecideAll(context)
//        samplesForDoc_DecideForKeys(context)
//        samplesForDoc_TrackEvent(context)
//        samplesForDoc_OptimizelyJSON(context)
//        samplesForDoc_CustomUserProfileService(context)
        samplesForDoc_EventDispatcher(context)

    }

    fun samplesForDecide(context: Context) {
        val defaultDecideOptions = Arrays.asList(OptimizelyDecideOption.DISABLE_DECISION_EVENT)
        val optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("FCnSegiEkRry9rhVMroit4")
                .withDefaultDecideOptions(defaultDecideOptions)
                .build(context)
        optimizelyManager.initialize(context, R.raw.datafile) { optimizelyClient: OptimizelyClient ->

            // createUserContext

            val userId = "user_123"
            val attributes: MutableMap<String, Any> = HashMap()
            attributes["is_logged_in"] = false
            attributes["app_version"] = "1.3.2"
            val user = optimizelyClient.createUserContext(userId, attributes)
            if (user == null) return@initialize
            // attributes can be set in this way too
            user.setAttribute("location", "NY")

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
            } catch (e: JsonParseException) {
                e.printStackTrace()
            }
            val vb = variables.toMap()?.get("discount") as Int
            val ruleKey = decision.ruleKey
            val flagKey = decision.flagKey
            val userContext = decision.userContext
            val reasons = decision.reasons

            Log.d("Samples", "decision: $decision")
            Log.d("Samples", "items: $variationKey $enabled $vs $vb $ruleKey $flagKey $userContext $reasons")

            // decideForKeys

            val keys = Arrays.asList("show_coupon", "bg-feature")
            val decisionsMultiple = user.decideForKeys(keys)
            val decision1 = decisionsMultiple.get(keys[0])
            val decision2 = decisionsMultiple.get(keys[1])
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

    fun samplesForInitialization(context: Context) {
        var optimizelyManager: OptimizelyManager
        var optimizelyClient: OptimizelyClient
        var user: OptimizelyUserContext
        var decision: OptimizelyDecision

        // Here are more sample codes for synchronous and asynchronous SDK initializations with multiple options

        // [Synchronous]

        // [S1] Synchronous initialization
        //      1. SDK is initialized instantly with a cached (or bundled) datafile
        //      2. A new datafile can be downloaded in background and cached after the SDK is initialized.
        //         The cached datafile will be used only when the SDK re-starts in the next session.
        optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .build(context)
        optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile)
        user = optimizelyClient.createUserContext("<User_ID>")!!
        decision = user.decide("<Flag_Key>")

        // [S2] Synchronous initialization
        //      1. SDK is initialized instantly with a cached (or bundled) datafile
        //      2. A new datafile can be downloaded in background and cached after the SDK is initialized.
        //         The cached datafile is used immediately to update the SDK project config.
        optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .build(context)
        optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile, true, true)
        user = optimizelyClient.createUserContext("<User_ID>")!!
        decision = user.decide("<Flag_Key>")

        // [S3] Synchronous initialization
        //      1. SDK is initialized instantly with a cached (or bundled) datafile
        //      2. A new datafile can be downloaded in background and cached after the SDK is initialized.
        //         The cached datafile is used immediately to update the SDK project config.
        //      3. Polling datafile periodically.
        //         The cached datafile is used immediately to update the SDK project config.
        optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .withDatafileDownloadInterval(15, TimeUnit.MINUTES)
                .build(context)
        optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile, true, true)
        user = optimizelyClient.createUserContext("<User_ID>")!!
        decision = user.decide("<Flag_Key>")

        // [Asynchronous]

        // [A1] Asynchronous initialization
        //      1. A datafile is downloaded from the server and the SDK is initialized with the datafile
        optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .build(context)
        optimizelyManager.initialize(context, null) { client: OptimizelyClient ->
            val user = client.createUserContext("<User_ID>")!!
            val decision = user.decide("<Flag_Key>")
        }


        // [A2] Asynchronous initialization
        //      1. A datafile is downloaded from the server and the SDK is initialized with the datafile
        //      2. Polling datafile periodically.
        //         The cached datafile is used immediately to update the SDK project config.
        optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .withDatafileDownloadInterval(15, TimeUnit.MINUTES)
                .build(context)
        optimizelyManager.initialize(context, null) { client: OptimizelyClient ->
            val user = client.createUserContext("<User_ID>")!!
            val decision = user.decide("<Flag_Key>")
        }
    }

    fun samplesForOptimizelyConfig(context: Context) {
        val optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("FCnSegiEkRry9rhVMroit4")
                .build(context)
        optimizelyManager.initialize(context, R.raw.datafile) { optimizelyClient: OptimizelyClient ->

            // -- sample starts here

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
                val flag = config.featuresMap.get(flagKey)!!

                for (experiment in flag.experimentRules) {
                    println("[OptimizelyExperiment]  -- Experiment Rule Key: " + experiment.key)
                    println("[OptimizelyExperiment]  -- Experiment Audiences: " + experiment.audiences)

                    val variationsMap = experiment.variationsMap
                    for (variationKey in variationsMap.keys) {
                        val variation = variationsMap.get(variationKey)!!
                        println("[OptimizelyVariation]    -- variation = { key: " + variationKey + ", id: " + variation.id + ", featureEnabled: " + variation.featureEnabled + " }")
                        // use variation data here...

                        val optimizelyVariableMap = variation.variablesMap
                        for (variableKey in optimizelyVariableMap.keys) {
                            val variable = optimizelyVariableMap.get(variableKey)!!
                            println("[OptimizelyVariable]      -- variable = key: " + variableKey + ", value: " + variable.value)
                            // use variable data here...

                        }
                    }
                }

                for (delivery in flag.deliveryRules) {
                    println("[OptimizelyExperiment]  -- Delivery Rule Key: " + delivery.key)
                    println("[OptimizelyExperiment]  -- Delivery Audiences: " + delivery.audiences)
                }

                // use experiments and other feature flag data here...

            }

            // listen to OPTIMIZELY_CONFIG_UPDATE to get updated data
            optimizelyClient.notificationCenter?.addNotificationHandler(
                    UpdateConfigNotification::class.java,
                    NotificationHandler { handler: UpdateConfigNotification? ->
                        val newConfig = optimizelyClient.optimizelyConfig
                    }
            )
        }
    }

    fun samplesForDoc_InitializeSDK(context: Context) {
        // -- sample starts here

        // Build a manager
        val optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("SDK_KEY_HERE")
                .withDatafileDownloadInterval(15, TimeUnit.MINUTES)
                .withEventDispatchInterval(30, TimeUnit.SECONDS)
                .build(context)

        // Instantiate a client synchronously with a bundled datafile
        val datafile = "REPLACE_WITH_YOUR_DATAFILE"
        val optimizelyClient = optimizelyManager.initialize(context, datafile)

        // Or, instantiate it asynchronously with a callback
        optimizelyManager.initialize(context, null) { client: OptimizelyClient ->
            // flag decision
            val user = client.createUserContext("USER_ID_HERE")!!
            val decision = user.decide("FLAG_KEY_HERE")
        }
    }

    fun samplesForDoc_GetClient(context: Context) {
        val optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context)

        // -- sample starts here
        val optimizelyClient = optimizelyManager.optimizely
    }

    fun samplesForDoc_DatafilePolling(context: Context) {
        // -- sample starts here

        // Poll every 15 minutes
        val optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("SDK_KEY_HERE")
                .withDatafileDownloadInterval(15, TimeUnit.MINUTES)
                .build(context)

        val optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile)
        optimizelyClient.notificationCenter?.addNotificationHandler(UpdateConfigNotification::class.java) { notification: UpdateConfigNotification? -> println("got datafile change") }
    }

    fun samplesForDoc_BundledDatafile(context: Context) {
        val optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context)

        // -- sample starts here

        // Initialize Optimizely asynchronously with a datafile.
        //  If it is not able to download a new datafile, it will
        //  initialize an OptimizelyClient with the one provided.

        optimizelyManager.initialize(context, R.raw.datafile) { optimizelyClient: OptimizelyClient ->
            val user = optimizelyClient.createUserContext("USER_ID_HERE")!!
            val decision = user.decide("FLAG_KEY_HERE")
        }

        // Initialize Optimizely synchronously
        //  This will immediately instantiate and return an
        //  OptimizelyClient with the datafile that was passed in.
        //  It'll also download a new datafile from the CDN and
        //  persist it to local storage.
        //  The newly downloaded datafile will be used the next
        //  time the SDK is initialized.

        optimizelyManager.initialize(context, R.raw.datafile)
    }

    fun samplesForDoc_ExampleUsage(context: Context) {
        // Build a manager
        val optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("SDK_KEY_HERE")
                .build(context)
        // Instantiate a client synchronously with a bundled datafile
        // copy datafile JSON from URL accessible in app>settings
        val datafile = "REPLACE_WITH_YOUR_DATAFILE"
        val optimizelyClient = optimizelyManager.initialize(context, datafile)

        // Create a user-context
        val attributes: MutableMap<String, Any> = HashMap()
        attributes["logged_in"] = true
        val user = optimizelyClient.createUserContext("user123", attributes)!!

        // Call the decide method
        val decision = user.decide("product_sort")

        // did the decision fail with a critical error?
        val variationKey = decision.variationKey
        if (variationKey == null) {
            val reasons = decision.reasons
            println("decision error: $reasons")
            return
        }

        // execute code based on flag enabled state
        val enabled = decision.enabled
        val variables = decision.variables
        if (enabled) {
            var vs: String? = null
            try {
                vs = variables.getValue("sort_method", String::class.java)
            } catch (e: JsonParseException) {
                e.printStackTrace()
            }
        }

        // or execute code based on flag variation:
        if (variationKey == "control") {
            // Execute code for control variation
        } else if (variationKey == "treatment") {
            // Execute code for treatment variation
        }

        // Track an event
        user.trackEvent("purchased")
    }

    fun samplesForDoc_CreateUserContext(context: Context) {
        val optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context)
        val optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile)

        // -- sample starts here

        // option 1: create a user, then set attributes
        var user: OptimizelyUserContext?
        user = optimizelyClient.createUserContext("user123")!!
        user.setAttribute("is_logged_in", false)
        user.setAttribute("app_version", "1.3.2")

        // option 2: pass attributes when creating the user

        val attributes: MutableMap<String, Any> = HashMap()
        attributes["is_logged_in"] = false
        attributes["app_version"] = "1.3.2"
        user = optimizelyClient.createUserContext("user123", attributes)
    }

    fun samplesForDoc_DecideOptions(context: Context) {
        // set global default decide options when initializing the client

        var options: List<OptimizelyDecideOption>
        options = Arrays.asList(OptimizelyDecideOption.DISABLE_DECISION_EVENT)
        val optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("FCnSegiEkRry9rhVMroit4")
                .withDefaultDecideOptions(options)
                .build(context)

        // set additional options in a decide call

        val optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile)
        val user = optimizelyClient.createUserContext("user123")
        options = Arrays.asList(OptimizelyDecideOption.DISABLE_DECISION_EVENT, OptimizelyDecideOption.DISABLE_DECISION_EVENT)
        val decisions = user!!.decideAll(options)
    }

    fun samplesForDoc_Decide(context: Context?) {
        val optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context)
        val optimizelyClient = optimizelyManager.initialize(context!!, R.raw.datafile)

        // -- sample starts here

        // create the user and decide which flag rule & variation they bucket into
        val attributes: MutableMap<String, Any> = HashMap()
        attributes["logged_in"] = true
        val user = optimizelyClient.createUserContext("user123", attributes)
        val decision = user!!.decide("product_sort")

        // variation. if null, decision fail with a critical error
        val variationKey = decision.variationKey

        // flag enabled state:
        val enabled = decision.enabled

        // all variable values
        val variables = decision.variables

        // String variable value
        var vs: String? = null
        try {
            vs = variables.getValue("sort_method", String::class.java)
        } catch (e: JsonParseException) {
            e.printStackTrace()
        }

        // Boolean variable value
        val vb = variables.toMap()!!["k_boolean"] as Boolean?

        // flag key for which decision was made
        val flagKey = decision.flagKey

        // user for which the decision was made
        val userContext = decision.userContext

        // reasons for the decision
        val reasons = decision.reasons
    }

    fun samplesForDoc_DecideAll(context: Context?) {
        val optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context)
        val optimizelyClient = optimizelyManager.initialize(context!!, R.raw.datafile)
        val attributes: MutableMap<String, Any> = HashMap()
        attributes["logged_in"] = true
        val user = optimizelyClient.createUserContext("user123", attributes)

        // -- sample starts here

        // make decisions for all active (unarchived) flags in the project for a user
        val options = Arrays.asList(OptimizelyDecideOption.ENABLED_FLAGS_ONLY)
        val decisions = user!!.decideAll(options)
        val allKeys: Set<String> = decisions.keys
        val decisionForFlag1 = decisions["flag_1"]
    }

    fun samplesForDoc_DecideForKeys(context: Context?) {
        val optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context)
        val optimizelyClient = optimizelyManager.initialize(context!!, R.raw.datafile)
        val attributes: MutableMap<String, Any> = HashMap()
        attributes["logged_in"] = true
        val user = optimizelyClient.createUserContext("user123", attributes)

        // -- sample starts here

        // make decisions for specific flags
        val keys = Arrays.asList("flag-1", "flag-2")
        val decisions = user!!.decideForKeys(keys)
        val decision1 = decisions["flag-1"]
        val decision2 = decisions["flag-2"]
    }

    fun samplesForDoc_TrackEvent(context: Context) {
        val optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context)
        val optimizelyClient = optimizelyManager.initialize(context!!, R.raw.datafile)

        // -- sample starts here

        val attributes = hashMapOf<String, Any>("logged_in" to true)
        val user = optimizelyClient.createUserContext("user123", attributes)

        val tags = hashMapOf<String, Any>("category" to "shoes", "purchase_count" to 2)
        user?.trackEvent("my_purchase_event_key", tags)
    }

    fun samplesForDoc_OptimizelyJSON(context: Context) {
        val optimizelyManager = OptimizelyManager.builder().withSDKKey("FCnSegiEkRry9rhVMroit4").build(context)
        val optimizelyClient = optimizelyManager.initialize(context, R.raw.datafile)
        val user = optimizelyClient.createUserContext("user123")
        val decision = user!!.decide("product_sort")
        val optlyJSON = decision.variables

        // -- sample starts here

        //declare a schema object into which you want to unmarshal OptimizelyJson content:
        data class SSub(var field: String)

        data class SObj(
                var field1: Int,
                var field2: Double,
                var field3: String,
                var field4: SSub
        )

        try {
            //parse all json key/value pairs into your schema, sObj
            val robj = optlyJSON.getValue(null, SObj::class.java)

            //or, parse the specified key/value pair with an integer value
            val rint = optlyJSON.getValue("field1", Int::class.java)

            //or, parse the specified key/value pair with a string value
            val rstr = optlyJSON.getValue("field4.field", String::class.java)
        } catch (e: JsonParseException) {
            e.printStackTrace()
        }
    }

    fun samplesForDoc_CustomUserProfileService(context: Context?) {
        // -- sample starts here

        class CustomUserProfileService : UserProfileService {
            @Throws(Exception::class)
            override fun lookup(userId: String): Map<String, Any>? {
                return null
            }

            @Throws(Exception::class)
            override fun save(userProfile: Map<String, Any>) {
            }
        }

        val customUserProfileService = CustomUserProfileService()
        val optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("<Your_SDK_Key>")
                .withUserProfileService(customUserProfileService)
                .build(context)
    }

    fun samplesForDoc_EventDispatcher(context: Context) {
        // -- sample starts here

        // Using an anonymous class here to implement the EventHandler interface.
        // Feel free to create an explicit class that implements the interface instead.
        val eventHandler = object : EventHandler {
            override fun dispatchEvent(logEvent: LogEvent) {
                // Send event to our log endpoint as documented in
                // https://developers.optimizely.com/x/events/api/index.html
            }
        }

        // Build a manager
        val optimizelyManager = OptimizelyManager.builder()
                .withSDKKey("SDK_KEY_HERE")
                .withEventDispatchInterval(60, TimeUnit.SECONDS)
                .withEventHandler(eventHandler)
                .build(context)

        // With the new Android O differences, you need to register the
        // service for the intent filter you desire in code instead of in the manifest.
        val eventRescheduler = EventRescheduler()
        context.registerReceiver(eventRescheduler,
                IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
    }

}

