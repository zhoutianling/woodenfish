package com.zero.woodenfish.model

sealed interface WoodenFishUiEvent {
    val id: Long

    data class TapFeedback(
        override val id: Long,
        val soundEnabled: Boolean,
        val hapticEnabled: Boolean
    ) : WoodenFishUiEvent
}
