package com.zero.woodenfish.model

data class WoodenFishState(
    val todayCount: Int = 0,
    val weekCount: Int = 0,
    val totalCount: Int = 0,
    val todayAutoCount: Int = 0,
    val streakDays: Int = 0,
    val selectedTab: WoodenFishTab = WoodenFishTab.TAP,
    val autoTapEnabled: Boolean = false,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val immersiveEnabled: Boolean = true,
    val autoTapIntervalMs: Long = DEFAULT_AUTO_TAP_INTERVAL_MS
) {
    companion object {
        const val DEFAULT_AUTO_TAP_INTERVAL_MS = 1_000L
        const val MIN_AUTO_TAP_INTERVAL_MS = 500L
        const val MAX_AUTO_TAP_INTERVAL_MS = 2_000L
    }
}
