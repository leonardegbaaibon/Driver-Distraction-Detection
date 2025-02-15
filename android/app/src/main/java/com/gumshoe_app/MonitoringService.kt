package com.gumshoe_app

import android.Manifest
import android.app.*
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.location.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStream
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.sqrt

class MonitoringService : Service(), SensorEventListener {

    // Unified data storage for telemetry records.
    private val monitoringDataList = CopyOnWriteArrayList<MonitoringData>()

    // Timing constants (milliseconds unless noted otherwise)
    private companion object {
        const val LOG_INTERVAL = 300000L           // 5 minutes
        const val COLLECTION_INTERVAL = 20000L       // 20 seconds telemetry period
        const val SPEED_SAMPLE_INTERVAL = 1000L      // 1 second speed sampling
        const val FOREGROUND_CHECK_INTERVAL = 3000L  // 3 seconds for foreground app sampling
        const val CHANNEL_ID = "MonitoringServiceChannel"
        const val TAG = "MonitoringService"
        const val NOTIFICATION_ID = 1
        const val GPS_MIN_ACCURACY = 20f             // Minimum accuracy in meters
        const val PREFS_NAME = "AuthPrefs"
        const val TOKEN_KEY = "auth_token"
        const val API_ENDPOINT = "https://api.blackboxservice.monster/v2/gumshoe/telemetry/batch"
        // Minimum delay between consecutive Gumshoe launches.
        const val GUMSHOE_LAUNCH_DELAY = 10000L      // 10 seconds delay

        // Speed threshold: 10 km/hr converted to m/s (≈ 2.78 m/s)
        const val SPEED_THRESHOLD = 10f / 3.6f
    }

    // Whitelist of target applications (update these package names as needed)
    private val targetApps = setOf("com.example.targetapp1", "com.example.targetapp2")

    // Service variables
    private var usageStatsManager: UsageStatsManager? = null
    private var handler: Handler? = null
    private var lastActiveApp: String = ""
    private var isMonitoring = false
    private var monitoringStartTime: Long = 0L

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var latestAccelerometerData: FloatArray? = null
    private var latestGyroscopeData: FloatArray? = null

    // Fused Location Provider fields
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var latestLocation: Location? = null

    // Camera helper (used to capture and encode images)
    private var cameraHelper: CameraHelper? = null
    // This variable will store the file path of the captured image.
    private var latestPhotoPath: String? = null

    // Handlers for periodic tasks
    private val logHandler = Handler(Looper.getMainLooper())
    private var speedHandler: Handler? = null
    private var foregroundAppHandler: Handler? = null

    // For storing speed samples every second (in m/s)
    private val speedSamples = mutableListOf<Float>()

    // List for recording foreground app logs as pairs of (packageName, timestamp)
    private val foregroundAppLogs = mutableListOf<Pair<String, Long>>()

    // Set to track distinct non‑OS apps (excluding this app) for triggering Gumshoe.
    private val recentDistinctApps = mutableSetOf<String>()

    // Variables to track the current foreground app and its start time for logging duration.
    private var currentForegroundApp: String? = null
    private var currentForegroundAppStartTime: Long = 0L

    // Track last Gumshoe launch time and ensure launch happens only once.
    private var lastGumshoeLaunchTime: Long = 0L
    private var gumshoeLaunched: Boolean = false

    // Foreground detection and camera management.
    private var isAppInForeground = false
    private val appForegroundReceiver = AppForegroundReceiver()

    // Variables for accelerometer-based speed estimation.
    // Gravity vector estimation.
    private val gravity = floatArrayOf(0f, 0f, 0f)
    // Timestamp for the last accelerometer event (for integration).
    private var lastAccelTimestamp: Long = 0L
    // Accumulator for speed integration over the current 1-second window (in m/s).
    private var integratedSpeed: Float = 0f
    // A Kalman filter instance for smoothing the acceleration measurement.
    private val accelKalmanFilter = KalmanFilter()

    // Store the last speed sample (in m/s).
    private var lastSpeedSample: Float = 0f

