package com.zero.woodenfish.ui.extension

import android.content.Context
import com.zero.woodenfish.R
import kotlin.math.roundToInt

fun Context.dpToPx(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

fun Context.dpToPx(value: Float): Float = value * resources.displayMetrics.density

fun Context.formatAutoTapInterval(intervalMs: Long): String {
    return getString(R.string.interval_per_tap_format, formatSecondsValue(intervalMs))
}

fun Context.formatSecondsValue(ms: Long): String {
    return getString(R.string.seconds_value_format, ms / MILLIS_PER_SECOND)
}

private const val MILLIS_PER_SECOND = 1_000f
