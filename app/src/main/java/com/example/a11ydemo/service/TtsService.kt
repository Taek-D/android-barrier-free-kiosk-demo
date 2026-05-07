package com.example.a11ydemo.service

import android.content.Context
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.a11ydemo.prefs.A11yPrefs
import java.util.ArrayDeque
import java.util.Locale

/**
 * 포커스 변경 시 음성 안내. 토글 상태는 A11yPrefs로 영속.
 *
 * Pitfall guards:
 * - C-1: onInit 전 호출된 발화는 pending 큐에 보관 → onInit(SUCCESS)에서 flush.
 * - C-2: object 싱글턴 + applicationContext만 — recreate 좀비 방지.
 * - M-2: Locale.KOREAN 실패 시 Locale.US 폴백.
 * - M-4: QUEUE_FLUSH로 새 포커스 발화가 이전 발화를 끊는다.
 */
object TtsService : TextToSpeech.OnInitListener {

    private const val TAG = "TtsService"
    private const val DEDUP_WINDOW_MS = 500L

    private var tts: TextToSpeech? = null

    @Volatile
    private var ready = false

    private val pending = ArrayDeque<String>()
    private var lastText: String? = null
    private var lastTime: Long = 0L

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TTS init failed: status=$status")
            ready = false
            return
        }
        val engine = tts ?: return
        val koResult = engine.setLanguage(Locale.KOREAN)
        if (koResult == TextToSpeech.LANG_MISSING_DATA || koResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Korean TTS unavailable, falling back to Locale.US")
            engine.setLanguage(Locale.US)
        }
        ready = true
        // pending flush
        while (pending.isNotEmpty()) {
            val text = pending.pollFirst() ?: break
            engine.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId())
        }
    }

    val isEnabled: Boolean get() = A11yPrefs.ttsEnabled

    fun setEnabled(enabled: Boolean) {
        A11yPrefs.ttsEnabled = enabled
        if (!enabled) {
            tts?.stop()
            pending.clear()
            lastText = null
        }
    }

    fun speak(text: String?) {
        if (text.isNullOrBlank()) return
        if (!A11yPrefs.ttsEnabled) return
        val now = SystemClock.elapsedRealtime()
        if (text == lastText && now - lastTime < DEDUP_WINDOW_MS) return
        lastText = text
        lastTime = now

        val engine = tts
        if (!ready || engine == null) {
            pending.offerLast(text)
            return
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId())
    }

    private fun utteranceId() = "utt-${SystemClock.elapsedRealtime()}"
}
