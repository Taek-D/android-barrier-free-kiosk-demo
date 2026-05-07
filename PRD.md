# Product Requirements Document: Android Kotlin 베리어프리 접근성 데모 앱

**Author**: castletaek9643@gmail.com
**Date**: 2026-05-07
**Status**: Draft
**Stakeholders**: 본인 (지원자), 위시켓 공고 클라이언트 (잠재 평가자)
**Source**: `android_barrier_free_demo_plan.md`

---

## 1. Executive Summary

위시켓 "키오스크 앱 베리어프리 기능 구현" 공고(예산 1,000만 원, 2026-05-20 마감)의 지원 자격인 **Android(Kotlin) Accessibility Service 경험**을 증명할 포트폴리오 데모 앱을 6일 안에 구축한다. 기존 WPF/C# 접근성 데모에서 검증된 4개 핵심 패턴(TTS·고대비·키패드·Focus Indicator)을 Android Kotlin 네이티브 SDK만으로 이식하여, 공고 요구사항 7항목과 1:1 매핑되는 GitHub 공개 저장소를 만든다.

---

## 2. Background & Context

### 문제 정의
- **포트폴리오 갭**: 현재 보유한 ♿ 배리어프리 키오스크 데모는 WPF/C#로 작성되어 접근성 설계 철학은 증명하지만 Android Kotlin 네이티브 코드 증거가 없다.
- **공고 매칭 실패 리스크**: 공고가 명시적으로 "Android(Kotlin) 커스텀 UI 및 Accessibility Service 제어 경험이 풍부한 분"을 요구하므로, 플랫폼 일치 증거 없이 지원 시 1차 필터에서 탈락할 가능성이 높다.
- **시간 압박**: 공고 마감 2026-05-20, 목표 완료 2026-05-15 (마감 5일 전 버퍼 확보).

### 기회
- WPF 데모의 핵심 설계 결정(테마/레이아웃 분리, TTS 중복 억제, FocusVisualStyle)이 Android API 구조와 상당히 유사하여 이식 난이도 ⭐~⭐⭐⭐ 수준.
- 외부 라이브러리 없이 네이티브 SDK만으로 구현 가능 → 공고 클라이언트의 기존 코드베이스에 의존성 추가 없이 이식 가능함을 어필 포인트로 활용.

### Prior Art
- 자체 WPF 데모: 동일 접근성 4항목을 다른 플랫폼에서 이미 동작 검증 완료. 본 프로젝트는 그 이식판이며, "동일 원리·다른 플랫폼"이라는 강력한 비교 자산이 됨.

---

## 3. Objectives & Success Metrics

### Goals
1. 공고 기능 요구사항 2-1~2-3 (하단 접근성 바, TTS, 고대비, 확대/축소, 음량, 키패드 포커스 이동, Focus Indicator) **7개 항목 모두 동작**하는 데모 앱을 GitHub에 공개한다.
2. README 체크리스트로 공고 요구사항 ↔ 구현 코드를 **1:1 매핑**하여 평가자가 30초 안에 검증 가능하게 만든다.
3. **외부 라이브러리 0개** — 네이티브 Android SDK만으로 구현해 이식성을 증명한다.
4. **2026-05-15까지** 코드 push + Notion 업로드 + 위시켓 지원서 재제출 완료.

### Non-Goals
1. **풀 키오스크 비즈니스 로직 구현 금지** — 결제·주문·메뉴 도메인은 데모 범위 밖. 접근성 4기능 시연에만 집중.
2. **Compose 사용 금지** — 공고 클라이언트의 기존 코드베이스가 View/XML 기반일 가능성이 높아 Compose는 오히려 마이너스. XML Layout으로 통일.
3. **Phase 2 기능 분리** — 전체 뷰포트 zoom, BLE/외부 키패드 페어링, AccessibilityService 시스템 등록 등은 본 데모 범위 밖.
4. **유닛테스트 커버리지 목표 설정 금지** — 6일 일정 안에 수동 시연 영상이 더 큰 평가 가치를 가진다.

### Success Metrics
| Metric | Current | Target | Measurement |
|---|---|---|---|
| Android Kotlin 포트폴리오 보유 | 0건 | 1건 (GitHub public) | 저장소 URL |
| 공고 요구사항 매핑률 | 0/7 | 7/7 | README 체크박스 |
| 외부 의존성 수 | N/A | 0개 (AndroidX 제외) | `app/build.gradle.kts` |
| 시연 자료 | 없음 | 기능별 스크린샷 또는 GIF ≥ 1개 | README 임베드 |
| 빌드 상태 | N/A | `./gradlew build` 성공 | CI 또는 로컬 |
| 위시켓 재지원 제출일 | N/A | ≤ 2026-05-15 | 지원 기록 |

