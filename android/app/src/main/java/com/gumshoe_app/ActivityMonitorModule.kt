//package com.gumshoe_app
//
//import android.app.AppOpsManager
//import android.app.usage.UsageStats
//import android.app.usage.UsageStatsManager
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.content.pm.PackageManager
//import android.hardware.Sensor
//import android.hardware.SensorEvent
//import android.hardware.SensorEventListener
//import android.hardware.SensorManager
//import android.os.Build
//import android.provider.Settings
//import android.telephony.TelephonyManager
//import android.util.Log
//import androidx.annotation.RequiresApi
//import com.facebook.react.bridge.*
//import com.facebook.react.modules.core.DeviceEventManagerModule
//import java.util.concurrent.Executors
//import java.util.concurrent.ScheduledFuture
//import java.util.concurrent.TimeUnit
//
//class ActivityMonitorModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), SensorEventListener {
//
//    private val usageStatsManager: UsageStatsManager by lazy {
//        reactContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
//    }
//
//    private var monitoring: Boolean = false
//    private var callReceiver: CallReceiver? = null
//    private var screenStateReceiver: ScreenStateReceiver? = null
//    private lateinit var sensorManager: SensorManager
//    private var isMoving: Boolean = false
//
//    // Variables to control app tracking state and timing
//    private var lastForegroundApp: String? = null
//    private var trackingAppChanges = false
//    private val scheduler = Executors.newSingleThreadScheduledExecutor()
//    private var scheduledTask: ScheduledFuture<*>? = null
//
//    private var unlockTime: Long = 0
//    private var lockTime: Long = 0
//    private val appsUsedDuringUnlock = mutableSetOf<String>()
//
//    override fun getName(): String {
//        return "ActivityMonitorModule"
//    }
//
//    private fun hasUsageStatsPermission(): Boolean {
//        val appOps = reactApplicationContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
//        val mode = appOps.checkOpNoThrow(
//            AppOpsManager.OPSTR_GET_USAGE_STATS,
//            android.os.Process.myUid(),
//            reactApplicationContext.packageName
//        )
//        return mode == AppOpsManager.MODE_ALLOWED
//    }
//
//    private fun requestUsageAccessPermission(context: Context) {
//        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
//        if (context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) == null) {
//            Log.e("ActivityMonitor", "Usage access permission setting not available on this device.")
//        } else {
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            context.startActivity(intent)
//        }
//    }
//
//    @ReactMethod
//    fun checkAndRequestUsageStatsPermission(promise: Promise) {
//        if (hasUsageStatsPermission()) {
//            promise.resolve(true)
//        } else {
//            requestUsageAccessPermission(reactApplicationContext)
//            promise.resolve(false)
//        }
//    }
//
//    @ReactMethod
//    fun startActivityMonitoring() {
//        if (!monitoring) {
//            monitoring = true
//
//            callReceiver = CallReceiver()
//            screenStateReceiver = ScreenStateReceiver()
//
//            reactApplicationContext.registerReceiver(callReceiver, IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
//            reactApplicationContext.registerReceiver(callReceiver, IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL))
//            reactApplicationContext.registerReceiver(screenStateReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
//            reactApplicationContext.registerReceiver(screenStateReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
//            reactApplicationContext.registerReceiver(screenStateReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
//
//            sensorManager = reactApplicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
//            val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
//            accelerometer?.let {
//                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
//            }
//        }
//    }
//
//    @ReactMethod
//    fun stopActivityMonitoring() {
//        monitoring = false
//        callReceiver?.let { reactApplicationContext.unregisterReceiver(it) }
//        screenStateReceiver?.let { reactApplicationContext.unregisterReceiver(it) }
//        sensorManager.unregisterListener(this)
//        scheduledTask?.cancel(true)
//    }
//
//    private fun startTrackingForegroundApp() {
//        if (trackingAppChanges) return
//
//        trackingAppChanges = true
//        scheduledTask = scheduler.scheduleAtFixedRate({
//            val currentApp = getForegroundApp()
//            if (currentApp != lastForegroundApp) {
//                lastForegroundApp = currentApp
//                sendForegroundAppEvent(currentApp)
//                if (unlockTime > 0) appsUsedDuringUnlock.add(currentApp ?: "unknown")
//            }
//        }, 0, 1, TimeUnit.SECONDS)
//    }
//
//    private fun stopTrackingForegroundApp() {
//        trackingAppChanges = false
//        scheduledTask?.cancel(true)
//        lastForegroundApp = null
//    }
//
//    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
//    private fun getForegroundApp(): String? {
//        val endTime = System.currentTimeMillis()
//        val startTime = endTime - 1000 * 10 // Last 10 seconds
//        val usageStatsList = usageStatsManager.queryUsageStats(
//            UsageStatsManager.INTERVAL_DAILY,
//            startTime, endTime
//        )
//
//        val recentUsageStats = usageStatsList.maxByOrNull { it.lastTimeUsed }
//        return recentUsageStats?.packageName
//    }
//
//    private fun sendForegroundAppEvent(foregroundApp: String?) {
//        val params = Arguments.createMap()
//        params.putString("foregroundApp", foregroundApp ?: "unknown")
//        sendEvent("onAppOpened", params)
//    }
//
//    private fun sendEvent(eventName: String, params: WritableMap) {
//        reactApplicationContext
//            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
//            .emit(eventName, params)
//    }
//
//    inner class CallReceiver : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            val params = Arguments.createMap()
//            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//
//            when (telephony.callState) {
//                TelephonyManager.CALL_STATE_RINGING -> {
//                    params.putString("callState", "incoming")
//                    params.putString("phoneNumber", intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER))
//                }
//                TelephonyManager.CALL_STATE_OFFHOOK -> params.putString("callState", "active")
//                TelephonyManager.CALL_STATE_IDLE -> params.putString("callState", "idle")
//            }
//            sendEvent("onCallStateChanged", params)
//        }
//    }
//
//    inner class ScreenStateReceiver : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            when (intent.action) {
//                Intent.ACTION_SCREEN_ON -> stopTrackingForegroundApp()
//                Intent.ACTION_USER_PRESENT -> {
//                    unlockTime = System.currentTimeMillis()
//                    appsUsedDuringUnlock.clear()
//                    startTrackingForegroundApp()
//                }
//                Intent.ACTION_SCREEN_OFF -> {
//                    lockTime = System.currentTimeMillis()
//                    stopTrackingForegroundApp()
//                    logUsageInterval()
//                }
//            }
//        }
//    }
//
//    private fun logUsageInterval() {
//        if (unlockTime > 0 && lockTime > unlockTime) {
//            val usageDuration = (lockTime - unlockTime) / 1000 // in seconds
//            Log.i("ActivityMonitor", "Device was unlocked for $usageDuration seconds.")
//            Log.i("ActivityMonitor", "Applications used: $appsUsedDuringUnlock")
//            // Optionally send to JS
//            val params = Arguments.createMap()
//            params.putInt("usageDuration", usageDuration.toInt())
//            params.putArray("appsUsed", Arguments.fromList(appsUsedDuringUnlock.toList()))
//            sendEvent("onDeviceLock", params)
//        }
//        unlockTime = 0
//        lockTime = 0
//    }
//
//    override fun onSensorChanged(event: SensorEvent?) {
//        event?.let {
//            val threshold = 1.0f
//            isMoving = Math.sqrt((event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]).toDouble()).toFloat() > threshold
//        }
//    }
//
//    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
//}
