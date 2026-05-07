package com.example.a11ydemo

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.a11ydemo.accessibility.FocusNavigator
import com.example.a11ydemo.accessibility.attachA11ySpeakRecursive
import com.example.a11ydemo.databinding.ActivityMainBinding
import com.example.a11ydemo.prefs.A11yPrefs
import com.example.a11ydemo.service.ThemeService
import com.example.a11ydemo.service.TtsService
import com.example.a11ydemo.ui.fragment.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        A11yPrefs.init(applicationContext)
        TtsService.init(applicationContext)
        ThemeService.applyTheme(this)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        wireBottomBar()

        supportFragmentManager.addOnBackStackChangedListener {
            binding.root.post { binding.root.attachA11ySpeakRecursive() }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.root.post { binding.root.attachA11ySpeakRecursive() }
    }

    /**
     * dispatchKeyEvent — 화이트리스트 외 키는 super 위임 (C-3).
     * BACK, EditText 입력, VOLUME 등 모든 비방향키가 정상 동작해야 한다.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event)

        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                currentFocus?.performClick()?.also { return true } ?: super.dispatchKeyEvent(event)
            }

            KeyEvent.KEYCODE_DPAD_UP -> handleUp(event)
            KeyEvent.KEYCODE_DPAD_DOWN -> moveFocus(View.FOCUS_DOWN, event)
            KeyEvent.KEYCODE_DPAD_LEFT -> moveFocus(View.FOCUS_LEFT, event)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveFocus(View.FOCUS_RIGHT, event)

            else -> super.dispatchKeyEvent(event)
        }
    }

    /**
     * BottomBar 자식 → 콘텐츠 마지막 포커서블로 동적 복귀(C-4 양방향 보강).
     * 그 외 위치에서는 시스템 focusSearch 위임.
     */
    private fun handleUp(event: KeyEvent): Boolean {
        val focused = currentFocus
        if (focused != null && isInBottomBar(focused)) {
            val last = FocusNavigator.findLastFocusable(binding.fragmentContainer)
            if (last != null) {
                last.requestFocus()
                return true
            }
        }
        return moveFocus(View.FOCUS_UP, event)
    }

    private fun moveFocus(direction: Int, event: KeyEvent): Boolean {
        val next = FocusNavigator.move(currentFocus, direction)
        if (next != null && next !== currentFocus) {
            next.requestFocus()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun isInBottomBar(view: View): Boolean {
        var cursor: View? = view
        while (cursor != null) {
            if (cursor.id == R.id.accessibility_bottom_bar) return true
            val parent = cursor.parent
            cursor = parent as? View
        }
        return false
    }

    private fun wireBottomBar() {
        binding.accessibilityBottomBar.setOnTtsClick {
            val next = !A11yPrefs.ttsEnabled
            if (next) {
                TtsService.setEnabled(true)
                TtsService.speak(getString(R.string.tts_enabled_announce))
            } else {
                TtsService.speak(getString(R.string.tts_disabled_announce))
                TtsService.setEnabled(false)
            }
        }
        binding.accessibilityBottomBar.setOnHighContrastClick {
            ThemeService.toggle(this)
        }
        binding.accessibilityBottomBar.setOnZoomInClick { /* Phase 4 */ }
        binding.accessibilityBottomBar.setOnZoomOutClick { /* Phase 4 */ }
    }
}
