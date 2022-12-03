package com.vyy.intelligenteye.utils

private const val normalTextColor = 50

// If the classification confidence percentage is below 50, the color will shift towards red.
// If the percentage is above 50, the color will shift towards green.
fun adjustPercentageColor(percentage: Int): Int {
    return if (percentage < 50) {
        val red = normalTextColor + (50 - percentage) * (255 - normalTextColor) / 50
        android.graphics.Color.rgb(red, normalTextColor, normalTextColor)
    } else {
        val green = normalTextColor + (percentage - 50) * (255 - normalTextColor) / 50
        android.graphics.Color.rgb(normalTextColor, green, normalTextColor)
    }
}