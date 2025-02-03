package com.gumshoe_app

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

class MainActivity : ReactActivity() {
    private val TAG = "GumshoeApp"
    private val PERMISSIONS_REQUEST_CODE = 123

    // List of required runtime permissions
    private val REQUIRED_PERMISSIONS = listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.INTERNET,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun getMainComponentName(): String = "Gumshoe_app"

    override fun createReactActivityDelegate(): ReactActivityDelegate =
        DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAllPermissions()
    }

    /**
     * Checks all required permissions and requests them if not granted.
     */
    private fun checkAllPermissions() {
        if (!areBasicPermissionsGranted()) {
            requestBasicPermissions()
        } else if (!checkOverlayPermission()) {
            requestOverlayPermission()
        } else if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
        } else {
            startMonitoringService()
        }
    }

    /**
     * Checks if all basic runtime permissions are granted.
     */
    private fun areBasicPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Requests all basic runtime permissions.
     */
    private fun requestBasicPermissions() {
        ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS.toTypedArray(),
            PERMISSIONS_REQUEST_CODE
        )
    }

    /**
     * Checks if the overlay permission is granted.
     */
    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    /**
     * Requests the overlay permission by opening system settings.
     */
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /**
     * Checks if the usage stats permission is granted.
     */
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Requests the usage stats permission by opening system settings.
     */
    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /**
     * Checks if accessibility permission is granted.
     */
//    private fun hasAccessibilityPermission(): Boolean {
//        val accessibilityEnabled = try {
//            Settings.Secure.getInt(
//                contentResolver,
//                Settings.Secure.ACCESSIBILITY_ENABLED
//            )
//        } catch (e: Settings.SettingNotFoundException) {
//            0
//        }
//        return accessibilityEnabled == 1
//    }

    /**
     * Requests accessibility permission by opening system settings.
     */
//    private fun requestAccessibilityPermission() {
//        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        startActivity(intent)
//    }

    /**
     * Handles the result of permission requests.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                checkAllPermissions()
            } else {
                openAppSettings()
            }
        }
    }

    /**
     * Opens the app's settings page for manual permission granting.
     */
    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /**
     * Starts the MonitoringService once all permissions are granted.
     */
    private fun startMonitoringService() {
        Log.d(TAG, "All permissions granted. Starting MonitoringService")
        val monitoringServiceIntent = Intent(this, MonitoringService::class.java)
        startForegroundService(monitoringServiceIntent)
    }

    /**
     * Re-checks permissions when the activity resumes.
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Resuming and re-checking permissions")
        checkAllPermissions()
    }
}

