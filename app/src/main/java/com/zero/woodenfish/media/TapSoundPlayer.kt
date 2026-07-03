package com.zero.woodenfish.media

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.zero.woodenfish.R

class TapSoundPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var soundReady = false
    private val soundPool: SoundPool
    private val soundId: Int

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
            soundReady = sampleId == soundId && status == LOAD_SUCCESS
        }
        soundId = soundPool.load(appContext, R.raw.muyu, SOUND_PRIORITY)
    }

    fun play() {
        if (!soundReady) return
        soundPool.play(soundId, VOLUME_FULL, VOLUME_FULL, SOUND_PRIORITY, NO_LOOP, PLAYBACK_RATE_NORMAL)
    }

    fun release() {
        soundPool.release()
    }

    private companion object {
        const val MAX_STREAMS = 4
        const val LOAD_SUCCESS = 0
        const val SOUND_PRIORITY = 1
        const val NO_LOOP = 0
        const val VOLUME_FULL = 1f
        const val PLAYBACK_RATE_NORMAL = 1f
    }
}
