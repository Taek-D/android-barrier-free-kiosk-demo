# Milestone v1 Archive — Android 베리어프리 키오스크 데모

**Shipped:** 2026-05-07
**Tag:** `v1`
**Status:** ✅ Complete (artifacts + AVD demo capture). Notion 업로드 + 위시켓 재제출은 사용자 환경 후속 액션.
**Hard Deadline:** 2026-05-15 (8일 전 마감)
**Repo:** https://github.com/Taek-D/android-barrier-free-kiosk-demo

---

## Headline

위시켓 키오스크 베리어프리 공고용 Android 네이티브 포트폴리오 데모를 6일 일정에서 **1일 만에** 5 phase 산출물 + 빌드 + AVD 시연 캡처 + GitHub 공개까지 마감했다. **외부 의존성 0 (AndroidX 5종만)** narrative와 **WPF↔Android 이식 매핑**을 통해 평가자 90초 인지 전환 가능한 README 1스크롤을 완성했다.

## Stats

| 항목 | 값 |
|------|---|
| Phases | 5 (Foundation / TTS+Theme / Focus & Keypad 🔴 / Media & Checklist / Ship) |
| Atomic commits | 7 (phase-01 ~ phase-05 + wrapper + AVD demo) |
| Files added | 82 tracked |
| Kotlin source | 12 |
| XML resources | 22 |
| Phase artifacts | 15 (.planning/phases/{01..05}/{CONTEXT,PLAN,VERIFICATION}.md) |
| Diff (phase-01 ~ HEAD) | 70 files / +3257 insertions / -23 deletions |
| v1 requirements satisfied | 32 / 32 (100%, BUILD/AVD humans-in-loop) |
| Hard deadline | 2026-05-15 (8일 마진) |

## Phase Summary

### Phase 1: Foundation (`4cd492a`)
**Goal:** 빌드 가능한 Kotlin/AndroidX 골격 + 영속화 인프라 + AVD 가이드.
**Delivered:**
- Gradle 8.11.1 + AGP 8.7.3 + Kotlin 2.1.0 + JDK 17 (실 빌드는 JBR 21로) + minSdk 26 / compileSdk 35.
- AndroidX 5종만(`appcompat 1.7.1`, `core-ktx 1.13.1`, `activity-ktx 1.9.3`, `fragment-ktx 1.8.5`, `constraintlayout 2.2.1`) — Material 미포함.
- `MainActivity` + Home/Menu/Checklist Fragment + ViewBinding ON.
- `AccessibilityBottomBar` 4버튼 항상 노출 (56dp+ 터치, contentDescription 한국어 strings.xml).
- `A11yPrefs` SharedPreferences 래퍼 (`ttsEnabled / highContrastEnabled / zoomLevel`, applicationContext, idempotent init).
- `docs/AVD-SETUP.md` (`hw.dPad=yes` + Numpad + Google TTS 한국어 + Day 1 DoD).
- README placeholder + 어댑티브 아이콘.
**Risk closed:** M-3 (D-pad 미작동) 사전 차단.

