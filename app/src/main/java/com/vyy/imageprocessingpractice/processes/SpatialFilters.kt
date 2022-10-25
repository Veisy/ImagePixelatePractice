package com.vyy.imageprocessingpractice.processes

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toDrawable
import com.vyy.imageprocessingpractice.utils.lastProcessTime
import kotlin.math.pow
import kotlin.math.sqrt

// Apply min filter to reduce salt and pepper noise
fun minFilter(bitmap: Bitmap, resources: Resources): BitmapDrawable =
    spatialFilter(bitmap, resources, ::getMinPixel)

// Apply max filter to reduce salt and pepper noise
fun maxFilter(bitmap: Bitmap, resources: Resources): BitmapDrawable =
    spatialFilter(bitmap, resources, ::getMaxPixel)

// Apply median filter to reduce salt and pepper noise
fun medianFilter(bitmap: Bitmap, resources: Resources): BitmapDrawable =
    spatialFilter(bitmap, resources, ::getMedianPixel)

// Apply average filter to reduce salt and pepper noise
fun averageFilter(bitmap: Bitmap, resources: Resources): BitmapDrawable =
    spatialFilter(bitmap, resources, ::getAveragePixel)

// Apply laplacian filter to sharpen the image
fun laplacianFilter(bitmap: Bitmap, resources: Resources): BitmapDrawable =
    spatialFilter(bitmap, resources, ::getLaplacianPixel)

fun sobelGradientFilter(bitmap: Bitmap, resources: Resources): BitmapDrawable =
    spatialFilter(bitmap, resources, ::getSobelGradientPixel)

// Apply Power-Law (Gamma) Transform
fun gammaTransformation(bitmap: Bitmap, resources: Resources): BitmapDrawable =
    spatialFilter(bitmap, resources, ::getGammaPixel)

// Take filter function as parameter and apply it to each pixel.
private fun spatialFilter(
    bitmap: Bitmap, resources: Resources, filterFunction: (IntArray, Int, Int, Int) -> Int
): BitmapDrawable {
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

    for (i in -2..2) {
        for (j in -2..2) {
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

    for (i in -2..2) {
        for (j in -2..2) {
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
    val reds = IntArray(25)
    val greens = IntArray(25)
    val blues = IntArray(25)

    var squareIndex = 0
    for (i in -2..2) {
        for (j in -2..2) {
            val imageIndex = (y + i) * width + (x + j)
            if (imageIndex >= 0 && imageIndex < pixels.size) {
                val pixel = pixels[imageIndex]
                val red = pixel shr 16 and 0xFF
                val green = pixel shr 8 and 0xFF
                val blue = pixel and 0xFF
                reds[squareIndex] = red
                greens[squareIndex] = green
                blues[squareIndex] = blue
                squareIndex++
            }
        }
    }

    reds.sort()
    greens.sort()
    blues.sort()

    val medianRed = reds[12]
    val medianGreen = greens[12]
    val medianBlue = blues[12]

    return 0xFF000000.toInt() or (medianRed shl 16) or (medianGreen shl 8) or medianBlue
}

// Get the average pixel value of the 3x3 neighborhood
private fun getAveragePixel(pixels: IntArray, width: Int, x: Int, y: Int): Int {
    var red = 0
    var green = 0
    var blue = 0

    for (i in -2..2) {
        for (j in -2..2) {
            val index = (y + i) * width + (x + j)
            if (index < 0 || index >= pixels.size) continue
            val pixel = pixels[index]
            red += pixel shr 16 and 0xff
            green += pixel shr 8 and 0xff
            blue += pixel and 0xff
        }
    }

    red /= 25
    green /= 25
    blue /= 25

    return 0xff000000.toInt() or (red shl 16) or (green shl 8) or blue
}

private fun getLaplacianPixel(pixels: IntArray, width: Int, x: Int, y: Int): Int {
    // A discrete convolution kernel that can approximate the second derivatives in the definition of the Laplacian.
    // Commonly used small kernels
    val laplacianFilter = arrayOf(
        intArrayOf(-1, -1, -1), intArrayOf(-1, 8, -1), intArrayOf(-1, -1, -1)
    )

    var newRed = 0
    var newGreen = 0
    var newBlue = 0

    for (i in -1..1) {
        for (j in -1..1) {
            val index = (y + i) * width + (x + j)
            if (index < 0 || index >= pixels.size) continue
            val pixel = pixels[index]
            val red = pixel shr 16 and 0xff
            val green = pixel shr 8 and 0xff
            val blue = pixel and 0xff

            newRed += red * laplacianFilter[i + 1][j + 1]
            newGreen += green * laplacianFilter[i + 1][j + 1]
            newBlue += blue * laplacianFilter[i + 1][j + 1]
        }
    }

    newRed = if (newRed > 255) 255 else if (newRed < 0) 0 else newRed
    newGreen = if (newGreen > 255) 255 else if (newGreen < 0) 0 else newGreen
    newBlue = if (newBlue > 255) 255 else if (newBlue < 0) 0 else newBlue

    return 0xff000000.toInt() or (newRed shl 16) or (newGreen shl 8) or newBlue
}

private fun getSobelGradientPixel(pixels: IntArray, width: Int, x: Int, y: Int): Int {
    val sobelX = arrayOf(
        intArrayOf(-1, 0, 1), intArrayOf(-2, 0, 2), intArrayOf(-1, 0, 1)
    )
    val sobelY = arrayOf(
        intArrayOf(-1, -2, -1), intArrayOf(0, 0, 0), intArrayOf(1, 2, 1)
    )

    var redX = 0
    var greenX = 0
    var blueX = 0
    var redY = 0
    var greenY = 0
    var blueY = 0

    for (i in -1..1) {
        for (j in -1..1) {
            val index = (y + i) * width + (x + j)
            if (index < 0 || index >= pixels.size) continue
            val pixel = pixels[index]
            val red = pixel shr 16 and 0xFF
            val green = pixel shr 8 and 0xFF
            val blue = pixel and 0xFF

            redX += red * sobelX[i + 1][j + 1]
            greenX += green * sobelX[i + 1][j + 1]
            blueX += blue * sobelX[i + 1][j + 1]
            redY += red * sobelY[i + 1][j + 1]
            greenY += green * sobelY[i + 1][j + 1]
            blueY += blue * sobelY[i + 1][j + 1]
        }
    }

    val red = sqrt((redX * redX + redY * redY).toDouble()).toInt()
    val green = sqrt((greenX * greenX + greenY * greenY).toDouble()).toInt()
    val blue = sqrt((blueX * blueX + blueY * blueY).toDouble()).toInt()

    return 0xFF000000.toInt() or (red shl 16) or (green shl 8) or blue
}

private fun getGammaPixel(pixels: IntArray, width: Int, x: Int, y: Int): Int {
    val gamma = 0.5

    val index = y * width + x
    if (index < 0 || index >= pixels.size) return 0
    val pixel = pixels[index]

    val red = pixel shr 16 and 0xFF
    val green = pixel shr 8 and 0xFF
    val blue = pixel and 0xFF

    val newRed = (red / 255.0).pow(gamma) * 255
    val newGreen = (green / 255.0).pow(gamma) * 255
    val newBlue = (blue / 255.0).pow(gamma) * 255

    return 0xFF000000.toInt() or (newRed.toInt().coerceAtMost(255) shl 16) or (newGreen.toInt()
        .coerceAtMost(255) shl 8) or newBlue.toInt().coerceAtMost(255)
}







