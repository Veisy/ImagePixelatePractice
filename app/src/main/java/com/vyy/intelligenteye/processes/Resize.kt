package com.vyy.intelligenteye.processes

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toDrawable
import com.vyy.intelligenteye.utils.lastProcessTime

/**
 * Scales image using bilinear interpolation. Best suited for up scaling.
 * @return Returns scaled image
 */
fun resize(
    bitmap: Bitmap, width: Int, height: Int, resources: Resources
): BitmapDrawable {
    // We use this variable to check if enough time has passed for a new operation.
    lastProcessTime = System.currentTimeMillis()

    val rawInput = IntArray(bitmap.height * bitmap.width)
    bitmap.getPixels(
        rawInput, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height
    )

    val rawOutput = IntArray(width * height)

    val oWidth: Int = bitmap.width
    val oX16 = IntArray(width)
    var max = oWidth - 1 shl 4
    for (newX in 0 until width) {
        oX16[newX] = (((newX shl 1) + 1) * oWidth shl 3) / width - 8
        if (oX16[newX] < 0) {
            oX16[newX] = 0
        } else if (oX16[newX] > max) {
            oX16[newX] = max
        }
    }

    val oHeight: Int = bitmap.height
    val oY16 = IntArray(height)
    max = oHeight - 1 shl 4
    for (newY in 0 until height) {
        oY16[newY] = ((((newY shl 1) + 1) * oHeight shl 3) / height - 8)
        if (oY16[newY] < 0) {
            oY16[newY] = 0
        } else if (oY16[newY] > max) {
            oY16[newY] = max
        }
    }

    val oX = IntArray(2)
    val oY = IntArray(2)
    val wX = IntArray(2)
    val wY = IntArray(2)
    var outWeight: Int
    var outColorWeight: Int
    var outAlpha: Int
    var outRed: Int
    var outGreen: Int
    var outBlue: Int
    var w: Int
    var argb: Int
    var a: Int
    var r: Int
    var g: Int
    var b: Int
    for (newY in 0 until height) {
        oY[0] = oY16[newY] ushr 4
        wY[1] = oY16[newY] and 0x0000000f
        wY[0] = 16 - wY[1]
        oY[1] = if (wY[1] == 0) oY[0] else oY[0] + 1
        for (newX in 0 until width) {
            oX[0] = oX16[newX] ushr 4
            wX[1] = oX16[newX] and 0x0000000f
            wX[0] = 16 - wX[1]
            oX[1] = if (wX[1] == 0) oX[0] else oX[0] + 1
            outWeight = 0
            outColorWeight = 0
            outAlpha = 0
            outRed = 0
            outGreen = 0
            outBlue = 0
            for (j in 0..1) {
                for (i in 0..1) {
                    if (wY[j] == 0 || wX[i] == 0) {
                        continue
                    }
                    w = wX[i] * wY[j]
                    outWeight += w
                    argb = rawInput[oX[i] + oY[j] * bitmap.width]
                    a = argb ushr 24
                    if (a == 0) {
                        continue
                    }
                    w *= a
                    outColorWeight += w
                    r = argb and 0x00ff0000 ushr 16
                    g = argb and 0x0000ff00 ushr 8
                    b = argb and 0x000000ff
                    outRed += w * r
                    outGreen += w * g
                    outBlue += w * b
                }
            }
            if (outColorWeight > 0) {
                outAlpha = outColorWeight / outWeight
                outRed /= outColorWeight
                outGreen /= outColorWeight
                outBlue /= outColorWeight
            }
            rawOutput[newX + newY * width] =
                (outAlpha shl 24 or (outRed shl 16) or (outGreen shl 8) or outBlue)
        }
    }
    val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    outputBitmap.setPixels(rawOutput, 0, width, 0, 0, width, height)
    return outputBitmap.toDrawable(resources)
}