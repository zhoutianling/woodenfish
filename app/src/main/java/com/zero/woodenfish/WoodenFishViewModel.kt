package com.zero.woodenfish

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zero.woodenfish.broadcast.ACTION_WOODEN_FISH_STATE_CHANGED
import com.zero.woodenfish.broadcast.sendWoodenFishStateChangedBroadcast
import com.zero.woodenfish.data.WoodenFishStateStore
import com.zero.woodenfish.model.TapSource
import com.zero.woodenfish.model.WoodenFishState
import com.zero.woodenfish.model.WoodenFishTab
import com.zero.woodenfish.model.WoodenFishUiEvent
import com.zero.woodenfish.service.AutoTapForegroundService

class WoodenFishViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val stateStore = WoodenFishStateStore(application)
    private val mutableState = MutableLiveData(stateStore.load())
    private val mutableEvents = MutableLiveData<WoodenFishUiEvent>()
    private val autoTapServiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshStateFromStore()
            if (intent.action == AutoTapForegroundService.ACTION_AUTO_TAP_RECORDED) {
                emitTapFeedback(soundEnabled = false, hapticEnabled = false)
            }
        }
    }
    private var nextEventId = 0L

    val state: LiveData<WoodenFishState> = mutableState
    val events: LiveData<WoodenFishUiEvent> = mutableEvents
    val latestEventId: Long
        get() = nextEventId

    init {
        registerAutoTapServiceReceiver()
    }

    fun onVisible() {
        if (currentState.autoTapEnabled) {
            AutoTapForegroundService.start(appContext)
            refreshStateFromStore()
        }
    }

    fun onHidden() {
        refreshStateFromStore()
    }

    fun onTabSelected(tab: WoodenFishTab) {
        updateState(currentState.copy(selectedTab = tab))
    }

    fun onManualTap() {
        recordTap(TapSource.MANUAL)
    }

    fun onClearTodayClicked() {
        updateState(stateStore.clearToday(currentState))
        appContext.sendWoodenFishStateChangedBroadcast()
    }

    fun onAutoTapEnabledChanged(enabled: Boolean) {
        val nextState = stateStore.setAutoTapEnabled(currentState, enabled)
        updateState(nextState)
        if (enabled) {
            AutoTapForegroundService.start(appContext)
        } else {
            AutoTapForegroundService.stop(appContext)
        }
    }

    fun onSoundEnabledChanged(enabled: Boolean) {
        val nextState = stateStore.setSoundEnabled(currentState, enabled)
        updateState(nextState)
        if (nextState.autoTapEnabled) {
            AutoTapForegroundService.refresh(appContext)
        }
    }

    fun onHapticEnabledChanged(enabled: Boolean) {
        updateState(stateStore.setHapticEnabled(currentState, enabled))
    }

    fun onImmersiveEnabledChanged(enabled: Boolean) {
        updateState(stateStore.setImmersiveEnabled(currentState, enabled))
    }

    fun onAutoTapIntervalChanged(intervalMs: Long) {
        val nextState = stateStore.setAutoTapInterval(currentState, intervalMs)
        updateState(nextState)
        if (nextState.autoTapEnabled) {
            AutoTapForegroundService.refresh(appContext)
        }
    }

    fun onNotificationPermissionGranted() {
        if (currentState.autoTapEnabled) {
            AutoTapForegroundService.refresh(appContext)
        }
    }

    override fun onCleared() {
        appContext.unregisterReceiver(autoTapServiceReceiver)
        super.onCleared()
    }

    private fun recordTap(source: TapSource) {
        val nextState = stateStore.recordTap(currentState, source)
        updateState(nextState)
        appContext.sendWoodenFishStateChangedBroadcast()
        emitTapFeedback(
            soundEnabled = nextState.soundEnabled,
            hapticEnabled = nextState.hapticEnabled
        )
    }

    private fun registerAutoTapServiceReceiver() {
        val filter = IntentFilter().apply {
            addAction(AutoTapForegroundService.ACTION_AUTO_TAP_RECORDED)
            addAction(ACTION_WOODEN_FISH_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            appContext,
            autoTapServiceReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun refreshStateFromStore() {
        updateState(stateStore.load().copy(selectedTab = currentState.selectedTab))
    }

    private fun emitTapFeedback(soundEnabled: Boolean, hapticEnabled: Boolean) {
        mutableEvents.value = WoodenFishUiEvent.TapFeedback(
            id = ++nextEventId,
            soundEnabled = soundEnabled,
            hapticEnabled = hapticEnabled
        )
    }

    private val currentState: WoodenFishState
        get() = mutableState.value ?: stateStore.load()

    private fun updateState(state: WoodenFishState) {
        mutableState.value = state
    }
}
