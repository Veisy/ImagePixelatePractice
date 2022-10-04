package com.vyy.imagepixelate

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import java.util.*


private var lastPixelationTime: Long = 0
private const val TIME_BETWEEN_TASKS = 400

fun checkIfShouldPixelate() =
    (System.currentTimeMillis() - lastPixelationTime) > TIME_BETWEEN_TASKS


/**
 * A simple pixelation algorithm. This uses a box blur algorithm where all the
 * pixels within some region are averaged, and that average pixel value is then
 * applied to all the pixels within that region.
 */
fun invokePixelation(
    bitmap: Bitmap, pixelWidth: Int, pixelHeight: Int, resources: Resources
): BitmapDrawable {
    // We use this variable to check if enough time has passed for a new operation.
    lastPixelationTime = System.currentTimeMillis()

    // width and height of the picture
    val width: Int = bitmap.width
    val height: Int = bitmap.height

    // Bitmap copy of the picture, on which the pixelate process will be performed.
    val mPixelatedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Pixelation width and height,
    // all pixels in this area will have the same color.
    val mPixelWidth = if (pixelWidth > 0) pixelWidth else 1
    val mPixelHeight = if (pixelHeight > 0) pixelHeight else 1

    val bitmapPixels = IntArray(width * height)

    // Get all pixels of the image, and load into bitmapPixels array.
    bitmap.getPixels(bitmapPixels, 0, width, 0, 0, width, height)

    // Pixels to be pixelated in each area, which will have the same color.
    val pixels = IntArray(mPixelWidth * mPixelHeight)

    // Each pixel in each area and their RGB values.
    // After adding them all, in order to divide and get the average,
    // we count the pixels with numPixels variable.
    var pixel: Int
    var red: Int
    var green: Int
    var blue: Int
    var numPixels: Int

    // If we are on the edges of the image,
    // the maximum width and height can be the dimensions of the image.
    // Other than that, the calculation is done normally.
    var maxX: Int
    var maxY: Int


    for (y in 0 until height step mPixelHeight) {
        for (x in 0 until width step mPixelWidth) {
            // At the beginning of each pixelation area,
            // the values are reset.
            numPixels = 0
            red = 0
            green = 0
            blue = 0

            maxX = (x + mPixelWidth).coerceAtMost(width)
            maxY = (y + mPixelHeight).coerceAtMost(height)

            // The RGB values of each pixel,
            // in this area are summed.
            for (i in x until maxX) {
                for (j in y until maxY) {
                    pixel = bitmapPixels[j * width + i]
                    red += Color.red(pixel)
                    green += Color.green(pixel)
                    blue += Color.blue(pixel)
                    numPixels++
                }
            }

            // After all RGB values are summed,
            // the average is taken by dividing by the number of pixels in the region.
            pixel = Color.rgb(red / numPixels, green / numPixels, blue / numPixels)

            // fill all elements of a IntArray with a specific value
            Arrays.fill(pixels, pixel)

            val w = (mPixelWidth).coerceAtMost(width - x)
            val h = (mPixelHeight).coerceAtMost(height - y)

            // We add the pixel block (with pixels of the same color)
            // to the resulting pixelated image.
            mPixelatedBitmap.setPixels(pixels, 0, w, x, y, w, h)
        }
    }

    return BitmapDrawable(resources, mPixelatedBitmap)
}

// Check if image is gray scale
fun isGrayScale(bitmap: Bitmap): Boolean {
    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            val pixel = bitmap.getPixel(x, y)

            // If there is even a single RGB pixel, the picture is not RGB.
            if(Color.red(pixel) != Color.green(pixel) || Color.red(pixel) != Color.blue(pixel)) {
                return false
            }
        }
    }
    return true
}
