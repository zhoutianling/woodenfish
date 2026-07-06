package com.zero.woodenfish.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.widget.RemoteViews
import com.zero.woodenfish.R
import com.zero.woodenfish.broadcast.ACTION_WOODEN_FISH_STATE_CHANGED
import com.zero.woodenfish.broadcast.sendWoodenFishStateChangedBroadcast
import com.zero.woodenfish.data.WoodenFishStateStore
import com.zero.woodenfish.model.TapSource
import com.zero.woodenfish.model.WoodenFishState

class WoodenFishAppWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WIDGET_TAP -> {
                handleWidgetTap(context)
                return
            }
            ACTION_WOODEN_FISH_STATE_CHANGED -> {
                updateAllWidgets(context)
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

    private fun handleWidgetTap(context: Context) {
        val stateStore = WoodenFishStateStore(context)
        val nextState = stateStore.recordTap(stateStore.load(), TapSource.MANUAL)
        playTapSoundIfNeeded(context, nextState)
        updateAllWidgets(context, nextState)
        context.sendWoodenFishStateChangedBroadcast()
    }

    private fun playTapSoundIfNeeded(context: Context, state: WoodenFishState) {
        if (!state.soundEnabled) return
        runCatching {
            MediaPlayer.create(context.applicationContext, R.raw.muyu)?.apply {
                setOnCompletionListener(MediaPlayer::release)
                setOnErrorListener { mediaPlayer, _, _ ->
                    mediaPlayer.release()
                    true
                }
                start()
            }
        }
    }

    companion object {
        private const val ACTION_WIDGET_TAP = "com.zero.woodenfish.action.WIDGET_TAP"
        private const val REQUEST_CODE_WIDGET_TAP = 20

        fun updateAllWidgets(context: Context, state: WoodenFishState = WoodenFishStateStore(context).load()) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WoodenFishAppWidgetProvider::class.java)
            )
            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context, state))
            }
        }

        private fun buildRemoteViews(context: Context, state: WoodenFishState): RemoteViews {
            return RemoteViews(context.packageName, R.layout.widget_wooden_fish).apply {
                val countText = state.todayCount.toString()
                setTextViewText(R.id.widget_count_text, countText)
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
