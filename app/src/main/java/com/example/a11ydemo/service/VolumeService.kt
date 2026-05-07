package com.example.a11ydemo.service

import android.content.Context
import android.media.AudioManager

/**
 * STREAM_MUSIC 음량 증감 (MEDIA-01). FLAG_SHOW_UI로 시스템 슬라이더 노출 →
 * 평가자가 시각으로 변화 확인 가능.
 */
object VolumeService {

    private var audioManager: AudioManager? = null

    fun init(context: Context) {
        if (audioManager != null) return
        audioManager = context.applicationContext
            .getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun increment() {
        audioManager?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun decrement() {
        audioManager?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }
}
