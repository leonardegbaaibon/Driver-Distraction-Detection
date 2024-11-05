package com.gumshoe_app

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.log2
import kotlin.math.pow

class RecordingModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "RecordingModule"
    }

    @ReactMethod
    fun startRecording(duration: Double, promise: Promise) {
        GlobalScope.launch {
            try {
                val sampleRate = 44100
                val bufferSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                // Initialize AudioRecord
                val audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )

                val audioBuffer = ShortArray(bufferSize)
                audioRecord.startRecording()

                val endTime = System.currentTimeMillis() + (duration * 1000).toLong()

                // Record audio for the specified duration
                val recordedAudioData = mutableListOf<Double>()
                while (System.currentTimeMillis() < endTime) {
                    val readCount = audioRecord.read(audioBuffer, 0, bufferSize)
                    if (readCount > 0) {
                        // Process audio buffer
                        for (i in 0 until readCount) {
                            recordedAudioData.add(audioBuffer[i].toDouble())
                        }
                    }
                }

                audioRecord.stop()
                audioRecord.release()

                // Ensure the audio data size is a power of 2
                val paddedAudioData = padToPowerOfTwo(recordedAudioData)

                // Analyze the recorded audio to identify touch material and extract frequency and amplitude
                val (touchMaterial, frequency, amplitude) = analyzeAudioData(paddedAudioData, sampleRate)

                // Create a WritableMap to return
                val response: WritableMap = Arguments.createMap()
                response.putString("message", "Recording completed successfully!")
                response.putString("material", touchMaterial)
                response.putDouble("frequency", frequency)
                response.putDouble("amplitude", amplitude)

                // Resolve the promise with the WritableMap
                promise.resolve(response)
            } catch (e: Exception) {
                promise.reject("ERROR_RECORDING", e)
            }
        }
    }

    private fun padToPowerOfTwo(audioData: List<Double>): DoubleArray {
        val originalSize = audioData.size
        val nextPowerOfTwo = 2.0.pow(kotlin.math.ceil(log2(originalSize.toDouble()))).toInt()

        // Create a new array of the nearest power of 2 size
        val paddedAudioData = DoubleArray(nextPowerOfTwo)

        // Copy the original audio data into the new array
        for (i in audioData.indices) {
            paddedAudioData[i] = audioData[i]
        }

        // The rest of the array will be padded with zeros (default value of DoubleArray)
        return paddedAudioData
    }

    private fun analyzeAudioData(audioData: DoubleArray, sampleRate: Int): Triple<String, Double, Double> {
        // Calculate average amplitude
        val averageAmplitude = audioData.map { abs(it) }.average()

        // Perform FFT on the padded audio data
        val fftOutput = fft(audioData)

        // Get the frequency with the highest magnitude
        val frequencies = fftOutput.mapIndexed { index, value -> Pair(index.toDouble(), abs(value)) }
        val (dominantFrequencyIndex, maxMagnitude) = frequencies.maxByOrNull { it.second } ?: Pair(0.0, 0.0)

        // Convert to actual frequency
        val dominantFrequency = (dominantFrequencyIndex / audioData.size) * sampleRate

        // Determine touch material based on amplitude
        val touchMaterial = when {
            averageAmplitude < 0.1 -> "No Touch Detected"
            averageAmplitude in 0.1..0.5 -> "Cloth or Plastic"
            averageAmplitude in 0.5..1.0 -> "Human Skin"
            else -> "Unknown Material"
        }

        return Triple(touchMaterial, dominantFrequency, averageAmplitude)
    }

    private fun fft(input: DoubleArray): DoubleArray {
        val n = input.size
        if (n == 1) return doubleArrayOf(input[0])

        if (n % 2 != 0) throw IllegalArgumentException("Input size must be a power of 2")

        val halfSize = n / 2

        // Split the array into even and odd parts
        val even = fft(input.filterIndexed { index, _ -> index % 2 == 0 }.toDoubleArray())
        val odd = fft(input.filterIndexed { index, _ -> index % 2 != 0 }.toDoubleArray())

        val result = DoubleArray(n)
        for (k in 0 until halfSize) {
            val expFactor = -2 * PI * k / n
            val cosExp = cos(expFactor)
            val sinExp = sin(expFactor)
            val oddComponent = odd[k] * cosExp - odd[k] * sinExp

            result[k] = even[k] + oddComponent
            result[k + halfSize] = even[k] - oddComponent
        }

        return result
    }
}
