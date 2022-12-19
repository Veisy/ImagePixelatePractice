package com.vyy.imageprocessingpractice.processes

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toDrawable
import com.vyy.imageprocessingpractice.utils.lastProcessTime

// Apply OTSU's method to a bitmap
fun applyOtsuMethod(bitmap: Bitmap, resources: Resources): BitmapDrawable {
    // We use this variable to check if enough time has passed for a new operation.
    lastProcessTime = System.currentTimeMillis()

    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val otsuPixels = IntArray(width * height)

    // Get histogram
    val histogram = IntArray(256)
    for (i in pixels.indices) {
        val grayScale = Color.red(pixels[i])
        histogram[grayScale]++
    }

    // Get total number of pixels
    val total = pixels.size

    // Get sum of all pixels
    var sum = 0
    for (i in 0 until 256) {
        sum += i * histogram[i]
    }

    var sumB = 0
    var wB = 0
    var wF: Int

    var varMax = 0.0
    var threshold = 0

    for (i in 0 until 256) {
        wB += histogram[i]
        if (wB == 0) {
            continue
        }

        wF = total - wB
        if (wF == 0) {
            break
        }

        sumB += i * histogram[i]
        val mB = sumB / wB
        val mF = (sum - sumB) / wF

        val varBetween = wB * wF * (mB - mF)* (mB - mF)


        if (varBetween > varMax) {
            varMax = varBetween.toDouble()
            threshold = i
        }
    }

    for (i in pixels.indices) {
        val grayScale = Color.red(pixels[i])
        if (grayScale > threshold) {
            otsuPixels[i] = Color.rgb(255, 255, 255)
        } else {
            otsuPixels[i] = Color.rgb(0, 0, 0)
        }
    }

    val otsuBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    otsuBitmap.setPixels(otsuPixels, 0, width, 0, 0, width, height)
    return otsuBitmap.toDrawable(resources)
}


// Apply average thresholding to a bitmap
fun applyAverageThreshold(bitmap: Bitmap, resources: Resources): BitmapDrawable {
    // We use this variable to check if enough time has passed for a new operation.
    lastProcessTime = System.currentTimeMillis()

    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val averagePixels = IntArray(width * height)

    // Get average of all pixels
    var sum = 0
    for (i in pixels.indices) {
        sum += Color.red(pixels[i])
    }
    val average = sum / pixels.size

    for (i in pixels.indices) {
        val r = pixels[i] shr 16 and 0xff
        val g = pixels[i] shr 8 and 0xff
        val b = pixels[i] and 0xff

        val grayScale = (r + g + b) / 3
        val thresholdValue = if (grayScale > average) 255 else 0
        averagePixels[i] = Color.rgb(thresholdValue, thresholdValue, thresholdValue)
    }
    val averageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    averageBitmap.setPixels(averagePixels, 0, width, 0, 0, width, height)
    return averageBitmap.toDrawable(resources)
}

// divide the picture into 25 parts, make a threshold according to the average value of each part, finally reassemble the 25 parts
fun applySpecialThreshold(bitmap: Bitmap, resources: Resources): BitmapDrawable {
    // We use this variable to check if enough time has passed for a new operation.
    lastProcessTime = System.currentTimeMillis()

    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val specialPixels = IntArray(width * height)

    // divide the picture into 25 parts, make a threshold according to the average value of each part, finally reassemble the 25 parts
    val partWidth = width / 5
    val partHeight = height / 5
    for (i in 0 until 5) {
        for (j in 0 until 5) {
            var sum = 0
            for (k in 0 until partWidth) {
                for (l in 0 until partHeight) {
                    sum += Color.red(pixels[(i * partWidth + k) + (j * partHeight + l) * width])
                }
            }
            val average = sum / (partWidth * partHeight)
            for (k in 0 until partWidth) {
                for (l in 0 until partHeight) {
                    val thresholdValue = if (Color.red(pixels[(i * partWidth + k) + (j * partHeight + l) * width]) > average) 255 else 0
                    specialPixels[(i * partWidth + k) + (j * partHeight + l) * width] = Color.rgb(thresholdValue, thresholdValue, thresholdValue)
                }
            }
        }
    }
    val specialBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    specialBitmap.setPixels(specialPixels, 0, width, 0, 0, width, height)
    return specialBitmap.toDrawable(resources)
}




