//package com.gumshoe_app
//
//import android.accessibilityservice.AccessibilityService
//import android.view.accessibility.AccessibilityEvent
//import android.util.Log
//import com.facebook.react.bridge.ReactApplicationContext
//import com.facebook.react.modules.core.DeviceEventManagerModule
//
//class InteractionMonitorService : AccessibilityService() {
//
//    private var tapCount = 0  // Variable to count taps
//
//    // React application context (to emit events)
//    private var reactApplicationContext: ReactApplicationContext? = null
//
//    // Set the reactApplicationContext (you may need to pass this context when initializing the service)
//    fun setReactApplicationContext(reactContext: ReactApplicationContext) {
//        this.reactApplicationContext = reactContext
//    }
//
//    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        if (event != null) {
//            when (event.eventType) {
//                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
//                    // Handle tap on clickable views
//                    tapCount++  // Increment tap count whenever a tap is detected
//                    Log.d("InteractionMonitorService", "View Clicked, count: $tapCount")
//                }
//                AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> {
//                    // Handle the start of a touch interaction (e.g., finger down)
//                    Log.d("InteractionMonitorService", "Touch Interaction Started")
//                }
//                AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> {
//                    // Handle the end of a touch interaction (e.g., finger lifted)
//                    Log.d("InteractionMonitorService", "Touch Interaction Ended")
//                    // Increment tap count when touch interaction ends (optional)
//                    tapCount++
//                }
//            }
//            // Send tap count to React Native frontend
//            sendTapCountToFrontend(tapCount)
//        }
//    }
//
//    override fun onInterrupt() {
//        // Handle interruptions if needed
//    }
//
//    private fun sendTapCountToFrontend(tapCount: Int) {
//        // Get the React Native context and emit the event with the tap count
//        reactApplicationContext?.let { reactContext ->
//            if (reactContext.hasActiveCatalystInstance()) {
//                reactContext
//                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
//                    .emit("TapCountEvent", tapCount)  // Emit event with the current tap count
//            }
//        }
//    }
//
//    override fun onServiceConnected() {
//        super.onServiceConnected()
//        Log.d("InteractionMonitorService", "Service connected")
//    }
//
//    // Start monitoring the accessibility events
//    fun startMonitoring() {
//        // This can be a simple method to initialize the service or the event listening
//        Log.d("InteractionMonitorService", "Started monitoring interactions.")
//    }
//}
