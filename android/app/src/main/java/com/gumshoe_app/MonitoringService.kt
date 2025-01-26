package com.gumshoe_app

import android.app.*
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.util.concurrent.CopyOnWriteArrayList

class MonitoringService : Service(), SensorEventListener, LocationListener {

    // Unified data storage
    private val monitoringDataList = CopyOnWriteArrayList<MonitoringData>()

    // Timing constants
    private companion object {
        const val LOG_INTERVAL = 300000L  // 5 minutes
        const val COLLECTION_INTERVAL = 20000L  // 20 seconds
        const val CHANNEL_ID = "MonitoringServiceChannel"
        const val TAG = "MonitoringService"
        const val NOTIFICATION_ID = 1
        const val GPS_MIN_ACCURACY = 20f  // Minimum accuracy in meters for reliable readings
    }

    // Service variables
    private var usageStatsManager: UsageStatsManager? = null
    private var handler: Handler? = null
    private var lastActiveApp: String = ""
    private var isMonitoring = false
    private var monitoringStartTime: Long = 0L
    private val screenStateReceiver = ScreenStateReceiver()

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var latestAccelerometerData: FloatArray? = null
    private var latestGyroscopeData: FloatArray? = null

    private var locationManager: LocationManager? = null
    private var latestGpsLocation: Location? = null
    private var latestNetworkLocation: Location? = null

    // Camera helper
    private lateinit var cameraHelper: CameraHelper
    private var latestPhotoPath: String? = null

    // Handlers for periodic tasks
    private val logHandler = Handler(Looper.getMainLooper())

    // Data class for unified collection
    data class MonitoringData(
        val accelerationX: Float,
        val accelerationY: Float,
        val accelerationZ: Float,
        val gyroscopeX: Float,
        val gyroscopeY: Float,
        val gyroscopeZ: Float,
        val speed: Float,
        val latitude: Double,
        val longitude: Double,
        val appUsage: AppUsage,
        val userPhoto: String  // Photo path or base64
    ) {
        fun toJson() = """
        {
            "accelerationX": $accelerationX,
            "accelerationY": $accelerationY,
            "accelerationZ": $accelerationZ,
            "gyroscopeX": $gyroscopeX,
            "gyroscopeY": $gyroscopeY,
            "gyroscopeZ": $gyroscopeZ,
            "speed": $speed,
            "latitude": $latitude,
            "longitude": $longitude,
            "appUsage": ${appUsage.toJson()},
            "userPhoto": "$userPhoto"
        }
        """.trimIndent()
    }

    data class AppUsage(
        val appName: String,
        val processName: String,
        val duration: Long
    ) {
        fun toJson() = """
        {
            "appName": "$appName",
            "processName": "$processName",
            "duration": $duration
        }
        """.trimIndent()
    }

    override fun onCreate() {
        super.onCreate()

        try {
            // Initialize camera helper with ProcessLifecycleOwner
            cameraHelper = CameraHelper(this, ProcessLifecycleOwner.get())

            // Initialize usage stats manager
            initializeUsageStatsManager()

            // Register screen state receiver
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(screenStateReceiver, filter)

            // Initialize SensorManager and sensors
            initializeSensors()

            // Initialize Location Manager and start updates
            initializeLocation()

            // Start periodic tasks
            startPeriodicTasks()

            // Delay starting the foreground service
            Handler(Looper.getMainLooper()).postDelayed({
                if (checkRequiredPermissions()) {
                    setupForegroundService()
                } else {
                    Log.e(TAG, "Required permissions not granted. Foreground service not started.")
                }
            }, 1000L)
        } catch (e: Exception) {
            Log.e(TAG, "Error during onCreate: ${e.message}", e)
            stopSelf()
        }
    }

    private fun checkRequiredPermissions(): Boolean {
        // Check for location permission
        val hasLocationPermission = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        // Check for usage stats permission
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        val hasUsageStatsPermission = mode == AppOpsManager.MODE_ALLOWED

        return hasLocationPermission && hasUsageStatsPermission
    }

    private fun startPeriodicTasks() {
        handler = Handler(Looper.getMainLooper())
        handler?.post(object : Runnable {
            override fun run() {
                collectAllData()
                handler?.postDelayed(this, COLLECTION_INTERVAL)
            }
        })

        // Logging remains every 5 minutes
        logHandler.postDelayed(::logAggregatedData, LOG_INTERVAL)
    }