    // Data classes for telemetry
    data class MonitoringData(
        val accelerationX: Float,
        val accelerationY: Float,
        val accelerationZ: Float,
        val gyroscopeX: Float,
        val gyroscopeY: Float,
        val gyroscopeZ: Float,
        val speed: Float,         // in m/s
        val latitude: Double,
        val longitude: Double,
        val appUsage: AppUsage,
        val userPhoto: String
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

    // Data class for app usage (duration is reported in seconds)
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
        setupForegroundService()

        if (!isTokenValid()) {
            Log.d(TAG, "No valid token - stopping service")
            stopSelf()
            return
        }

        try {
            // Initialize the camera helper using the application context for lifecycle safety.
            cameraHelper = CameraHelper(applicationContext, ProcessLifecycleOwner.get())
            initializeUsageStatsManager()

            // Register the foreground state receiver.
            registerReceiver(appForegroundReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
            registerReceiver(appForegroundReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

            initializeSensors()
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            setupLocationCallback()
            startLocationUpdates()
            startPeriodicTasks()
            startSpeedSampling()
            startForegroundAppCheck()
        } catch (e: Exception) {
            Log.e(TAG, "Error during onCreate: ${e.message}", e)
            stopSelf()
        }
    }

    private fun isTokenValid(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(TOKEN_KEY, null)?.isNotEmpty() ?: false
    }

    private fun initializeUsageStatsManager() {
        try {
            usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing UsageStatsManager: ${e.message}", e)
            stopSelf()
        }
    }

    private fun initializeSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gyroscope?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                latestLocation = locationResult.lastLocation
                Log.d(TAG, "Location update: lat=${latestLocation?.latitude}, lon=${latestLocation?.longitude}")
            }
        }
    }

    private fun startLocationUpdates() {
        if (!checkLocationPermission()) {
            Log.e(TAG, "Location permission not granted.")
            return
        }
        val locationRequest = LocationRequest.create().apply {
            interval = COLLECTION_INTERVAL
            fastestInterval = COLLECTION_INTERVAL / 2
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        Log.d(TAG, "Fused location updates started.")
    }

    private fun getBestLocation(): Location? = latestLocation

    private fun startPeriodicTasks() {
        handler = Handler(Looper.getMainLooper())
        handler?.post(object : Runnable {
            override fun run() {
                collectAllData()
                // Clear foreground app logs after each collection.
                foregroundAppLogs.clear()
                handler?.postDelayed(this, COLLECTION_INTERVAL)
            }
        })
        Handler(Looper.getMainLooper()).postDelayed(::logAggregatedData, LOG_INTERVAL)
    }

    /**
     * Called every second.
     * Computes the speed for the current 1-second window using integrated accelerometer data.
     * If a nonzero fused location (GPS) speed is available, that value is used instead.
     * After taking the sample, the integration is reset.
     */
    private fun startSpeedSampling() {
        speedHandler = Handler(Looper.getMainLooper())
        speedHandler?.post(object : Runnable {
            override fun run() {
                sampleSpeed()
                speedHandler?.postDelayed(this, SPEED_SAMPLE_INTERVAL)
            }
        })
    }

    private fun sampleSpeed() {
        val fusedSpeed = latestLocation?.speed ?: 0f  // in m/s
        // Use fused speed if available; otherwise, use the integrated speed.
        val currentSpeed = if (fusedSpeed > 0f) fusedSpeed else integratedSpeed
        lastSpeedSample = currentSpeed
//        Log.d(TAG, "Speed sample (m/s): $currentSpeed")
        speedSamples.add(currentSpeed)
        // Reset integration for the next 1-second window.
        SpeedEmitterModule.sendSpeedUpdate(currentSpeed)

        integratedSpeed = 0f
        lastAccelTimestamp = 0L
    }

    private fun collectAllData() {
        val accelerometer = latestAccelerometerData ?: floatArrayOf(0f, 0f, 0f)
        val gyroscope = latestGyroscopeData ?: floatArrayOf(0f, 0f, 0f)
        val location = getBestLocation()
        val avgSpeed = if (speedSamples.isNotEmpty()) speedSamples.sum() / speedSamples.size
        else location?.speed ?: 0f
        Log.d(TAG, "Average speed (m/s) over 20 sec window: $avgSpeed")
        speedSamples.clear()

        // Determine dominant app usage.
        val dominantAppUsage = getDominantAppUsage()

        val photoBase64 = if (latestPhotoPath != null) {
            cameraHelper?.encodePhotoToBase64(File(latestPhotoPath!!)) ?: "no_photo"
        } else {
            "no_photo"
        }
        if (photoBase64 != "no_photo") {
            Log.d(TAG, "Encoded photo for telemetry: ${photoBase64.take(100)}... (length: ${photoBase64.length})")
        }
        monitoringDataList.add(
            MonitoringData(
                accelerationX = accelerometer[0],
                accelerationY = accelerometer[1],
                accelerationZ = accelerometer[2],
                gyroscopeX = gyroscope[0],
                gyroscopeY = gyroscope[1],
                gyroscopeZ = gyroscope[2],
                speed = avgSpeed,
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0,
                appUsage = dominantAppUsage,
                userPhoto = photoBase64
            )
        )
        latestPhotoPath = null
    }

    // Computes the dominant (most frequently logged) foreground app.
    private fun getDominantAppUsage(): AppUsage {
        if (foregroundAppLogs.isEmpty()) {
            return AppUsage("Unknown", "unknown", COLLECTION_INTERVAL / 1000)
        }
        val grouped = foregroundAppLogs.groupBy { it.first }
        val dominantEntry = grouped.maxByOrNull { it.value.size }
            ?: return AppUsage("Unknown", "unknown", COLLECTION_INTERVAL / 1000)
        val dominantApp = dominantEntry.key
        val durationSeconds = dominantEntry.value.size * (FOREGROUND_CHECK_INTERVAL / 1000)
        return AppUsage(getAppName(dominantApp), dominantApp, durationSeconds)
    }

    private fun logAggregatedData() {
        if (monitoringDataList.isEmpty()) {
            Log.w(TAG, "No telemetry data to send. Skipping API request.")
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
//        Log.d(TAG, "Sending telemetry data: $jsonPayload")
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

    // Returns the human-readable app name for the package.
    private fun getAppName(packageName: String): String {
        return try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    // Periodically checks the foreground app and records its usage.
    private fun startForegroundAppCheck() {
        foregroundAppHandler = Handler(Looper.getMainLooper())
        foregroundAppHandler?.post(object : Runnable {
            override fun run() {
                checkForegroundApp()
                foregroundAppHandler?.postDelayed(this, FOREGROUND_CHECK_INTERVAL)
            }
        })
    }

    /**
     * Modified checkForegroundApp():
     * 1. Queries the UsageStatsManager over the past 5 seconds.
     * 2. Filters out OS apps (packages starting with "com.android") and this app's package.
     * 3. Adds the detected non‑OS app to recentDistinctApps.
     * 4. If three distinct non‑OS apps are detected and the last speed sample is above the SPEED_THRESHOLD,
     *    and if gumshoe hasn't been launched already, then launch the Gumshoe app and set gumshoeLaunched to true.
     * 5. Otherwise, if the app is not in the foreground, capture a photo.
     */
    private fun checkForegroundApp() {
        val currentTime = System.currentTimeMillis()
        val stats = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            currentTime - 5000,
            currentTime
        )
        if (stats != null && stats.isNotEmpty()) {
            val recentApp = stats.maxByOrNull { it.lastTimeUsed }
            val detectedApp = recentApp?.packageName ?: "unknown"
            // Filter out OS apps and this app's package.
            if (!detectedApp.startsWith("com.android") && detectedApp != packageName) {
                recentDistinctApps.add(detectedApp)
//                Log.d(TAG, "Non-OS app detected: $detectedApp. Distinct count: ${recentDistinctApps.size}")
            }
            // Only trigger Gumshoe if:
            // (a) At least three distinct non-OS apps have been seen,
            // (b) The last speed sample is above the threshold,
            // (c) And Gumshoe hasn't been launched yet.
            if (!gumshoeLaunched && recentDistinctApps.size >= 3 && lastSpeedSample >= SPEED_THRESHOLD) {
//                Log.d(TAG, "Three distinct non-OS apps detected and speed threshold met. Launching Gumshoe.")
                val gumshoeIntent = Intent(this, MainActivity::class.java)
                gumshoeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(gumshoeIntent)
                gumshoeLaunched = true
                recentDistinctApps.clear()
                return
            }
        }
        if (isAppInForeground) {
//            Log.d(TAG, "Skipping foreground check - app is in foreground")
            return
        }
        // Capture a photo (if desired) if no gumshoe trigger occurs.
        cameraHelper?.takePhoto { file ->
            if (checkStoragePermission() && file.exists()) {
                latestPhotoPath = file.absolutePath
//                Log.d(TAG, "Photo captured successfully")
            } else {
                Log.e(TAG, "Failed to save photo")
            }
        }
    }

    // New permission check for storage.
    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
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
            Log.e(TAG, "Failed to setup foreground service: ${e.message}", e)
            stopSelf()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        if (!isMonitoring) startMonitoring()
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
            Sensor.TYPE_ACCELEROMETER -> {
                // Save a copy of the raw accelerometer data.
                latestAccelerometerData = event.values.clone()

                // Remove gravity using a simple low-pass filter.
                val alpha = 0.8f
                for (i in 0..2) {
                    gravity[i] = alpha * gravity[i] + (1 - alpha) * event.values[i]
                }
                val linearAcc = FloatArray(3)
                linearAcc[0] = event.values[0] - gravity[0]
                linearAcc[1] = event.values[1] - gravity[1]
                linearAcc[2] = event.values[2] - gravity[2]
                val accMagnitude = sqrt(
                    (linearAcc[0] * linearAcc[0] +
                            linearAcc[1] * linearAcc[1] +
                            linearAcc[2] * linearAcc[2]).toDouble()
                ).toFloat()

                // Smooth the acceleration measurement.
                val filteredAcc = accelKalmanFilter.update(accMagnitude)
                // Integrate the filtered acceleration over time for the current 1-second window.
                if (lastAccelTimestamp != 0L) {
                    val dt = (event.timestamp - lastAccelTimestamp) / 1_000_000_000.0f
                    integratedSpeed += filteredAcc * dt
                }
                lastAccelTimestamp = event.timestamp
            }
            Sensor.TYPE_GYROSCOPE -> {
                latestGyroscopeData = event.values.clone()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Optional: Handle sensor accuracy changes.
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        try {
            unregisterReceiver(appForegroundReceiver)
            cameraHelper?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error: ${e.message}")
        }
        sensorManager?.unregisterListener(this)
        if (this::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } else {
            Log.w(TAG, "fusedLocationClient not initialized, skipping removal")
        }
        handler?.removeCallbacksAndMessages(null)
        logHandler.removeCallbacksAndMessages(null)
        speedHandler?.removeCallbacksAndMessages(null)
        foregroundAppHandler?.removeCallbacksAndMessages(null)
        Log.d(TAG, "MonitoringService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Inner class for foreground detection.
    inner class AppForegroundReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_USER_PRESENT -> {
                    isAppInForeground = true
                    cameraHelper?.shutdown()
                    Log.d(TAG, "App in foreground - pausing camera")
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isAppInForeground = false
                    if (!isMonitoring) startMonitoring()
                }
            }
        }
    }

    // A simple Kalman filter for smoothing the acceleration measurement.
    private class KalmanFilter {
        private var Q = 0.001f  // Process noise covariance
        private var R = 0.1f    // Measurement noise covariance
        private var P = 1f      // Estimation error covariance
        private var X = 0f      // Estimated value

        fun update(measurement: Float): Float {
            P += Q
            val K = P / (P + R)
            X += K * (measurement - X)
            P *= (1 - K)
            return X
        }
    }
}
