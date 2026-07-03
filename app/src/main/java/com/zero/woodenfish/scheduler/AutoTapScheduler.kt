package com.zero.woodenfish.scheduler

import android.os.Handler
import android.os.Looper

class AutoTapScheduler(
    private val intervalMs: () -> Long,
    private val onTick: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            onTick()
            if (running) {
                handler.postDelayed(this, intervalMs())
            }
        }
    }

    fun start() {
        if (running) return
        running = true
        handler.postDelayed(tickRunnable, intervalMs())
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tickRunnable)
    }

    fun restart() {
        stop()
        start()
    }
}
