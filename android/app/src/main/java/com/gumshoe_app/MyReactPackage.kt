package com.gumshoe_app

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import java.util.Collections

class MyReactPackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        val modules: MutableList<NativeModule> = ArrayList()
        modules.add(MyNativeModule(reactContext))
        modules.add(CalendarModule(reactContext))
        modules.add(UltrasonicModule(reactContext))
        modules.add(RecordingModule(reactContext))
        modules.add(MotionsensingModule(reactContext))
        modules.add(MotionSensorModule(reactContext))
        modules.add(ActivityMonitorModule(reactContext))
        modules.add(CameraModule(reactContext))




        return modules
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return Collections.emptyList()
    }
}