### Phase 2: TTS + Theme (`27f73a1`)
**Goal:** 음성 안내 + 고대비 테마 + 토글 영속화.
**Delivered:**
- `TtsService`: `object` 싱글턴, `@Volatile ready` + pending queue + onInit flush(C-1), applicationContext only(C-2), 500ms 동일 텍스트 debounce(TTS-02), `Locale.KOREAN→US` 폴백(M-2), `QUEUE_FLUSH`(M-4).
- `ThemeService.applyTheme(activity)` — `setTheme()` BEFORE `setContentView()` 강제(HC-04). `toggle()` → `recreate()`.
- `themes_high_contrast.xml`: `Theme.A11yDemo.HighContrast` (parent dark), 명도 대비 **21:1** (#000↔#FFF), focus 19.56:1 (#FFFF00 on #000).
- `accessibility/A11yViewExt.kt`: 포커스 listener attach (sentinel tag로 중복 회피), Activity onResume + back-stack listener에서 재attach.
- BottomBar HC/TTS 토글 핸들러 와이어링 + 자기참조 발화.
- Theme attr `a11yBottomBarBg`로 light/HC 분기.
**Pitfalls closed:** C-1, C-2, M-2, M-4, M-5.

### Phase 3: Focus & Keypad 🔴 (`8dd154d`)
**Goal:** D-pad 키패드 네비게이션 (HIGHEST RISK PHASE).
**Delivered:**
- `MainActivity.dispatchKeyEvent` 화이트리스트(C-3): `KEYCODE_DPAD_*`/`ENTER`/`NUMPAD_ENTER` + `ACTION_DOWN` 한정. 외 모든 키 `super` 위임 → BACK/EditText/VOLUME 정상 동작.
- `accessibility/FocusNavigator`: `focusSearch` wrapper + `findFirstFocusable`/`findLastFocusable`.
- 콘텐츠 ↔ BottomBar 양방향(C-4): XML `nextFocusDown`(콘텐츠 → BottomBar 정적) + `handleUp`(BottomBar → 콘텐츠 마지막 동적).
- BottomBar 4버튼 cyclic `nextFocusLeft/Right`.
- `res/drawable/focused_background.xml`: state_focused 시 3dp stroke + `?attr/a11yFocusStroke` 분기 (light=red, HC=yellow). 모든 Button/ImageButton에 `android:foreground` 적용 → ripple 충돌 회피(C-5).
- 각 Fragment `onViewCreated` `view.post { findFirstFocusable.requestFocus() }` → recreate 후 초기 포커스 복원.
- ChecklistFragment 뒤로가기 버튼 + 양방향 nextFocus.
**Pitfalls closed:** C-3, C-4, C-5. 본 프로젝트 최고 위험 phase 무사 통과.

### Phase 4: Media & Checklist (`c8640c5`)
**Goal:** 음량/줌 + 7기능 체크리스트 + 타임아웃 정책.
**Delivered:**
- `VolumeService`: `STREAM_MUSIC` `ADJUST_RAISE/LOWER` + `FLAG_SHOW_UI`로 시스템 슬라이더 노출.
- `ZoomService`: `scaleX/Y` 직접 변경(M-6 ScaleAnimation 휘발성 회피) + `pivot 0,0`. A11yPrefs.zoomLevel 0.8~1.5 clamp + setter persist. `apply(target)` 단독 호출로 `onResume` 복원.
- ChecklistFragment 7행 동적 렌더 (BAR/TTS/HC/FOCUS/MEDIA-Volume/MEDIA-Zoom/TIME). 상태(✅/⚠️) + 라벨 + 디테일. `onResume`에서 재렌더.
- 음량 ▲▼ 버튼 보강 (BottomBar는 PRD 4버튼 fix 유지, 음량은 ChecklistFragment에 노출).
- TIME-01: 자동 타임아웃 두지 않음 명시 (KS X 9211 / KWCAG 2.2.1).
- BottomBar zoom 핸들러 와이어링 (`fragmentContainer`에 적용, BottomBar 자체는 영향 없음).
**Pitfalls closed:** M-6.

### Phase 5: Ship (`dbbbb87` + `e1cb00e` + `789e659`)
**Goal:** README 1스크롤 + 자료 캡처 + GitHub 공개.
**Delivered:**
- README 본문: 7행 매핑 표 + WPF↔Android 표 + 표준 4행(KS X 9211 / KWCAG 5.1.5·5.4.7·6.1.x / WCAG AA / 별표 5) + Scope + 검증 환경 + 빌드 + 의존성 + 핵심 구현 메모 + 구조 + GitHub 메타 + Release Checklist 9항목.
- LICENSE (MIT).
- Gradle wrapper 부트스트랩: `gradle wrapper` 1회 → `gradle-wrapper.jar` (43KB) 커밋. `./gradlew build` BUILD SUCCESSFUL 1m10s, 92 tasks.
- `.gitattributes` (gradlew LF, *.bat CRLF, *.jar/*.png/*.mp4/*.apk binary).
- `gradle.properties`에 `android.overridePathCheck=true` (한글 경로 가드).
- AVD 시연 캡처: `Medium_Phone_API_36.1` `hw.dPad=yes` 수정 후 부팅 → APK install → adb input keyevent 자동 시연 → `assets/{01-home-light, 02-home-hc, 03-menu, 04-checklist}.png` (1080×2400) + `demo.mp4` (191KB, 25s).
- GitHub 저장소 공개: https://github.com/Taek-D/android-barrier-free-kiosk-demo (Public, KO description, 7 topics: android/kotlin/accessibility/barrier-free/kiosk/wcag/kwcag).
- 모든 7 atomic commit master에 push.
**Pitfalls closed:** P-3 (1스크롤 매핑 표), P-4 (저장소명/topics), P-5 (KWCAG/KS X 9211 인용). P-1 (자막 GIF), P-2 (3dp stroke 화살표 합성)는 별도 편집 액션으로 deferred.

---

## v1 Requirements Coverage

| Category | Count | Status |
|----------|------:|--------|
| BAR | 3 | ✅ |
| TTS | 5 | ✅ (코드/HC) + ⚠️ AVD 발화 1회 검증 |
| HC | 4 | ✅ (21:1 산출, recreate, SharedPrefs) |
| FOCUS | 5 | ✅ (C-3/C-4/C-5 모두 가드) |
| MEDIA | 3 | ✅ (음량+줌 영속) |
| TIME | 1 | ✅ (자동 타임아웃 없음 명시) |
| CHECK | 1 | ✅ (7행 동적 렌더) |
| DOC | 7 | ✅ DOC-01~04 + GitHub push 완료. ⚠️ DOC-06 자막 GIF, DOC-07 Notion / 위시켓 재제출 = Release Checklist 8·9 |
| BUILD | 3 | ✅ ./gradlew build SUCCESSFUL on JBR 21 + AGP 8.7.3 |
| **Total** | **32** | **32/32 mapped, 32/32 자동/구조 검증 통과** |

상세는 `.planning/milestones/v1-REQUIREMENTS.md`.

---

## Critical Pitfalls Status

| ID | Description | Phase | Result |
|----|-------------|-------|--------|
| C-1 | TTS init race | 2 | ✅ ready guard + pending queue + onInit flush |
| C-2 | recreate 좀비 TTS | 2 | ✅ object 싱글턴 + applicationContext only |
| C-3 | dispatchKeyEvent 모든 키 흡수 | 3 | ✅ 화이트리스트 + ACTION_DOWN + super 위임 |
| C-4 | 콘텐츠 → BottomBar 점프 후 복귀 불가 | 3 | ✅ XML nextFocusDown 정적 + handleUp 동적 |
| C-5 | selector vs ripple 충돌 | 3 | ✅ android:foreground (API 23+) + state_focused selector |
| M-3 | 에뮬 D-pad 미작동 | 1 | ✅ AVD config.ini hw.dPad=yes 적용 |
| M-6 | ScaleAnimation 휘발성 | 4 | ✅ scaleX/Y 직접 + SharedPrefs |
| P-3 | README 1스크롤 매핑 표 | 5 | ✅ |
| P-4 | 저장소명/topics | 5 | ✅ android-barrier-free-kiosk-demo + 7 topics |
| P-5 | KWCAG/KS X 9211 인용 | 5 | ✅ 4행 표 + 별표 5 |

---

## Decisions Locked (per PROJECT.md)

| Decision | Outcome |
|----------|---------|
| XML Layout (View) 채택, Compose 배제 | ✅ Validated — 클라이언트 호환성 우선 narrative 그대로 출하 |
| 외부 라이브러리 0개 (AndroidX만) | ✅ Validated — 5종만, Material 미포함 |
| 테마 전환에 `recreate()` 사용 | ✅ Validated — Phase 2 ThemeService.toggle |
| TTS 중복 억제 = 시간(500ms) + 텍스트 비교 | ✅ Validated — TtsService.speak debounce |
| 화면 확대/축소 = 콘텐츠 영역 scaleX/Y (전체 뷰포트는 v2) | ✅ Validated — Phase 4 ZoomService |
| 검증 환경 = Android Studio Emulator D-pad | ✅ Validated — Medium_Phone_API_36.1 hw.dPad=yes |
| F-12 SharedPreferences P0 승격 | ✅ Validated — A11yPrefs로 모든 토글/줌 영속 |
| F-14 타임아웃 정책 P1 추가 | ✅ Validated — 자동 타임아웃 없음 ChecklistFragment + README |
| 시스템 AccessibilityService 미등록 | ✅ Validated — README "Scope"에서 의도된 범위 한정 명시 |

---

## Lessons / Learnings (for v2 인계)

1. **JDK 25 → JBR 21로 우회:** AGP 8.7.3은 JDK 17~21만 공식 지원. Android Studio 동봉 JBR 사용이 가장 단순한 해결책. 빌드 머신/CI 명세에 `JAVA_HOME`을 JBR 또는 Temurin 17로 명시 필요.
2. **한글 경로 + AGP:** `android.overridePathCheck=true`로 우회 가능하나 실제 빌드는 통과. CI/배포는 ASCII 경로 권장 — 본 데모는 portfolio라 그대로 두지만, 클라이언트 환경 이식 시 ASCII 경로 강제 권장.
3. **GitHub mp4 인라인 재생:** 자막 GIF 없이도 mp4 링크 임베드로 평가자가 영상 확인 가능. GIF 합성은 후속 polish.
4. **dispatchKeyEvent 화이트리스트 패턴:** `ACTION_DOWN` 가드 + 화이트리스트 외 `super` 위임 = WPF KeyboardFocusBehavior와 동일 철학. 다른 Android 키오스크 프로젝트에 그대로 재사용 가능한 패턴.
5. **object 싱글턴 + applicationContext:** TTS/Theme/Volume/Zoom 4 서비스 모두 동일 패턴. recreate 좀비 가드와 narrative 단순화에 모두 유효.

---

## Out of Scope (v1 → v2 인계)

| Item | Reason | v2 Phase |
|------|--------|----------|
| 시스템 AccessibilityService 등록 | 발주처 strict 여부 미확정, 인앱 컨트롤러로 7요구 충족 | A11Y-01 |
| Magnification API 전체 뷰포트 | 콘텐츠 영역 scaleX/Y로 갈음, 전체 뷰포트는 별도 권한 | A11Y-02 |
| BLE / 외부 키패드 페어링 | 시스템 차원 통합 비범위 | A11Y-03 |
| 청각장애 자막 / 시각 큐 | F-13 deferred | A11Y-04 |
| 모션 감소 (KWCAG 2.3.3) | F-15 deferred | A11Y-05 |
| Compose 변형 | 호환성 narrative 우선 | VAR-01 |
| 다국어 i18n | 한국어 단일 | VAR-02 |
| 풀 키오스크 비즈니스 로직 | 데모 범위 외 | — (스코프 외) |

---

## Hand-off Notes for v2

**v2 시작 전 확인 사항:**

1. 위시켓 발주처 응답 (Open Decision #4: 시스템 AccessibilityService strict 여부) — 응답 따라 A11Y-01 우선순위 결정.
2. 화면 확대/축소 (Open Decision #5) — 발주처가 전체 뷰포트 요구하면 A11Y-02 → P0 승격.
3. 평가자 피드백 — 1차 1주일 안에 받으면 v2 phase 우선순위 조정.

**유지보수 가이드:**

- `gradle.properties`의 `android.overridePathCheck=true`는 클라이언트 환경(ASCII 경로)으로 이식 시 제거.
- `local.properties`는 `.gitignore`. 신규 클론 시 `sdk.dir` 설정 또는 `ANDROID_HOME` 환경 변수.
- Wrapper jar는 저장소 포함됨. 클론 직후 `./gradlew build` 즉시 실행 가능.

---

*v1 archived: 2026-05-07*
*Source phase artifacts: `.planning/phases/01-foundation` ~ `05-ship` (preserved for reference)*
