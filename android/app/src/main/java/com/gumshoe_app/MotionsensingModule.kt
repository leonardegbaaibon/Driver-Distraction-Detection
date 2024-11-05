package com.gumshoe_app

import android.view.MotionEvent
import android.view.View
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.modules.core.DeviceEventManagerModule

class MotionsensingModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "MotionsensingModule"
    }

    @ReactMethod
    fun startTouchDetection() {
        val activity = reactContext.currentActivity
        val view = activity?.window?.decorView?.findViewById<View>(android.R.id.content)

        view?.setOnTouchListener { _: View?, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Emit event for hand detected
                    emitEvent("HAND_DETECTED", "Hand is on the phone.")
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Emit event for hand removed
                    emitEvent("HAND_REMOVED", "Hand is no longer on the phone.")
                    true
                }
                else -> false
            }
        }
    }

    private fun emitEvent(eventName: String, message: String) {
        // Updated check for active Catalyst instance
        if (reactContext.hasActiveCatalystInstance()) {
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, message)
        }
    }

    @ReactMethod
    fun stopTouchDetection() {
        val activity = reactContext.currentActivity
        val view = activity?.window?.decorView?.findViewById<View>(android.R.id.content)

        // Remove touch listener to stop detection
        view?.setOnTouchListener(null)
    }
}
