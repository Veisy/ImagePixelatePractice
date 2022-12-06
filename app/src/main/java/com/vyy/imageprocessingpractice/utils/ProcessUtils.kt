package com.vyy.imageprocessingpractice.utils

import android.graphics.Bitmap
import android.graphics.Color

var lastProcessTime: Long = 0
private const val TIME_PROCESSES_TASKS = 400

fun checkEnoughTimePassed() =
    (System.currentTimeMillis() - lastProcessTime) > TIME_PROCESSES_TASKS
