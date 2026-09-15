package com.variaflow.mobile

import android.app.Application
import com.arthenica.ffmpegkit.FFmpegKitConfig

class VariaFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FFmpegKitConfig.enableLogCallback { }
    }
}
