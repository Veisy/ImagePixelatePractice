package com.vyy.imageprocessingpractice.processes

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toDrawable
import com.vyy.imageprocessingpractice.utils.lastProcessTime

// Apply min filter to reduce salt and pepper noise
fun minFilter(bitmap: Bitmap, resources: Resources): BitmapDrawable
    = spatialFilter(bitmap, resources, ::getMinPixel)

fun maxFilter(bitmap: Bitmap, resources: Resources): BitmapDrawable
        = spatialFilter(bitmap, resources, ::getMaxPixel)

fun medianFilter(bitmap: Bitmap, resources: Resources): BitmapDrawable
        = spatialFilter(bitmap, resources, ::getMedianPixel)

fun averageFilter(bitmap: Bitmap, resources: Resources): BitmapDrawable
        = spatialFilter(bitmap, resources, ::getAveragePixel)

// Take filter function as parameter and apply it to each pixel
fun spatialFilter(bitmap: Bitmap, resources: Resources, filterFunction: (IntArray, Int, Int, Int) -> Int): BitmapDrawable {
    // We use this variable to check if enough time has passed for a new operation.
    lastProcessTime = System.currentTimeMillis()

    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val newPixels = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val index = y * width + x
            val newPixel = filterFunction(pixels, width, x, y)
            newPixels[index] = newPixel
        }
    }

    val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    newBitmap.setPixels(newPixels, 0, width, 0, 0, width, height)
    return newBitmap.toDrawable(resources)
}

// Get the min pixel value of the 3x3 neighborhood
private fun getMinPixel(pixels: IntArray, width: Int, x: Int, y: Int): Int {
    var minRed = 255
    var minGreen = 255
    var minBlue = 255

    for (i in -1..1) {
        for (j in -1..1) {
            val index = (y + i) * width + (x + j)
            if (index < 0 || index >= pixels.size) continue
            val pixel = pixels[index]
            val red = pixel shr 16 and 0xff
            val green = pixel shr 8 and 0xff
            val blue = pixel and 0xff
            if (red < minRed) minRed = red
            if (green < minGreen) minGreen = green
            if (blue < minBlue) minBlue = blue
        }
    }

    return 0xff000000.toInt() or (minRed shl 16) or (minGreen shl 8) or minBlue
}

// Get the max pixel value of the 3x3 neighborhood
private fun getMaxPixel(pixels: IntArray, width: Int, x: Int, y: Int): Int {
    var maxRed = 0
    var maxGreen = 0
    var maxBlue = 0

    for (i in -1..1) {
        for (j in -1..1) {
            val index = (y + i) * width + (x + j)
            if (index < 0 || index >= pixels.size) continue
            val pixel = pixels[index]
            val red = pixel shr 16 and 0xff
            val green = pixel shr 8 and 0xff
            val blue = pixel and 0xff
            if (red > maxRed) maxRed = red
            if (green > maxGreen) maxGreen = green
            if (blue > maxBlue) maxBlue = blue
        }
    }

    return 0xff000000.toInt() or (maxRed shl 16) or (maxGreen shl 8) or maxBlue
}

// Get the median pixel value of the 3x3 neighborhood
private fun getMedianPixel(pixels: IntArray, width: Int, x: Int, y: Int): Int {
    val reds = mutableListOf<Int>()
    val greens = mutableListOf<Int>()
    val blues = mutableListOf<Int>()

    for (i in -1..1) {
        for (j in -1..1) {
            val index = (y + i) * width + (x + j)
            if (index >= 0 && index < pixels.size) {
                val pixel = pixels[index]
                val red = pixel shr 16 and 0xFF
                val green = pixel shr 8 and 0xFF
                val blue = pixel and 0xFF
                reds.add(red)
                greens.add(green)
                blues.add(blue)
            }
        }
    }

    reds.sort()
    greens.sort()
    blues.sort()

    val medianRed = reds[4]
    val medianGreen = greens[4]
    val medianBlue = blues[4]

    return 0xFF000000.toInt() or (medianRed shl 16) or (medianGreen shl 8) or medianBlue
}

// Get the average pixel value of the 3x3 neighborhood
private fun getAveragePixel(pixels: IntArray, width: Int, x: Int, y: Int): Int {
    var red = 0
    var green = 0
    var blue = 0

    for (i in -1..1) {
        for (j in -1..1) {
            val index = (y + i) * width + (x + j)
            if (index < 0 || index >= pixels.size) continue
            val pixel = pixels[index]
            red += pixel shr 16 and 0xff
            green += pixel shr 8 and 0xff
            blue += pixel and 0xff
        }
    }

    red /= 9
    green /= 9
    blue /= 9

    return 0xff000000.toInt() or (red shl 16) or (green shl 8) or blue
}






