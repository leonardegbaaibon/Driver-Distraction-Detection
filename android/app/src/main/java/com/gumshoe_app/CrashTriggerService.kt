import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlin.math.sqrt

class CrashTriggerService : Service(), SensorEventListener, LocationListener {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "CrashTriggerServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    // Constants
    private val accelThreshold = 50.0f // m/s²
    private val gyroThreshold = 300.0f // degrees/sec
    private val speedDropPercentage = 80.0f // Speed drop threshold
    private val timeWindow = 200 // milliseconds

    // Variables
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private var prevSpeed = 0.0f
    private var potentialCrash = false
    private var isCrash = false
    private var startTime = System.currentTimeMillis()

    override fun onCreate() {
        super.onCreate()
        Log.d("CrashTriggerService", "Service started")

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // Initialize SensorManager and LocationManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Register sensors
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_FASTEST
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_FASTEST
        )

        // Request location updates
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                this
            )
        } catch (e: SecurityException) {
            Log.e("CrashTriggerService", "Location permission not granted")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Crash Detection Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Detects potential vehicle crashes"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Crash Detection Active")
            .setContentText("Monitoring sensors for crash detection.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> handleAccelerometerData(it.values)
                Sensor.TYPE_GYROSCOPE -> handleGyroscopeData(it.values)
            }
        }
    }

    private fun handleAccelerometerData(values: FloatArray) {
        val totalAccel = sqrt(
            (values[0] * values[0] +
                    values[1] * values[1] +
                    values[2] * values[2]).toDouble()
        ).toFloat()

        if (totalAccel >= accelThreshold) {
            Log.d("CrashTriggerService", "Potential crash: High acceleration!")
            potentialCrash = true
        }
    }

    private fun handleGyroscopeData(values: FloatArray) {
        val totalGyro = sqrt(
            (values[0] * values[0] +
                    values[1] * values[1] +
                    values[2] * values[2]).toDouble()
        ).toFloat()

        if (totalGyro >= gyroThreshold) {
            Log.d("CrashTriggerService", "Potential crash: High rotation!")
            potentialCrash = true
        }
    }

    override fun onLocationChanged(location: Location) {
        val currentSpeed = location.speed * 3.6f // Convert m/s to km/h
        val speedDrop = if (prevSpeed > 0) {
            ((prevSpeed - currentSpeed) / prevSpeed) * 100
        } else {
            0.0f
        }

        if (speedDrop >= speedDropPercentage) {
            Log.d("CrashTriggerService", "Potential crash: Sudden speed drop!")
            evaluateCrash()
        }

        prevSpeed = currentSpeed
    }

    private fun evaluateCrash() {
        val currentTime = System.currentTimeMillis()
        if (potentialCrash && (currentTime - startTime) <= timeWindow) {
            isCrash = true
            triggerEmergencyProtocol()
        }
        resetCrashEvaluation()
    }

    private fun triggerEmergencyProtocol() {
        Log.d("CrashTriggerService", "Crash detected! Triggering emergency protocol.")
        // Add emergency response actions here (e.g., send notification, call emergency services)
    }

    private fun resetCrashEvaluation() {
        potentialCrash = false
        startTime = System.currentTimeMillis()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
        Log.d("CrashTriggerService", "Service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
