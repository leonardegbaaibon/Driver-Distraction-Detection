package com.gumshoe_app

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UltrasonicModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "UltrasonicModule"
    }

    @ReactMethod
    fun generateUltrasonic(frequency: Double, duration: Double, promise: Promise) {
        GlobalScope.launch {
            try {
                // AudioTrack configuration
                val sampleRate = 44100 // Sample rate for audio
                val numSamples = (duration * sampleRate).toInt()
                val generatedSnd = DoubleArray(numSamples)
                val buffer = ShortArray(numSamples)

                // Generate sine wave
                for (i in 0 until numSamples) {
                    generatedSnd[i] = Math.sin(2.0 * Math.PI * i / (sampleRate / frequency))
                }

                // Convert the sine wave to 16-bit PCM data
                for (i in 0 until numSamples) {
                    buffer[i] = (generatedSnd[i] * 32767).toInt().toShort()
                }

                // Initialize AudioTrack object
                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffer.size * 2,
                    AudioTrack.MODE_STATIC
                )

                // Write buffer data to AudioTrack
                audioTrack.write(buffer, 0, buffer.size)

                // Play the audio
                audioTrack.play()

                // Wait for the duration of the sound to finish
                Thread.sleep((duration * 1000).toLong())

                // Stop and release resources
                audioTrack.stop()
                audioTrack.release()

                // Resolve the promise when done
                promise.resolve("Ultrasonic sound played successfully!")
            } catch (e: Exception) {
                promise.reject("ERROR_PLAYING_SOUND", e)
            }
        }
    }
}
