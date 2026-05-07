# Phase 2: TTS + Theme — Context

**Gathered:** 2026-05-07
**Status:** Ready for planning
**Mode:** Inline autonomous

<domain>
## Phase Boundary

Day 2-3 음성 안내(TTS) + 고대비 테마. TtsService는 contentDescription을 발화하므로 동일 Phase에서 누락 보강 패스(BAR-03 재검증)를 함께 수행. ThemeService recreate가 SharedPreferences를 통해 토글 상태를 보존(HC-03).

Requirements: TTS-01/02/03/04/05, HC-01/02/03/04, BAR-03 재검증.

</domain>

<decisions>
## Implementation Decisions (locked)

### TtsService (C-1, C-2, M-2, M-4 가드)
- **위치:** `service/TtsService.kt`, `object` 싱글턴 (C-2 좀비 회피).
- **API:**
  - `fun init(context: Context)` — applicationContext만 사용. idempotent.
  - `fun speak(text: String)` — debounce + queue. 동일 텍스트 500ms 내 중복 무시.
  - `fun setEnabled(enabled: Boolean)` — A11yPrefs와 동기화. enabled=false면 즉시 stop().
  - `val isEnabled: Boolean` — A11yPrefs.ttsEnabled 위임.
  - `fun shutdown()` — 사용하지 않음 (object 싱글턴 라이프사이클 = 프로세스).
- **Init race (C-1):** `@Volatile private var ready = false` + `private val pending = ArrayDeque<String>()`. `onInit(SUCCESS)`에서 `ready=true` 후 pending flush.
- **Locale (M-2):** `tts.setLanguage(Locale.KOREAN)` → `LANG_MISSING_DATA`/`LANG_NOT_SUPPORTED` 시 `Locale.US` 폴백 + log.
- **Debounce (TTS-02):** `private var lastText: String? = null; private var lastTime: Long = 0L`. 동일 + 500ms 미만이면 skip.
- **QueueMode (M-4):** `tts.speak(text, QUEUE_FLUSH, null, utteranceId)` — 새 포커스가 이전 발화 끊도록.
- **Focus → speak 와이어링:** `accessibility/A11yViewExt.kt`에 `View.attachA11ySpeak()` 확장 함수. 모든 포커서블 뷰에서 `setOnFocusChangeListener { v, has -> if (has && TtsService.isEnabled) TtsService.speak(v.contentDescription?.toString() ?: textIfButton(v)) }`. Phase 2에서는 Activity가 화면 전환 시 root 뷰 트리를 walk해서 attach (간단한 reflection-free 재귀).

### ThemeService (HC-01, HC-04, R-recreate)
- **위치:** `service/ThemeService.kt`, `object`.
- **API:**
  - `fun applyTheme(activity: AppCompatActivity)` — A11yPrefs.highContrastEnabled에 따라 `setTheme(R.style.Theme_A11yDemo_HighContrast)` 또는 `setTheme(R.style.Theme_A11yDemo)`. **반드시 super.onCreate 직전 + setContentView 이전에 호출** (HC-04).
  - `fun toggle(activity: AppCompatActivity)` — A11yPrefs.highContrastEnabled 반전 후 `activity.recreate()`.
- **HC-04 와이어링:** `MainActivity.onCreate` 첫 줄들 순서:
  1. `A11yPrefs.init(applicationContext)`
  2. `TtsService.init(applicationContext)`
  3. `ThemeService.applyTheme(this)` — `setTheme()`
  4. `super.onCreate(savedInstanceState)`
  5. `setContentView(...)`
- **재진입 안전:** recreate 후에도 `savedInstanceState != null` 가드로 Fragment 중복 commit 방지 (Phase 1 골격에서 이미 처리). zoom/focus 등 transient 상태는 Phase 4에서 `onSaveInstanceState`로 보존.

### themes_high_contrast.xml (HC-02, 21:1)
- **위치:** `res/values/themes_high_contrast.xml`. `Theme.A11yDemo.HighContrast`.
- **팔레트:**
  - background: `#FF000000` (순흑)
  - on-surface text: `#FFFFFFFF` (순백) → 명도 대비 21:1
  - primary (focus/accent): `#FFFFFF00` (순황) → 흑 위에서 19.56:1
  - secondary text: `#FFFFFFFF`
  - bottom_bar bg: `#FF000000` + 1px white border
- **상속:** parent `Theme.AppCompat.NoActionBar` (Light가 아닌 dark 베이스).
- **statusBar:** `#FF000000`.

### A11yPrefs 활용
- Phase 1에서 정의된 API 그대로 사용. `ttsEnabled` 토글 시 `TtsService.setEnabled` 호출. `highContrastEnabled` 토글 시 `ThemeService.toggle()` (recreate trigger).

### MainActivity 와이어링 (BAR 클릭 핸들러 attach)
- `binding.accessibilityBottomBar.setOnTtsClick { ... }` → A11yPrefs flip + TtsService.setEnabled + 토스트/스낵바 대신 TTS 안내(자기참조).
- `setOnHighContrastClick { ThemeService.toggle(this) }`.
- zoom 2개는 Phase 4 와이어링 (Phase 2는 빈 람다 유지).

### Focus → TTS 자동 attach
- `MainActivity.onAttachedToWindow` 또는 Fragment commit 후 `view.post { walkAndAttach(rootView) }`. 새 Fragment가 commit되면 `OnBackStackChangedListener`로 재attach.
- 또는 `attachA11ySpeak`을 ViewTreeObserver로 글로벌 attach. Phase 2 단순화: Activity ContentView walk + Fragment lifecycle hook.

### contentDescription 보강 패스 (BAR-03 재검증)
- Phase 1 grep 결과 15건 매칭됨. Phase 2에서 누락 화면이 추가되지 않으므로 신규 누락 0건 예상. 검증 단계에서 grep 재실행 + 결과 VERIFICATION에 첨부.

### shutdown / recreate 시퀀스 (C-2)
- `object TtsService`이므로 프로세스 lifetime 단일 TextToSpeech 인스턴스. recreate가 새 액티비티를 만들어도 TtsService 자체는 변하지 않음. `init`은 idempotent.

### 기존 Phase 1 파일에 대한 변경
- `MainActivity.kt`: onCreate 시퀀스 변경 + BottomBar 핸들러 attach.
- `themes.xml`: `Theme.A11yDemo` light variant 그대로 유지. HC variant는 별도 파일 추가.
- `colors.xml`: HC 팔레트 색상 추가.

</decisions>

<code_context>
- Phase 1 산출물 그대로 유지. 신규 파일 위주 + MainActivity 일부 수정.
- A11yPrefs.kt는 변경 없음 — Phase 2 서비스가 read/write만 사용.
</code_context>

<specifics>
## Critical Pitfalls Watched (C-1, C-2, M-2, M-4, M-5)

| ID | One-liner |
|----|-----------|
| C-1 | `@Volatile ready` + pending 큐 + onInit(SUCCESS) flush |
| C-2 | `object TtsService` + applicationContext만 |
| M-2 | `Locale.KOREAN` 실패 시 `Locale.US` 폴백 + 로그 |
| M-4 | `QUEUE_FLUSH` (포커스 갱신 시 이전 발화 cut) |
| M-5 | HC 팔레트 21:1 고정 (#000000 / #FFFFFF / #FFFF00) |

</specifics>

<deferred>
- Phase 3: dispatchKeyEvent + FocusNavigator + state_focused selector + XML nextFocus*
- Phase 4: VolumeService, ZoomService, ChecklistFragment 7기능, 타임아웃
- Phase 5: README/GIF/배포

</deferred>
