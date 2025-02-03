package com.gumshoe_app

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import java.util.*

class MyReactPackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        val modules = mutableListOf<NativeModule>()

        // Add your existing modules
        modules.add(MyNativeModule(reactContext))
        modules.add(CalendarModule(reactContext))
        modules.add(MotionsensingModule(reactContext))
        modules.add(CameraModule(reactContext))

        // Add new modules for token management and service control
        modules.add(AndroidUtilsModule(reactContext))
        modules.add(MonitoringControllerModule(reactContext))

        return modules
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }
}