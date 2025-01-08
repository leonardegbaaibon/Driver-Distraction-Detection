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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class MonitoringService : Service(), SensorEventListener {

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

    companion object {
        const val CHANNEL_ID = "MonitoringServiceChannel"
        const val TAG = "MonitoringService"
        const val MONITORING_INTERVAL = 5000L
        const val NOTIFICATION_ID = 1
        const val SENSOR_LOG_INTERVAL = 1000L // Log every 1 second
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

            // Initialize SensorManager
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

            accelerometer?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            gyroscope?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }

            // Delay starting the foreground service
            Handler(Looper.getMainLooper()).postDelayed({
                if (checkRequiredPermissions()) {
                    setupForegroundService()
                } else {
                    Log.e(TAG, "Required permissions not granted. Foreground service not started.")
                }
            }, 1000L) // Delay foreground service by 1 second
        } catch (e: Exception) {
            Log.e(TAG, "Error during onCreate: ${e.message}", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")

        if (!isMonitoring) {
            // Start monitoring as a background service
            startMonitoring()
        }

        return START_STICKY
    }

    private fun setupForegroundService() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create notification channel if necessary (for Android 8+)
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

            // Create the notification
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Gumshoe Monitoring Active")
                .setContentText("Monitoring phone usage in progress")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()

            // Start the service as a foreground service
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Foreground service started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup foreground service", e)
            stopSelf()
        }
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
                    Log.d(TAG, "Accelerometer - X: $x, Y: $y, Z: $z")
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
        Log.d(TAG, "MonitoringService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    inner class ScreenStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_USER_PRESENT -> startMonitoring()
                Intent.ACTION_SCREEN_OFF -> stopMonitoring()
            }
        }
    }
}
