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
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import android.util.AttributeSet
import android.view.View

open class BaseActivity : AppCompatActivity() {
    private var showCoupon = false
    open fun setShowCoupon(v: Boolean?) {
        v?.let {
            showCoupon = it
        }
    }
    open fun getShowCoupon(): Boolean {
        return showCoupon
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showCoupon = intent.getBooleanExtra("show_coupon", false)

        val application = applicationContext as MyApplication
        application?.currentActivity = this
        if (showCoupon) {
            intent = Intent(this, CouponActivity::class.java)
            startActivity(intent)
        }
    }
}