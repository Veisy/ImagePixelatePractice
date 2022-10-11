package com.vyy.imageprocessingpractice.processes

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toDrawable
import com.vyy.imageprocessingpractice.utils.lastProcessTime
import kotlin.math.*

// Check if image is gray scale
fun isGrayScale(bitmap: Bitmap): Boolean {
    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            val pixel = bitmap.getPixel(x, y)

            // If there is even a single RGB pixel, the picture is not RGB.
            if (Color.red(pixel) != Color.green(pixel) || Color.blue(pixel) != Color.red(pixel)) {
                return false
            }
        }
    }
    return true
}

// Convert rgb bitmap to gray scale
fun rgbToGray(bitmap: Bitmap, resources: Resources): BitmapDrawable {
    val width = bitmap.width
    val height = bitmap.height
    val grayScaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = bitmap.getPixel(x, y)
            val grayScale = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
            grayScaleBitmap.setPixel(x, y, Color.rgb(grayScale, grayScale, grayScale))
        }
    }
    return grayScaleBitmap.toDrawable(resources)
}

// convert rgb bitmap to hsi bitmap
fun rgbToHsi(bitmap: Bitmap, resources: Resources): BitmapDrawable {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val hsiPixels = IntArray(width * height)

    for (i in pixels.indices) {
        val r = Color.red(pixels[i])
        val g = Color.green(pixels[i])
        val b = Color.blue(pixels[i])


        val hue = getHsiHue(r, g, b)
        val saturation = getHsiSaturation(r, g, b)
        val intensity = getHsiIntensity(r, g, b)
        hsiPixels[i] = Color.rgb(hue, saturation, intensity)
    }
    val hsiBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    hsiBitmap.setPixels(hsiPixels, 0, width, 0, 0, width, height)
    return hsiBitmap.toDrawable(resources)
}

private fun getHsiHue(r: Int, g: Int, b: Int): Int {
    val denominator = sqrt((r - g).toDouble().pow(2) + (r - b) * (g - b)) + 0.000001

    val h = acos((0.5 * ((r - g) + (r - b))) / denominator)
    return if (b > g) {
        (360 - Math.toDegrees(h)).toInt()
    } else {
        Math.toDegrees(h).toInt()
    }
}

private fun getHsiSaturation(r: Int, g: Int, b: Int): Int {
    val min = minOf(r, g, b)
    return if (r + g + b == 0) {
        255
    } else {
        1 - 3 * min / (r + g + b)
    }
}

private fun getHsiIntensity(r: Int, g: Int, b: Int): Int {
    return (r + g + b) / 3
}

fun rgbToHsv(
    bitmap: Bitmap, resources: Resources
): BitmapDrawable {
    // We use this variable to check if enough time has passed for a new operation.
    lastProcessTime = System.currentTimeMillis()

    val width: Int = bitmap.width
    val height: Int = bitmap.height

    val bitmapPixels = IntArray(width * height)
    bitmap.getPixels(bitmapPixels, 0, width, 0, 0, width, height)

    val hsvPixels = IntArray(width * height)

    for (index in bitmapPixels.indices) {
        hsvPixels[index] = getHsvPixel(
            Color.red(bitmapPixels[index]),
            Color.green(bitmapPixels[index]),
            Color.blue(bitmapPixels[index])
        )
    }

    val hsvBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    hsvBitmap.setPixels(hsvPixels, 0, width, 0, 0, width, height)
    return hsvBitmap.toDrawable(resources)
}

private fun getHsvPixel(r: Int, g: Int, b: Int): Int {
    var h: Double
    var s: Double
    var v: Double

    val delta: Double

    val min = minOf(r, g, b)
    val max = maxOf(r, g, b)

    v = max.toDouble()
    delta = (max - min).toDouble()

    if (max != 0)
        s = delta / max
    else {
        s = 0.0
        h = -1.0
        return Color.rgb(h.toInt(), s.toInt(), v.toInt())
    }

    h = if (r == max)
        (g - b) / delta // between yellow & magenta
    else if (g == max)
        2 + (b - r) / delta // between cyan & yellow
    else
        4 + (r - g) / delta // between magenta & cyan

    h *= 60.0 // degrees

    if (h < 0)
        h += 360.0

    h *= 1.0
    s *= 100.0
    v = (v / 256.0) * 100.0
    return Color.rgb(h.toInt(), s.toInt(), v.toInt())
}



