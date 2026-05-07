# v1 Requirements (archived) — Android 베리어프리 키오스크 데모

**Defined:** 2026-05-07
**Shipped:** 2026-05-07
**Status:** ✅ All 32 v1 requirements satisfied (artifacts complete; some `human_needed` items closed via `Release Checklist 1~7` 자동 처리, 8·9는 사용자 후속 액션).

---

## Bottom Bar (BAR)

- [x] **BAR-01**: 모든 Fragment 화면 하단 4버튼 컨트롤 바 항상 노출 — `view_accessibility_bottom_bar.xml`, `AccessibilityBottomBar.kt`, `activity_main.xml` ConstraintLayout `bottom` 앵커.
- [x] **BAR-02**: 56dp×56dp 이상 터치 타깃 — `@dimen/a11y_min_touch=56dp` 4버튼 모두 적용.
- [x] **BAR-03**: 모든 인터랙티브 뷰 `contentDescription` / `android:text` 명시 — strings.xml grep 4 layout 41건 매칭. Phase 2 TTS attach 시 보강 패스 통과.

## TTS

- [x] **TTS-01**: 포커스 변경 시 라벨 음성 안내 — `accessibility/A11yViewExt.kt#attachA11ySpeak` + `OnFocusChangeListener` chaining.
- [x] **TTS-02**: 동일 텍스트 500ms 내 중복 억제 — `TtsService.speak`의 `DEDUP_WINDOW_MS=500L`.
- [x] **TTS-03**: BottomBar TTS 토글 + 재시작 후 유지 — `MainActivity.wireBottomBar` + `A11yPrefs.ttsEnabled` SharedPrefs.
- [x] **TTS-04**: onInit 전 발화는 pending 큐 → SUCCESS 시 flush — `@Volatile ready` + `ArrayDeque<String> pending`.
- [x] **TTS-05**: 한국어 미설치 시 `Locale.US` 폴백 — `setLanguage(Locale.KOREAN)` 결과 `LANG_MISSING_DATA`/`LANG_NOT_SUPPORTED` 분기.

## High Contrast (HC)

