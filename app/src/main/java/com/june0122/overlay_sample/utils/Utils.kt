package com.june0122.overlay_sample.utils

import android.util.DisplayMetrics

//@Suppress("SameParameterValue")
fun clipInt(min: Int, max: Int, v: Int) = when {
    v < min -> min
    v > max -> max
    else -> v
}

fun Float.dp2px(dm: DisplayMetrics) =
        (this * dm.density + 0.5f).toInt()

fun Int.px2dp(dm: DisplayMetrics) =
        this.toFloat() / dm.density