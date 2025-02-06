package com.gumshoe_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
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
                cameraProvider.bindToLifecycle(
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

    // Updated: Convert photo to base64 with compression (and optional resizing)
    fun encodePhotoToBase64(photoFile: File): String {
        // Decode the file into a Bitmap
        val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath) ?: return ""

        // Optionally, resize the bitmap if needed (example code below; adjust as required)
        // val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, originalBitmap.width / 2, originalBitmap.height / 2, true)
        // For this example, we'll use the originalBitmap
        val bitmapToCompress = originalBitmap

        // Compress the bitmap
        val outputStream = ByteArrayOutputStream()
        // Adjust quality (0-100) as needed; lower quality means smaller file size
        bitmapToCompress.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        val compressedBytes = outputStream.toByteArray()

        // Encode to Base64; using NO_WRAP to avoid newline characters if preferred
        return Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
    }
}
