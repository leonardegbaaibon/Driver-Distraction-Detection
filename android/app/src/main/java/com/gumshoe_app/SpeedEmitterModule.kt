package com.gumshoe_app

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule

class SpeedEmitterModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "SpeedEmitter"
    }

    companion object {
        // Hold a reference to the ReactApplicationContext.
        private var reactAppContext: ReactApplicationContext? = null

        fun initialize(context: ReactApplicationContext) {
            reactAppContext = context
        }

        fun sendSpeedUpdate(speed: Float) {
            val params: WritableMap = Arguments.createMap().apply {
                putDouble("speed", speed.toDouble())
            }
            reactAppContext?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                ?.emit("SpeedUpdate", params)
        }
    }
}
