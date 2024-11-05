package com.gumshoe_app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

class MotionSensorModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), SensorEventListener {

    private var sensorManager: SensorManager = reactContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var promise: Promise? = null

    // Variables to store previous values for gesture detection
    private var lastAccelValues = FloatArray(3)
    private var lastGyroValues = FloatArray(3)
    private var gestureDetected = false // Flag to prevent multiple gesture detections in quick succession

    override fun getName(): String {
        return "MotionSensorModule"
    }

    @ReactMethod
    fun startSensorDetection(promise: Promise) {
        this.promise = promise
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)
    }

    @ReactMethod
    fun stopSensorDetection() {
        sensorManager.unregisterListener(this)
        promise = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                handleAccelerometerData(event.values)
            }
            Sensor.TYPE_GYROSCOPE -> {
                handleGyroscopeData(event.values)
            }
        }
    }

    private fun handleAccelerometerData(values: FloatArray) {
        val xAcc = values[0]
        val yAcc = values[1]
        val zAcc = values[2]

        // Thresholds for detecting slight and significant movements
        val slightMovementThreshold = 0.2f
        val significantMovementThreshold = 1.0f

        // Detect slight movement (e.g., phone being shifted)
        if (Math.abs(xAcc - lastAccelValues[0]) > slightMovementThreshold ||
            Math.abs(yAcc - lastAccelValues[1]) > slightMovementThreshold ||
            Math.abs(zAcc - lastAccelValues[2]) > slightMovementThreshold) {

            if (!gestureDetected) {
                if (Math.abs(xAcc - lastAccelValues[0]) > significantMovementThreshold ||
                    Math.abs(yAcc - lastAccelValues[1]) > significantMovementThreshold ||
                    Math.abs(zAcc - lastAccelValues[2]) > significantMovementThreshold) {
                    // Significant movement detected (e.g., phone picked up)
                    resolveGesture("Significant movement detected!")
                } else {
                    // Slight movement detected (e.g., phone shifted)
                    resolveGesture("Slight movement detected!")
                }
            }
        }

        // Store last accelerometer values for comparison
        lastAccelValues = values.clone()
    }

    private fun handleGyroscopeData(values: FloatArray) {
        val xRate = values[0] // Rotation around X-axis
        val yRate = values[1] // Rotation around Y-axis
        val zRate = values[2] // Rotation around Z-axis

        // Gesture detection logic
        detectGesture(xRate, yRate, zRate)

        // Store last gyroscope values for future comparisons
        lastGyroValues = values.clone()
    }

    private fun detectGesture(xRate: Float, yRate: Float, zRate: Float) {
        // Define thresholds for slight and significant gestures
        val slightGestureThreshold = 0.3f
        val significantGestureThreshold = 1.5f

        // Prevent multiple gestures from being detected in quick succession
        if (gestureDetected) return

        // Detect slight gestures (e.g., small rotations)
        when {
            xRate > slightGestureThreshold && xRate < significantGestureThreshold -> {
                resolveGesture("Slight rotation detected!")
            }
            xRate > significantGestureThreshold -> {
                resolveGesture("Significant swipe right detected!")
            }
            xRate < -significantGestureThreshold -> {
                resolveGesture("Significant swipe left detected!")
            }
            yRate > significantGestureThreshold -> {
                resolveGesture("Significant swipe down detected!")
            }
            yRate < -significantGestureThreshold -> {
                resolveGesture("Significant swipe up detected!")
            }
            zRate > significantGestureThreshold -> {
                resolveGesture("Significant clockwise circular motion detected!")
            }
            zRate < -significantGestureThreshold -> {
                resolveGesture("Significant counterclockwise circular motion detected!")
            }
        }
    }

    private fun resolveGesture(gesture: String) {
        if (promise != null) {
            promise?.resolve(gesture)
            gestureDetected = true
            resetPromise()
        }
    }

    private fun resetPromise() {
        promise = null
        gestureDetected = false // Reset the gesture detection flag
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle changes in sensor accuracy if needed
    }
}
