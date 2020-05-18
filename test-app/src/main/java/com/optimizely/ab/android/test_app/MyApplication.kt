/****************************************************************************
 * Copyright 2016-2017, Optimizely, Inc. and contributors                   *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                            *
 * *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.android.test_app

import android.app.Application
import android.content.Context
import android.os.Build
import com.optimizely.ab.android.sdk.OptimizelyManager
import java.util.*
import java.util.concurrent.TimeUnit

class MyApplication : Application() {
    var optimizelyManager: OptimizelyManager? = null
        private set

    val attributes: Map<String, *>
        get() {
            val attributes: MutableMap<String, Any> = HashMap()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                attributes["locale"] = resources.configuration.locales[0].toString()
            } else {
                attributes["locale"] = resources.configuration.locale.toString()
            }
            val abbr = getAbbriviation(getLocality(location))

            attributes["location"] = abbr

            return attributes
        }// comment this out to get a brand new user id every time this function is called.
    // useful for incrementing results page count for QA purposes

    // this is a convenience method that creates and persists an anonymous user id,
    // which we need to pass into the activate and track calls
    val anonUserId: String
        get() {
            // this is a convenience method that creates and persists an anonymous user id,
            // which we need to pass into the activate and track calls
            val sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE)
            var id = sharedPreferences.getString("userId", null)
            if (id == null) {
                id = UUID.randomUUID().toString()

                // comment this out to get a brand new user id every time this function is called.
                // useful for incrementing results page count for QA purposes
                sharedPreferences.edit().putString("userId", id).apply()
            }
            return id
        }

    private val userAge: Int
        private get() = 65

    override fun onCreate() {
        super.onCreate()

        // This app is built against a real Optimizely project with real experiments set.  Automated
        // espresso tests are run against this project id.  Changing it will make the Optimizely
        // must match the project id of the compiled in Optimizely data file in rest/raw/data_file.json.
        val builder = OptimizelyManager.builder()
        optimizelyManager = builder.withEventDispatchInterval(60L)
                .withDatafileDownloadInterval(TimeUnit.MINUTES.toSeconds(15))
                .withSDKKey("FCnSegiEkRry9rhVMroit4")
                .build(applicationContext)
    }

    var currentActivity: BaseActivity? = null

    private fun getAbbriviation(locality: Any): String {
        return "NY"
    }

    private fun getLocality(location: Any?): Any {
        return "NY"
    }

    private val location: Any?
        private get() = null

    companion object {
        // Project ID owned by mobile-test@optimizely.com
        // if you'd like to configure your own experiment please check out https://developers.optimizely.com/x/solutions/sdks/getting-started/index.html?language=android&platform=mobile
        // to create your own project and experiment. Then just replace your project ID below.
        const val PROJECT_ID = "10554895220"
    }
}