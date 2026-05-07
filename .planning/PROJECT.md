# Android Kotlin 베리어프리 접근성 데모 앱

## What This Is

위시켓 "키오스크 앱 베리어프리 기능 구현" 공고(예산 1,000만 원, 마감 2026-05-20)의 1차 평가자가 GitHub README 30초 스캔으로 Android(Kotlin) Accessibility Service 구현 역량을 확신하게 만드는 포트폴리오 데모 앱이다. 기존 WPF/C# 접근성 데모에서 검증된 4개 패턴(TTS·고대비·키패드·Focus Indicator)을 외부 라이브러리 없이 Android 네이티브 SDK만으로 이식하여, 공고 요구사항 7항목과 1:1 매핑되는 공개 저장소를 6일 안에 만든다.

## Core Value

평가자가 GitHub README와 시연 GIF를 90초 안에 훑고 "이 사람 Android Accessibility 진짜 만들 줄 안다"고 결론짓게 만드는 것. 다른 모든 가치보다 이 인지 전환이 우선한다.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] 모든 화면 하단에 항상 노출되는 `AccessibilityBottomBar` (4버튼: 고대비·TTS·확대·축소, 각 56dp+, contentDescription 설정)
- [ ] 포커스 변경 시 음성 안내 (`TtsService.speakIfChanged`, 동일 텍스트 500ms 내 중복 억제, On/Off 토글)
- [ ] 고대비 테마 전환 (`themes_high_contrast.xml` + `ThemeService.toggle()` + `recreate()`, WCAG AA 4.5:1 이상)
- [ ] 외부 키패드 방향키로 포커스 이동 (`dispatchKeyEvent` override + `focusSearch()`, DPAD UP/DOWN/LEFT/RIGHT/CENTER + ENTER)
- [ ] Focus Indicator drawable selector (`state_focused` 시 3dp+ stroke, 모든 포커서블 뷰에 적용)
- [ ] README 1:1 매핑 (공고 요구사항 ↔ 코드 위치 표 + 기능별 스크린샷/GIF ≥ 1개)
- [ ] AudioManager STREAM_MUSIC 음량 증감 버튼
- [ ] 콘텐츠 영역 ScaleAnimation 확대/축소
- [ ] ChecklistFragment에서 7기능 동작 상태를 앱 내 시연
- [ ] GitHub 공개 push + Notion 프로젝트 DB 업로드 + 위시켓 지원서 재제출 (≤ 2026-05-15)

### Out of Scope

- 풀 키오스크 비즈니스 로직(결제·주문·메뉴) — 데모 목적이 접근성 4기능 시연이라 무관, 일정 압박
- Jetpack Compose UI — 클라이언트 기존 코드베이스가 View/XML 기반일 가능성이 높아 호환성 손실 리스크
- 전체 뷰포트 zoom (Magnification API) — Phase 2로 분리, MVP에서는 콘텐츠 영역 ScaleAnimation으로 갈음
- BLE/외부 키패드 페어링 — 시스템 차원 통합은 데모 범위 밖
- AccessibilityService 시스템 등록 — 인앱 컨트롤러만으로 공고 요구사항 충족 가능, Phase 2
- 유닛테스트 커버리지 목표 — 6일 안에 시연 영상이 평가 가치가 더 큼
- Material Dialog/Toast 고대비 일관성 — Nice-to-have, P2

## Context

- **포트폴리오 갭**: 기존 WPF/C# 데모는 접근성 설계 철학을 증명하지만 Android Kotlin 네이티브 코드 증거가 없어 공고 1차 필터에서 탈락 위험.
- **WPF → Android 이식 매핑**: System.Speech.Synthesis ↔ TextToSpeech, FocusVisualStyle ↔ state_focused selector, KeyboardFocusBehavior ↔ dispatchKeyEvent + focusSearch 등 API 구조 유사 → 이식 난이도 ⭐~⭐⭐⭐.
- **외부 의존성 0**: 공고 클라이언트 기존 코드베이스에 의존성 추가 없이 이식 가능함을 어필하기 위한 의도적 제약.
- **검증 환경 한계**: 실 디바이스 미보유 가능 → README에 "Android Studio Emulator D-pad 기준 검증" 명시 필요.
- **재활용 자산**: 이번 데모는 향후 키오스크/접근성 도메인 다른 발주처에도 재활용되는 포트폴리오 자산.

## Constraints

- **Timeline**: 2026-05-15까지 GitHub push + 지원서 재제출 — 공고 마감 5일 전 버퍼 확보
- **Tech stack**: Kotlin + AndroidX View/XML — Compose 금지, 외부 라이브러리 0개(AndroidX 제외)
- **Min SDK**: API 26 (Android 8.0) — 공공 키오스크 타깃 기준
- **UI framework**: XML Layout + Fragment — Compose는 클라이언트 코드베이스 호환성 리스크
- **테스트**: 수동 시연 영상 우선, 유닛테스트는 시간 허용 시에만
- **Solo project**: 단독 1인 6일 작업, cross-team 의존성 없음

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| XML Layout (View) 채택, Compose 배제 | 클라이언트 기존 코드베이스 호환성 우선 | — Pending |
| 외부 라이브러리 0개 (AndroidX만) | 의존성 오염 없는 이식성 어필 | — Pending |
| 테마 전환에 `recreate()` 사용 | Android 표준 패턴, savedInstanceState로 상태 보존 | — Pending |
| TTS 중복 억제 = 시간(500ms)+텍스트 비교 | WPF SpeakOnFocusBehavior와 동일 원리 재사용 | — Pending |
| 화면 확대/축소 = 콘텐츠 영역 ScaleAnimation (전체 뷰포트는 Phase 2) | 공고 요구 모호성 + 6일 일정 압박 | — Pending |
| 검증 환경 = Android Studio Emulator D-pad | 실 디바이스 미보유 가능성, README 명시 | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-05-07 after initialization*
