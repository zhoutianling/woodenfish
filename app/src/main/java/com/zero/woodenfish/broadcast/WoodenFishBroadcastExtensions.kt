package com.zero.woodenfish.broadcast

import android.content.Context
import android.content.Intent

const val ACTION_WOODEN_FISH_STATE_CHANGED = "com.zero.woodenfish.action.STATE_CHANGED"
const val ACTION_WOODEN_FISH_TAP_FEEDBACK = "com.zero.woodenfish.action.TAP_FEEDBACK"

fun Context.sendWoodenFishStateChangedBroadcast() {
    sendBroadcast(Intent(ACTION_WOODEN_FISH_STATE_CHANGED).setPackage(packageName))
}

fun Context.sendWoodenFishTapFeedbackBroadcast() {
    sendBroadcast(Intent(ACTION_WOODEN_FISH_TAP_FEEDBACK).setPackage(packageName))
}
