package com.gumshoe_app

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class MonitoringControllerModule(private val reactContext: ReactApplicationContext)
    : ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "MonitoringController"

    @ReactMethod
    fun startService() {
        val serviceIntent = Intent(reactContext, MonitoringService::class.java)
        ContextCompat.startForegroundService(reactContext, serviceIntent)
    }

    @ReactMethod
    fun stopService() {
        val serviceIntent = Intent(reactContext, MonitoringService::class.java)
        reactContext.stopService(serviceIntent)
    }
}