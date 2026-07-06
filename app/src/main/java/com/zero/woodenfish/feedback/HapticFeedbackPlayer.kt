package com.zero.woodenfish.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticFeedbackPlayer(context: Context) {
    private val vibrator: Vibrator? = getVibrator(context)

    fun playLightTap() {
        if (vibrator?.hasVibrator() != true) return
        vibrator.vibrate(VibrationEffect.createOneShot(LIGHT_TAP_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    companion object {
        private const val LIGHT_TAP_DURATION_MS = 14L

        @Volatile
        private var cachedVibrator: Vibrator? = null

        fun playLightTap(context: Context) {
            val vibrator = cachedVibrator ?: getVibrator(context).also { cachedVibrator = it }
            if (vibrator?.hasVibrator() != true) return
            vibrator.vibrate(VibrationEffect.createOneShot(LIGHT_TAP_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
        }

        private fun getVibrator(context: Context): Vibrator? {
            val appContext = context.applicationContext
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                appContext.getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }
    }
}
