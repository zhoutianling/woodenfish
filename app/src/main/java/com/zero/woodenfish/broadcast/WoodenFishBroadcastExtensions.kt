package com.zero.woodenfish.broadcast

import android.content.Context
import android.content.Intent

const val ACTION_WOODEN_FISH_STATE_CHANGED = "com.zero.woodenfish.action.STATE_CHANGED"

fun Context.sendWoodenFishStateChangedBroadcast() {
    sendBroadcast(Intent(ACTION_WOODEN_FISH_STATE_CHANGED).setPackage(packageName))
}
