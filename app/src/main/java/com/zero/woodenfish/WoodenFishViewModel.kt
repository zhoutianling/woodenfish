package com.zero.woodenfish

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zero.woodenfish.data.WoodenFishStateStore
import com.zero.woodenfish.model.TapSource
import com.zero.woodenfish.model.WoodenFishState
import com.zero.woodenfish.model.WoodenFishTab
import com.zero.woodenfish.model.WoodenFishUiEvent
import com.zero.woodenfish.scheduler.AutoTapScheduler

class WoodenFishViewModel(application: Application) : AndroidViewModel(application) {
    private val stateStore = WoodenFishStateStore(application)
    private val autoTapScheduler = AutoTapScheduler(
        intervalMs = { currentState.autoTapIntervalMs },
        onTick = { recordTap(TapSource.AUTO) }
    )
    private val mutableState = MutableLiveData(stateStore.load())
    private val mutableEvents = MutableLiveData<WoodenFishUiEvent>()
    private var nextEventId = 0L

    val state: LiveData<WoodenFishState> = mutableState
    val events: LiveData<WoodenFishUiEvent> = mutableEvents
    val latestEventId: Long
        get() = nextEventId

    fun onVisible() {
        if (currentState.autoTapEnabled) {
            autoTapScheduler.start()
        }
    }

    fun onHidden() {
        autoTapScheduler.stop()
    }

    fun onTabSelected(tab: WoodenFishTab) {
        updateState(currentState.copy(selectedTab = tab))
    }

    fun onManualTap() {
        recordTap(TapSource.MANUAL)
    }

    fun onClearTodayClicked() {
        updateState(stateStore.clearToday(currentState))
    }

    fun onAutoTapEnabledChanged(enabled: Boolean) {
        val nextState = stateStore.setAutoTapEnabled(currentState, enabled)
        updateState(nextState)
        if (enabled) {
            autoTapScheduler.restart()
        } else {
            autoTapScheduler.stop()
        }
    }

    fun onSoundEnabledChanged(enabled: Boolean) {
        updateState(stateStore.setSoundEnabled(currentState, enabled))
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
            autoTapScheduler.restart()
        }
    }

    override fun onCleared() {
        autoTapScheduler.stop()
        super.onCleared()
    }

    private fun recordTap(source: TapSource) {
        val nextState = stateStore.recordTap(currentState, source)
        updateState(nextState)
        mutableEvents.value = WoodenFishUiEvent.TapFeedback(
            id = ++nextEventId,
            soundEnabled = nextState.soundEnabled,
            hapticEnabled = nextState.hapticEnabled
        )
    }

    private val currentState: WoodenFishState
        get() = mutableState.value ?: stateStore.load()

    private fun updateState(state: WoodenFishState) {
        mutableState.value = state
    }
}
