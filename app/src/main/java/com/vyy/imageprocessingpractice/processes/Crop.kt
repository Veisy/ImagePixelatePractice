package com.vyy.imageprocessingpractice.processes

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import com.vyy.imageprocessingpractice.utils.lastProcessTime

// Crop bitmap image and return as BitmapDrawable
fun crop(
    bitmap: Bitmap,
    fromX: Int,
    fromY: Int,
    toX: Int,
    toY: Int,
    resources: Resources
): BitmapDrawable {
    // We use this variable to check if enough time has passed for a new operation.
    lastProcessTime = System.currentTimeMillis()

    // Crop the bitmap to the given width and height.
    val croppedBitmap = Bitmap.createBitmap(
        bitmap,
        fromX,
        fromY,
        toX,
        toY
    )

    return BitmapDrawable(resources, croppedBitmap)
}