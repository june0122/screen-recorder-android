@file:Suppress("unused")

package com.june0122.overlay_sample.utils

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import androidx.core.content.ContextCompat
import java.math.RoundingMode
import java.text.DecimalFormat

fun clipInt(min: Int, max: Int, v: Int) = when {
    v < min -> min
    v > max -> max
    else -> v
}

fun Float.dp2px(dm: DisplayMetrics) =
        (this * dm.density + 0.5f).toInt()

fun Int.px2dp(dm: DisplayMetrics) =
        this.toFloat() / dm.density

inline fun <reified T> systemService(context: Context): T? =
        ContextCompat.getSystemService(context, T::class.java)

fun getScreenSize(context: Context) = Point().also {
    systemService<DisplayManager>(context)
            ?.getDisplay(Display.DEFAULT_DISPLAY)
            ?.getRealSize(it)
}

/** A list of all available flags : CEILING, DOWN, FLOOR, HALF_DOWN, HALF_EVEN, HALF_UP, UNNECESSARY, UP */
fun Double.roundOffDecimal() : Double? {
    val df = DecimalFormat("#.##")
    df.roundingMode = RoundingMode.HALF_UP
    return df.format(this).toDouble()
}