package com.gumshoe_app

import android.app.*
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.os.*
import android.util.Log
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import androidx.core.app.NotificationCompat
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class MonitoringService : Service(), SensorEventListener, LocationListener {

    private var usageStatsManager: UsageStatsManager? = null
    private var handler: Handler? = null
    private var lastActiveApp: String = ""
    private var isMonitoring = false
    private var monitoringStartTime: Long = 0L
    private val monitoringRunnable = Runnable { monitorForegroundApp() }
    private val screenStateReceiver = ScreenStateReceiver()

    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var captureSession: CameraCaptureSession? = null

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var lastSensorLogTime: Long = 0L

    // Location and vehicle motion properties
    private var locationManager: LocationManager? = null
    private var lastLocation: Location? = null
    private var isInVehicle = false
    private var lastVehicleStateChange = 0L
    private val vehicleMotionThreshold = 8.0f // m/s (approximately 29 km/h)
    private val locationUpdateInterval = 5000L // 5 seconds
    private val minLocationDistance = 10f // 10 meters

    // Variables to store the latest sensor data
    private var latestAccelerometerData: FloatArray? = null
    private var latestGyroscopeData: FloatArray? = null

    // Variables to store the latest location data
    private var latestGpsLocation: Location? = null
    private var latestNetworkLocation: Location? = null

    // Variables for tracking app usage
    private var lastAppSwitchTime: Long = 0L // Track the last time the app switched
    private var lastForegroundApp: String = "" // Track the last foreground app

    // Handler for scheduling tasks
    private val sensorHandler = Handler(Looper.getMainLooper())
    private val sensorRunnable = object : Runnable {
        override fun run() {
            collectSensorData()
            sensorHandler.postDelayed(this, 1000L) // Collect sensor data every 1 second
        }
    }

    private val locationHandler = Handler(Looper.getMainLooper())
    private val locationRunnable = object : Runnable {
        override fun run() {
            logAllData() // Log all data in JSON format
            locationHandler.postDelayed(this, locationUpdateInterval)
        }
    }

    companion object {
        const val CHANNEL_ID = "MonitoringServiceChannel"
        const val TAG = "MonitoringService"
        const val MONITORING_INTERVAL = 5000L
        const val NOTIFICATION_ID = 1
        const val SENSOR_LOG_INTERVAL = 1000L // Log every 1 second
        private const val VEHICLE_STATE_DEBOUNCE_TIME = 30000L // 30 seconds
        private const val SIGNIFICANT_ACCELERATION_THRESHOLD = 9.8f // m/s²
        private const val VEHICLE_EXIT_SPEED_THRESHOLD = 3.0f  // Lower threshold for exit detection (m/s)
        private const val GPS_MIN_ACCURACY = 20f  // Minimum accuracy in meters for reliable readings
    }

    override fun onCreate() {
        super.onCreate()

        try {
            // Initialize usage stats manager
            initializeUsageStatsManager()

            // Register screen state receiver
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(screenStateReceiver, filter)

            // Initialize Camera Manager
            cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

            // Initialize SensorManager and sensors
            initializeSensors()

            // Initialize Location Manager and start updates
            initializeLocation()

            // Start collecting sensor data every 1 second
            sensorHandler.post(sensorRunnable)

            // Start logging all data every 5 seconds
            locationHandler.post(locationRunnable)

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
                // Request regular updates from GPS provider
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    locationUpdateInterval,
                    minLocationDistance,
                    this
                )

                // Request regular updates from Network provider
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    locationUpdateInterval,
                    minLocationDistance,
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

    private fun collectSensorData() {
        // This method is called every 1 second to collect sensor data
        val currentTime = System.currentTimeMillis()

        // Store accelerometer data if available
        latestAccelerometerData?.let { data ->
            val x = data[0]
            val y = data[1]
            val z = data[2]
            Log.d(TAG, "Accelerometer - Time: $currentTime, X: $x, Y: $y, Z: $z")
        }

        // Store gyroscope data if available
        latestGyroscopeData?.let { data ->
            val x = data[0]
            val y = data[1]
            val z = data[2]
            Log.d(TAG, "Gyroscope - Time: $currentTime, X: $x, Y: $y, Z: $z")
        }
    }

    private fun logAllData() {
        // Get the latest sensor data
        val accelerationX = latestAccelerometerData?.get(0) ?: 0f
        val accelerationY = latestAccelerometerData?.get(1) ?: 0f
        val accelerationZ = latestAccelerometerData?.get(2) ?: 0f

        val gyroscopeX = latestGyroscopeData?.get(0) ?: 0f
        val gyroscopeY = latestGyroscopeData?.get(1) ?: 0f
        val gyroscopeZ = latestGyroscopeData?.get(2) ?: 0f

        // Get the latest location data
        val bestLocation = when {
            latestGpsLocation == null -> latestNetworkLocation
            latestNetworkLocation == null -> latestGpsLocation
            else -> if (latestGpsLocation!!.accuracy <= latestNetworkLocation!!.accuracy) latestGpsLocation else latestNetworkLocation
        }

        val latitude = bestLocation?.latitude ?: 0.0
        val longitude = bestLocation?.longitude ?: 0.0
        val speed = bestLocation?.speed ?: 0f

        // Get the latest app usage data
        val appName = lastForegroundApp
        val processName = lastForegroundApp // Use the same as appName for simplicity
        val duration = if (lastAppSwitchTime > 0) System.currentTimeMillis() - lastAppSwitchTime else 0L

        // Create the JSON-like string
        val jsonData = """
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
                "appUsage": {
                    "appName": "$appName",
                    "processName": "$processName",
                    "duration": $duration
                },
                "userPhoto": "string" // Placeholder for user photo
            }
        """.trimIndent()

        // Log the JSON data
        Log.d(TAG, "Logged Data: $jsonData")
    }

    override fun onLocationChanged(location: Location) {
        when (location.provider) {
            LocationManager.GPS_PROVIDER -> latestGpsLocation = location
            LocationManager.NETWORK_PROVIDER -> latestNetworkLocation = location
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        if (!isMonitoring) {
            startMonitoring()
        }
        return START_STICKY
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

    private fun startMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true
            monitoringStartTime = System.currentTimeMillis()
            handler = Handler(Looper.getMainLooper())
            handler?.postDelayed(monitoringRunnable, MONITORING_INTERVAL)
            Log.d(TAG, "Monitoring started")
        }
    }

    private fun stopMonitoring() {
        if (isMonitoring) {
            isMonitoring = false
            handler?.removeCallbacksAndMessages(null)
            Log.d(TAG, "Monitoring stopped")
        }
    }

    private fun monitorForegroundApp() {
        if (!isMonitoring) return

        if (!checkRequiredPermissions()) {
            Log.e(TAG, "Permissions lost. Stopping service.")
            stopSelf()
            return
        }

        try {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - MONITORING_INTERVAL

            val stats = usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            val recentApp = stats?.maxByOrNull { it.lastTimeUsed }

            recentApp?.let {
                val packageName = it.packageName
                val appName = try {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(packageName, 0)
                    ).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    packageName
                }

                Log.d(TAG, "Current active app: $appName")

                // Check if the app has changed
                if (packageName != lastForegroundApp) {
                    // Log the time spent on the previous app
                    if (lastForegroundApp.isNotEmpty() && lastAppSwitchTime > 0) {
                        val timeSpent = endTime - lastAppSwitchTime
                        Log.i(TAG, "App switched to background: $lastForegroundApp, Time spent: ${timeSpent / 1000} seconds")
                    }

                    // Update the last app and switch time
                    lastForegroundApp = packageName
                    lastAppSwitchTime = endTime

                    // Log the new foreground app
                    Log.i(TAG, "New foreground app detected: $appName")
                }
            } ?: run {
                Log.w(TAG, "No foreground app detected")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error monitoring foreground app", e)
        }

        handler?.postDelayed(monitoringRunnable, MONITORING_INTERVAL)
    }

    private fun checkRequiredPermissions(): Boolean {
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        // Store the latest sensor data
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                latestAccelerometerData = event.values
            }
            Sensor.TYPE_GYROSCOPE -> {
                latestGyroscopeData = event.values
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Optional: Handle sensor accuracy changes
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        unregisterReceiver(screenStateReceiver)
        captureSession?.close()
        cameraDevice?.close()
        sensorManager?.unregisterListener(this)
        locationManager?.removeUpdates(this)
        sensorHandler.removeCallbacksAndMessages(null) // Stop sensor data collection
        locationHandler.removeCallbacksAndMessages(null) // Stop location comparison
        Log.d(TAG, "MonitoringService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Required LocationListener methods
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {
        Log.w(TAG, "Location provider disabled: $provider")
    }

    inner class ScreenStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_USER_PRESENT -> startMonitoring()
                Intent.ACTION_SCREEN_OFF -> stopMonitoring()
            }
        }
    }

    private fun isGpsEnabled(): Boolean {
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
    }
}