package com.vyy.imageprocessingpractice.processes

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import com.vyy.imageprocessingpractice.utils.lastProcessTime

fun resize(
    bitmap: Bitmap, width: Int, height: Int, resources: Resources
): BitmapDrawable {
    // We use this variable to check if enough time has passed for a new operation.
    lastProcessTime = System.currentTimeMillis()

    // Resize the bitmap to the given width and height.
    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

    return BitmapDrawable(resources, resizedBitmap)
}