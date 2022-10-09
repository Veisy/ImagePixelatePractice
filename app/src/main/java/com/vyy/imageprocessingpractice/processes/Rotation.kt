package com.vyy.imageprocessingpractice.processes

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.util.DisplayMetrics
import androidx.core.graphics.drawable.toDrawable
import com.vyy.imageprocessingpractice.utils.lastProcessTime

fun reflectOnXAxis(bitmap: Bitmap, resources: Resources) =
    scale(bitmap, 1f, -1f, resources)

fun reflectOnYAxis(bitmap: Bitmap, resources: Resources) =
    scale(bitmap, -1f, 1f, resources)

// Reflect bitmap image and return as BitmapDrawable
private fun scale(
    bitmap: Bitmap,
    scaleX: Float,
    scaleY: Float,
    resources: Resources
): BitmapDrawable {
    // We use this variable to check if enough time has passed for a new operation.
    lastProcessTime = System.currentTimeMillis()

    val matrix = Matrix()
    matrix.preScale(scaleX, scaleY)
    val reflectedBitmap = Bitmap.createBitmap(
        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false
    )

    reflectedBitmap.density = DisplayMetrics.DENSITY_DEFAULT
    return reflectedBitmap.toDrawable(resources)
}