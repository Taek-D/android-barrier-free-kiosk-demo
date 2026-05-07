# Research Synthesis — Android Kotlin 베리어프리 접근성 데모

**Project:** Android Kotlin 베리어프리 접근성 데모 앱 (위시켓 공고 포트폴리오)
**Synthesized:** 2026-05-07
**Source files:** STACK.md · FEATURES.md · ARCHITECTURE.md · PITFALLS.md
**Overall confidence:** HIGH

---

## 1. Executive Summary

6일 안에 위시켓 키오스크 접근성 공고(예산 1,000만 원, 마감 2026-05-20)의 1차 평가자가 GitHub README 90초 스캔으로 "Android Kotlin Accessibility 만들 줄 안다"고 결론 짓게 만드는 포트폴리오 데모다. 스택은 **Kotlin 2.1.0 + AGP 8.7.3 + AndroidX 5종(Material 제외)** 으로 고정, 외부 의존성 0개를 narrative의 핵심으로 삼는다. 시스템 `AccessibilityService` 등록 대신 **인앱 컨트롤러 패턴**으로 가고, README "Scope" 섹션으로 의도된 범위 한정임을 명시한다.

규제 anchor는 **KS X 9211 무인정보단말기 접근성 지침 + 과기부 검증기준 별표 5 + KWCAG 2.2 + WCAG AA**의 4중 매핑이며, 7개 P0/P1 기능이 모두 이 표준과 1:1 매핑된다. 가장 큰 일정 리스크는 **Day 4 (FocusNavigator + dispatchKeyEvent + focused selector)** — Critical pitfall 5개 중 3개(C-3/C-4/C-5)가 이 날 몰려 있다. 완화책: Day 1 셋업 시 `hw.dPad=yes` AVD 미리 생성, 화이트리스트 키 처리, `android:foreground` 활용.

리서치 결과 PRD 갭 3건 발견: (1) **F-12 SharedPreferences 영속화는 P2 → P0 승격** (recreate 상태 보존이 어차피 필수), (2) **F-6 contentDescription 일반화 acceptance 명시**, (3) **F-14 타임아웃 정책 한 줄 명시** (KS X 9211 응답시간 항목). 모두 5분 이내 변경, 평가자 신호 가치 큼.

---

## 2. Stack Decisions (Locked)

### Pinned versions

| Component | Version | Why this, not latest |
|---|---|---|
| AGP | **8.7.3** | AGP 9.x = 브레이킹 DSL, 6일에 zero payoff |
| Gradle | **8.11.1** | AGP 8.7과 짝, 9.x 회피 |
| Kotlin | **2.1.0** | AGP 8.7과 검증된 페어. 2.3.x는 AGP 9 전용 |
| JDK | **17 (Temurin)** | AGP 8.7 hard requirement |
| compileSdk / targetSdk | **35 / 35** | Android 15. 36은 AGP 8.9+ 필요 |
| minSdk | **26** | PRD 제약 |
| `appcompat` | **1.7.1** | 테마 전환 백본 |
| `core-ktx` | **1.13.1** | compileSdk 35 호환 |
| `activity-ktx` | **1.9.3** | OnBackPressedDispatcher |
| `fragment-ktx` | **1.8.5** | FragmentManager DSL |
| `constraintlayout` | **2.2.1** | bottom-bar 앵커 |
| ViewBinding | **ON** / Compose **OFF** | — |

**Total: 5 AndroidX artifacts. 0 non-AndroidX.**

### What NOT to use

Compose / `com.google.android.material` (narrative 흐림) / Hilt·Dagger / Room·DataStore / Navigation Component / RecyclerView·ViewPager2 / `kotlinx-coroutines-android` / 시스템 AccessibilityService 등록 / AGP 9.x·Kotlin 2.3.x / ktlint·Detekt·Spotless / `lifecycle-viewmodel-ktx`.

---

## 3. Feature Set (PRD ↔ F-ID ↔ Status)

