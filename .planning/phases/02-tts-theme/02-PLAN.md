# Phase 2: TTS + Theme — Plan

**Phase:** 02 — TTS + Theme
**Goal:** TtsService (init race + queue + debounce) + ThemeService (HC-04 setTheme 순서 + recreate) + themes_high_contrast (21:1) + Focus→TTS auto attach + BottomBar 토글 와이어링.
**Risk:** 🟡 Medium
**Requirements:** TTS-01/02/03/04/05, HC-01/02/03/04, BAR-03 재검증.

---

## Tasks

### T1. `service/TtsService.kt` (C-1, C-2, M-2, M-4)
- `object TtsService : TextToSpeech.OnInitListener`.
- 필드: `private var tts: TextToSpeech? = null`, `@Volatile private var ready = false`, `private val pending = ArrayDeque<String>()`, `private var lastText: String? = null`, `private var lastTime = 0L`.
- `init(context)`: idempotent. `tts ?: TextToSpeech(context.applicationContext, this).also { tts = it }`.
- `onInit(status)`: SUCCESS 시 setLanguage(Locale.KOREAN) → MISSING_DATA/NOT_SUPPORTED면 `Locale.US`. 이후 `ready=true` + pending flush(while pop). 그 외 status는 ready=false 유지.
- `speak(text)`: enabled 가드(A11yPrefs.ttsEnabled) → debounce(500ms+동일 text) → ready false면 pending offer → ready true면 `tts!!.speak(text, QUEUE_FLUSH, null, "utt-${SystemClock.elapsedRealtime()}")`.
- `setEnabled(enabled)`: A11yPrefs.ttsEnabled = enabled. enabled false면 `tts?.stop()` + pending.clear().
- `isEnabled` getter: A11yPrefs.ttsEnabled.

### T2. `accessibility/A11yViewExt.kt`
- `fun View.attachA11ySpeak()` — 이미 attach된 경우 재attach 회피 위해 tag로 sentinel 처리.
- internal label 추출: contentDescription > (TextView) text > null.
- `OnFocusChangeListener { v, hasFocus -> if (hasFocus && TtsService.isEnabled) TtsService.speak(label) }`.
- `fun ViewGroup.attachA11ySpeakRecursive()` — 자식 walk, focusable 뷰만 attach.

### T3. `service/ThemeService.kt` (HC-04)
- `object`.
- `applyTheme(activity)` — A11yPrefs.highContrastEnabled true면 `activity.setTheme(R.style.Theme_A11yDemo_HighContrast)` else `Theme_A11yDemo`. 호출 측이 super.onCreate 이전에 호출해야 함을 KDoc로 강조.
- `toggle(activity)` — A11yPrefs.highContrastEnabled = !current → `activity.recreate()`.

### T4. `res/values/themes_high_contrast.xml` (HC-02 21:1)
- `<style name="Theme.A11yDemo.HighContrast" parent="Theme.AppCompat.NoActionBar">` — 색상 attr 오버라이드 (background/text/primary/statusBar).
- 색상 추가: `colors.xml`에 `a11y_hc_background=#000`, `a11y_hc_on_surface=#FFF`, `a11y_hc_primary=#FFFF00`, `a11y_hc_bottom_bar_bg=#000` 추가.

### T5. `MainActivity.kt` 수정
- onCreate 시퀀스: A11yPrefs.init → TtsService.init → ThemeService.applyTheme(this) → super.onCreate → setContentView.
- onResume: 첫 진입 또는 backstack 변경 시 `binding.fragmentContainer.attachA11ySpeakRecursive()`. Fragment lifecycle 안정 확보 위해 `view.post { ... }`.
- BottomBar 핸들러:
  - `setOnTtsClick { val next = !A11yPrefs.ttsEnabled; TtsService.setEnabled(next); TtsService.speak(if (next) getString(R.string.tts_enabled_announce) else getString(R.string.tts_disabled_announce)) }` — disabled 발화는 setEnabled 직전에 호출.
  - `setOnHighContrastClick { ThemeService.toggle(this) }`.
  - zoom 2개: 빈 람다 유지(Phase 4 와이어링).
- `OnBackStackChangedListener`: re-attach focus listener (Fragment 교체 시 새 뷰).

### T6. strings.xml 보강
- `tts_enabled_announce` = "음성 안내를 켰습니다", `tts_disabled_announce` = "음성 안내를 껐습니다", `hc_enabled_announce` = (선택, recreate 후 자기참조 발화로 처리).

### T7. contentDescription 누락 보강 패스 (BAR-03 재검증)
- Phase 1 layout 4종 + Phase 2에서 추가되지 않음. grep 재실행으로 누락 0건 확인.

### T8. `02-VERIFICATION.md`
- TTS-01~05, HC-01~04, BAR-03 각 검증 결과. 자동(grep, 코드 inspection) + 수동(에뮬 발화/테마 전환).

---

## DoD

1. ✅ object TtsService + ready/pending + Locale 폴백 + QUEUE_FLUSH + 500ms debounce 코드 inspect 통과.
2. ✅ ThemeService.applyTheme이 super.onCreate 이전에 호출되는 위치(MainActivity onCreate 첫 4줄).
3. ✅ themes_high_contrast.xml 명도 대비 21:1 (#000 ↔ #FFF) — 자동 산출.
4. ✅ BottomBar TTS/HC 토글이 A11yPrefs read/write를 통해 영속.
5. ⚠️ 발화/테마 전환/재시작 후 토글 유지는 사용자 환경 수동 검증.
