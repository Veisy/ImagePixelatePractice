package com.vyy.imageprocessingpractice.processes

import addImages
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toDrawable
import multiplyImages

fun specialImageOperation(originalBitmap: Bitmap, resources: Resources) : List<BitmapDrawable> {

    val a = originalBitmap.toDrawable(resources)

    val b = laplacianFilter(a.bitmap, resources)

    val c = addImages(b.bitmap, originalBitmap, resources)

    val d = sobelGradientFilter(a.bitmap, resources)

    val e = medianFilter(d.bitmap, resources)

    val f = multiplyImages(c.bitmap, e.bitmap, resources)

    val g = addImages(a.bitmap, f.bitmap, resources)

    val h = gammaTransformation(g.bitmap, resources)

    return listOf(b, c, d, e, f, g, h)
}