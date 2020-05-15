package com.optimizely.ab.android.test_app

import android.support.v7.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    private var showCoupon = false
    fun setShowCoupon(`val`: Boolean?) {}
    fun getShowCoupon(): Boolean {
        return showCoupon
    }
}