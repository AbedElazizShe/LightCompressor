package com.abedelazizshe.lightcompressorlibrary.utils

import kotlin.math.roundToInt

fun uInt32ToLong(int32: Int): Long {
    return int32.toLong()
}

fun uInt32ToInt(uInt32: Long): Int {
    if (uInt32 > Int.MAX_VALUE || uInt32 < 0) {
        throw Exception("uInt32 value is too large")
    }
    return uInt32.toInt()
}

fun uInt64ToLong(uInt64: Long): Long {
    if (uInt64 < 0) throw Exception("uInt64 value is too large")
    return uInt64
}


fun uInt32ToInt(uInt32: Int): Int {
    if (uInt32 < 0) {
        throw Exception("uInt32 value is too large")
    }
    return uInt32
}

private fun roundEven(value: Int): Int = value + 1 and 1.inv()

fun generateWidthHeightValue(value: Double, factor: Double): Int =
    roundEven((((value * factor) / 16).roundToInt() * 16))
