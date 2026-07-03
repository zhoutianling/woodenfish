package com.zero.woodenfish.ui.extension

import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import com.google.android.material.textview.MaterialTextView

fun ImageView.playWoodenFishTapScale() {
    animate().cancel()
    scaleX = FISH_RESTING_SCALE
    scaleY = FISH_RESTING_SCALE
    animate()
        .scaleX(FISH_PRESSED_SCALE)
        .scaleY(FISH_PRESSED_SCALE)
        .setDuration(FISH_PRESS_DURATION_MS)
        .withEndAction {
            animate()
                .scaleX(FISH_RESTING_SCALE)
                .scaleY(FISH_RESTING_SCALE)
                .setDuration(FISH_RELEASE_DURATION_MS)
                .setInterpolator(OvershootInterpolator(FISH_RELEASE_TENSION))
                .start()
        }
        .start()
}

fun MaterialTextView.playMeritFloat() {
    animate().cancel()
    visibility = View.VISIBLE
    alpha = 0f
    translationY = 0f
    animate()
        .alpha(FULL_ALPHA)
        .translationY(-context.dpToPx(MERIT_ENTER_TRANSLATION_DP).toFloat())
        .setDuration(MERIT_ENTER_DURATION_MS)
        .withEndAction {
            animate()
                .alpha(NO_ALPHA)
                .translationY(-context.dpToPx(MERIT_EXIT_TRANSLATION_DP).toFloat())
                .setDuration(MERIT_EXIT_DURATION_MS)
                .withEndAction { visibility = View.INVISIBLE }
                .start()
        }
        .start()
}

private const val FISH_PRESSED_SCALE = 0.86f
private const val FISH_RESTING_SCALE = 1f
private const val FISH_RELEASE_TENSION = 2.4f
private const val FISH_PRESS_DURATION_MS = 70L
private const val FISH_RELEASE_DURATION_MS = 170L
private const val MERIT_ENTER_DURATION_MS = 80L
private const val MERIT_EXIT_DURATION_MS = 520L
private const val MERIT_ENTER_TRANSLATION_DP = 8
private const val MERIT_EXIT_TRANSLATION_DP = 56
private const val FULL_ALPHA = 1f
private const val NO_ALPHA = 0f
