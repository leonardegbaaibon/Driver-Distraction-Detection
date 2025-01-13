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
    private val locationUpdateInterval = 10000L // 10 seconds
    private val minLocationDistance = 10f // 10 meters

    companion object {
        const val CHANNEL_ID = "MonitoringServiceChannel"
        const val TAG = "MonitoringService"
        const val MONITORING_INTERVAL = 5000L
        const val NOTIFICATION_ID = 1
        const val SENSOR_LOG_INTERVAL = 1000L // Log every 1 second
        private const val VEHICLE_STATE_DEBOUNCE_TIME = 30000L // 30 seconds
        private const val SIGNIFICANT_ACCELERATION_THRESHOLD = 1.5f // m/s²
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

    private fun startLocationUpdates() {
        try {
            if (!isGpsEnabled()) {
                Log.e(TAG, "GPS is disabled!")
                return
            }
            
            if (checkLocationPermission()) {
                // Try to get last known location immediately
                val lastKnownLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                lastKnownLocation?.let {
                    Log.d(TAG, "Last known location - Lat: ${it.latitude}, Lon: ${it.longitude}")
                }

                // Request regular updates
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    locationUpdateInterval,
                    minLocationDistance,
                    this
                )
                
                // Also request updates from network provider as backup
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

                if (packageName != lastActiveApp) {
                    Log.i(TAG, "New foreground app detected: $appName")
                    lastActiveApp = packageName
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

    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "onLocationChanged called!")  // Debug log to confirm callback is triggered
        
        val previousLocation = lastLocation
        lastLocation = location

        // Always log new location immediately
        Log.i(TAG, """
            New Location Received:
            Latitude: ${location.latitude}
            Longitude: ${location.longitude}
            Provider: ${location.provider}
            Accuracy: ${location.accuracy}m
            Time: ${java.util.Date(location.time)}
        """.trimIndent())

        // Calculate distance and speed if we have a previous location
        previousLocation?.let {
            val timeDiff = (location.time - it.time) / 1000f  // Convert to seconds
            val distance = location.distanceTo(it)
            val calculatedSpeed = distance / timeDiff

            Log.d(TAG, """
                Location updated:
                Lat: ${location.latitude}, Lon: ${location.longitude}
                Speed (GPS): ${location.speed} m/s
                Speed (calc): $calculatedSpeed m/s
                Accuracy: ${location.accuracy}m
                Bearing: ${location.bearing}°
                Altitude: ${location.altitude}m
                Time: ${location.time}
            """.trimIndent())
        } ?: Log.d(TAG, "Initial location received")

        // Check for vehicle motion based on GPS speed
        checkVehicleMotion(location)
    }

    private fun checkVehicleMotion(location: Location) {
        val currentTime = System.currentTimeMillis()
        val speed = location.speed

        // Only process location updates with good accuracy
        if (location.accuracy > GPS_MIN_ACCURACY) {
            Log.d(TAG, "Skipping location update due to poor accuracy: ${location.accuracy}m")
            return
        }

        // Enhanced vehicle state detection
        if (speed >= vehicleMotionThreshold && !isInVehicle &&
            currentTime - lastVehicleStateChange > VEHICLE_STATE_DEBOUNCE_TIME) {
            isInVehicle = true
            lastVehicleStateChange = currentTime
            Log.i(TAG, "Vehicle motion detected - Speed: $speed m/s, Accuracy: ${location.accuracy}m")
            logVehicleTransition("ENTER")
        } else if (speed < VEHICLE_EXIT_SPEED_THRESHOLD && isInVehicle &&
            currentTime - lastVehicleStateChange > VEHICLE_STATE_DEBOUNCE_TIME) {
            isInVehicle = false
            lastVehicleStateChange = currentTime
            Log.i(TAG, "Vehicle motion stopped - Speed: $speed m/s, Accuracy: ${location.accuracy}m")
            logVehicleTransition("EXIT")
        }
    }

    private fun logVehicleTransition(type: String) {
        lastLocation?.let { location ->
            val logEntry = """
                Vehicle $type:
                Time: ${System.currentTimeMillis()}
                Location: ${location.latitude}, ${location.longitude}
                Speed: ${location.speed} m/s
                Accuracy: ${location.accuracy}m
                Bearing: ${location.bearing}°
            """.trimIndent()
            Log.i(TAG, logEntry)
            
            // TODO: Add your preferred method to save this data (e.g., to a database or file)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSensorLogTime >= SENSOR_LOG_INTERVAL) {
            lastSensorLogTime = currentTime

            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // Calculate total acceleration for vehicle motion detection
                    val totalAcceleration = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                    // Use acceleration data to improve vehicle motion detection
                    if (totalAcceleration > SIGNIFICANT_ACCELERATION_THRESHOLD) {
                        Log.d(TAG, "Significant acceleration detected: $totalAcceleration m/s²")
                        lastLocation?.let { checkVehicleMotion(it) }
                    }

                    Log.d(TAG, "Accelerometer - X: $x, Y: $y, Z: $z, Total: $totalAcceleration")
                }
                Sensor.TYPE_GYROSCOPE -> {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    Log.d(TAG, "Gyroscope - X: $x, Y: $y, Z: $z")
                }
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