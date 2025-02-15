package com.gumshoe_app

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class CapturePhotoActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CapturePhotoActivity", "onCreate called")
        capturePhoto()
    }

    private fun capturePhoto() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                imageCapture = ImageCapture.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
                val photoFile = createPhotoFile()
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture?.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(this),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Log.d("CapturePhotoActivity", "Photo captured: ${photoFile.absolutePath}")
                            finish() // Close the activity immediately after capturing.
                        }
                        override fun onError(exception: ImageCaptureException) {
                            Log.e("CapturePhotoActivity", "Error capturing photo: ${exception.message}")
                            finish()
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("CapturePhotoActivity", "Error initializing camera: ${e.message}")
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun createPhotoFile(): File {
        val storageDir = getExternalFilesDir("photos")
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }
}