---

## 4. Target Users & Segments

### Primary
- **위시켓 공고 클라이언트** (1차 평가자): GitHub README와 코드를 빠르게 훑고 합격/탈락을 결정. 영어 또는 한국어 30초 스캔에서 결론을 낸다.
- **위시켓 매니저** (전형 코디네이터): 지원자 자격 사전 필터링.

### Secondary
- **향후 유사 공고 평가자**: 키오스크/접근성 도메인 다른 발주처. 본 데모는 재활용 자산이 된다.
- **자기 자신**: 다음 Android 접근성 작업 시 참조할 살아있는 코드 스니펫 모음.

### What "Good" Looks Like
- 클라이언트가 README를 열고 → 체크리스트에서 7/7 매핑 확인 → GIF 1~2개로 동작 확인 → "이 사람 진짜 만들 줄 안다" 결론까지 90초 이내.

---

## 5. User Stories & Requirements

### P0 — Must Have (공고 요구사항 1:1)
| # | User Story | Acceptance Criteria |
|---|---|---|
| P0-1 | 평가자로서, 모든 화면 하단에 접근성 컨트롤 바가 항상 보여서 기능 진입점을 즉시 찾고 싶다 | `AccessibilityBottomBar` 4버튼(고대비·TTS·확대·축소) 노출, 각 56dp+, contentDescription 설정 |
| P0-2 | 시각 장애 사용자로서, 포커스가 이동할 때마다 음성 안내를 듣고 싶다 | `TtsService.speakIfChanged` 호출, 동일 텍스트 500ms 내 재발화 억제, On/Off 토글 동작 |
| P0-3 | 저시력 사용자로서, 고대비 테마로 전환하여 가독성을 높이고 싶다 | `themes_high_contrast.xml` 적용, `recreate()` 후 전체 화면 반영, WCAG AA 4.5:1 이상 |
| P0-4 | 운동 제약 사용자로서, 외부 키패드 방향키로 포커스를 이동하고 싶다 | `dispatchKeyEvent` override, DPAD UP/DOWN/LEFT/RIGHT/CENTER + ENTER 처리, `focusSearch()` 동작 |
| P0-5 | 모든 사용자로서, 현재 포커스가 어디 있는지 시각적으로 명확히 알고 싶다 | `focused_background.xml` selector, `state_focused` 시 3dp+ stroke |
| P0-6 | 평가자로서, GitHub README만으로 공고 요구사항이 어떻게 충족되는지 즉시 확인하고 싶다 | 공고 항목 ↔ 코드 위치 매핑 표 + 기능별 스크린샷/GIF 임베드 |

### P1 — Should Have
| # | User Story | Acceptance Criteria |
|---|---|---|
| P1-1 | 청각 보조 사용자로서, 시스템 음량을 빠르게 조절하고 싶다 | `VolumeService` (`AudioManager.STREAM_MUSIC`) 증감 버튼 동작 |
| P1-2 | 저시력 사용자로서, 콘텐츠 영역을 확대/축소하고 싶다 | 특정 콘텐츠 영역 `ScaleAnimation`으로 확대/축소 (전체 뷰포트는 P2) |
| P1-3 | 평가자로서, 기능별 체크리스트 화면을 앱 내에서 직접 보고 싶다 | `ChecklistFragment`에서 구현 항목과 동작 상태를 시연 |

### P2 — Nice to Have / Future
| # | User Story | Acceptance Criteria |
|---|---|---|
| P2-1 | 사용자가 앱을 재시작해도 고대비/TTS 설정이 유지된다 | `SharedPreferences`에 모드 상태 영속화 |
| P2-2 | 전체 뷰포트 zoom (Pinch + Magnification API) | Phase 2로 분리 — 본 MVP 범위 밖 |
| P2-3 | AccessibilityService 시스템 등록 | Phase 2 — 본 데모는 인앱 컨트롤만 시연 |
| P2-4 | Material Dialog/Toast 고대비 테마 일관성 | `MaterialAlertDialogBuilder` 커스텀 적용 |

---

## 6. Solution Overview

### 아키텍처
```
app/
├── ui/         (MainActivity, HomeFragment, MenuFragment, ChecklistFragment, AccessibilityBottomBar)
├── service/    (TtsService, ThemeService, VolumeService)
├── accessibility/ (FocusNavigator)
└── res/        (themes.xml, themes_high_contrast.xml, focused_background.xml)
```

