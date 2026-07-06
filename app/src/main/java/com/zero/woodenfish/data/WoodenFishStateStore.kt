package com.zero.woodenfish.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.zero.woodenfish.model.TapSource
import com.zero.woodenfish.model.WoodenFishTab
import com.zero.woodenfish.model.WoodenFishState
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

class WoodenFishStateStore(
    context: Context,
    private val currentDate: () -> LocalDate = { LocalDate.now() },
    private val weekFields: WeekFields = WeekFields.of(Locale.getDefault())
) {
    private val preferences: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): WoodenFishState {
        return rolloverIfNeeded(readState())
    }

    fun recordTap(source: TapSource): WoodenFishState {
        return recordTapInternal(source = source, selectedTab = null)
    }

    fun recordTap(state: WoodenFishState, source: TapSource): WoodenFishState {
        return recordTapInternal(source = source, selectedTab = state.selectedTab)
    }

    private fun recordTapInternal(source: TapSource, selectedTab: WoodenFishTab?): WoodenFishState {
        val currentState = rolloverIfNeeded(readState()).let { state ->
            selectedTab?.let { state.copy(selectedTab = it) } ?: state
        }
        val today = currentDate()
        val todayKey = today.toString()
        val newStreakDays = calculateStreakDays(currentState.streakDays, today)
        val nextState = currentState.copy(
            todayCount = currentState.todayCount + 1,
            weekCount = currentState.weekCount + 1,
            totalCount = currentState.totalCount + 1,
            todayAutoCount = currentState.todayAutoCount + if (source == TapSource.AUTO) 1 else 0,
            streakDays = newStreakDays
        )

        preferences.edit {
            putInt(KEY_TODAY_COUNT, nextState.todayCount)
                .putInt(KEY_WEEK_COUNT, nextState.weekCount)
                .putInt(KEY_TOTAL_COUNT, nextState.totalCount)
                .putInt(KEY_TODAY_AUTO_COUNT, nextState.todayAutoCount)
                .putInt(KEY_STREAK_DAYS, nextState.streakDays)
                .putString(KEY_LAST_TAP_DATE, todayKey)
        }

        return nextState
    }

    fun clearToday(state: WoodenFishState): WoodenFishState {
        val currentState = rolloverIfNeeded(readState()).copy(selectedTab = state.selectedTab)
        val nextState = currentState.copy(
            todayCount = 0,
            weekCount = (currentState.weekCount - currentState.todayCount).coerceAtLeast(0),
            totalCount = (currentState.totalCount - currentState.todayCount).coerceAtLeast(0),
            todayAutoCount = 0
        )

        preferences.edit()
            .putInt(KEY_TODAY_COUNT, nextState.todayCount)
            .putInt(KEY_WEEK_COUNT, nextState.weekCount)
            .putInt(KEY_TOTAL_COUNT, nextState.totalCount)
            .putInt(KEY_TODAY_AUTO_COUNT, nextState.todayAutoCount)
            .apply()

        return nextState
    }

    fun setAutoTapEnabled(state: WoodenFishState, enabled: Boolean): WoodenFishState {
        return rolloverIfNeeded(readState()).copy(selectedTab = state.selectedTab, autoTapEnabled = enabled).also {
            preferences.edit(commit = true) { putBoolean(KEY_AUTO_ENABLED, enabled) }
        }
    }

    fun setSoundEnabled(state: WoodenFishState, enabled: Boolean): WoodenFishState {
        return rolloverIfNeeded(readState()).copy(selectedTab = state.selectedTab, soundEnabled = enabled).also {
            preferences.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        }
    }

    fun setHapticEnabled(state: WoodenFishState, enabled: Boolean): WoodenFishState {
        return rolloverIfNeeded(readState()).copy(selectedTab = state.selectedTab, hapticEnabled = enabled).also {
            preferences.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
        }
    }

    fun setImmersiveEnabled(state: WoodenFishState, enabled: Boolean): WoodenFishState {
        return rolloverIfNeeded(readState()).copy(selectedTab = state.selectedTab, immersiveEnabled = enabled).also {
            preferences.edit().putBoolean(KEY_IMMERSIVE_ENABLED, enabled).apply()
        }
    }

    fun setAutoTapInterval(state: WoodenFishState, intervalMs: Long): WoodenFishState {
        val boundedIntervalMs = intervalMs.coerceIn(
            WoodenFishState.MIN_AUTO_TAP_INTERVAL_MS,
            WoodenFishState.MAX_AUTO_TAP_INTERVAL_MS
        )
        return rolloverIfNeeded(readState()).copy(
            selectedTab = state.selectedTab,
            autoTapIntervalMs = boundedIntervalMs
        ).also {
            preferences.edit().putLong(KEY_INTERVAL_MS, boundedIntervalMs).apply()
        }
    }

    private fun readState(): WoodenFishState {
        return WoodenFishState(
            todayCount = preferences.getInt(KEY_TODAY_COUNT, 0),
            weekCount = preferences.getInt(KEY_WEEK_COUNT, 0),
            totalCount = preferences.getInt(KEY_TOTAL_COUNT, 0),
            todayAutoCount = preferences.getInt(KEY_TODAY_AUTO_COUNT, 0),
            streakDays = preferences.getInt(KEY_STREAK_DAYS, 0),
            autoTapEnabled = preferences.getBoolean(KEY_AUTO_ENABLED, false),
            soundEnabled = preferences.getBoolean(KEY_SOUND_ENABLED, true),
            hapticEnabled = preferences.getBoolean(KEY_HAPTIC_ENABLED, true),
            immersiveEnabled = preferences.getBoolean(KEY_IMMERSIVE_ENABLED, true),
            autoTapIntervalMs = preferences.getLong(
                KEY_INTERVAL_MS,
                WoodenFishState.DEFAULT_AUTO_TAP_INTERVAL_MS
            ).coerceIn(
                WoodenFishState.MIN_AUTO_TAP_INTERVAL_MS,
                WoodenFishState.MAX_AUTO_TAP_INTERVAL_MS
            )
        )
    }

    private fun rolloverIfNeeded(state: WoodenFishState): WoodenFishState {
        val today = currentDate()
        val todayKey = today.toString()
        val weekKey = weekKey(today)
        var nextState = state
        val editor = preferences.edit()
        var changed = false

        if (preferences.getString(KEY_LAST_DATE, null) != todayKey) {
            nextState = nextState.copy(todayCount = 0, todayAutoCount = 0)
            editor.putString(KEY_LAST_DATE, todayKey)
                .putInt(KEY_TODAY_COUNT, nextState.todayCount)
                .putInt(KEY_TODAY_AUTO_COUNT, nextState.todayAutoCount)
            changed = true
        }

        if (preferences.getString(KEY_LAST_WEEK, null) != weekKey) {
            nextState = nextState.copy(weekCount = 0)
            editor.putString(KEY_LAST_WEEK, weekKey)
                .putInt(KEY_WEEK_COUNT, nextState.weekCount)
            changed = true
        }

        if (changed) {
            editor.apply()
        }

        return nextState
    }

    private fun calculateStreakDays(currentStreakDays: Int, today: LocalDate): Int {
        val todayKey = today.toString()
        val lastTapDate = preferences.getString(KEY_LAST_TAP_DATE, null)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        return when {
            lastTapDate == today -> currentStreakDays
            lastTapDate?.plusDays(1) == today -> currentStreakDays + 1
            else -> 1
        }
    }

    private fun weekKey(date: LocalDate): String {
        val weekYear = date.get(weekFields.weekBasedYear())
        val weekOfYear = date.get(weekFields.weekOfWeekBasedYear())
        return "$weekYear-$weekOfYear"
    }

    private companion object {
        const val PREFERENCES_NAME = "wooden_fish_state"
        const val KEY_TODAY_COUNT = "today_count"
        const val KEY_WEEK_COUNT = "week_count"
        const val KEY_TOTAL_COUNT = "total_count"
        const val KEY_TODAY_AUTO_COUNT = "today_auto_count"
        const val KEY_STREAK_DAYS = "streak_days"
        const val KEY_LAST_DATE = "last_date"
        const val KEY_LAST_WEEK = "last_week"
        const val KEY_LAST_TAP_DATE = "last_tap_date"
        const val KEY_AUTO_ENABLED = "auto_enabled"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        const val KEY_IMMERSIVE_ENABLED = "immersive_enabled"
        const val KEY_INTERVAL_MS = "interval_ms"
    }
}