    private fun collectAllData() {
        // Get latest sensor values
        val accelerometer = latestAccelerometerData ?: floatArrayOf(0f, 0f, 0f)
        val gyroscope = latestGyroscopeData ?: floatArrayOf(0f, 0f, 0f)

        // Get best location
        val location = getBestLocation()
        val speed = location?.speed?.times(3.6f) ?: 0f  // Convert m/s to km/h

        // Get app usage
        val currentApp = getCurrentAppUsage()

        // Include the photo path or base64
        val photo = latestPhotoPath ?: "string"  // Placeholder if no photo is available

        monitoringDataList.add(
            MonitoringData(
                accelerationX = accelerometer[0],
                accelerationY = accelerometer[1],
                accelerationZ = accelerometer[2],
                gyroscopeX = gyroscope[0],
                gyroscopeY = gyroscope[1],
                gyroscopeZ = gyroscope[2],
                speed = speed,
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0,
                appUsage = currentApp,
                userPhoto = photo
            )
        )

        // Reset the photo path after including it in the data
        latestPhotoPath = null
    }

    private fun getCurrentAppUsage(): AppUsage {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - COLLECTION_INTERVAL

        return try {
            val stats = usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            val recentApp = stats?.maxByOrNull { it.lastTimeUsed }
            recentApp?.let {
                val duration = if (it.packageName == lastActiveApp) {
                    COLLECTION_INTERVAL
                } else {
                    endTime - monitoringStartTime
                }

                AppUsage(
                    appName = getAppName(it.packageName),
                    processName = it.packageName,
                    duration = duration
                )
            } ?: AppUsage("Unknown", "unknown", COLLECTION_INTERVAL)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app usage: ${e.message}")
            AppUsage("Error", "error", 0)
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun logAggregatedData() {
        val jsonArray = monitoringDataList.joinToString(
            prefix = "[",
            postfix = "]",
            separator = ",\n"
        ) { it.toJson() }

        Log.d(TAG, "Aggregated Data:\n$jsonArray")
        monitoringDataList.clear()
        logHandler.postDelayed(::logAggregatedData, LOG_INTERVAL)
    }

    private fun initializeSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun initializeLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (checkLocationPermission()) {
            Log.d(TAG, "Location permission granted, starting updates")
            startLocationUpdates()
        } else {
            Log.e(TAG, "Location permission NOT granted")
        }
    }

    private fun startLocationUpdates() {
        try {
            if (!isGpsEnabled()) {
                Log.e(TAG, "GPS is disabled!")
                return
            }

            if (checkLocationPermission()) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    COLLECTION_INTERVAL,
                    10f,  // Minimum distance in meters
                    this
                )

                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    COLLECTION_INTERVAL,
                    10f,  // Minimum distance in meters
                    this
                )

                Log.d(TAG, "Location updates requested successfully")
            } else {
                Log.e(TAG, "Location permission missing when trying to start updates")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when requesting location updates: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting location updates: ${e.message}")
        }
    }

    private fun getBestLocation(): Location? {
        return when {
            latestGpsLocation == null -> latestNetworkLocation
            latestNetworkLocation == null -> latestGpsLocation
            else -> if (latestGpsLocation!!.accuracy <= latestNetworkLocation!!.accuracy) latestGpsLocation else latestNetworkLocation
        }
    }

    private fun setupForegroundService() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Monitoring Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    lightColor = Color.BLUE
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Gumshoe Monitoring Active")
                .setContentText("Monitoring phone usage in progress")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()

            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Foreground service started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup foreground service", e)
            stopSelf()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun initializeUsageStatsManager() {
        try {
            usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing UsageStatsManager", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        if (!isMonitoring) {
            startMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true
            monitoringStartTime = System.currentTimeMillis()
            Log.d(TAG, "Monitoring started")
        }
    }

    private fun stopMonitoring() {
        if (isMonitoring) {
            isMonitoring = false
            Log.d(TAG, "Monitoring stopped")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> latestAccelerometerData = event.values
            Sensor.TYPE_GYROSCOPE -> latestGyroscopeData = event.values
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Optional: Handle sensor accuracy changes
    }

    override fun onLocationChanged(location: Location) {
        when (location.provider) {
            LocationManager.GPS_PROVIDER -> latestGpsLocation = location
            LocationManager.NETWORK_PROVIDER -> latestNetworkLocation = location
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {
        Log.w(TAG, "Location provider disabled: $provider")
    }

    inner class ScreenStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_USER_PRESENT -> {
                    startMonitoring()
                    takePhoto()
                }
                Intent.ACTION_SCREEN_OFF -> stopMonitoring()
            }
        }
    }

    private fun takePhoto() {
        cameraHelper.takePhoto { photoFile ->
            // Convert the photo to base64 or store its path
            val photoPath = photoFile.absolutePath
            Log.d(TAG, "Photo captured: $photoPath")

            // Store the photo path for later use in aggregated data
            latestPhotoPath = photoPath
        }
    }

    private fun isGpsEnabled(): Boolean {
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        unregisterReceiver(screenStateReceiver)
        sensorManager?.unregisterListener(this)
        locationManager?.removeUpdates(this)
        handler?.removeCallbacksAndMessages(null)
        logHandler.removeCallbacksAndMessages(null)
        Log.d(TAG, "MonitoringService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}