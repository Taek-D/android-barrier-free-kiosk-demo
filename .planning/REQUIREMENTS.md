# Requirements: Android Kotlin 베리어프리 접근성 데모 앱

**Defined:** 2026-05-07
**Core Value:** 평가자가 GitHub README와 시연 GIF를 90초 안에 훑고 "이 사람 Android Accessibility 진짜 만들 줄 안다"고 결론짓게 한다.
**Source:** `PRD.md` (P0/P1/P2) + research SUMMARY 권장사항 적용 (F-12 P0 승격, F-6 명시화, F-14 신규 P1)

---

## v1 Requirements

위시켓 키오스크 접근성 공고 요구사항 7항목 + 포트폴리오 어필 기능. 모두 2026-05-15까지 GitHub 공개 + 위시켓 재제출.

### Bottom Bar (BAR)

- [ ] **BAR-01**: 사용자가 모든 Fragment 화면에서 화면 하단의 4버튼 접근성 컨트롤 바(고대비·TTS·확대·축소)를 항상 볼 수 있다
- [ ] **BAR-02**: 각 버튼은 56dp×56dp 이상의 터치 타깃을 가진다 (KS X 9211 버튼 간격)
- [ ] **BAR-03**: 모든 인터랙티브 뷰(바 버튼·콘텐츠 버튼·메뉴 항목)에 strings.xml 기반 `contentDescription` 또는 `android:text`가 설정되어 있어 grep 검증이 가능하다

### TTS (TTS)

- [ ] **TTS-01**: 사용자가 포커스를 이동하면 해당 뷰의 라벨이 음성으로 안내된다 (`OnFocusChangeListener` 기반)
- [ ] **TTS-02**: 동일 텍스트가 500ms 이내에 재포커스되면 음성이 중복 발화되지 않는다 (`speakIfChanged` debounce)
- [ ] **TTS-03**: 사용자가 하단 바의 TTS 버튼을 탭하면 음성 안내 On/Off가 토글되며 상태가 앱 재실행 후에도 유지된다
- [ ] **TTS-04**: TTS 엔진 초기화가 완료되기 전에 호출된 발화는 큐에 보관되었다가 `onInit(SUCCESS)` 시 flush된다 (race 가드)
- [ ] **TTS-05**: 시스템 한국어 TTS가 없으면 `Locale.US`로 폴백하고 README에 검증 환경(예: Google TTS 한국어)을 명시한다

### High Contrast (HC)

- [ ] **HC-01**: 사용자가 하단 바의 고대비 버튼을 탭하면 `themes_high_contrast.xml`이 적용되어 전체 화면이 고대비 모드로 전환된다 (`recreate()` 후)
- [ ] **HC-02**: 고대비 모드에서 텍스트와 배경의 명도 대비율은 WCAG AA 4.5:1 이상이다 (구현 목표 21:1)
- [ ] **HC-03**: 고대비/TTS 토글 상태는 `SharedPreferences`에 저장되어 앱 재시작 후에도 유지된다 (research F-12 P0 승격)
- [ ] **HC-04**: `setTheme()`는 `setContentView()` 이전 `onCreate()`에서 호출되어 적용 누락이 발생하지 않는다

### Focus / Keypad (FOCUS)

- [ ] **FOCUS-01**: 사용자가 외부 키패드(또는 에뮬레이터 D-pad)의 방향키 ↑↓←→로 포커스를 이동할 수 있다
- [ ] **FOCUS-02**: 사용자가 ENTER 또는 DPAD_CENTER로 포커스된 뷰를 활성화(클릭)할 수 있다
- [ ] **FOCUS-03**: BACK·EditText·VOLUME 등 비방향키는 `dispatchKeyEvent`에서 `super`로 위임되어 정상 동작한다 (화이트리스트 처리)
- [ ] **FOCUS-04**: 콘텐츠 영역에서 DOWN을 누르면 하단 바로 점프한 뒤 UP으로 다시 콘텐츠로 돌아올 수 있다 (XML `nextFocusUp/Down/Left/Right` 명시 연결)
- [ ] **FOCUS-05**: 포커스된 뷰는 `state_focused` selector에 의해 3dp 이상의 stroke 인디케이터가 표시된다 (Material ripple과 충돌 없도록 `android:foreground` 활용)

