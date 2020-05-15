package com.optimizely.ab.android.test_app

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import android.view.View

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

        button.setOnClickListener() {
            finish()
        }
    }
}