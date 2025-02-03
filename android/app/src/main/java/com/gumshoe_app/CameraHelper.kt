package com.gumshoe_app

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Base64

class CameraHelper(private val context: Context, private val lifecycleOwner: LifecycleOwner) {

    private var imageCapture: ImageCapture? = null

    fun takePhoto(onPhotoCaptured: (File) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Set up the image capture use case
            imageCapture = ImageCapture.Builder().build()

            // Select the back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind the camera to the lifecycle
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageCapture
                )

                // Create a file to save the photo
                val photoFile = createPhotoFile()

                // Set up the image capture listener
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture?.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Log.d("CameraHelper", "Photo saved: ${photoFile.absolutePath}")
                            onPhotoCaptured(photoFile)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("CameraHelper", "Error capturing photo: ${exception.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("CameraHelper", "Error setting up camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun createPhotoFile(): File {
        val storageDir = context.getExternalFilesDir("photos")
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    // Optional: Convert photo to base64
    fun encodePhotoToBase64(photoFile: File): String {
        val inputStream = photoFile.inputStream()
        val bytes = inputStream.readBytes()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
    }
}