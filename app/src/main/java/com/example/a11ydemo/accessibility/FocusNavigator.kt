package com.example.a11ydemo.accessibility

import android.view.View
import android.view.ViewGroup

/**
 * 키패드 포커스 이동 헬퍼. 대부분 시스템 focusSearch에 위임하고,
 * 콘텐츠 ↔ BottomBar 간 동적 점프(C-4)에서만 helper를 사용한다.
 */
object FocusNavigator {

    fun move(currentFocus: View?, direction: Int): View? =
        currentFocus?.focusSearch(direction)

    fun findFirstFocusable(root: View?): View? {
        if (root == null) return null
        if (root.isFocusable && root.visibility == View.VISIBLE) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val hit = findFirstFocusable(root.getChildAt(i))
                if (hit != null) return hit
            }
        }
        return null
    }

    fun findLastFocusable(root: View?): View? {
        if (root == null) return null
        if (root is ViewGroup) {
            for (i in root.childCount - 1 downTo 0) {
                val hit = findLastFocusable(root.getChildAt(i))
                if (hit != null) return hit
            }
        }
        if (root.isFocusable && root.visibility == View.VISIBLE) return root
        return null
    }
}