| F-ID | Feature | PRD | 규제 매핑 | Status |
|---|---|---|---|---|
| F-1 | AccessibilityBottomBar (4버튼 항상 노출) | P0-1 | KS X 9211 진입 일관성 | P0 locked |
| F-2 | TTS speakIfChanged + 토글 + 500ms debounce | P0-2 | 별표 5 시각장애 / KWCAG 4.1.2 | P0 locked |
| F-3 | 고대비 테마 + recreate() | P0-3 | KWCAG 1.4.3 / KS X 9211 대비 | P0 locked |
| F-4 | dispatchKeyEvent + DPAD focusSearch | P0-4 | 별표 5 지체장애 대체입력 | P0 locked |
| F-5 | Focus indicator selector (≥3dp) | P0-5 | WCAG 2.4.7 / KWCAG 강화 | P0 locked |
| F-6 | 모든 인터랙티브 뷰 contentDescription | P0-1 (암묵) | 별표 5 / KWCAG 1.1.1 | **P0 명시화 필요** ⚠️ |
| F-7 | 56dp+ 최소 터치 타깃 | P0-1 (암묵) | KS X 9211 버튼 간격 | P0 locked |
| F-8 | README 1:1 매핑 표 + GIF | P0-6 | (포트폴리오) | P0 locked |
| F-9 | AudioManager 음량 증감 | P1-1 | KS X 9211 음량 | P1 locked |
| F-10 | ScaleAnimation 콘텐츠 영역 | P1-2 | 별표 5 확대 기능 | P1 locked |
| F-11 | ChecklistFragment | P1-3 | (어필) | P1 locked |
| **F-12** | **SharedPreferences 영속화** | **P2-1** | 키오스크 도메인 신호 | **⚠️ P0 승격 권장 (ROI 최고)** |
| F-13 | 청각장애 자막 / 시각 큐 | (PRD 갭) | 별표 5 청각장애 | P1 신규 (선택) |
| **F-14** | **응답시간/타임아웃 정책 명시** | **(PRD 갭)** | **KS X 9211 / KWCAG 2.2.1** | **⚠️ P1 추가 권장** |
| F-15 | 모션 감소 모드 존중 | (PRD 갭) | KWCAG 2.3.3 | P2 선택 |

### Anti-features
결제·주문·메뉴 / Compose / 시스템 AccessibilityService 등록 / Magnification API 전체 뷰포트 / BLE 페어링 / 점자·휠체어 HW 항목 / 다국어 i18n / 다크모드 (≠ 고대비) / 음성명령 STT / Material Dialog 고대비 / 유닛테스트 커버리지 목표.
**키워드 4개:** "HW", "시스템 권한", "외부 의존", "도메인 로직" → 걸리면 cut.

---

## 4. Architecture Skeleton

### Package layout (validated + 2 packages added)

```
app/src/main/java/com/example/a11ydemo/
├── MainActivity.kt              (theme owner + KeyEvent entry)
├── ui/
│   ├── fragment/  Home / Menu(Zoomable) / Checklist
│   └── view/      AccessibilityBottomBar.kt
├── service/       Tts / Theme / Volume   (object 싱글턴)
├── accessibility/ FocusNavigator / A11yViewExt    ★ NEW
└── prefs/         A11yPrefs                       ★ NEW (SharedPrefs wrapper)

res/
├── layout/   activity_main · view_accessibility_bottom_bar · fragment_*
├── drawable/ focused_background.xml (state_focused)
├── values/   colors · strings (모든 contentDescription) · themes / themes_high_contrast
└── (NO values-night/)            ← 수동 테마, 시스템 야간모드 무관
```

**Mental model (30초 grok):** Activity = entry points · Fragments = content + per-screen focus order · Services = side effects (object 싱글턴) · FocusNavigator = pure function.

### Lifecycle gotchas (1줄 each)

| # | Gotcha | One-liner mitigation |
|---|---|---|
| 6.1 | recreate() + Fragment back stack | `FragmentContainerView` + `if (savedInstanceState == null)` 가드, `setRetainInstance` 금지 |
| 6.2 | savedInstanceState vs SharedPreferences | HC/TTS 토글 → SharedPrefs / focused id, zoom level → savedInstanceState |
| 6.3 | TTS init race | `@Volatile var ready` + `pending` 큐, `onInit(SUCCESS)` flush |
| 6.4 | Theme attr resolution | `?attr/...` 만 사용, 하드코딩 색상 금지 |
| 6.5 | Custom View on theme switch | `onAttachedToWindow`에서 read (init {} 금지) |
| 6.6 | Focus 초기 상태 after recreate | `view.post { firstFocusable.requestFocus() }`, savedInstanceState null 시 |
| 6.7 | dispatchKeyEvent swallowing | ACTION_DOWN + 화이트리스트만, 외 `super.dispatchKeyEvent()` |

---

## 5. Critical Pitfalls (Top 10)

### Critical (5)

