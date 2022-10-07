package com.vyy.imageprocessingpractice.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable


// Check if image is gray scale
fun isGrayScale(bitmap: Bitmap): Boolean {
    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            val pixel = bitmap.getPixel(x, y)

            // If there is even a single RGB pixel, the picture is not RGB.
            if(Color.red(pixel) != Color.green(pixel) || Color.blue(pixel) != Color.red(pixel)) {
                return false
            }
        }
    }
    return true
}

// Convert image to gray scale
fun convertToGrayScale(bitmap: Bitmap, resources: Resources): BitmapDrawable {
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
    return BitmapDrawable(resources, grayScaleBitmap)
}
