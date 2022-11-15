package com.vyy.intelligenteye.processes

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toDrawable
import com.vyy.intelligenteye.utils.lastProcessTime

// Crop bitmap image and return as BitmapDrawable
fun crop(
    bitmap: Bitmap, fromX: Int, fromY: Int, toX: Int, toY: Int, resources: Resources
): BitmapDrawable {
    // We use this variable to check if enough time has passed for a new operation.
    lastProcessTime = System.currentTimeMillis()

    // width and height of the picture
    val width: Int = bitmap.width
    val height: Int = bitmap.height

    val newWidth = toX - fromX
    val newHeight = toY - fromY

    val bitmapPixels = IntArray(width * height)

    // Get all pixels of the image, and load into bitmapPixels array.
    bitmap.getPixels(bitmapPixels, 0, width, 0, 0, width, height)

    // Crop bitmapPixels array
    val croppedPixels = IntArray(newWidth * newHeight)
    var index = 0
    for (y in fromY until toY) {
        for (x in fromX until toX) {
            croppedPixels[index] = bitmapPixels[y * width + x]
            index++
        }
    }

    // Create empty bitmap with the new width and height
    val croppedBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
    croppedBitmap.setPixels(croppedPixels, 0, newWidth, 0, 0, newWidth, newHeight)
    return croppedBitmap.toDrawable(resources)
}