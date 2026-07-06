package com.zero.woodenfish.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.zero.woodenfish.MainActivity
import com.zero.woodenfish.R
import com.zero.woodenfish.appwidget.WoodenFishAppWidgetProvider
import com.zero.woodenfish.broadcast.sendWoodenFishStateChangedBroadcast
import com.zero.woodenfish.data.WoodenFishStateStore
import com.zero.woodenfish.media.TapSoundPlayer
import com.zero.woodenfish.model.TapSource
import com.zero.woodenfish.model.WoodenFishState
import com.zero.woodenfish.scheduler.AutoTapScheduler

class AutoTapForegroundService : Service() {
    private lateinit var stateStore: WoodenFishStateStore
    private lateinit var tapSoundPlayer: TapSoundPlayer
    private lateinit var autoTapScheduler: AutoTapScheduler
    private var latestNotificationUpdateTimeMs = 0L

    override fun onCreate() {
        super.onCreate()
        stateStore = WoodenFishStateStore(this)
        tapSoundPlayer = TapSoundPlayer(this)
        autoTapScheduler = AutoTapScheduler(
            intervalMs = { stateStore.load().autoTapIntervalMs },
            onTick = ::recordAutoTap
        )
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                stopAutoTap()
                START_NOT_STICKY
            }
            ACTION_REFRESH -> {
                startOrRefreshAutoTap()
                START_STICKY
            }
            else -> {
                startOrRefreshAutoTap()
                START_STICKY
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        autoTapScheduler.stop()
        tapSoundPlayer.release()
        super.onDestroy()
    }

    private fun startOrRefreshAutoTap() {
        val state = stateStore.load()
        if (!state.autoTapEnabled) {
            stopSelf()
            return
        }
        startInForeground(state)
        autoTapScheduler.restart()
        sendWoodenFishStateChangedBroadcast()
    }

    private fun stopAutoTap() {
        autoTapScheduler.stop()
        stateStore.setAutoTapEnabled(stateStore.load(), false)
        sendWoodenFishStateChangedBroadcast()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun recordAutoTap() {
        val currentState = stateStore.load()
        if (!currentState.autoTapEnabled) {
            stopAutoTap()
            return
        }

        val nextState = stateStore.recordTap(TapSource.AUTO)
        if (nextState.soundEnabled) {
            tapSoundPlayer.play()
        }
        WoodenFishAppWidgetProvider.renderAutoTapFeedback(this, nextState)
        startInForegroundIfNeeded(nextState)
        sendBroadcast(Intent(ACTION_AUTO_TAP_RECORDED).setPackage(packageName))
    }

    private fun startInForegroundIfNeeded(state: WoodenFishState) {
        val now = System.currentTimeMillis()
        if (now - latestNotificationUpdateTimeMs < NOTIFICATION_UPDATE_INTERVAL_MS) return
        latestNotificationUpdateTimeMs = now
        startInForeground(state)
    }

    private fun startInForeground(state: WoodenFishState) {
        latestNotificationUpdateTimeMs = System.currentTimeMillis()
        val notification = buildNotification(state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(state: WoodenFishState): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_CODE_OPEN_APP,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, AutoTapForegroundService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            REQUEST_CODE_STOP,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bottom_nav_wooden_fish)
            .setContentTitle(getString(R.string.auto_tap_notification_title))
            .setContentText(getString(R.string.auto_tap_notification_text, state.todayCount))
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_bottom_nav_settings,
                getString(R.string.stop_auto_tap),
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.auto_tap_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_AUTO_TAP_RECORDED = "com.zero.woodenfish.action.AUTO_TAP_RECORDED"
        private const val ACTION_START = "com.zero.woodenfish.action.START_AUTO_TAP"
        private const val ACTION_REFRESH = "com.zero.woodenfish.action.REFRESH_AUTO_TAP"
        private const val ACTION_STOP = "com.zero.woodenfish.action.STOP_AUTO_TAP"
        private const val NOTIFICATION_CHANNEL_ID = "auto_tap"
        private const val NOTIFICATION_ID = 108
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 5_000L
        private const val REQUEST_CODE_OPEN_APP = 10
        private const val REQUEST_CODE_STOP = 11

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, serviceIntent(context, ACTION_START))
        }

        fun refresh(context: Context) {
            ContextCompat.startForegroundService(context, serviceIntent(context, ACTION_REFRESH))
        }

        fun stop(context: Context) {
            context.startService(serviceIntent(context, ACTION_STOP))
        }

        private fun serviceIntent(context: Context, action: String): Intent {
            return Intent(context, AutoTapForegroundService::class.java).setAction(action)
        }
    }
}
