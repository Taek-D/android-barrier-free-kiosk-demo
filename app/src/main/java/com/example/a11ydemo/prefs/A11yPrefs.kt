package com.example.a11ydemo.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * 접근성 토글/줌 영속화. recreate 좀비 회피를 위해 applicationContext만 사용.
 * Phase 2 ThemeService/TtsService, Phase 4 ZoomService가 import만으로 사용한다.
 */
object A11yPrefs {

    private const val PREFS_NAME = "a11y_prefs"
    private const val KEY_TTS_ENABLED = "tts_enabled"
    private const val KEY_HIGH_CONTRAST_ENABLED = "high_contrast_enabled"
    private const val KEY_ZOOM_LEVEL = "zoom_level"

    private const val ZOOM_MIN = 0.8f
    private const val ZOOM_MAX = 1.5f
    private const val ZOOM_DEFAULT = 1.0f

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var ttsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_TTS_ENABLED, value).apply()

    var highContrastEnabled: Boolean
        get() = prefs.getBoolean(KEY_HIGH_CONTRAST_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_HIGH_CONTRAST_ENABLED, value).apply()

    var zoomLevel: Float
        get() = prefs.getFloat(KEY_ZOOM_LEVEL, ZOOM_DEFAULT)
        set(value) {
            val clamped = value.coerceIn(ZOOM_MIN, ZOOM_MAX)
            prefs.edit().putFloat(KEY_ZOOM_LEVEL, clamped).apply()
        }
}
