package com.example.a11ydemo.service

import androidx.appcompat.app.AppCompatActivity
import com.example.a11ydemo.R
import com.example.a11ydemo.prefs.A11yPrefs

/**
 * 고대비 테마 토글. setTheme()는 setContentView() 이전에 호출되어야 한다(HC-04).
 * 호출 패턴:
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *       A11yPrefs.init(applicationContext)
 *       ThemeService.applyTheme(this)        // ← super.onCreate 이전
 *       super.onCreate(savedInstanceState)
 *       setContentView(...)
 *   }
 */
object ThemeService {

    fun applyTheme(activity: AppCompatActivity) {
        val themeRes = if (A11yPrefs.highContrastEnabled) {
            R.style.Theme_A11yDemo_HighContrast
        } else {
            R.style.Theme_A11yDemo
        }
        activity.setTheme(themeRes)
    }

    fun toggle(activity: AppCompatActivity) {
        A11yPrefs.highContrastEnabled = !A11yPrefs.highContrastEnabled
        activity.recreate()
    }
}
