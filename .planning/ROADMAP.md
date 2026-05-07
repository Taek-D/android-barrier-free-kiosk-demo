# Roadmap: Android Kotlin 베리어프리 접근성 데모 앱

**Created:** 2026-05-07
**Granularity:** coarse (5 phases)
**Timeline:** 6 calendar days (2026-05-08 ~ 2026-05-13) + 2-day buffer (~2026-05-15)
**Coverage:** 32/32 v1 requirements mapped (100%)
**Source:** PROJECT.md · REQUIREMENTS.md · research/SUMMARY.md §6 build order
**Core Value:** 평가자가 GitHub README와 시연 GIF를 90초 안에 훑고 "이 사람 Android Accessibility 진짜 만들 줄 안다"고 결론짓게 한다.

---

## Phases

- [ ] **Phase 1: Foundation** — 프로젝트 셋업, Activity/Fragment 골격, AVD 준비, A11yPrefs, README placeholder
- [ ] **Phase 2: TTS + Theme** — TtsService, contentDescription pass, ThemeService, recreate, SharedPreferences 영속화
- [ ] **Phase 3: Focus & Keypad** — FocusNavigator, dispatchKeyEvent, focused selector, per-Fragment focus order (🔴 HIGHEST RISK)
- [ ] **Phase 4: Media & Checklist** — VolumeService, ScaleAnimation 줌, ChecklistFragment, 타임아웃 정책
- [ ] **Phase 5: Ship** — README 마감, GIF/MP4, 표준 인용, GitHub push, Notion DB, 위시켓 재제출

---

## Phase Details

### Phase 1: Foundation
**Goal**: Day 1에 빌드 가능한 Kotlin/AndroidX 앱 골격을 세우고, 이후 모든 Phase가 의존하는 영속화 계층(A11yPrefs)과 검증 환경(`hw.dPad=yes` AVD)을 미리 확보한다.
**Depends on**: Nothing (first phase)
**Requirements**: BUILD-01, BUILD-02, BUILD-03, BAR-01, BAR-02, BAR-03, DOC-04
**Risk Level**: 🟢 Low
**Pitfalls Watch**: M-3 (에뮬레이터 D-pad 미작동 — Day 4 위험 차단을 위해 Day 1에 `hw.dPad=yes` AVD 사전 검증)
**Success Criteria** (what must be TRUE):
  1. `./gradlew build`가 AGP 8.7.3 + Gradle 8.11.1 + Kotlin 2.1.0 + JDK 17 환경에서 오류 없이 통과한다 (외부 의존성은 AndroidX 5종만, Material 미포함)
  2. 평가자가 앱을 실행하면 모든 Fragment(Home/Menu/Checklist) 화면 하단에 `AccessibilityBottomBar` 4버튼(고대비·TTS·확대·축소)이 56dp+ 터치 타깃으로 항상 노출된다 (탭은 Phase 2~4에서 와이어링)
  3. 모든 인터랙티브 뷰에 strings.xml 기반 `contentDescription` 또는 `android:text`가 설정되어 grep 검증이 가능하다 (기본 패스, Phase 2 TTS attach 시 누락 재검증)
  4. `A11yPrefs` SharedPreferences 래퍼가 존재해 Phase 2 ThemeService/TtsService가 import만으로 토글 상태를 영속화할 수 있다
  5. `hw.dPad=yes` AVD 프로필이 생성되어 Numpad ↑↓←→/Enter 키로 시스템 포커스 이동이 작동함을 수동 확인했고, README placeholder의 검증 환경 섹션에 명시되어 있다
**Plans**: TBD
**UI hint**: yes

