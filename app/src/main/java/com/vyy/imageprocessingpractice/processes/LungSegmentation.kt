package com.vyy.imageprocessingpractice.processes

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toDrawable
import com.vyy.imageprocessingpractice.utils.lastProcessTime
import kotlin.math.pow
import kotlin.math.sqrt

var topmostPixelIndex: Int = 0
var leftmostPixelIndex: Int = 0
var rightmostPixelIndex: Int = 0
var bottommostPixelIndex: Int = 0

fun lungOperations(originalBitmap: Bitmap, resources: Resources): List<BitmapDrawable> {

    val a = originalBitmap.toDrawable(resources)

    highlightCorners(a.bitmap, resources)

    val b = crop(
        a.bitmap,
        fromX = topmostPixelIndex,
        fromY = leftmostPixelIndex,
        toX = bottommostPixelIndex,
        toY = rightmostPixelIndex,
        resources
    )

    val c = crop(
        b.bitmap,
        fromX = (b.bitmap.width / 7.5).toInt(),
        fromY = (b.bitmap.height / 10),
        toX = (b.bitmap.width / 1.15).toInt(),
        toY = (b.bitmap.height / 1.4).toInt(),
        resources
    )

    val d = lungOperation1(c.bitmap, resources)
    val e = lungOperation2(d.bitmap, resources)
    val f = applyOtsuMethod(e.bitmap, resources)
    return listOf(b, c, d, e, f)
}

private fun highlightCorners(bitmap: Bitmap, resources: Resources): BitmapDrawable =
    applySpecialFilter(bitmap, resources, ::getHighlightedOrBlackPixel)

private fun lungOperation1(bitmap: Bitmap, resources: Resources): BitmapDrawable =
    applySpecialFilter(bitmap, resources, ::operation1)

private fun lungOperation2(bitmap: Bitmap, resources: Resources): BitmapDrawable =
    applySpecialFilter(bitmap, resources, ::operation2)

// Take filter function as parameter and apply it to each pixel.
private fun applySpecialFilter(
    bitmap: Bitmap, resources: Resources, filterFunction: (IntArray, Int, Int, Int) -> Int
): BitmapDrawable {
    // We use this variable to check if enough time has passed for a new operation.
    lastProcessTime = System.currentTimeMillis()

    topmostPixelIndex = 0
    leftmostPixelIndex = 0
    rightmostPixelIndex = 0
    bottommostPixelIndex = 0

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

private fun getHighlightedOrBlackPixel(pixels: IntArray, width: Int, x: Int, y: Int): Int {
    val differenceThreshold = 150
    val nearbyPixels = IntArray(9)

    var squareIndex = 0
    for (i in -1..1) {
        for (j in -1..1) {
            val index = (y + i) * width + (x + j)
            if (index < 0 || index >= pixels.size) continue
            val pixel = pixels[index]
            val red = pixel shr 16 and 0xff
            val green = pixel shr 8 and 0xff
            val blue = pixel and 0xff
            nearbyPixels[squareIndex] = (red + green + blue) / 3
            squareIndex++
        }
    }

    val pixelColor: Int = if ((nearbyPixels.max() - nearbyPixels.min()) > differenceThreshold) {
        if (topmostPixelIndex == 0 || topmostPixelIndex > x) {
            topmostPixelIndex = x
        } else if (leftmostPixelIndex == 0 || leftmostPixelIndex > y) {
            leftmostPixelIndex = y
        } else if (bottommostPixelIndex == 0 || bottommostPixelIndex < x) {
            bottommostPixelIndex = x
        } else if (rightmostPixelIndex == 0 || rightmostPixelIndex < y) {
            rightmostPixelIndex = y
        }

        50
    } else {
        200
    }

    return 0xff000000.toInt() or (pixelColor shl 16) or (pixelColor shl 8) or pixelColor
}

private fun operation1(pixels: IntArray, width: Int, x: Int, y: Int): Int {
    val differenceThreshold = 125
    val nearbyPixels = IntArray(441)

    var squareIndex = 0
    for (i in -10..10) {
        for (j in -10..10) {
            val index = (y + i) * width + (x + j)
            if (index < 0 || index >= pixels.size) continue
            val pixel = pixels[index]
            val red = pixel shr 16 and 0xff
            val green = pixel shr 8 and 0xff
            val blue = pixel and 0xff
            nearbyPixels[squareIndex] = (red + green + blue) / 3
            squareIndex++
        }
    }

    val pixelColor: Int =
        if ((nearbyPixels.max() - nearbyPixels.min()) < differenceThreshold && nearbyPixels.average() > 175) {
            200
        } else {
            pixels[y * width + x]
        }

    return 0xff000000.toInt() or (pixelColor shl 16) or (pixelColor shl 8) or pixelColor
}

private fun operation2(pixels: IntArray, width: Int, x: Int, y: Int): Int {

    // pixel color
    val pixel = pixels[y * width + x]
    val red = pixel shr 16 and 0xff
    val green = pixel shr 8 and 0xff
    val blue = pixel and 0xff

    val pixelOldColor = (red + green + blue) / 3

    val pixelNewColor: Int = if (pixelOldColor < 100
        && (getDistanceToCenter(x, y, width, pixels.size / width) > width / 2)
    ) {
        200
    } else {
        pixelOldColor
    }

    return 0xff000000.toInt() or (pixelNewColor shl 16) or (pixelNewColor shl 8) or pixelNewColor
}

private fun getDistanceToCenter(x: Int, y: Int, width: Int, height: Int): Double {
    val centerX = width / 2.0
    val centerY = height / 2.0
    return sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
}

