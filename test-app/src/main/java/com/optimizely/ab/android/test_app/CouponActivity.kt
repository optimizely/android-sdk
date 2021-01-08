/****************************************************************************
 * Copyright 2020, Optimizely, Inc. and contributors                        *
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

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.AttributeSet
import android.view.View
import android.widget.TextView

class CouponActivity : BaseActivity() {
    override fun setShowCoupon(v: Boolean?) {
        super.setShowCoupon(v)
        if (!getShowCoupon()) {
            finish()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.coupon)

        val button = findViewById<View>(R.id.redeem)

        val txt = findViewById<View>(R.id.message) as TextView

        val app = application as MyApplication

        val userId = app.anonUserId ?: "something"
        val attributes = app.attributes ?: HashMap<String, Any>()

        val message = app.optimizelyManager?.optimizely?.getFeatureVariableString("show_coupon", "message", userId, attributes)
        val discount = app.optimizelyManager?.optimizely?.getFeatureVariableInteger("show_coupon", "discount", userId, attributes)

        txt.text = "$message $discount%"

        when (app.optimizelyManager?.optimizely?.getFeatureVariableString("show_coupon", "text_color", userId, attributes)) {
            "yellow" -> txt.setTextColor(Color.YELLOW)
        }

        button.setOnClickListener() {
            finish()
        }
    }
}