### Volume / Zoom (MEDIA)

- [ ] **MEDIA-01**: 사용자가 음량 증감 버튼을 탭하면 `AudioManager.STREAM_MUSIC` 볼륨이 한 단계씩 변한다
- [ ] **MEDIA-02**: 사용자가 확대/축소 버튼을 탭하면 콘텐츠 영역(MenuFragment)이 `ScaleAnimation` 또는 직접 `scaleX/scaleY` 변경으로 0.8x ~ 1.5x 범위에서 단계 변경된다
- [ ] **MEDIA-03**: zoom level은 `savedInstanceState`로 회전·테마 전환 시 보존된다

### Timeout / Response Time (TIME)

- [ ] **TIME-01**: 데모 화면에 자동 타임아웃을 두지 않거나, 두는 경우 사용자에게 잔여 시간 + 연장 버튼을 표시한다 (KS X 9211 응답시간 / KWCAG 2.2.1, research F-14)

### Checklist (CHECK)

- [ ] **CHECK-01**: `ChecklistFragment`에서 7개 핵심 기능(BAR/TTS/HC/FOCUS/MEDIA/TIME 카테고리)의 동작 상태를 앱 내에서 시연·확인할 수 있다

### Documentation (DOC)

- [ ] **DOC-01**: README는 첫 1스크롤 안에 (a) 한 줄 설명, (b) 시연 GIF (자막 오버레이), (c) 공고 요구사항 7행 ↔ 코드 위치 매핑 표, (d) WPF↔Android 이식 매핑 표를 포함한다
- [ ] **DOC-02**: README에 표준 준수 섹션을 두어 KS X 9211, KWCAG 2.2 (5.1.5 / 5.4.7 / 6.1.x), WCAG AA를 명시 인용한다
- [ ] **DOC-03**: README에 "Scope" 섹션을 두어 시스템 `AccessibilityService` 미등록·인앱 컨트롤러 패턴 채택 이유를 명시한다 (Phase 2 후속)
- [ ] **DOC-04**: README에 검증 환경(Android Studio Emulator + `hw.dPad=yes` AVD + Numpad 매핑 + Google TTS 한국어)을 명시한다
- [ ] **DOC-05**: GitHub 저장소를 공개로 push하고 한국어 description + 관련 topics(`android`, `kotlin`, `accessibility`, `barrier-free`, `kiosk`)를 설정한다
- [ ] **DOC-06**: 시연 자료로 GIF(자막 포함) + MP4 링크를 README에 임베드 또는 링크한다
- [ ] **DOC-07**: Notion 프로젝트 DB에 본 프로젝트를 업로드하고 위시켓 공고에 지원서를 재제출한다 (≤ 2026-05-15)

### Build (BUILD)

- [ ] **BUILD-01**: `./gradlew build`가 오류 없이 통과한다 (AGP 8.7.3 + Gradle 8.11.1 + Kotlin 2.1.0)
- [ ] **BUILD-02**: 의존성은 AndroidX 5종(`appcompat`, `core-ktx`, `activity-ktx`, `fragment-ktx`, `constraintlayout`)만 사용하며 `com.google.android.material`을 포함하지 않는다
- [ ] **BUILD-03**: minSdk 26, compileSdk/targetSdk 35, JDK 17로 설정되어 있다

---

## v2 Requirements

본 MVP 일정(6일) 안에 다루지 않으나 향후 확장 가능한 항목.

### Phase 2 — Accessibility Depth

- **A11Y-01**: 시스템 `AccessibilityService` 등록 (시스템 차원 통합)
- **A11Y-02**: 전체 뷰포트 Magnification API (현재는 콘텐츠 영역 ScaleAnimation만)
- **A11Y-03**: BLE/외부 키패드 페어링 + 키 매핑 UI

### Phase 2 — Coverage Expansion

- **A11Y-04**: 청각장애 자막 / 시각 큐 (별표 5 청각장애, research F-13)
- **A11Y-05**: 모션 감소 모드 존중 (KWCAG 2.3.3, research F-15)
- **A11Y-06**: Material Dialog/Toast 고대비 일관성 (`MaterialAlertDialogBuilder` 커스텀)

### Phase 2 — Variants

