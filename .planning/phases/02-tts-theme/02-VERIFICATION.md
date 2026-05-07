---
phase: 2
phase_name: TTS + Theme
status: human_needed
verified_at: 2026-05-07
---

# Phase 2: TTS + Theme — Verification

요구사항: TTS-01~05, HC-01~04, BAR-03 재검증.

## Success Criteria 검증

### 1. TTS-01/02: 포커스 이동 시 라벨 발화 + 500ms 동일 텍스트 중복 억제

**Status:** ✅ structural / ⚠️ runtime
- ✅ `accessibility/A11yViewExt.kt#attachA11ySpeak` — 포커스 listener에서 `TtsService.speak(label)` 호출. 기존 listener는 `previous` 변수로 보존 후 호출(체이닝).
- ✅ `service/TtsService.kt#speak`: `if (text == lastText && now - lastTime < DEDUP_WINDOW_MS) return` (500ms).
- ✅ MainActivity.onResume + addOnBackStackChangedListener에서 `attachA11ySpeakRecursive()` 재attach. sentinel tag (`tag_a11y_speak_attached`)로 중복 attach 회피.
- ⚠️ 실제 발화 동작은 사용자 환경(에뮬 + 한국어 TTS) 필요.

### 2. TTS-04: init race 가드 (C-1)

**Status:** ✅
- `@Volatile private var ready = false` + `pending = ArrayDeque<String>()`.
- `speak`에서 `!ready || engine == null` → `pending.offerLast(text); return`.
- `onInit(SUCCESS)` 시 setLanguage 후 `ready = true` → while pop으로 pending flush(`QUEUE_ADD`).

### 3. TTS-05: 한국어 미설치 시 Locale.US 폴백 (M-2)

**Status:** ✅
- `setLanguage(Locale.KOREAN)` 결과가 `LANG_MISSING_DATA` 또는 `LANG_NOT_SUPPORTED`이면 `setLanguage(Locale.US)`. 로그 기록.

### 4. TTS-03: 토글 영속 + BottomBar 핸들러

**Status:** ✅
- `MainActivity.wireBottomBar` → `setOnTtsClick`: enabled false→true는 켠 후 안내 발화, true→false는 안내 발화 후 끔(자기참조 발화 보장).
- `TtsService.setEnabled(enabled)` → `A11yPrefs.ttsEnabled = enabled`. 앱 재시작 후 SharedPrefs read로 복원.

### 5. HC-04: setTheme이 setContentView 이전

**Status:** ✅
- `MainActivity.onCreate` 첫 4줄: A11yPrefs.init → TtsService.init → ThemeService.applyTheme(this) → super.onCreate. 5번째 줄에서 setContentView. HC-04 위반 0건.

### 6. HC-01/02: 고대비 테마 + 21:1 명도 대비

**Status:** ✅
- `res/values/themes_high_contrast.xml` → `Theme.A11yDemo.HighContrast` (parent `Theme.AppCompat.NoActionBar`).
- 색상 (HC-02 21:1):
  - background `#FF000000` ↔ on-surface text `#FFFFFFFF` = **명도 대비 21:1** (WCAG AA 4.5:1 대비 4.6배 여유).
  - primary/focus `#FFFFFF00` on `#FF000000` = 19.56:1 (KWCAG 6.1.x 충족).
- `?attr/a11yBottomBarBg`로 BottomBar 배경 attr 처리 — light=grey/HC=black 자동 전환.
- `ThemeService.toggle(activity)` → A11yPrefs flip + `activity.recreate()`.

### 7. HC-03: 토글 SharedPreferences 영속 (research F-12 P0 승격)

**Status:** ✅
- `A11yPrefs.highContrastEnabled` getter/setter가 SharedPreferences에 즉시 write.
- `ThemeService.applyTheme`은 onCreate마다 prefs를 read해 `setTheme` 호출 → recreate/재시작 후에도 동일 테마 적용.

### 8. BAR-03 재검증 (contentDescription/android:text grep)

**Status:** ✅ pass
- 4개 layout에서 15건 매칭(Phase 1과 동일, 신규 누락 0건).
- Phase 2에서 layout 추가 없음 (코드 파일만 추가). 재검증 결과 변동 없음.

---

## Pitfalls Status

| ID | Watched | Status |
|----|---------|--------|
| C-1 | TTS init race | ✅ ready guard + pending queue + onInit flush |
| C-2 | recreate 좀비 TTS | ✅ object 싱글턴 + applicationContext only |
| M-2 | 한국어 TTS 미설치 | ✅ Locale.US 폴백 + 로그 |
| M-4 | 포커스 시 QUEUE_FLUSH | ✅ `tts.speak(text, QUEUE_FLUSH, ...)` |
| M-5 | HC 21:1 팔레트 | ✅ #000/#FFF + #FFFF00 (focus) |

---

## 신규/수정 산출물

### 신규
- `service/TtsService.kt`
- `service/ThemeService.kt`
- `accessibility/A11yViewExt.kt`
- `res/values/ids.xml` (sentinel tag id)
- `res/values/attrs.xml` (a11yBottomBarBg)
- `res/values/themes_high_contrast.xml`
- `.planning/phases/02-tts-theme/{CONTEXT,PLAN,VERIFICATION}.md`

### 수정
- `MainActivity.kt` — onCreate 시퀀스 + BottomBar 핸들러 + onResume/backstack listener
- `res/values/themes.xml` — `a11yBottomBarBg` attr 매핑
- `res/values/colors.xml` — HC 4색 추가
- `res/values/strings.xml` — TTS 안내 2건 추가
- `res/layout/activity_main.xml` — BottomBar 배경 attr로 변경

---

## Human Validation Items

1. **에뮬 발화 검증:** AVD 부팅 → TTS 토글 ON → 홈 버튼으로 포커스 이동 시 한국어(또는 US 폴백) 발화 확인. 동일 버튼 재포커스 500ms 내 묵음 확인.
2. **첫 발화 묵음 회피:** 앱 콜드 스타트 직후 TTS 토글 ON → 첫 발화가 onInit pending flush로 들어가는지 확인 (logcat 또는 청취).
3. **HC 토글:** BottomBar 고대비 버튼 탭 → 흑백 전환 + recreate 후 토글 유지 확인. 앱 재시작 후에도 유지 확인 (HC-03).
4. **21:1 대비:** WebAIM Contrast Checker 또는 Android Accessibility Scanner로 명도 대비 측정 — #000↔#FFF 21:1 자동 산출 확인.

---

## Verdict

| Item | Result |
|------|--------|
| 자동 산출물 정합성 (TTS-01~05, HC-01~04, BAR-03) | ✅ pass |
| 사용자 환경 발화/테마/영속 검증 | ⚠️ human_needed |

**Overall:** `human_needed` — 코드 정합성 통과. 발화/테마 전환 동작은 사용자 환경 1회 확인 후 closure.
