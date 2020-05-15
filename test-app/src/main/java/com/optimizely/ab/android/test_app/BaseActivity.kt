package com.optimizely.ab.android.test_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
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
        showCoupon = intent.getBooleanExtra("show_coupon", false)
        super.onCreate(savedInstanceState)

        val application = applicationContext as MyApplication
        application?.currentActivity = this
        if (showCoupon) {
            intent = Intent(application!!.baseContext, CouponActivity::class.java)
            startActivity(intent)
        }
    }
}