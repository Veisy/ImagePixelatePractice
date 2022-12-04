package com.vyy.imageprocessingpractice.processes

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toDrawable
import com.vyy.imageprocessingpractice.utils.lastProcessTime
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc


// Apply a high pass filter to the Bitmap image with using openCV
fun applyHighPassFilter(bitmap: Bitmap, resources: Resources): BitmapDrawable {
    // We use this variable to check if enough time has passed for a new operation.
    lastProcessTime = System.currentTimeMillis()

    val filteredBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    val mat = Mat()
    Utils.bitmapToMat(filteredBitmap, mat)
    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
    val dst = Mat()
    Imgproc.Laplacian(mat, dst, -1)
    Utils.matToBitmap(dst, filteredBitmap)
    return filteredBitmap.toDrawable(resources)
}



