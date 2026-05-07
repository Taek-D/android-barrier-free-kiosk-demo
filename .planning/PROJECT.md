# Android Kotlin 베리어프리 접근성 데모 앱

## Current State

**Shipped:** v1 (2026-05-07) — `.planning/milestones/v1-ROADMAP.md` · [GitHub](https://github.com/Taek-D/android-barrier-free-kiosk-demo)

5 phases / 32 requirements / 7 atomic commits / 1일 마감. README 1스크롤에 매핑 표 + WPF↔Android 표 + 표준 4행 + Scope + 시연 자료(스크린샷 4장 + mp4)가 모두 출하됐고, BUILD/AVD 자동 검증까지 통과. 외부 시스템 액션(Notion 업로드 / 위시켓 재제출)만 사용자 환경 후속 처리로 남아 있다.

## What This Is

위시켓 "키오스크 앱 베리어프리 기능 구현" 공고(예산 1,000만 원, 마감 2026-05-20)의 1차 평가자가 GitHub README 30초 스캔으로 Android(Kotlin) Accessibility Service 구현 역량을 확신하게 만드는 포트폴리오 데모 앱이다. 기존 WPF/C# 접근성 데모에서 검증된 4개 패턴(TTS·고대비·키패드·Focus Indicator)을 외부 라이브러리 없이 Android 네이티브 SDK만으로 이식하여, 공고 요구사항 7항목과 1:1 매핑되는 공개 저장소를 만들었다.

## Core Value

평가자가 GitHub README와 시연 GIF를 90초 안에 훑고 "이 사람 Android Accessibility 진짜 만들 줄 안다"고 결론짓게 만드는 것. 다른 모든 가치보다 이 인지 전환이 우선한다.

## Validated (v1)

- **외부 의존성 0 + AndroidX 5종만** narrative — 실제로 `com.google.android.material` 미포함 빌드 통과, `appcompat 1.7.1` / `core-ktx 1.13.1` / `activity-ktx 1.9.3` / `fragment-ktx 1.8.5` / `constraintlayout 2.2.1` 5종으로 7기능 + 음량 + 줌 + 체크리스트 모두 구현.
- **XML Layout (View/Fragment) 채택, Compose 배제** — 클라이언트 코드베이스 호환성 narrative 그대로 출하.
- **테마 전환 = `recreate()` + `setTheme()` BEFORE `setContentView()`** — Phase 2 ThemeService에서 동작 검증.
- **TTS 중복 억제 = 시간(500ms) + 텍스트 비교 + `QUEUE_FLUSH`** — TtsService.speak.
- **콘텐츠 영역 scaleX/Y 영속 zoom** — ScaleAnimation 휘발성 회피, SharedPreferences로 재시작 후에도 유지.
- **검증 환경 = Android Studio Emulator + `hw.dPad=yes` AVD + Numpad 매핑** — `Medium_Phone_API_36.1`로 1회 자동 검증 완료.
- **F-12 SharedPreferences 영속화 P0 승격** — A11yPrefs 단일 파사드.
- **F-14 타임아웃 정책 명시** — 자동 타임아웃 두지 않음, ChecklistFragment + README "표준 준수"에 KS X 9211 / KWCAG 2.2.1 인용.
- **시스템 `AccessibilityService` 미등록 + 인앱 컨트롤러 패턴** — README "Scope" 섹션으로 의도된 범위 한정 명시.

## Out of Scope (carried)

- 풀 키오스크 비즈니스 로직(결제·주문·메뉴) — 데모 범위 외, 일정 압박.
- 점자 키패드·휠체어 클리어런스 등 KS X 9211 HW 항목 — Android SW 데모 범위 밖.
- 음성 명령(STT) — 별표 5 표준 외, 도메인 로직.
- 다크모드(시스템 night mode) — 고대비와 별도 개념, 혼동 회피.
- 유닛테스트 커버리지 목표 — 6일 일정 안에 시연 영상이 더 큰 평가 가치.

## Next Milestone Goals (v2 후보)

발주처 응답 + 1차 평가자 피드백 수신 후 우선순위 확정. 현재 후보 (PRIORITY TBD):

| ID | Goal | Trigger |
|----|------|---------|
| A11Y-01 | 시스템 `AccessibilityService` 등록 + 시스템 차원 통합 | 발주처가 strict 시스템 서비스 요구하는 경우 |
| A11Y-02 | Magnification API 전체 뷰포트 | 발주처가 콘텐츠 영역 vs 전체 뷰포트 명확화 시 |
| A11Y-04 | 청각장애 자막 / 시각 큐 | 별표 5 청각장애 커버리지 확장 |
| A11Y-05 | 모션 감소 모드 (KWCAG 2.3.3) | 평가자 fine-grain 피드백 |
| VAR-01 | Compose 버전 변형 | 다른 클라이언트 어필 자산 |
| VAR-02 | 다국어 i18n (영어/일본어 등) | 글로벌 키오스크 도메인 |

## Open Decisions (Pending — 발주처 응답)

1. "Accessibility Service 제어 경험" — 시스템 서비스 strict 여부. v1은 README "Scope" 헤지로 보험.
2. 화면 확대/축소 — 콘텐츠 영역 vs 전체 뷰포트. v1은 콘텐츠 영역 결정.

## Hand-off Notes (v2 시작 시)

- v1 archive: `.planning/milestones/v1-ROADMAP.md` + `v1-REQUIREMENTS.md` + 5 phase artifacts (`.planning/phases/01..05`).
- 빌드 환경: JBR 21 / AGP 8.7.3 / Gradle 8.11.1. JDK 25는 비호환 — `JAVA_HOME`을 JBR 또는 Temurin 17~21로 명시.
- 한글 경로 가드: `gradle.properties#android.overridePathCheck=true`. 클라이언트 환경 ASCII 경로면 제거.
- `local.properties` 환경별, `.gitignore` 처리됨.
- 시작 명령: `/gsd-new-milestone v2`.

---

_v1 closed: 2026-05-07. Last updated after `/gsd-complete-milestone v1`._