### Phase 2: TTS + Theme
**Goal**: Day 2-3에 음성 안내(TTS)와 고대비 테마 두 축을 완성한다. TTS가 contentDescription을 사용하므로 동일 Phase에서 누락 보강 패스를 함께 수행하고, Theme recreate가 SharedPreferences를 통해 토글 상태를 보존하게 만든다.
**Depends on**: Phase 1 (A11yPrefs, BottomBar 골격)
**Requirements**: TTS-01, TTS-02, TTS-03, TTS-04, TTS-05, HC-01, HC-02, HC-03, HC-04, BAR-03 (재검증)
**Risk Level**: 🟡 Medium
**Pitfalls Watch**: C-1 (TTS init race — `@Volatile ready` + pending 큐), C-2 (recreate 좀비 인스턴스 — `object TtsService` + applicationContext), M-1 (Fragment 유실 — savedInstanceState null 가드), M-2 (한국어 TTS 미설치 — `Locale.US` 폴백 + README), M-4 (포커스 시 QUEUE_FLUSH), M-5 (명도 21:1 팔레트)
**Success Criteria** (what must be TRUE):
  1. 사용자가 BottomBar TTS 버튼을 탭해 ON으로 두면, 모든 포커서블 뷰로 포커스가 이동할 때 해당 라벨이 한국어(또는 `Locale.US` 폴백)로 음성 안내되고, 동일 텍스트가 500ms 내 재포커스되면 중복 발화되지 않는다
  2. TTS 엔진이 `onInit` 전에 호출된 발화는 pending 큐에 보관되었다가 `SUCCESS` 시점에 flush되어 첫 안내가 묵음 처리되지 않는다
  3. 사용자가 BottomBar 고대비 버튼을 탭하면 `themes_high_contrast.xml`이 적용되어 전체 화면이 명도 대비 21:1(WCAG AA 4.5:1 이상 충족) 고대비 모드로 전환되고, `setTheme()`은 `setContentView()` 이전 `onCreate()`에서 호출되어 적용 누락이 없다
  4. 고대비 ON / TTS OFF 상태에서 앱을 강제 종료 후 재실행하면 두 토글 상태가 SharedPreferences로부터 복원되어 동일하게 유지된다
  5. grep으로 `contentDescription|android:text` 검색 시 모든 인터랙티브 뷰가 매칭되어 누락이 0건이다
**Plans**: TBD
**UI hint**: yes

### Phase 3: Focus & Keypad
**Goal**: Day 4에 외부 키패드(에뮬레이터 D-pad) 기반 방향키 네비게이션을 완성한다. 본 프로젝트의 **최고 위험 단계** — Critical pitfall 5개 중 3개가 여기에 몰려 있어 와이어링 순서(FocusNavigator → dispatchKeyEvent → XML focus order)를 엄격히 지킨다.
**Depends on**: Phase 1 (Activity/Fragment 골격), Phase 2 (테마/TTS 안정화 후 포커스 인디케이터 충돌 회피)
**Requirements**: FOCUS-01, FOCUS-02, FOCUS-03, FOCUS-04, FOCUS-05
**Risk Level**: 🔴 **HIGH (HIGHEST RISK PHASE)**
**Pitfalls Watch**: C-3 (`dispatchKeyEvent` 모든 키 swallow → BACK/EditText/VOLUME 사망 — DPAD/ENTER 화이트리스트 + ACTION_DOWN 한정 + 외 `super`), C-4 (콘텐츠→하단 바 점프 후 복귀 불가 — XML `nextFocusUp/Down/Left/Right` 명시 연결 필수), C-5 (selector vs Material ripple 충돌 — `android:foreground` API 23+ 활용, AndroidX-only 빌드라 Material 미포함이지만 ripple 자체는 잔존), M-3 (D-pad — Phase 1에서 사전 차단됨)
**Success Criteria** (what must be TRUE):
  1. 사용자가 에뮬레이터 D-pad(또는 외부 키패드)의 ↑↓←→로 모든 Fragment 화면에서 포커스를 이동할 수 있고, ENTER/DPAD_CENTER로 포커스된 뷰를 활성화(클릭)할 수 있다
  2. 콘텐츠 영역의 첫 행에서 ↓를 누르면 하단 바로 점프하고, 하단 바에서 ↑를 누르면 다시 콘텐츠로 복귀한다 (XML `nextFocus*` 양방향 연결 검증)
  3. 포커스된 뷰는 `state_focused` selector에 의해 3dp 이상의 stroke 인디케이터가 표시되며, ripple/배경과 충돌 없이 시각적으로 명확하다 (`android:foreground` 활용)
  4. BACK 키, EditText 입력, VOLUME 하드웨어 키 등 비방향키는 `dispatchKeyEvent`에서 `super`로 위임되어 정상 동작한다 (화이트리스트 외 키는 swallow하지 않음을 수동 검증)
  5. 테마 recreate 후에도 `view.post { firstFocusable.requestFocus() }`로 초기 포커스가 복원되어 키보드 사용자가 화면 진입 즉시 조작 가능하다
