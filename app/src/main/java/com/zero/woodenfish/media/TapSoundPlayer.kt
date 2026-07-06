package com.zero.woodenfish.media

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.zero.woodenfish.R

class TapSoundPlayer private constructor(context: Context) {
    private val soundPool: SoundPool
    private val soundId: Int
    private var soundReady = false
    private var pendingPlay = false

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        soundPool = SoundPool.Builder()
            .setAudioAttributes(attributes)
            .setMaxStreams(MAX_STREAMS)
            .build()
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (sampleId == soundId && status == LOAD_SUCCESS) {
                soundReady = true
                if (pendingPlay) {
                    pendingPlay = false
                    doPlay()
                }
            }
        }
        soundId = soundPool.load(context.applicationContext, R.raw.muyu2, SOUND_PRIORITY)
    }

    fun play() {
        if (!soundReady) {
            pendingPlay = true
            return
        }
        doPlay()
    }

    private fun doPlay() {
        soundPool.play(soundId, VOLUME_FULL, VOLUME_FULL, SOUND_PRIORITY, NO_LOOP, PLAYBACK_RATE_NORMAL)
    }

    companion object {
        private const val MAX_STREAMS = 4
        private const val LOAD_SUCCESS = 0
        private const val SOUND_PRIORITY = 1
        private const val NO_LOOP = 0
        private const val VOLUME_FULL = 1f
        private const val PLAYBACK_RATE_NORMAL = 1f

        @Volatile
        private var instance: TapSoundPlayer? = null

        fun getInstance(context: Context): TapSoundPlayer {
            return instance ?: synchronized(this) {
                instance ?: TapSoundPlayer(context.applicationContext).also { instance = it }
            }
        }
    }
}