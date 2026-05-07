package com.example.a11ydemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.a11ydemo.accessibility.attachA11ySpeakRecursive
import com.example.a11ydemo.databinding.ActivityMainBinding
import com.example.a11ydemo.prefs.A11yPrefs
import com.example.a11ydemo.service.ThemeService
import com.example.a11ydemo.service.TtsService
import com.example.a11ydemo.ui.fragment.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Prefs init (applicationContext, idempotent)
        A11yPrefs.init(applicationContext)
        // 2. TTS engine 부트 (init race 가드, applicationContext)
        TtsService.init(applicationContext)
        // 3. setTheme() — setContentView 이전 (HC-04)
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

        // Fragment 교체 시 새 뷰에 focus listener 재attach
        supportFragmentManager.addOnBackStackChangedListener {
            binding.root.post { binding.root.attachA11ySpeakRecursive() }
        }

        // dispatchKeyEvent override는 Phase 3에서 추가.
    }

    override fun onResume() {
        super.onResume()
        binding.root.post { binding.root.attachA11ySpeakRecursive() }
    }

    private fun wireBottomBar() {
        binding.accessibilityBottomBar.setOnTtsClick {
            val next = !A11yPrefs.ttsEnabled
            // enabled false→true 전환 시: 먼저 enabled 켠 뒤 안내 발화
            // enabled true→false 전환 시: 종료 안내를 켜진 상태에서 발화한 뒤 끔
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
        binding.accessibilityBottomBar.setOnZoomInClick {
            // Phase 4 와이어링.
        }
        binding.accessibilityBottomBar.setOnZoomOutClick {
            // Phase 4 와이어링.
        }
    }
}
