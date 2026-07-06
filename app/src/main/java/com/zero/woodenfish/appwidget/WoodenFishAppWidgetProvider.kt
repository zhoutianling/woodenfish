package com.zero.woodenfish.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import com.zero.woodenfish.R
import com.zero.woodenfish.broadcast.ACTION_WOODEN_FISH_STATE_CHANGED
import com.zero.woodenfish.broadcast.sendWoodenFishTapFeedbackBroadcast
import com.zero.woodenfish.data.WoodenFishStateStore
import com.zero.woodenfish.feedback.HapticFeedbackPlayer
import com.zero.woodenfish.media.TapSoundPlayer
import com.zero.woodenfish.model.TapSource
import com.zero.woodenfish.model.WoodenFishState
import java.util.concurrent.atomic.AtomicInteger

class WoodenFishAppWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WIDGET_TAP -> {
                val pendingResult = goAsync()
                runCatching {
                    handleWidgetTap(context.applicationContext) {
                        pendingResult.finish()
                    }
                }.onFailure {
                    pendingResult.finish()
                }
                return
            }
            ACTION_WOODEN_FISH_STATE_CHANGED -> {
                updateAllWidgets(context)
                return
            }
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                refreshForCalendarChange(context)
                return
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val state = WoodenFishStateStore(context).load()
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context, state))
        }
    }

    private fun handleWidgetTap(context: Context, onFeedbackCompleted: () -> Unit) {
        val stateStore = WoodenFishStateStore(context)
        val nextState = stateStore.recordTap(TapSource.MANUAL)
        renderManualTapFeedback(context, nextState, onFeedbackCompleted)
        playHapticFeedbackIfNeeded(context, nextState)
        playTapSoundIfNeeded(context, nextState)
        context.sendWoodenFishTapFeedbackBroadcast()
    }

    private fun playHapticFeedbackIfNeeded(context: Context, state: WoodenFishState) {
        if (state.hapticEnabled) HapticFeedbackPlayer.playLightTap(context)
    }

    private fun playTapSoundIfNeeded(context: Context, state: WoodenFishState) {
        if (state.soundEnabled) TapSoundPlayer.getInstance(context).play()
    }

    private fun refreshForCalendarChange(context: Context) {
        val state = WoodenFishStateStore(context).load()
        updateAllWidgets(context, state)
    }

    companion object {
        private const val ACTION_WIDGET_TAP = "com.zero.woodenfish.action.WIDGET_TAP"
        private const val REQUEST_CODE_WIDGET_TAP = 20
        private const val WIDGET_TAP_RECOVERING_DELAY_MS = 70L
        private const val WIDGET_TAP_REST_DELAY_MS = 200L
        private val feedbackGeneration = AtomicInteger()

        fun updateAllWidgets(context: Context, state: WoodenFishState = WoodenFishStateStore(context).load()) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WoodenFishAppWidgetProvider::class.java)
            )
            if (appWidgetIds.isNotEmpty()) {
                appWidgetManager.updateAppWidget(appWidgetIds, buildRemoteViews(context, state))
            }
        }

        fun renderManualTapFeedback(
            context: Context,
            state: WoodenFishState,
            onFeedbackCompleted: () -> Unit = {}
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = getAppWidgetIds(context)
            if (appWidgetIds.isEmpty()) {
                onFeedbackCompleted()
                return
            }
            val generation = feedbackGeneration.incrementAndGet()
            appWidgetManager.updateAppWidget(
                appWidgetIds,
                buildRemoteViews(context, state, WidgetTapFrame.PRESSED)
            )
            Handler(Looper.getMainLooper()).apply {
                postDelayed(
                    {
                        if (generation == feedbackGeneration.get()) {
                            appWidgetManager.updateAppWidget(
                                appWidgetIds,
                                buildRemoteViews(context, state, WidgetTapFrame.RECOVERING)
                            )
                        }
                    },
                    WIDGET_TAP_RECOVERING_DELAY_MS
                )
                postDelayed(
                    {
                        restoreWidgetsIfCurrent(context, state, generation)
                        onFeedbackCompleted()
                    },
                    WIDGET_TAP_REST_DELAY_MS
                )
            }
        }

        fun renderAutoTapFeedback(context: Context, state: WoodenFishState) {
            renderManualTapFeedback(context, state)
        }

        private fun restoreWidgetsIfCurrent(
            context: Context,
            state: WoodenFishState,
            generation: Int
        ) {
            if (generation == feedbackGeneration.get()) {
                runCatching { updateAllWidgets(context.applicationContext, state) }
            }
        }

        private fun getAppWidgetIds(context: Context): IntArray {
            return AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, WoodenFishAppWidgetProvider::class.java)
            )
        }

        private fun buildRemoteViews(
            context: Context,
            state: WoodenFishState,
            tapFrame: WidgetTapFrame = WidgetTapFrame.RESTING
        ): RemoteViews {
            return RemoteViews(context.packageName, R.layout.widget_wooden_fish).apply {
                val countText = state.todayCount.toString()
                setTextViewText(R.id.widget_count_text, countText)
                setViewVisibility(
                    R.id.widget_fish_image,
                    if (tapFrame == WidgetTapFrame.RESTING) View.VISIBLE else View.INVISIBLE
                )
                setViewVisibility(
                    R.id.widget_fish_pressed_image,
                    if (tapFrame == WidgetTapFrame.PRESSED) View.VISIBLE else View.INVISIBLE
                )
                setViewVisibility(
                    R.id.widget_fish_recovering_image,
                    if (tapFrame == WidgetTapFrame.RECOVERING) View.VISIBLE else View.INVISIBLE
                )
                setViewVisibility(
                    R.id.widget_merit_pressed_text,
                    if (tapFrame == WidgetTapFrame.PRESSED) View.VISIBLE else View.INVISIBLE
                )
                setViewVisibility(
                    R.id.widget_merit_recovering_text,
                    if (tapFrame == WidgetTapFrame.RECOVERING) View.VISIBLE else View.INVISIBLE
                )
                setContentDescription(
                    R.id.widget_root,
                    context.getString(R.string.widget_tap_content_description, state.todayCount)
                )
                setOnClickPendingIntent(R.id.widget_root, buildTapPendingIntent(context))
            }
        }

        private fun buildTapPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, WoodenFishAppWidgetProvider::class.java)
                .setAction(ACTION_WIDGET_TAP)
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_WIDGET_TAP,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}

private enum class WidgetTapFrame {
    RESTING,
    PRESSED,
    RECOVERING
}
