package com.vyy.intelligenteye.utils

var lastProcessTime: Long = 0
private const val TIME_PROCESSES_TASKS = 300

fun checkEnoughTimePassed() =
    (System.currentTimeMillis() - lastProcessTime) > TIME_PROCESSES_TASKS