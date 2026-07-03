package com.zero.woodenfish.ui.extension

import android.view.View
import androidx.core.view.ViewCompat

fun View.disableAutomaticSystemInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets -> insets }
}