**Plans**: TBD
**UI hint**: yes

### Phase 4: Media & Checklist
**Goal**: Day 5에 음량/줌 미디어 컨트롤과 ChecklistFragment를 완성하고, 타임아웃 정책(KS X 9211 / KWCAG 2.2.1)을 명시한다. 앞 Phase 서비스 시그니처가 안정화된 뒤 Checklist를 마지막으로 와이어링해 회귀 위험을 최소화한다.
**Depends on**: Phase 2 (서비스 패턴 확정), Phase 3 (포커스 안정 — 줌/음량 버튼도 키패드 도달 가능)
**Requirements**: MEDIA-01, MEDIA-02, MEDIA-03, TIME-01, CHECK-01
**Risk Level**: 🟢 Low
**Pitfalls Watch**: M-6 (ScaleAnimation 휘발성 — `scaleX/scaleY` 직접 변경으로 영속), M-1 재발 (savedInstanceState로 zoom level 보존)
**Success Criteria** (what must be TRUE):
  1. 사용자가 BottomBar의 음량 증감 버튼을 탭하면 `AudioManager.STREAM_MUSIC` 볼륨이 한 단계씩 변경되어 시스템 음량 슬라이더에 반영된다
  2. 사용자가 BottomBar의 확대/축소 버튼을 탭하면 콘텐츠 영역(MenuFragment)이 0.8x ~ 1.5x 범위에서 단계적으로 `scaleX/scaleY`가 변경되며, 화면 회전 또는 테마 전환 후에도 `savedInstanceState`로 zoom level이 보존된다
  3. 앱 데모 화면은 자동 타임아웃을 두지 않거나, 둔다면 사용자에게 잔여 시간 + 연장 버튼을 표시한다 (README "표준 준수"에 KS X 9211 응답시간 / KWCAG 2.2.1 명시)
  4. `ChecklistFragment`에서 7개 핵심 기능(BAR/TTS/HC/FOCUS/MEDIA/TIME)의 동작 상태를 평가자가 앱 내에서 한 화면으로 시연·확인할 수 있다
**Plans**: TBD
**UI hint**: yes

### Phase 5: Ship
**Goal**: Day 6에 README 1스크롤 매핑 표를 완성하고 자막 GIF + MP4를 임베드해 GitHub 공개 push, Notion DB 업로드, 위시켓 재제출까지 마감 5일 전(2026-05-15)에 끝낸다. **평가자 90초 인지 전환이 본 Phase의 유일한 KPI**.
**Depends on**: Phase 1~4 (모든 기능 동작 + screenshot/GIF 캡처 가능 상태)
**Requirements**: DOC-01, DOC-02, DOC-03, DOC-05, DOC-06, DOC-07
**Risk Level**: 🟡 Medium
**Pitfalls Watch**: P-1 (GIF 오디오 부재 → 자막 오버레이 + MP4 별도 + Logcat 캡처), P-2 (3dp stroke 스크린샷 안 보임 → 빨간 화살표 + 캡션 + 라이트/HC 좌우 비교), P-3 (1스크롤 매핑 표 없음 → 평가자 이탈 — 한 줄 설명 → GIF → 7행 매핑 표 → WPF↔Android 표 순서), P-4 (저장소명 → `android-barrier-free-kiosk-demo` + topics + 한국어 description), P-5 (KWCAG/KS X 9211 인용 누락 → 4행 표 작성)
**Success Criteria** (what must be TRUE):
  1. README 첫 1스크롤 안에 (a) 한 줄 설명, (b) 자막 GIF, (c) 공고 요구사항 7행 ↔ 코드 위치 매핑 표, (d) WPF↔Android 이식 매핑 표가 모두 포함되어 평가자가 30초 스캔으로 7기능 위치를 식별할 수 있다
  2. README "표준 준수" 섹션에 KS X 9211, KWCAG 2.2 (5.1.5 / 5.4.7 / 6.1.x), WCAG AA가 4행 표로 인용되어 있고, "Scope" 섹션에 시스템 `AccessibilityService` 미등록·인앱 컨트롤러 패턴 채택 이유가 명시되어 있다
  3. 시연 GIF(자막 오버레이)와 MP4 링크가 README에 임베드 또는 링크되어, 오디오 없는 GIF에서도 TTS 동작이 자막으로 증명된다
  4. GitHub 저장소가 공개로 push되어 있고 한국어 description + topics(`android`, `kotlin`, `accessibility`, `barrier-free`, `kiosk`)가 설정되어 있다 (저장소명: `android-barrier-free-kiosk-demo` 권장)
  5. Notion 프로젝트 DB 업로드 + 위시켓 공고 지원서 재제출이 2026-05-15(공고 마감 5일 전 버퍼) 이전에 완료되어 있다
