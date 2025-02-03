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
import androidx.lifecycle.ProcessLifecycleOwner
import java.util.concurrent.CopyOnWriteArrayList
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStream

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
        const val GPS_MIN_ACCURACY = 20f  // Minimum accuracy in meters
        const val PREFS_NAME = "AuthPrefs"
        const val TOKEN_KEY = "auth_token"
        const val API_ENDPOINT = "https://api.blackboxservice.monster/v2/gumshoe/telemetry/batch"
    }

    // Service variables
    private var usageStatsManager: UsageStatsManager? = null
    private var handler: Handler? = null
    private var lastActiveApp: String = ""
    private var isMonitoring = false
    private var monitoringStartTime: Long = 0L
    private val screenStateReceiver = ScreenStateReceiver()
    private var isReceiverRegistered = false // Flag to track receiver registration

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
        val userPhoto: String  // Photo in Base64 or placeholder
    ) {
        fun toJson(): String {
            return """
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
    }

    data class AppUsage(
        val appName: String,
        val processName: String,
        val duration: Long
    ) {
        fun toJson(): String {
            return """
            {
                "appName": "$appName",
                "processName": "$processName",
                "duration": $duration
            }
            """.trimIndent()
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Always start foreground service first to avoid exceptions
        setupForegroundService()

        if (!isTokenValid()) {
            Log.d(TAG, "No valid token - stopping service")
            stopSelf()
            return
        }

        try {
            // Initialize camera helper
            cameraHelper = CameraHelper(this, ProcessLifecycleOwner.get())

            // Initialize usage stats manager
            initializeUsageStatsManager()

            // Register screen state receiver
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(screenStateReceiver, filter)
            isReceiverRegistered = true // Set the flag to true

            // Initialize sensors and location
            initializeSensors()
            initializeLocation()

            // Start periodic tasks
            startPeriodicTasks()

            // Start foreground service
//            setupForegroundService()
        } catch (e: Exception) {
            Log.e(TAG, "Error during onCreate: ${e.message}", e)
            stopSelf()
        }
    }

    private fun isTokenValid(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(TOKEN_KEY, null)?.isNotEmpty() ?: false
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
        Handler(Looper.getMainLooper()).postDelayed(::logAggregatedData, LOG_INTERVAL)
    }

    private fun collectAllData() {
        // Collect sensor, location, and app usage data
        val accelerometer = latestAccelerometerData ?: floatArrayOf(0f, 0f, 0f)
        val gyroscope = latestGyroscopeData ?: floatArrayOf(0f, 0f, 0f)
        val location = getBestLocation()
        val speed = location?.speed?.times(3.6f) ?: 0f  // Convert m/s to km/h
        val currentApp = getCurrentAppUsage()

        // Include the photo in Base64 or a placeholder
        val photoBase64 = if (latestPhotoPath != null) {
            cameraHelper.encodePhotoToBase64(File(latestPhotoPath!!))
        } else {
            "no_photo"
        }

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
                userPhoto = photoBase64
            )
        )

        // Reset the photo path after including it in the data
        latestPhotoPath = null
    }

    private fun logAggregatedData() {
        if (monitoringDataList.isEmpty()) {
            Log.w(TAG, "No data to send. Skipping API request.")
            return
        }

        val token = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(TOKEN_KEY, null) ?: run {
            Log.w(TAG, "Token missing during data transmission")
            stopSelf()
            return
        }

        val jsonPayload = """
        {
            "telemetryRecords": ${monitoringDataList.joinToString(prefix = "[", postfix = "]", separator = ",\n") { it.toJson() }}
        }
    """.trimIndent()

        Log.d(TAG, "Sending telemetry data: $jsonPayload")
        sendDataToApi(jsonPayload, token)

        monitoringDataList.clear()
        Handler(Looper.getMainLooper()).postDelayed(::logAggregatedData, LOG_INTERVAL)
    }

    private fun sendDataToApi(jsonData: String, token: String) {
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(API_ENDPOINT)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.doOutput = true
                connection.readTimeout = 10000
                connection.connectTimeout = 15000

                val outputStream: OutputStream = connection.outputStream
                outputStream.write(jsonData.toByteArray(Charsets.UTF_8))
                outputStream.close()

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    Log.d(TAG, "Data sent successfully. Response code: $responseCode")
                } else {
                    val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e(TAG, "API Error: $responseCode - $errorStream")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error: ${e.message}", e)
            } finally {
                connection?.disconnect()
            }
        }.start()
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
                .setContentTitle("Gumshoe helps you drive safely")
                .setContentText("Checking potential accident")
                .setSmallIcon(R.drawable.ic_notification)
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
            // Store the photo path for later use in aggregated data
            latestPhotoPath = photoFile.absolutePath
            Log.d(TAG, "Photo captured: $latestPhotoPath")
        }
    }

    private fun isGpsEnabled(): Boolean {
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()

        // Unregister the receiver only if it was registered
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(screenStateReceiver)
                isReceiverRegistered = false // Reset the flag
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Receiver was not registered: ${e.message}")
            }
        }

        sensorManager?.unregisterListener(this)
        locationManager?.removeUpdates(this)
        handler?.removeCallbacksAndMessages(null)
        logHandler.removeCallbacksAndMessages(null)
        Log.d(TAG, "MonitoringService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}