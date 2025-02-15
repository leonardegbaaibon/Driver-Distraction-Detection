package com.gumshoe_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
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

class CameraHelper(private val context: Context, private val lifecycleOwner: LifecycleOwner) {

    private var imageCapture: ImageCapture? = null
    private var isCameraInitializing = false
    private var pendingPhotoRequest: (() -> Unit)? = null

    init {
        initializeCamera()
    }

    private fun initializeCamera() {
        if (isCameraInitializing) return
        isCameraInitializing = true

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                imageCapture = ImageCapture.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture)
                Log.d("CameraHelper", "Camera initialized successfully")
                pendingPhotoRequest?.invoke()
                pendingPhotoRequest = null
            } catch (e: Exception) {
                Log.e("CameraHelper", "Camera initialization failed: ${e.message}")
            } finally {
                isCameraInitializing = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto(onPhotoCaptured: (File) -> Unit) {
        if (imageCapture == null) {
            Log.w("CameraHelper", "Camera not ready. Queuing photo request")
            pendingPhotoRequest = { takePhoto(onPhotoCaptured) }
            if (!isCameraInitializing) initializeCamera()
            return
        }

        val photoFile = createPhotoFile()
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
                    Log.e("CameraHelper", "Capture failed: ${exception.message}")
                    // Retry initialization on error
                    imageCapture = null
                    initializeCamera()
                }
            }
        )
    }

    fun shutdown() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
                imageCapture = null
                Log.d("CameraHelper", "Camera resources released")
            } catch (e: Exception) {
                Log.e("CameraHelper", "Shutdown failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun createPhotoFile(): File {
        val storageDir = context.getExternalFilesDir("photos") ?: context.filesDir
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        return File.createTempFile("GUMSEE_${timeStamp}_", ".jpg", storageDir).apply {
            parentFile?.mkdirs()
        }
    }

    fun encodePhotoToBase64(photoFile: File): String {
        if (!photoFile.exists()) {
            Log.e("CameraHelper", "Photo file missing: ${photoFile.absolutePath}")
            return "error_missing_file"
        }

        return try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4  // Reduce memory usage
            }
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath, options)
                ?: return "error_decode_failed"

            val maxDimension = 640  // Balance quality and size
            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * 0.5f).toInt(),
                (bitmap.height * 0.5f).toInt(),
                true
            )

            ByteArrayOutputStream().use { outputStream ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP).also {
                    Log.d("CameraHelper", "Encoded image (${it.length} chars)")
                }
            }
        } catch (e: Exception) {
            Log.e("CameraHelper", "Encoding failed: ${e.message}")
            "error_encoding"
        }
    }
}