### 핵심 설계 결정
| 결정 | 선택 | 이유 |
|---|---|---|
| 언어 | Kotlin | 공고 직접 요구 |
| UI | XML Layout (View) | 기존 코드베이스 호환성 우선, Compose 배제 |
| 테마 전환 | `setTheme` + `recreate()` | Android 표준 패턴, savedInstanceState로 상태 보존 |
| TTS 중복 억제 | 시간(500ms)+텍스트 비교 | WPF SpeakOnFocusBehavior와 동일 원리 |
| 포커스 이동 | `dispatchKeyEvent` + `focusSearch()` | XML `nextFocus*` 미설정 시 자동 폴백 |
| 의존성 | AndroidX만 | 클라이언트 코드베이스 오염 방지 |
| 최소 SDK | API 26 (Android 8.0) | 공공 키오스크 타깃 기준 |

### WPF → Android 이식 매핑
포트폴리오 어필 핵심 자산. README와 지원서에 동일 표를 노출하여 "단순 API 따라하기가 아닌 아키텍처 이해" 증명.

---

## 7. Open Questions
| Question | Owner | Deadline |
|---|---|---|
| `recreate()` 호출 시 Fragment back stack 처리 — savedInstanceState vs SharedPreferences 중 우선 채택? | 본인 | Day 3 시작 전 |
| 시연 영상 형식 — GIF (가벼움) vs MP4 (품질) 중 README 임베드 표준? | 본인 | Day 6 |
| 실 디바이스 테스트 가능 여부 — 미보유 시 README에 "Android Studio Emulator D-pad 기준 검증" 명시 필요 | 본인 | Day 4 |
| 화면 확대/축소 범위 — 공고가 "특정 영역"인지 "전체 뷰포트"인지 발주처 확인 가능? | 본인 (지원 후 질의) | 위시켓 메시지로 별도 |
| GitHub 저장소명 확정 — `android-barrier-free-demo` 외 후보? | 본인 | Day 1 |

---

## 8. Timeline & Phasing

### Phase 1 — MVP (6일)
| Day | 산출물 | DoD |
|---|---|---|
| Day 1 (05-08) | 프로젝트 셋업, MainActivity + 3 Fragment 뼈대, AccessibilityBottomBar 레이아웃 | 빈 앱 빌드 성공 |
| Day 2 (05-09) | TtsService + 모든 contentDescription + 포커스 TTS 연결 | 포커스 이동 시 음성 출력 확인 |
| Day 3 (05-10) | 고대비 테마 2종 + ThemeService + recreate() | 토글 시 전체 화면 테마 전환 |
| Day 4 (05-11) | FocusNavigator + dispatchKeyEvent + focused_background drawable | 키보드 방향키로 포커스 이동 |
| Day 5 (05-12) | VolumeService + ScaleAnimation + ChecklistFragment | 7기능 모두 동작 |
| Day 6 (05-13) | README + 스크린샷/GIF + GitHub push + Notion 업로드 + 지원서 재작성 | 공개 URL + 지원 제출 |

**버퍼**: Day 7~8 (05-14~05-15) — 에뮬레이터 키이벤트 트러블슈팅, 시연 영상 재촬영, README 다듬기.

### Phase 2 — Post-Launch (선택)
- AccessibilityService 시스템 통합
- 전체 뷰포트 Magnification API
- Compose 버전 추가 변형 (선택 어필 자산)

### Cross-team Dependencies
- 없음. 단독 1인 프로젝트.

### Risks (요약)
| 리스크 | 대응 |
|---|---|
| `recreate()` 시 Fragment 상태 유실 | savedInstanceState + SharedPreferences |
| 에뮬레이터 D-pad 시뮬레이션 한계 | README에 검증 환경 명시 |
| TTS 초기화 비동기 딜레이 | `isInitialized` 가드 |
| 일정 슬립 (Day 4 키이벤트 디버깅) | Day 7~8 버퍼 활용, 최악의 경우 P1-1/P1-2 일시 강등 |

---

## Next Actions

다음 중 원하는 후속 작업을 알려주시면 진행하겠습니다:

- **Pre-mortem**: 이 PRD의 실패 시나리오를 사전 점검 (`/pm-execution:pre-mortem`)
- **User stories 분해**: P0 항목들을 엔지니어링 티켓으로 변환 (`/pm-execution:write-stories`)
- **Sprint plan**: Day 1~6 일정을 더 세부적인 태스크로 (`/pm-execution:sprint`)
- **Stakeholder update**: 지원서 첨부용 1페이지 요약본
- **README 초안**: 평가자 30초 스캔 최적화 버전
