package com.gumshoe_app

import kotlin.math.*

fun fft(input: DoubleArray): DoubleArray {
    val n = input.size
    if (n <= 1) return input

    // Split the input into even and odd parts
    val even = fft(input.filterIndexed { index, _ -> index % 2 == 0 }.toDoubleArray())
    val odd = fft(input.filterIndexed { index, _ -> index % 2 != 0 }.toDoubleArray())

    val combined = DoubleArray(n)
    for (k in 0 until n / 2) {
        val t = exp(-2.0 * PI * k / n) * odd[k]
        combined[k] = even[k] + t
        combined[k + n / 2] = even[k] - t
    }
    return combined
}
