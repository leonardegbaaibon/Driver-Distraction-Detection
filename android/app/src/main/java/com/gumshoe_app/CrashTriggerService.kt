package com.gumshoe_app

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.sqrt

class CrashTriggerService : Service(), SensorEventListener, LocationListener {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "CrashTriggerServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    // Variables
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager

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
        val x = values[0]
        val y = values[1]
        val z = values[2]

        Log.d("CrashTriggerService", "Accelerometer Data - X: $x, Y: $y, Z: $z")
        sendDataToBackend("accelerometer", x, y, z)
    }

    private fun handleGyroscopeData(values: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]

        Log.d("CrashTriggerService", "Gyroscope Data - X: $x, Y: $y, Z: $z")
        sendDataToBackend("gyroscope", x, y, z)
    }

    private fun sendDataToBackend(sensorType: String, x: Float, y: Float, z: Float) {
        val url = "https://your-backend-endpoint.com/sensor-data" // Replace with your backend URL
        val jsonData = JSONObject().apply {
            put("sensor_type", sensorType)
            put("x", x)
            put("y", y)
            put("z", z)
            put("timestamp", System.currentTimeMillis())
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                connection.outputStream.use { it.write(jsonData.toString().toByteArray()) }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("CrashTriggerService", "Data sent successfully")
                } else {
                    Log.e("CrashTriggerService", "Failed to send data: $responseCode")
                }
            } catch (e: Exception) {
                Log.e("CrashTriggerService", "Error sending data", e)
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        // Optional: If location data is needed, log and send it here
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
