package com.vyy.imageprocessingpractice.utils

var lastProcessTime: Long = 0
private const val TIME_PROCESSES_TASKS = 400

fun checkEnoughTimePassed() =
    (System.currentTimeMillis() - lastProcessTime) > TIME_PROCESSES_TASKS