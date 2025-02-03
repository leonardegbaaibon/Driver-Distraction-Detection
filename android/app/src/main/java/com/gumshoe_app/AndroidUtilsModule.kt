package com.gumshoe_app

import android.content.Context
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class AndroidUtilsModule(private val reactContext: ReactApplicationContext)
    : ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "AndroidUtils"

    @ReactMethod
    fun saveToken(token: String) {
        val prefs = reactContext.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("auth_token", token)
            apply()
        }
    }

    @ReactMethod
    fun deleteToken() {
        val prefs = reactContext.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            remove("auth_token")
            apply()
        }
    }
}