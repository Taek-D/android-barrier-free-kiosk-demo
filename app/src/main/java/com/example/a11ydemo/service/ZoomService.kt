package com.example.a11ydemo.service

import android.view.View
import com.example.a11ydemo.prefs.A11yPrefs

/**
 * 콘텐츠 영역 줌. scaleX/scaleY 직접 변경(M-6 ScaleAnimation 휘발성 회피).
 * 레벨은 A11yPrefs(SharedPreferences)에 영속 (MEDIA-03 — 회전/테마 전환/재시작 후 유지).
 */
object ZoomService {

    private const val STEP = 0.1f

    fun zoomIn(target: View) {
        A11yPrefs.zoomLevel = A11yPrefs.zoomLevel + STEP // setter에서 0.8~1.5 clamp
        apply(target)
    }

    fun zoomOut(target: View) {
        A11yPrefs.zoomLevel = A11yPrefs.zoomLevel - STEP
        apply(target)
    }

    fun apply(target: View) {
        val level = A11yPrefs.zoomLevel
        target.pivotX = 0f
        target.pivotY = 0f
        target.scaleX = level
        target.scaleY = level
    }

    fun current(): Float = A11yPrefs.zoomLevel
}
