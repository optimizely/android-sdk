/****************************************************************************
 * Copyright 2017-2020, Optimizely, Inc. and contributors                        *
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

import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.optimizely.ab.android.shared.CountingIdlingResourceManager

class ConversionFragment : Fragment() {
    private var conversionButton: Button? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_conversion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        conversionButton = view.findViewById<View>(R.id.btn_variation_conversion) as Button
        val myApplication = activity.application as MyApplication
        val optimizelyManager = myApplication.optimizelyManager
        conversionButton?.setOnClickListener {
            val userId = myApplication.anonUserId

            // This tracks a conversion event for the event named `sample_conversion`
            val optimizely = optimizelyManager?.optimizely
            optimizely?.track("sample_conversion", userId)
            optimizely?.track("my_conversion", userId)

            // Utility method for verifying event dispatches in our automated tests
            CountingIdlingResourceManager.increment() // increment for conversion event
            val intent = Intent(myApplication.baseContext, EventConfirmationActivity::class.java)
            startActivity(intent)
        }
    }
}