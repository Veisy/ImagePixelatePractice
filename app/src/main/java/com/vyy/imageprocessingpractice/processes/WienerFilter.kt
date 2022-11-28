package com.vyy.imageprocessingpractice.processes

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.vyy.imageprocessingpractice.utils.bitmapToDoubleArray
import com.vyy.imageprocessingpractice.utils.doubleArrayToBitmap
import com.vyy.imageprocessingpractice.utils.lastProcessTime
import com.vyy.imageprocessingpractice.utils.nearest2Power
import org.jtransforms.fft.DoubleFFT_2D
import kotlin.math.pow

// Apply Wiener filter to Bitmap image
fun applyWienerFilter(bitmap: Bitmap, resources: Resources, k: Double): BitmapDrawable {
    // We use this variable to check if enough time has passed for a new operation.
    lastProcessTime = System.currentTimeMillis()

    val width = nearest2Power(bitmap.width)
    val height = nearest2Power(bitmap.height)

    // Since FFT works only with 2's power images, we need to resize image to nearest 2's power
    val bitmapWithNearest2PowerDimens = resize(bitmap, width, height, resources).toBitmap()

    val imageDoubleArray = bitmapToDoubleArray(bitmapWithNearest2PowerDimens)
    // Apply 2D FFT
    DoubleFFT_2D(width.toLong(), height.toLong()).realForward(imageDoubleArray)

    // Apply Wiener filter
    val wienerFilter = wienerFilter(imageDoubleArray, k)

    // Apply inverse FFT to complex numbers
    DoubleFFT_2D(width.toLong(), height.toLong()).realInverse(wienerFilter, true)

    return doubleArrayToBitmap(wienerFilter, width, height).toDrawable(resources)
}

// Apply Wiener filter to filter motion blur
fun wienerFilter(imageDoubleArray: DoubleArray, k: Double): DoubleArray {
    val wienerFilter = DoubleArray(imageDoubleArray.size)

    // Calculate power spectrum
    val powerSpectrum = DoubleArray(imageDoubleArray.size / 2)
    for (i in 0 until imageDoubleArray.size / 2) {
        powerSpectrum[i] = imageDoubleArray[i * 2].pow(2) + imageDoubleArray[i * 2 + 1].pow(2)
    }

    // Calculate Wiener filter
    for (i in 0 until imageDoubleArray.size / 2) {
        wienerFilter[i * 2] = imageDoubleArray[i * 2] / (powerSpectrum[i] + k)
        wienerFilter[i * 2 + 1] = imageDoubleArray[i * 2 + 1] / (powerSpectrum[i] + k)
    }

    return wienerFilter
}


