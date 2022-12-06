package com.vyy.imageprocessingpractice.processes

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toDrawable
import com.vyy.imageprocessingpractice.utils.lastProcessTime

var topmostPixelIndex: Int = 0
var leftmostPixelIndex: Int = 0
var rightmostPixelIndex: Int = 0
var bottommostPixelIndex: Int = 0

fun lungOperations(originalBitmap: Bitmap, resources: Resources): List<BitmapDrawable> {

    val a = originalBitmap.toDrawable(resources)

    val b = highlightCorners(a.bitmap, resources)

    val c = crop(
        b.bitmap,
        fromX = topmostPixelIndex,
        fromY = leftmostPixelIndex,
        toX = bottommostPixelIndex,
        toY = rightmostPixelIndex,
        resources
    )

    val d = crop(
        a.bitmap,
        fromX = topmostPixelIndex,
        fromY = leftmostPixelIndex,
        toX = bottommostPixelIndex,
        toY = rightmostPixelIndex,
        resources
    )

    val e = crop(d.bitmap, fromX = (d.bitmap.width/7.5).toInt(), fromY = (d.bitmap.height/10), toX = (d.bitmap.width/1.15).toInt(), toY = (d.bitmap.height/1.4).toInt(), resources)
    return listOf(b, c, d, e)
}

private fun highlightCorners(bitmap: Bitmap, resources: Resources): BitmapDrawable =
    applySpecialFilter(bitmap, resources, ::getHighlightedOrBlackPixel)

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