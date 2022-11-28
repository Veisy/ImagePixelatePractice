package com.vyy.imageprocessingpractice.utils

import android.graphics.Bitmap
import android.graphics.Color

var lastProcessTime: Long = 0
private const val TIME_PROCESSES_TASKS = 400

fun checkEnoughTimePassed() =
    (System.currentTimeMillis() - lastProcessTime) > TIME_PROCESSES_TASKS

// Nearest 2's power
fun nearest2Power(n: Int): Int {
    var i = 1
    while (i < n) {
        i *= 2
    }
    return i
}

// Convert Bitmap image to double array
fun bitmapToDoubleArray(bitmap: Bitmap): DoubleArray {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val doubleArray = DoubleArray(width * height)
    for (i in pixels.indices) {
        // Take average of RGB values
        doubleArray[i] = (Color.red(pixels[i]) + Color.green(pixels[i]) + Color.blue(pixels[i])) / 3.0
    }
    return doubleArray
}

// Convert double array to Bitmap image
fun doubleArrayToBitmap(doubleArray: DoubleArray, width: Int, height: Int): Bitmap {
    val pixels = IntArray(width * height)
    for (i in pixels.indices) {
        // Set pixel RGB values to the same value
        val value = doubleArray[i].toInt()
        pixels[i] = 0xff000000.toInt() or (value shl 16) or (value shl 8) or value
    }
    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    resultBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return resultBitmap
}