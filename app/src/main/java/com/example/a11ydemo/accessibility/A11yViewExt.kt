package com.example.a11ydemo.accessibility

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.a11ydemo.R
import com.example.a11ydemo.service.TtsService

/**
 * 포커스 변경 시 라벨을 TtsService에 전달. 모든 포커서블 뷰에 한 번만 attach
 * (sentinel tag로 중복 회피). recreate/Fragment 교체 후 root 뷰에 재호출 가능.
 */
fun View.attachA11ySpeak() {
    if (!isFocusable) return
    if (getTag(R.id.tag_a11y_speak_attached) == true) return

    val previous = onFocusChangeListener
    setOnFocusChangeListener { v, hasFocus ->
        previous?.onFocusChange(v, hasFocus)
        if (hasFocus) {
            TtsService.speak(v.a11yLabel())
        }
    }
    setTag(R.id.tag_a11y_speak_attached, true)
}

fun ViewGroup.attachA11ySpeakRecursive() {
    attachA11ySpeak()
    for (i in 0 until childCount) {
        when (val child = getChildAt(i)) {
            is ViewGroup -> child.attachA11ySpeakRecursive()
            else -> child.attachA11ySpeak()
        }
    }
}

private fun View.a11yLabel(): String? {
    contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
    if (this is TextView) {
        text?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
    }
    return null
}
