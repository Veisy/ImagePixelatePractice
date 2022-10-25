import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toDrawable

// Add two bitmap images together
fun addImages(bitmap1: Bitmap, bitmap2: Bitmap, resources: Resources): BitmapDrawable =
    imageArithmetic(bitmap1, bitmap2, resources, ::add)

// Product of two images
fun multiplyImages(bitmap1: Bitmap, bitmap2: Bitmap, resources: Resources): BitmapDrawable =
    imageArithmetic(bitmap1, bitmap2, resources, ::multiply)

private fun imageArithmetic(
    bitmap1: Bitmap,
    bitmap2: Bitmap,
    resources: Resources,
    arithmeticFunction: (Int, Int) -> Int
): BitmapDrawable {
    // width and height of the picture
    val width: Int = bitmap1.width
    val height: Int = bitmap1.height

    // Create empty bitmap with the same width and height
    val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Get all pixels of the image, and load into bitmapPixels array.
    val bitmapPixels1 = IntArray(width * height)
    bitmap1.getPixels(bitmapPixels1, 0, width, 0, 0, width, height)

    val bitmapPixels2 = IntArray(width * height)
    bitmap2.getPixels(bitmapPixels2, 0, width, 0, 0, width, height)

    // Create empty array for new pixels
    val newPixels = IntArray(width * height)

    // Add two images together
    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel1 = bitmapPixels1[y * width + x]
            val red1 = pixel1 shr 16 and 0xff
            val green1 = pixel1 shr 8 and 0xff
            val blue1 = pixel1 and 0xff

            val pixel2 = bitmapPixels2[y * width + x]
            val red2 = pixel2 shr 16 and 0xff
            val green2 = pixel2 shr 8 and 0xff
            val blue2 = pixel2 and 0xff

            val newRed = if ((red1 + red2) > 255) 255 else arithmeticFunction(red1, red2)
            val newGreen = if ((green1 + green2) > 255) 255 else arithmeticFunction(green1, green2)
            val newBlue = if ((blue1 + blue2) > 255) 255 else arithmeticFunction(blue1, blue2)

            newPixels[y * width + x] = 0xff000000.toInt() or (newRed shl 16) or (newGreen shl 8) or newBlue
        }
    }
    // Set new pixels to the new bitmap
    newBitmap.setPixels(newPixels, 0, width, 0, 0, width, height)
    return newBitmap.toDrawable(resources)
}

private fun add(a: Int, b: Int, ): Int {
    return a + b
}

private fun multiply(a: Int, b: Int, ): Int {
    return a * b
}