| ID | Warning | Prevention |
|---|---|---|
| **C-1** | TTS `speak()`가 `onInit` 전에 호출 → 첫 안내 묵음 | `ready` 가드 + pending 큐, `onInit(SUCCESS)`에서 flush |
| **C-2** | 매 recreate()마다 새 TTS 인스턴스 → 좀비, 음성 겹침 | `object TtsService` 싱글턴 + `applicationContext`만 사용 |
| **C-3** | dispatchKeyEvent 모든 키에 `return true` → BACK·EditText·VOLUME 죽음 | 화이트리스트 (DPAD_*/ENTER) + 외 `super.dispatchKeyEvent()` |
| **C-4** | 콘텐츠 어디서든 DOWN → 하단 바 점프, 다시 위 못 옴 | XML `nextFocusUp/Down/Left/Right` 명시 연결 |
| **C-5** | selector가 Material ripple과 충돌, stroke 미표시 | `android:foreground` (API 23+) + state_focused item을 default 위에 배치 |

### Portfolio Presentation (5) — Day 6 평가자 90초 인지

| ID | Warning | Prevention |
|---|---|---|
| **P-1** | GIF 오디오 없어 TTS 동작 의심 | 자막 오버레이 + MP4 별도 + Logcat 캡처 |
| **P-2** | 스크린샷 3dp stroke가 1~2px → 안 보임 | 빨간 화살표 + 캡션 + 라이트/HC 좌우 비교 |
| **P-3** | README 1스크롤에 매핑 표 없음 → 평가자 이탈 | 1스크롤: 한 줄 설명 → GIF → 7행 매핑 표 → WPF↔Android 표 |
| **P-4** | 저장소명 `myapp` → 위시켓 매니저 무엇인지 모름 | `android-barrier-free-kiosk-demo` + topics + 한국어 description |
| **P-5** | "WCAG AA"만 인용, KWCAG/KS X 9211 누락 | README "표준 준수" 4행 표 (KS X 9211 / KWCAG 5.1.5 / 5.4.7 / 6.1.x) |

### Moderate (참조용)
M-1 Fragment 유실(savedInstanceState null 가드), M-2 한국어 TTS 미설치(`Locale.US` 폴백 + README 환경 명시), M-3 에뮬레이터 D-pad(Day 1 `hw.dPad=yes`), M-4 QUEUE_FLUSH (포커스), M-5 명도 대비 21:1 팔레트 강제, M-6 ScaleAnimation 휘발성(scaleX/scaleY 직접).

---

## 6. Build Order (6일 + 리스크 등급)

| Day | Risk | 산출물 | Pitfall watch |
|---|---|---|---|
| **Day 1** | 🟢 Low | 셋업, MainActivity + 3 Fragment 빈 껍데기, BottomBar 레이아웃, **A11yPrefs**, **`hw.dPad=yes` AVD 미리**, README placeholder | A11yPrefs는 Day 3 Theme보다 먼저. M-3 회피. |
| **Day 2** | 🟡 Med | TtsService (init guard, queue, debounce) + A11yViewExt + 모든 contentDescription + 한국어 폴백 | C-1, C-2, M-2, M-4. **TTS attach가 누락 contentDescription 노출.** |
| **Day 3** | 🟡 Med | themes 2종 (21:1) + ThemeService + recreate() + savedInstanceState null 가드 + **F-12 영속화 (승격)** | M-1, M-5. **`setTheme()` BEFORE `setContentView()` 필수.** |
| **Day 4** | 🔴 **HIGH** | FocusNavigator + dispatchKeyEvent + focused selector + Per-Fragment XML focus order | C-3, C-4, C-5, M-3. **순서:** XML focus order는 dispatchKeyEvent 와이어링 *후*. 역순 = silent breakage. |
| **Day 5** | 🟢 Low | VolumeService + Zoomable + ScaleAnimation (scaleX/scaleY) + ChecklistFragment | M-6. ChecklistFragment 마지막 — 서비스 시그니처 안정화 후. |
| **Day 6** | 🟡 Med | README 매핑 표 + GIF (자막) + MP4 + 표준 준수 섹션 + 저장소명/topics + push + Notion + 위시켓 재제출 | P-1~P-5. README placeholder를 Day 1부터 매일 채우기. |

**버퍼:** Day 7~8 (05-14~05-15).

**Critical insights:**
1. A11yPrefs **before** ThemeService (Day 1 ↔ Day 3)
2. TtsService **before** contentDescription pass (Day 2 same-day)
3. FocusNavigator wiring **before** XML `nextFocus*` (Day 4 내 순서)
4. Theme recreate **before** ScaleAnimation (Day 3 < Day 5)
5. ChecklistFragment **last**

---

## 7. PRD Adjustments Recommended (Ranked by ROI)

