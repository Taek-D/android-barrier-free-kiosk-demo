package com.example.a11ydemo.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.a11ydemo.databinding.ViewAccessibilityBottomBarBinding

/**
 * 4버튼(고대비/TTS/확대/축소) 항상 노출 컨트롤러. (BAR-01/02)
 * Phase 1은 setter만 노출 — Phase 2~4가 토글/줌 핸들러를 attach.
 */
class AccessibilityBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewAccessibilityBottomBarBinding =
        ViewAccessibilityBottomBarBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = HORIZONTAL
    }

    fun setOnHighContrastClick(action: () -> Unit) {
        binding.btnHighContrast.setOnClickListener { action() }
    }

    fun setOnTtsClick(action: () -> Unit) {
        binding.btnTts.setOnClickListener { action() }
    }

    fun setOnZoomInClick(action: () -> Unit) {
        binding.btnZoomIn.setOnClickListener { action() }
    }

    fun setOnZoomOutClick(action: () -> Unit) {
        binding.btnZoomOut.setOnClickListener { action() }
    }
}
