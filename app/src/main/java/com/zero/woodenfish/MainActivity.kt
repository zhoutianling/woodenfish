package com.zero.woodenfish

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.zero.woodenfish.databinding.ActivityMainBinding
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToLong

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var binding: ActivityMainBinding

    private val handler = Handler(Looper.getMainLooper())
    private var soundPool: SoundPool? = null
    private var soundId = 0
    private var soundReady = false
    private var vibrator: Vibrator? = null
    private var syncingControls = false

    private var todayCount = 0
    private var weekCount = 0
    private var totalCount = 0
    private var todayAutoCount = 0
    private var streakDays = 0
    private var autoEnabled = false
    private var soundEnabled = true
    private var hapticEnabled = true
    private var immersiveEnabled = true
    private var intervalMs = DEFAULT_INTERVAL_MS

    private val autoTapRunnable = object : Runnable {
        override fun run() {
            if (!autoEnabled) return
            performTap(fromAuto = true)
            handler.postDelayed(this, intervalMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        configureInsets()
        initSound()
        vibrator = resolveVibrator()
        loadState()
        setupListeners()
        syncUi()
        applyImmersiveMode()
        showPage(R.id.tab_tap)
    }

    override fun onResume() {
        super.onResume()
        if (autoEnabled) restartAutoTap()
    }

    override fun onPause() {
        handler.removeCallbacks(autoTapRunnable)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoTapRunnable)
        soundPool?.release()
        soundPool = null
        super.onDestroy()
    }

    private fun configureInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.contentArea.setPadding(0, bars.top, 0, 0)
            binding.bottomNavigation.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomMargin = bars.bottom + dp(16)
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun setupListeners() {
        binding.navTapItem.setOnClickListener { showPage(R.id.tab_tap) }
        binding.navRecordItem.setOnClickListener { showPage(R.id.tab_record) }
        binding.navSettingsItem.setOnClickListener { showPage(R.id.tab_settings) }

        binding.fishStage.isClickable = true
        binding.fishStage.isFocusable = true
        binding.fishStage.setOnClickListener { performTap(fromAuto = false) }
        binding.manualTapButton.setOnClickListener { performTap(fromAuto = false) }
        binding.clearTodayButton.setOnClickListener { clearToday() }

        binding.autoSwitch.setOnCheckedChangeListener { _, checked ->
            if (!syncingControls) setAutoEnabled(checked)
        }
        binding.settingsAutoSwitch.setOnCheckedChangeListener { _, checked ->
            if (!syncingControls) setAutoEnabled(checked)
        }
        binding.soundSwitch.setOnCheckedChangeListener { _, checked ->
            if (!syncingControls) {
                soundEnabled = checked
                prefs.edit().putBoolean(KEY_SOUND_ENABLED, checked).apply()
                syncUi()
            }
        }
        binding.hapticSwitch.setOnCheckedChangeListener { _, checked ->
            if (!syncingControls) {
                hapticEnabled = checked
                prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, checked).apply()
                syncUi()
            }
        }
        binding.immersiveSwitch.setOnCheckedChangeListener { _, checked ->
            if (!syncingControls) {
                immersiveEnabled = checked
                prefs.edit().putBoolean(KEY_IMMERSIVE_ENABLED, checked).apply()
                applyImmersiveMode()
                syncUi()
            }
        }
        binding.intervalSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                intervalMs = (value * 1000f).roundToLong()
                prefs.edit().putLong(KEY_INTERVAL_MS, intervalMs).apply()
                if (autoEnabled) restartAutoTap()
                syncUi()
            }
        }
    }

    private fun showPage(itemId: Int) {
        val tapSelected = itemId == R.id.tab_tap
        val recordSelected = itemId == R.id.tab_record
        val settingsSelected = itemId == R.id.tab_settings
        binding.pageTap.isVisible = tapSelected
        binding.pageRecord.isVisible = recordSelected
        binding.pageSettings.isVisible = settingsSelected
        setNavItemSelected(tapSelected, binding.navTapItem, binding.navTapIcon, binding.navTapLabel)
        setNavItemSelected(recordSelected, binding.navRecordItem, binding.navRecordIcon, binding.navRecordLabel)
        setNavItemSelected(settingsSelected, binding.navSettingsItem, binding.navSettingsIcon, binding.navSettingsLabel)
    }

    private fun setNavItemSelected(selected: Boolean, vararg views: View) {
        views.forEach { it.isSelected = selected }
    }

    private fun performTap(fromAuto: Boolean) {
        normalizeDateState()
        todayCount += 1
        weekCount += 1
        totalCount += 1
        if (fromAuto) todayAutoCount += 1
        updateStreakForTap()
        prefs.edit()
            .putInt(KEY_TODAY_COUNT, todayCount)
            .putInt(KEY_WEEK_COUNT, weekCount)
            .putInt(KEY_TOTAL_COUNT, totalCount)
            .putInt(KEY_TODAY_AUTO_COUNT, todayAutoCount)
            .putInt(KEY_STREAK_DAYS, streakDays)
            .apply()

        playTapSound()
        vibrateLightly()
        animateTap()
        syncUi()
    }

    private fun animateTap() {
        binding.fishImage.animate().cancel()
        binding.fishImage.scaleX = 1f
        binding.fishImage.scaleY = 1f
        binding.fishImage.animate()
            .scaleX(0.86f)
            .scaleY(0.86f)
            .setDuration(70L)
            .withEndAction {
                binding.fishImage.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(170L)
                    .setInterpolator(OvershootInterpolator(2.4f))
                    .start()
            }
            .start()

        binding.meritText.animate().cancel()
        binding.meritText.visibility = View.VISIBLE
        binding.meritText.alpha = 0f
        binding.meritText.translationY = 0f
        binding.meritText.animate()
            .alpha(1f)
            .translationY(-dp(8).toFloat())
            .setDuration(80L)
            .withEndAction {
                binding.meritText.animate()
                    .alpha(0f)
                    .translationY(-dp(56).toFloat())
                    .setDuration(520L)
                    .withEndAction { binding.meritText.visibility = View.INVISIBLE }
                    .start()
            }
            .start()

        binding.tapEffectView.emitTap()
    }

    private fun setAutoEnabled(enabled: Boolean) {
        autoEnabled = enabled
        prefs.edit().putBoolean(KEY_AUTO_ENABLED, enabled).apply()
        if (enabled) {
            restartAutoTap()
        } else {
            handler.removeCallbacks(autoTapRunnable)
        }
        syncUi()
    }

    private fun restartAutoTap() {
        handler.removeCallbacks(autoTapRunnable)
        handler.postDelayed(autoTapRunnable, intervalMs)
    }

    private fun clearToday() {
        totalCount = (totalCount - todayCount).coerceAtLeast(0)
        weekCount = (weekCount - todayCount).coerceAtLeast(0)
        todayCount = 0
        todayAutoCount = 0
        prefs.edit()
            .putInt(KEY_TODAY_COUNT, todayCount)
            .putInt(KEY_WEEK_COUNT, weekCount)
            .putInt(KEY_TOTAL_COUNT, totalCount)
            .putInt(KEY_TODAY_AUTO_COUNT, todayAutoCount)
            .apply()
        syncUi()
    }

    private fun syncUi() {
        val intervalText = formatInterval(intervalMs)
        binding.todayCountText.text = todayCount.toString()
        binding.weekCountText.text = weekCount.toString()
        binding.recordTodayText.text = getString(R.string.record_today_format, todayCount)
        binding.recordTotalText.text = getString(R.string.record_total_format, totalCount)
        binding.recordStreakText.text = getString(R.string.record_streak_format, streakDays)
        binding.recordAutoText.text = getString(R.string.record_auto_format, todayAutoCount)
        binding.autoStatusText.text = if (autoEnabled) getString(R.string.auto_on) else getString(R.string.auto_off)
        binding.autoIntervalText.text = intervalText
        binding.settingsAutoSummary.text = intervalText
        binding.intervalValueText.text = getString(R.string.current_interval_range_format, formatSeconds(intervalMs))

        syncingControls = true
        binding.autoSwitch.isChecked = autoEnabled
        binding.settingsAutoSwitch.isChecked = autoEnabled
        binding.soundSwitch.isChecked = soundEnabled
        binding.hapticSwitch.isChecked = hapticEnabled
        binding.immersiveSwitch.isChecked = immersiveEnabled
        binding.intervalSlider.value = intervalMs / 1000f
        syncingControls = false
    }

    private fun loadState() {
        normalizeDateState()
        todayCount = prefs.getInt(KEY_TODAY_COUNT, 0)
        weekCount = prefs.getInt(KEY_WEEK_COUNT, 0)
        totalCount = prefs.getInt(KEY_TOTAL_COUNT, 0)
        todayAutoCount = prefs.getInt(KEY_TODAY_AUTO_COUNT, 0)
        streakDays = prefs.getInt(KEY_STREAK_DAYS, 0)
        autoEnabled = prefs.getBoolean(KEY_AUTO_ENABLED, false)
        soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        hapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
        immersiveEnabled = prefs.getBoolean(KEY_IMMERSIVE_ENABLED, true)
        intervalMs = prefs.getLong(KEY_INTERVAL_MS, DEFAULT_INTERVAL_MS).coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
    }

    private fun normalizeDateState() {
        val today = LocalDate.now()
        val todayKey = today.toString()
        val thisWeekKey = weekKey(today)
        val editor = prefs.edit()
        var changed = false

        if (prefs.getString(KEY_LAST_DATE, null) != todayKey) {
            todayCount = 0
            todayAutoCount = 0
            editor.putString(KEY_LAST_DATE, todayKey)
            editor.putInt(KEY_TODAY_COUNT, 0)
            editor.putInt(KEY_TODAY_AUTO_COUNT, 0)
            changed = true
        }

        if (prefs.getString(KEY_LAST_WEEK, null) != thisWeekKey) {
            weekCount = 0
            editor.putString(KEY_LAST_WEEK, thisWeekKey)
            editor.putInt(KEY_WEEK_COUNT, 0)
            changed = true
        }

        if (changed) editor.apply()
    }

    private fun updateStreakForTap() {
        val today = LocalDate.now()
        val todayKey = today.toString()
        val lastTapKey = prefs.getString(KEY_LAST_TAP_DATE, null)
        if (lastTapKey == todayKey) return

        val lastTapDate = lastTapKey?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        streakDays = if (lastTapDate?.plusDays(1) == today) {
            streakDays + 1
        } else {
            1
        }
        prefs.edit()
            .putString(KEY_LAST_TAP_DATE, todayKey)
            .putInt(KEY_STREAK_DAYS, streakDays)
            .apply()
    }

    private fun weekKey(date: LocalDate): String {
        val fields = WeekFields.of(Locale.getDefault())
        val weekYear = date.get(fields.weekBasedYear())
        val week = date.get(fields.weekOfWeekBasedYear())
        return "$weekYear-$week"
    }

    private fun initSound() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setAudioAttributes(attributes)
            .setMaxStreams(4)
            .build()
            .also { pool ->
                pool.setOnLoadCompleteListener { _, sampleId, status ->
                    soundReady = sampleId == soundId && status == 0
                }
                soundId = pool.load(this, R.raw.muyu, 1)
            }
    }

    private fun playTapSound() {
        if (!soundEnabled || !soundReady) return
        soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    private fun vibrateLightly() {
        if (!hapticEnabled) return
        vibrator?.vibrate(VibrationEffect.createOneShot(14L, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun applyImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (immersiveEnabled) {
                hide(WindowInsetsCompat.Type.statusBars())
            } else {
                show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    private fun resolveVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun formatInterval(ms: Long): String = getString(R.string.interval_per_tap_format, formatSeconds(ms))

    private fun formatSeconds(ms: Long): String = getString(R.string.seconds_short_format, ms / 1000f)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToLong().toInt()

    private companion object {
        const val PREFS_NAME = "wooden_fish_state"
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
        const val DEFAULT_INTERVAL_MS = 1000L
        const val MIN_INTERVAL_MS = 500L
        const val MAX_INTERVAL_MS = 2000L
    }
}