| # | Adjustment | Cost | Why | Action |
|---|---|---|---|---|
| **1** | **F-12 SharedPreferences: P2-1 → P0 승격** | 5분 (5줄) | recreate 상태 보존 어차피 필수, 도메인 신호 강함 | PRD §5 P0에 추가, P2-1 제거 |
| **2** | **F-6 contentDescription acceptance 명시화** | 0 | P0-1 acceptance에 한 줄로만 존재. grep 즉시 검증 항목 | P0-1에 "**모든 인터랙티브 뷰**에 strings.xml 기반 contentDescription" 명시 |
| **3** | **F-14 타임아웃 정책 README 1줄 + Activity 1줄** | 10분 | KS X 9211 응답시간 / KWCAG 2.2.1 | PRD §5에 P1-4 추가: "타임아웃 없음 명시 또는 연장 버튼" |
| **4** | Day 1 DoD에 `hw.dPad=yes` AVD 동작 확인 추가 | 5분 | Day 4 시작 시 D-pad 미동작 = Day 4 위험 | PRD §8 Day 1 DoD 갱신 |
| **5** | README "Scope" 섹션으로 시스템 AccessibilityService 미등록 명시 | 5분 | 공고 문구 모호성 헤지, Phase 2 stub | Open Question #2 PRD에 박제 |
| 6 | Day 6 README 1스크롤 매핑 표 구조 권장 | 0 | P-3 차단 | PRD §6 갱신 |
| 7 | F-13 자막/시각 큐 P1 (선택) | 30분 | 별표 5 청각장애 | 시간 허락 시 |
| 8 | F-15 모션 감소 P2 (선택) | 15분 | KWCAG 2.3.3 | 여유 시 |

**Top 3만 적용해도** 평가자 통과율 + 도메인 신뢰성 모두 유의미 상승.

---

## 8. Open Questions for Roadmap

### Day-1 결정 필요
1. **저장소명 확정** — `android-barrier-free-kiosk-demo` vs `android-accessibility-kiosk-kotlin` (P-4 회피)
2. **Material Components 포함 여부** — 권장: omit + Day 5 비교 캡처 후 좋은 쪽 채택
3. **AVD 프로필** — `hw.dPad=yes` + Numpad 매핑 + Google TTS 한국어 사전 설치

### 발주처 확인 (지원 후 위시켓 메시지)
4. **"Accessibility Service 제어 경험" — 시스템 서비스 strict?** README "Scope" 헤지로 보험.
5. **화면 확대/축소 — 콘텐츠 영역 vs 전체 뷰포트?** PRD는 콘텐츠 영역 결정.
6. **시연 영상 형식** — 결론: 둘 다 (GIF 인라인 + MP4 링크).

### Day-3 결정 필요
7. **recreate() Fragment 처리** — 결론 박제: persistent 토글 → SharedPreferences, transient → savedInstanceState (역할 분리).

### Day-4 결정 필요
8. **실 디바이스 보유 여부** — 미보유 시 README "Emulator + Numpad 매핑" 명시.

---

## 9. Confidence Assessment

| Area | Confidence | Notes |
|---|---|---|
| Stack | **HIGH** | Maven 검증. Material drop만 MEDIUM (평가자 기호 미상). |
| Features | **HIGH** | 규제 4중 매핑 직접 인용. F-12/13/14 ROI 추정만 MEDIUM. |
| Architecture | **HIGH** | API 14~21 안정 API. FragmentContainerView API 26 + TTS Bundle overload 2건만 구현 시 Context7 재확인 권장. |
| Pitfalls | **HIGH** | Context7 + 공식 + cvs-health + 한국 표준 + 검증된 GitHub 이슈. |

---

## 10. Sources (aggregated)

**공식 표준 / 한국 규제 (HIGH):** KS X 9211 / 별표 5 / 장차법 시행령 / KWCAG 2.2 / WCAG AA·AAA
**Android 공식 (HIGH):** AGP 8.7·9.x 릴리스 노트, Gradle 호환성, TextToSpeech, dispatchKeyEvent, focusSearch, FragmentContainerView, Saving state with fragments, Keyboard navigation, TV Focus system, Emulator Extended controls, Principles for improving app accessibility
**커뮤니티 (MEDIUM):** cvs-health/android-view-accessibility-techniques, Android Design Patterns Fragment State Loss, WebAIM Contrast Checker
**Project files:** `.planning/PROJECT.md`, `PRD.md`, `android_barrier_free_demo_plan.md` §4-1 §7 §8
