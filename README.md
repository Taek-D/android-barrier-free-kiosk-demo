# Android Barrier-Free Kiosk Demo (Kotlin)

> 위시켓 키오스크 베리어프리 공고용 포트폴리오 데모. **외부 의존성 0 + AndroidX 5종만**으로 Android 네이티브 SDK의 접근성 4기능(고대비·TTS·키패드·Focus Indicator)을 시연한다.

[![Status](https://img.shields.io/badge/status-WIP--Day1-orange)](.) [![SDK](https://img.shields.io/badge/minSdk-26-blue)](.) [![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF)](.) [![AGP](https://img.shields.io/badge/AGP-8.7.3-3DDC84)](.)

> **Phase 진행:** Phase 1 (Foundation) — 골격/영속화/AVD 가이드 완료. Phase 2~5에서 TTS, 고대비, 포커스, 미디어, 출시 마감을 채운다.

## TL;DR (Phase 5에서 본문 채움)

> 한 줄 설명 + 자막 GIF + 7행 매핑 표 + WPF↔Android 표가 들어갈 자리.

```
[ GIF placeholder — Phase 5에서 자막 오버레이 GIF 임베드 ]
```

### 공고 요구사항 ↔ 코드 위치 매핑 (stub)

| # | 공고 요구사항 | 코드 위치 | Phase |
|---|---------------|-----------|-------|
| 1 | 모든 화면 하단 4버튼 컨트롤 바 | `app/src/main/res/layout/view_accessibility_bottom_bar.xml`, `ui/view/AccessibilityBottomBar.kt` | 1 |
| 2 | 음성 안내 (TTS, 포커스 변경 시 발화) | `service/TtsService.kt` | 2 |
| 3 | 고대비 테마 (recreate + WCAG AA) | `service/ThemeService.kt`, `res/values/themes_high_contrast.xml` | 2 |
| 4 | 외부 키패드 방향키 포커스 이동 | `MainActivity.dispatchKeyEvent`, `accessibility/FocusNavigator.kt` | 3 |
| 5 | Focus Indicator (≥3dp stroke) | `res/drawable/focused_background.xml` | 3 |
| 6 | 음량/확대/축소 미디어 컨트롤 | `service/VolumeService.kt`, `service/ZoomService.kt` | 4 |
| 7 | 시연 GIF + Notion + 위시켓 재제출 | (Phase 5 산출물) | 5 |

### WPF ↔ Android 이식 매핑 (stub)

| WPF/C# | Android | 비고 |
|--------|---------|------|
| `System.Speech.Synthesis.SpeechSynthesizer` | `android.speech.tts.TextToSpeech` | init race + queue 가드 |
| `FocusVisualStyle` | `state_focused` selector + `android:foreground` | API 23+ 권장 |
| `KeyboardFocusBehavior` | `dispatchKeyEvent` + `focusSearch()` | 화이트리스트 처리 |
| `Theme.Resources` swap | `recreate()` + `setTheme()` before `setContentView()` | savedInstanceState 가드 |

## 표준 준수 (Phase 5에서 인용 채움)

| 표준 | 적용 항목 | 본 데모 매핑 |
|------|-----------|--------------|
| KS X 9211 | 무인정보단말기 접근성 (응답시간/버튼 간격/대비) | BAR-02, TIME-01, HC-02 |
| KWCAG 2.2 | 5.1.5 키보드 접근, 5.4.7 포커스 표시, 6.1.x 명도 대비 | FOCUS-01, FOCUS-05, HC-02 |
| WCAG AA | 명도 대비 4.5:1 이상 | HC-02 (구현 21:1) |
| 별표 5 | 시각/지체장애 대체입력 | TTS-01, FOCUS-01 |

## Scope (의도된 범위 한정)

- 본 데모는 **인앱 컨트롤러 패턴**만 구현하며, 시스템 차원 `AccessibilityService` 등록은 v2 범위입니다.
- 풀 키오스크 비즈니스 로직(결제·주문·메뉴 도메인)은 데모 범위 밖입니다.
- Jetpack Compose 변형은 v2 (클라이언트 코드베이스 호환성 우선).
- 자세한 out-of-scope 목록은 `.planning/REQUIREMENTS.md` §"Out of Scope" 참조.

## 검증 환경

- **OS:** Windows 11 / macOS / Linux 어디서나
- **JDK:** 17 (Temurin 권장)
- **Build:** AGP 8.7.3 + Gradle 8.11.1 + Kotlin 2.1.0
- **AVD:** Pixel 6 / API 35 / `hw.dPad=yes` — 셋업 가이드는 [`docs/AVD-SETUP.md`](docs/AVD-SETUP.md).
- **TTS:** Google 텍스트 음성 변환 엔진(한국어 사전 설치). 미설치 시 `Locale.US` 자동 폴백.
- **D-pad 입력:** Numpad ↑↓←→/Enter (Num Lock OFF) 또는 에뮬레이터 Extended Controls.
- **Day 1 수동 확인:** `[ ]` D-pad 포커스 이동 동작 확인 — _완료 시 날짜 마킹_.

## 빌드 & 실행

처음 한 번만:

```bash
# Gradle Wrapper jar 생성 (텍스트 산출 한계로 저장소에 jar 미포함)
gradle wrapper --gradle-version 8.11.1
```

이후:

```bash
./gradlew assembleDebug      # APK 빌드
./gradlew installDebug       # AVD에 설치 (에뮬레이터 실행 중일 때)
```

또는 Android Studio에서 폴더 열기 → "Sync Project with Gradle Files" → Run.

## 의존성 (외부 0, AndroidX 5종)

```kotlin
implementation("androidx.appcompat:appcompat:1.7.1")
implementation("androidx.core:core-ktx:1.13.1")
implementation("androidx.activity:activity-ktx:1.9.3")
implementation("androidx.fragment:fragment-ktx:1.8.5")
implementation("androidx.constraintlayout:constraintlayout:2.2.1")
```

`com.google.android.material` / Compose / Hilt / Room / Navigation Component / RxJava / coroutines-android **모두 미포함**.

## 라이선스 / 저작권

© 2026 — 위시켓 베리어프리 공고 포트폴리오. 데모 목적 공개.

---

_Phase 진행 상태는 `.planning/STATE.md`, 체계적인 요구사항/로드맵은 `.planning/REQUIREMENTS.md`, `.planning/ROADMAP.md` 참조._