- **VAR-01**: Compose 버전 별도 변형 (선택 어필 자산)
- **VAR-02**: 다국어 i18n (현재는 한국어 단일)

---

## Out of Scope

명시적으로 제외된 항목. 데모 범위가 흐려지지 않도록 박제한다.

| Feature | Reason |
|---------|--------|
| 풀 키오스크 비즈니스 로직 (결제·주문·메뉴 도메인) | 데모 목적은 접근성 4기능 시연. 도메인 로직은 평가 가치 무관 |
| Jetpack Compose UI | 클라이언트 기존 코드베이스가 View/XML 가능성 높음 → 호환성 손실 리스크 |
| Hilt / Dagger / Room / DataStore / Navigation Component / RxJava | 외부 의존성 0 narrative 보존 |
| `com.google.android.material` | AndroidX 외부 라이브러리, narrative 흐림. 필요 시 Phase 2 |
| 점자 키패드·휠체어 클리어런스 등 KS X 9211 HW 항목 | Android SW 데모 범위 밖 (README "Scope"에 명시) |
| 음성 명령(STT) | 별표 5 표준 외, 도메인 로직 |
| 다크모드(시스템 night mode) | 고대비와 별도 개념, 혼동 회피 |
| 유닛테스트 커버리지 목표 | 6일 일정 안에 시연 영상이 더 큰 평가 가치 |

---

## Traceability

각 v1 requirement이 어느 Phase에 매핑되는지. ROADMAP.md 5-phase 구조 (coarse) 적용.

| Requirement | Phase | Status |
|-------------|-------|--------|
| BAR-01 | Phase 1 | Pending |
| BAR-02 | Phase 1 | Pending |
| BAR-03 | Phase 1 | Pending |
| TTS-01 | Phase 2 | Pending |
| TTS-02 | Phase 2 | Pending |
| TTS-03 | Phase 2 | Pending |
| TTS-04 | Phase 2 | Pending |
| TTS-05 | Phase 2 | Pending |
| HC-01 | Phase 2 | Pending |
| HC-02 | Phase 2 | Pending |
| HC-03 | Phase 2 | Pending |
| HC-04 | Phase 2 | Pending |
| FOCUS-01 | Phase 3 | Pending |
| FOCUS-02 | Phase 3 | Pending |
| FOCUS-03 | Phase 3 | Pending |
| FOCUS-04 | Phase 3 | Pending |
| FOCUS-05 | Phase 3 | Pending |
| MEDIA-01 | Phase 4 | Pending |
| MEDIA-02 | Phase 4 | Pending |
| MEDIA-03 | Phase 4 | Pending |
| TIME-01 | Phase 4 | Pending |
| CHECK-01 | Phase 4 | Pending |
| DOC-01 | Phase 5 | Pending |
| DOC-02 | Phase 5 | Pending |
| DOC-03 | Phase 5 | Pending |
| DOC-04 | Phase 1 | Pending |
| DOC-05 | Phase 5 | Pending |
| DOC-06 | Phase 5 | Pending |
| DOC-07 | Phase 5 | Pending |
| BUILD-01 | Phase 1 | Pending |
| BUILD-02 | Phase 1 | Pending |
| BUILD-03 | Phase 1 | Pending |

**Coverage:**
- v1 requirements: 32 total
- Mapped to phases: 32 ✓
- Unmapped: 0

**Phase distribution:**
- Phase 1 (Foundation): 7 (BUILD-01..03, BAR-01..03, DOC-04)
- Phase 2 (TTS + Theme): 9 (TTS-01..05, HC-01..04)
- Phase 3 (Focus & Keypad): 5 (FOCUS-01..05)
- Phase 4 (Media & Checklist): 5 (MEDIA-01..03, TIME-01, CHECK-01)
- Phase 5 (Ship): 6 (DOC-01..03, DOC-05..07)

Note on BAR-03: 일차 책임은 Phase 1 (초기 contentDescription 패스). Phase 2 TTS attach 시 누락 보강 재검증을 수행하지만 traceability 카운트는 Phase 1 단일 매핑으로 유지한다.

---
*Requirements defined: 2026-05-07*
*Last updated: 2026-05-07 after roadmap creation (32/32 mapped to 5 phases)*
