# Android Barrier-Free Kiosk Demo (Kotlin)

> 위시켓 "키오스크 앱 베리어프리 기능 구현" 공고용 포트폴리오 데모. **외부 의존성 0 + AndroidX 5종만**으로 Android 네이티브 SDK의 접근성 4기능(고대비·TTS·키패드·Focus Indicator) + 음량/줌/체크리스트를 시연합니다.

[![Status](https://img.shields.io/badge/status-v1--ready-brightgreen)](.) [![SDK](https://img.shields.io/badge/minSdk-26-blue)](.) [![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF)](.) [![AGP](https://img.shields.io/badge/AGP-8.7.3-3DDC84)](.) [![License](https://img.shields.io/badge/license-MIT-lightgrey)](.)

```
[ GIF placeholder — 시연 GIF (자막 오버레이) 추가 위치
  AVD에서 ScreenToGif/OBS로 캡처 → assets/demo.gif → 아래 마크다운 src 교체 ]
```

<!-- 캡처 후 교체: ![Demo](assets/demo.gif) | [📺 MP4 풀버전](assets/demo.mp4) -->

## 공고 요구사항 ↔ 코드 위치 매핑

| # | 요구사항 | 코드/리소스 위치 | Phase |
|---|----------|------------------|------:|
| 1 | 모든 화면 하단 4버튼 컨트롤 바 | `app/src/main/res/layout/view_accessibility_bottom_bar.xml`, `ui/view/AccessibilityBottomBar.kt` | 1 |
| 2 | 음성 안내 (포커스 변경 시 발화) | `service/TtsService.kt`, `accessibility/A11yViewExt.kt` | 2 |
| 3 | 고대비 테마 (WCAG AA, 명도 21:1) | `service/ThemeService.kt`, `res/values/themes_high_contrast.xml` | 2 |
| 4 | 외부 키패드 방향키 포커스 이동 | `MainActivity.dispatchKeyEvent`, `accessibility/FocusNavigator.kt` | 3 |
| 5 | Focus Indicator (≥3dp stroke) | `res/drawable/focused_background.xml`, `res/values/themes*.xml` (`a11yFocusStroke`) | 3 |
| 6 | 음량 / 화면 확대·축소 | `service/VolumeService.kt`, `service/ZoomService.kt`, `ui/fragment/ChecklistFragment.kt` | 4 |
| 7 | 7기능 체크리스트 시연 + 시연 영상 | `ui/fragment/ChecklistFragment.kt`, `assets/demo.gif`(추가 예정) | 4·5 |

## WPF ↔ Android 이식 매핑

| WPF / C# | Android | 비고 |
|----------|---------|------|
| `System.Speech.Synthesis.SpeechSynthesizer` | `android.speech.tts.TextToSpeech` | onInit race + pending queue + Locale.KOREAN→US 폴백 |
| `FocusVisualStyle` | `state_focused` selector + `android:foreground` | API 23+ 권장, ripple 충돌 회피 |
| `KeyboardFocusBehavior` | `dispatchKeyEvent` + `focusSearch()` | 화이트리스트 + ACTION_DOWN 한정 |
| `Theme.Resources` swap | `recreate()` + `setTheme()` before `setContentView()` | savedInstanceState null 가드 |
| `AudioElement.Volume` | `AudioManager.adjustStreamVolume(STREAM_MUSIC, …)` | `FLAG_SHOW_UI` |
| `ScaleTransform` | `View.scaleX/scaleY` (직접 변경) | ScaleAnimation 휘발성 회피 |

## 표준 준수

| 표준 | 항목 | 본 데모 매핑 |
|------|------|--------------|
| **KS X 9211** (무인정보단말기 접근성) | 응답시간 / 버튼 간격 / 명도 대비 | TIME-01 (자동 타임아웃 없음), BAR-02 (56dp+ 터치), HC-02 |
| **KWCAG 2.2** | 5.1.5 키보드 접근, 5.4.7 포커스 표시, 6.1.x 명도 대비 | FOCUS-01·02·03, FOCUS-05, HC-02 |
| **WCAG AA** | 명도 대비 4.5:1 이상 | HC-02 (구현 21:1, 4.6배 여유) |
| **별표 5** (장차법 시행령) | 시각·지체장애 대체입력 | TTS-01 (음성 안내), FOCUS-01 (D-pad) |

## Scope (의도된 범위 한정)

- 본 데모는 **인앱 컨트롤러 패턴**만 구현하며, 시스템 차원 `AccessibilityService` 등록은 v2 범위입니다. 키오스크는 단일 앱 풀스크린 환경이라 인앱 컨트롤러로 7요구사항을 모두 충족합니다.
- 풀 키오스크 비즈니스 로직(결제·주문·메뉴 도메인)은 데모 범위 밖입니다.
- Jetpack Compose 변형, 시스템 Magnification API 전체 뷰포트, BLE 외부 키패드 페어링은 v2 로드맵입니다.
- 자세한 out-of-scope 목록은 [`.planning/REQUIREMENTS.md`](.planning/REQUIREMENTS.md) §"Out of Scope" 참조.

## 검증 환경

- **OS:** Windows 11 / macOS / Linux
- **JDK:** 17 (Temurin 권장)
- **Build:** AGP 8.7.3 + Gradle 8.11.1 + Kotlin 2.1.0
- **AVD:** Pixel 6 / API 35 / `hw.dPad=yes` — 셋업 가이드는 [`docs/AVD-SETUP.md`](docs/AVD-SETUP.md).
- **TTS:** Google 텍스트 음성 변환 엔진(한국어 사전 설치). 미설치 시 `Locale.US` 자동 폴백.
- **D-pad 입력:** Numpad ↑↓←→ + 5/Enter (Num Lock OFF) 또는 에뮬레이터 Extended Controls.

## 빌드 & 실행

처음 한 번만 (Wrapper jar는 텍스트 산출 한계로 저장소 미포함):

```bash
gradle wrapper --gradle-version 8.11.1
```

이후:

```bash
./gradlew assembleDebug      # APK 빌드
./gradlew installDebug       # AVD에 설치 (에뮬레이터 실행 중일 때)
```

또는 Android Studio에서 폴더 열기 → "Sync Project with Gradle Files" → Run.

## 의존성

```kotlin
implementation("androidx.appcompat:appcompat:1.7.1")
implementation("androidx.core:core-ktx:1.13.1")
implementation("androidx.activity:activity-ktx:1.9.3")
implementation("androidx.fragment:fragment-ktx:1.8.5")
implementation("androidx.constraintlayout:constraintlayout:2.2.1")
```

`com.google.android.material` / Jetpack Compose / Hilt / Room / Navigation Component / RxJava / coroutines-android — **모두 미포함**. 클라이언트 기존 코드베이스(WPF 이식)에 의존성 오염 없이 이식 가능함을 어필합니다.

## 핵심 구현 메모 (평가자 30초 검토용)

- **TTS init race 가드:** `@Volatile var ready` + `pending` 큐 + `onInit(SUCCESS)`에서 flush. 첫 발화 묵음 회피.
- **테마 전환 좀비 방지:** `object TtsService` 싱글턴 + `applicationContext`만 사용. recreate되어도 단일 인스턴스 유지.
- **dispatchKeyEvent 화이트리스트:** `DPAD_*`/`ENTER` + `ACTION_DOWN` 한정. BACK/EditText/VOLUME 등 비방향키는 `super` 위임 → 시스템 동작 보존.
- **focus indicator 충돌 회피:** `android:foreground` (API 23+)에 `state_focused` selector 적용. `?selectableItemBackground`(ripple)와 layer 분리.
- **고대비 영속:** `setTheme()` BEFORE `setContentView()`. `SharedPreferences`로 토글 영속 → 재시작/recreate 후 복원.
- **줌 영속:** `View.scaleX/scaleY` 직접 변경 (ScaleAnimation 휘발성 회피) + SharedPreferences read-on-resume.

## 프로젝트 구조 요약

```
app/src/main/
├── java/com/example/a11ydemo/
│   ├── MainActivity.kt                  # theme owner + dispatchKeyEvent + bar 와이어링
│   ├── prefs/A11yPrefs.kt               # SharedPreferences 영속화
│   ├── service/{Tts,Theme,Volume,Zoom}Service.kt
│   ├── accessibility/{FocusNavigator,A11yViewExt}.kt
│   └── ui/
│       ├── fragment/{Home,Menu,Checklist}Fragment.kt
│       └── view/AccessibilityBottomBar.kt
└── res/
    ├── layout/   activity_main, view_accessibility_bottom_bar, fragment_*, item_checklist_row
    ├── drawable/ focused_background, ic_a11y_*, ic_launcher_*
    └── values/   strings, dimens, colors, attrs, ids, themes (+ themes_high_contrast)
```

## GitHub 저장소 메타 (push 시 반영)

- **저장소명:** `android-barrier-free-kiosk-demo`
- **공개 범위:** Public
- **Description (KO):** 위시켓 키오스크 베리어프리 공고용 Android 네이티브 데모. 외부 의존성 0, AndroidX 5종만으로 구현한 접근성 4기능 + 음량/줌/체크리스트.
- **Topics:** `android`, `kotlin`, `accessibility`, `barrier-free`, `kiosk`, `wcag`, `kwcag`

## Release Checklist (≤ 2026-05-15)

- [ ] `gradle wrapper --gradle-version 8.11.1` 1회 실행 (저장소에 `gradle-wrapper.jar` 추가).
- [ ] `./gradlew build` exit 0 확인.
- [ ] AVD `a11y_demo_dpad_api35` 부팅 + D-pad 이동 + TTS 발화 + HC 토글 + 줌 영속 1회 검증.
- [ ] AVD에서 자막 오버레이 GIF + MP4 캡처 → `assets/demo.gif`, `assets/demo.mp4` → 위 placeholder 교체.
- [ ] 라이트/HC 좌우 비교 스크린샷(3dp stroke 강조 화살표) → `assets/focus-comparison.png`.
- [ ] GitHub 새 저장소 (`android-barrier-free-kiosk-demo`, public) 생성 + description/topics 설정.
- [ ] `git remote add origin <url> && git push -u origin master`.
- [ ] Notion 프로젝트 DB에 페이지 작성 (저장소 링크 + 한 줄 설명 + 매핑 표 복사).
- [ ] 위시켓 공고에 지원서 재제출 (저장소 + Notion 링크 첨부).

## 라이선스

MIT License. © 2026 — 위시켓 베리어프리 공고 포트폴리오. 데모 목적 공개.

---

상세 진행 상태: [`.planning/STATE.md`](.planning/STATE.md). 요구사항: [`.planning/REQUIREMENTS.md`](.planning/REQUIREMENTS.md). Phase 산출물: `.planning/phases/01-foundation` ~ `05-ship`.
