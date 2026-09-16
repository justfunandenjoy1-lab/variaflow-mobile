package com.variaflow.mobile

import android.app.Application

class VariaFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // অ্যাপ ওপেন হওয়ার সময় যাতে কোনো ভারী প্রসেস বাধা না দেয়
    }
}