**Plans**: TBD
**UI hint**: no

---

## Phase Dependency Graph

```
Phase 1 (Foundation)
    │
    ├──> Phase 2 (TTS + Theme)
    │        │
    │        ├──> Phase 3 (Focus & Keypad)  🔴 HIGHEST RISK
    │        │        │
    │        │        └──> Phase 4 (Media & Checklist)
    │        │                 │
    │        │                 └──> Phase 5 (Ship)
    │        └──> (Phase 4도 Phase 2 서비스 패턴에 의존)
    └──> (Phase 3도 Phase 1 골격에 직접 의존)
```

Sequential execution. Within-phase plan parallelism는 config.json `parallelization: true`로 허용된다 (다중 플랜이 있는 Phase에 한해).

---

## Coverage Validation

**Mapped:** 32/32 v1 requirements ✓
**Orphans:** 0
**Duplicates:** 0

| Phase | Requirements (count) |
|-------|----------------------|
| 1. Foundation | BUILD-01, BUILD-02, BUILD-03, BAR-01, BAR-02, BAR-03, DOC-04 (7) |
| 2. TTS + Theme | TTS-01, TTS-02, TTS-03, TTS-04, TTS-05, HC-01, HC-02, HC-03, HC-04 (9) |
| 3. Focus & Keypad | FOCUS-01, FOCUS-02, FOCUS-03, FOCUS-04, FOCUS-05 (5) |
| 4. Media & Checklist | MEDIA-01, MEDIA-02, MEDIA-03, TIME-01, CHECK-01 (5) |
| 5. Ship | DOC-01, DOC-02, DOC-03, DOC-05, DOC-06, DOC-07 (6) |
| **Total** | **32** |

Note: BAR-03 (`contentDescription` grep 검증)은 1차 패스를 Phase 1에서 수행하고, Phase 2에서 TTS attach 시 누락 보강 재검증을 수행한다. Traceability table에는 일차 책임인 Phase 1로 매핑한다 (중복 카운트 방지).

---

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation | 0/? | Not started | - |
| 2. TTS + Theme | 0/? | Not started | - |
| 3. Focus & Keypad | 0/? | Not started | - |
| 4. Media & Checklist | 0/? | Not started | - |
| 5. Ship | 0/? | Not started | - |

(Plan counts populated by `/gsd-plan-phase`)

---

## Day-to-Phase Calendar Map (informational)

PRD §8 daily schedule이 본 Phase 구조와 어떻게 정렬되는지에 대한 참고용 맵. 실제 진행은 Phase 단위로 추적한다.

| Calendar | Phase | Notes |
|----------|-------|-------|
| 2026-05-08 (Day 1) | Phase 1 | 셋업, 골격, AVD, A11yPrefs, README placeholder |
| 2026-05-09 (Day 2) | Phase 2 | TtsService + contentDescription pass |
| 2026-05-10 (Day 3) | Phase 2 | ThemeService + recreate + SharedPrefs 영속화 |
| 2026-05-11 (Day 4) | Phase 3 | 🔴 FocusNavigator + dispatchKeyEvent + selector |
| 2026-05-12 (Day 5) | Phase 4 | Volume + Zoom + Checklist + Timeout policy |
| 2026-05-13 (Day 6) | Phase 5 | README 마감 + GIF/MP4 + push + Notion + 재제출 |
| 2026-05-14~15 | Buffer | 회귀 / 평가자 피드백 / 보강 캡처 |

Hard deadline: **2026-05-15** (공고 마감 2026-05-20의 5일 전 버퍼).

---

*Roadmap created: 2026-05-07*
*Build order: research/SUMMARY.md §6 (6-day plan, Day 4 = 🔴 HIGH)*