- [x] **HC-01**: BottomBar 고대비 토글 + recreate — `ThemeService.toggle(activity)` → `A11yPrefs.highContrastEnabled` flip + `activity.recreate()`.
- [x] **HC-02**: 명도 대비 WCAG AA 4.5:1 이상 — `themes_high_contrast.xml` 흑(#000) 백(#FFF) **21:1** + focus 황(#FFFF00) 19.56:1.
- [x] **HC-03**: 토글 SharedPreferences 영속 — `A11yPrefs.highContrastEnabled` getter/setter, recreate/재시작 후 read.
- [x] **HC-04**: `setTheme()` BEFORE `setContentView()` — `MainActivity.onCreate` 4줄 시퀀스(prefs/tts/volume init → applyTheme → super.onCreate).

## Focus / Keypad (FOCUS)

- [x] **FOCUS-01**: D-pad ↑↓←→ 포커스 이동 — `dispatchKeyEvent` 화이트리스트 + `FocusNavigator.move(focusSearch)` + `requestFocus`.
- [x] **FOCUS-02**: ENTER/DPAD_CENTER로 활성화 — `KEYCODE_ENTER`/`KEYCODE_NUMPAD_ENTER`/`KEYCODE_DPAD_CENTER` → `currentFocus.performClick()`.
- [x] **FOCUS-03**: BACK/EditText/VOLUME 등 비방향키 super 위임 — `else -> super.dispatchKeyEvent(event)` + `event.action != ACTION_DOWN` 가드.
- [x] **FOCUS-04**: 콘텐츠 ↔ BottomBar 양방향 — XML `nextFocusDown` 3 fragment 정적 + `handleUp` BottomBar→콘텐츠 동적.
- [x] **FOCUS-05**: state_focused 3dp stroke + ripple 회피 — `focused_background.xml` + `android:foreground` API 23+ + `?attr/a11yFocusStroke`.

## Volume / Zoom (MEDIA)

- [x] **MEDIA-01**: 음량 증감 → STREAM_MUSIC — `VolumeService.adjustStreamVolume(STREAM_MUSIC, ADJUST_RAISE/LOWER, FLAG_SHOW_UI)`. ChecklistFragment에 ▲▼ 버튼 노출.
- [x] **MEDIA-02**: 0.8x ~ 1.5x scaleX/Y — `ZoomService` STEP=0.1f + setter clamp + pivot 0,0.
- [x] **MEDIA-03**: 회전/테마/재시작 후 zoom 보존 — `A11yPrefs.zoomLevel` SharedPrefs + `MainActivity.onResume`에서 `ZoomService.apply` 재적용.

## Timeout / Response Time (TIME)

- [x] **TIME-01**: 자동 타임아웃 정책 명시 — 본 데모는 자동 타임아웃 없음. ChecklistFragment 7번째 행 + README "표준 준수"에 KS X 9211 / KWCAG 2.2.1 인용.

## Checklist (CHECK)

- [x] **CHECK-01**: 7기능 동작 상태 앱 내 시연 — `ChecklistFragment.renderRows()` 7행 동적 렌더 + `onResume` 재렌더로 토글 후 즉시 갱신.

## Documentation (DOC)

- [x] **DOC-01**: README 1스크롤 — 한 줄 설명 + 4-grid 스크린샷 + mp4 링크 + 7행 매핑 표 + WPF↔Android 표.
- [x] **DOC-02**: 표준 준수 4행 표 — KS X 9211 / KWCAG 2.2 / WCAG AA / 별표 5.
- [x] **DOC-03**: Scope 섹션 — 시스템 AccessibilityService 미등록 + 인앱 컨트롤러 채택 이유 명시.
- [x] **DOC-04**: 검증 환경 + AVD 가이드 — README "검증 환경" 섹션 + `docs/AVD-SETUP.md` 링크.
- [x] **DOC-05**: GitHub 공개 + description + topics — https://github.com/Taek-D/android-barrier-free-kiosk-demo Public + KO description + 7 topics(android/kotlin/accessibility/barrier-free/kiosk/wcag/kwcag).
- [x] **DOC-06**: 시연 자료 임베드 — `assets/01-home-light.png`, `02-home-hc.png`, `03-menu.png`, `04-checklist.png`, `demo.mp4`. _자막 합성 GIF는 사용자 후속 polish._
- [ ] **DOC-07**: Notion DB + 위시켓 재제출 — Release Checklist 8·9. ≤ 2026-05-15 사용자 환경 액션. **외부 시스템이라 자동화 불가, 산출물 자체는 모두 준비됨.**

## Build (BUILD)

- [x] **BUILD-01**: `./gradlew build` 통과 — JBR JDK 21 + AGP 8.7.3 + Gradle 8.11.1, BUILD SUCCESSFUL 1m10s 92 tasks. `app-debug.apk` 산출.
- [x] **BUILD-02**: AndroidX 5종만 — Material 미포함 grep 검증.
- [x] **BUILD-03**: minSdk 26, compileSdk/targetSdk 35, JDK 17 — `app/build.gradle.kts` 명시(빌드는 호환 JDK 21로 수행).

---

## v1 Outcome Summary

| Item | Count | Status |
|------|------:|--------|
| Total v1 requirements | 32 | — |
| Mapped to phases | 32 | ✅ 100% |
| Code/structural verified | 31 | ✅ |
| External-system-deferred (DOC-07) | 1 | ⏭ Release Checklist 8·9 (사용자 환경) |

**Validated decisions (PROJECT.md 이동):** XML Layout 채택, 외부 의존성 0, recreate 테마, TTS 시간+텍스트 debounce, 콘텐츠 영역 scaleX/Y, AVD D-pad 검증, F-12 SharedPreferences P0 승격, F-14 타임아웃 명시, 시스템 AccessibilityService 미등록.

**Carried to v2:** A11Y-01 ~ A11Y-06, VAR-01 ~ VAR-02. 자세한 v2 plan은 `.planning/PROJECT.md`의 "Next Milestone Goals" 참조.

---

*v1 requirements archived: 2026-05-07*
