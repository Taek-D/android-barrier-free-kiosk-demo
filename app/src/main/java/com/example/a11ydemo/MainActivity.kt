package com.example.a11ydemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.a11ydemo.databinding.ActivityMainBinding
import com.example.a11ydemo.prefs.A11yPrefs
import com.example.a11ydemo.ui.fragment.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // setTheme()은 Phase 2 ThemeService가 setContentView 직전에 호출 (HC-04).
        A11yPrefs.init(applicationContext)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        // BottomBar 와이어링은 Phase 2(TTS/HC) ~ Phase 4(zoom)에서 attach.
        // dispatchKeyEvent override는 Phase 3에서 추가.
    }
}
