package com.gumshoe_app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.util.Base64
import android.util.Log
import androidx.core.app.ActivityCompat
import com.facebook.react.bridge.*
import java.nio.ByteBuffer

class CameraModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null

    override fun getName(): String {
        return "CameraModule"
    }

    @ReactMethod
    fun captureImage(promise: Promise) {
        cameraManager = reactContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = getFrontCameraId() // Get front camera ID

        if (ActivityCompat.checkSelfPermission(reactContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera(promise)
        } else {
            promise.reject("PermissionDenied", "Camera permission not granted.")
        }
    }

    private fun openCamera(promise: Promise) {
        cameraManager?.let { manager ->
            try {
                manager.openCamera(cameraId!!, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        captureImageFromCamera(camera, promise)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        promise.reject("CameraError", "Error opening camera: $error")
                    }
                }, null)
            } catch (e: Exception) {
                promise.reject("CameraError", e.localizedMessage)
            }
        }
    }

    private fun captureImageFromCamera(camera: CameraDevice, promise: Promise) {
        val imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)

        val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder.addTarget(imageReader.surface)

        // Setting up the ImageReader listener
        imageReader.setOnImageAvailableListener({ reader ->
            val image: Image = reader.acquireLatestImage()
            val buffer: ByteBuffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
            promise.resolve(base64Image)
            image.close()
        }, null)

        // Using CameraCaptureSession.Builder
        camera.createCaptureSession(
            listOf(imageReader.surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        session.capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                result: TotalCaptureResult
                            ) {
                                super.onCaptureCompleted(session, request, result)
                                Log.d("CameraModule", "Image capture completed.")
                            }
                        }, null)
                    } catch (e: Exception) {
                        promise.reject("CameraError", "Failed to capture image: ${e.localizedMessage}")
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    promise.reject("CameraError", "Failed to configure capture session.")
                }
            },
            null
        )
    }

    private fun getFrontCameraId(): String {
        val cameraIdList = cameraManager?.cameraIdList
        cameraIdList?.forEach { id ->
            val characteristics = cameraManager?.getCameraCharacteristics(id)
            val facing = characteristics?.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return id
            }
        }
        throw RuntimeException("No front camera found.")
    }
}
