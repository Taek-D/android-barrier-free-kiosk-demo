---
phase: 1
phase_name: Foundation
status: human_needed
verified_at: 2026-05-07
---

# Phase 1: Foundation — Verification

요구사항: BUILD-01/02/03, BAR-01/02/03, DOC-04 (총 7건).

## Success Criteria 검증

### 1. `./gradlew build`가 AGP 8.7.3 + Gradle 8.11.1 + Kotlin 2.1.0 + JDK 17 환경에서 오류 없이 통과 (BUILD-01/02/03)

**Status:** ⚠️ human_needed
**자동 검증 (산출물 정합성):**
- ✅ `build.gradle.kts` (root): AGP 8.7.3 + Kotlin 2.1.0 `apply false` 명시
- ✅ `gradle/wrapper/gradle-wrapper.properties`: `gradle-8.11.1-bin.zip`
- ✅ `app/build.gradle.kts`: `compileSdk=35`, `minSdk=26`, `targetSdk=35`, `JavaVersion.VERSION_17`, `jvmTarget=17`
- ✅ AndroidX 5종만: appcompat 1.7.1, core-ktx 1.13.1, activity-ktx 1.9.3, fragment-ktx 1.8.5, constraintlayout 2.2.1
- ✅ 금지 의존성 0건: `com.google.android.material` / Compose / Hilt / Coroutines-android grep 결과 코드/빌드 파일 매칭 없음 (research 문서 참조 only)
- ✅ ViewBinding ON, namespace `com.example.a11ydemo`

**수동 단계 (사용자 환경 필요):**
1. `gradle wrapper --gradle-version 8.11.1` 1회 실행 → `gradle-wrapper.jar` 생성 (텍스트 산출 한계로 jar 미포함)
2. `./gradlew build` 실행 → exit 0 확인.
3. Android Studio "Sync Project with Gradle Files"는 동등.

### 2. 모든 Fragment 화면 하단에 `AccessibilityBottomBar` 4버튼 항상 노출 (BAR-01/02)

**Status:** ✅ verified by structure
- ✅ `activity_main.xml`: 단일 인스턴스 BottomBar를 ConstraintLayout `bottom` 앵커로 고정. `FragmentContainerView`는 BottomBar 위에서 fill.
- ✅ `view_accessibility_bottom_bar.xml`: 4 ImageButton (`btn_high_contrast`, `btn_tts`, `btn_zoom_in`, `btn_zoom_out`).
- ✅ 56dp 터치 타깃: `minWidth/minHeight = @dimen/a11y_min_touch (56dp)` 4개 모두 적용.
- ✅ Bar 자체는 72dp 높이로 56dp 버튼 + 패딩 여유 확보.
- ⚠️ **클릭 와이어링은 비어있음** (Phase 2 TTS/HC, Phase 4 zoom에서 attach) — 의도된 상태.

### 3. 인터랙티브 뷰 contentDescription / android:text grep 검증 (BAR-03)

**Status:** ✅ pass (Phase 1 화면 한정)
- ✅ Grep: 4개 layout에서 15건 매칭 (`view_accessibility_bottom_bar.xml`:4, `fragment_menu.xml`:5, `fragment_home.xml`:4, `fragment_checklist.xml`:2).
- ✅ BottomBar 4 ImageButton 모두 `contentDescription` 보유 (strings.xml `cd_btn_*`).
- ✅ 텍스트 버튼은 `android:text` 보유 (Home 2개, Menu 4개).
- ⚠️ Phase 2 TTS attach 시 누락 보강 패스 재실행 필요 (재검증 일정).

### 4. `A11yPrefs` SharedPreferences 래퍼 노출 (HC-03 사전 인프라)

**Status:** ✅ verified
- ✅ `prefs/A11yPrefs.kt`: `object` 싱글턴, `init(context)` 멱등, applicationContext 전용.
- ✅ API: `ttsEnabled`, `highContrastEnabled`, `zoomLevel` 3개 var. zoomLevel은 0.8f~1.5f clamp.
- ✅ `MainActivity.onCreate` 첫 줄 `A11yPrefs.init(applicationContext)` (super.onCreate 이전).
- ✅ Phase 2 ThemeService/TtsService가 import만으로 read/write 가능한 시그니처.

### 5. `hw.dPad=yes` AVD + Day 1 수동 D-pad 검증 (DOC-04, M-3 차단)

**Status:** ⚠️ human_needed
**자동 검증:**
- ✅ `docs/AVD-SETUP.md` 작성: AVD 생성 절차, Numpad 매핑, Google TTS 한국어 설치, Day 1 DoD 체크리스트, 트러블슈팅.
- ✅ `README.md` 검증 환경 섹션에 `[`docs/AVD-SETUP.md`](docs/AVD-SETUP.md)` 링크 + Day 1 수동 확인 체크박스 1줄 명시.

**수동 단계 (사용자 환경 필요 — 본 Phase의 명시적 human gate):**
1. AVD `a11y_demo_dpad_api35` 생성 (`hw.dPad=yes`).
2. Numpad `2` 누름 → 홈 화면 버튼으로 시스템 포커스 이동 확인.
3. Numpad `5` 누름 → 포커스된 버튼 클릭 동작 확인.
4. README 검증 환경 체크박스를 `[x]`로 변경 + 날짜 마킹 (Phase 5 마감 직전 README 정리 시 동시 처리 가능).

---

## 자동 산출물 인벤토리 (24 files)

### `.planning/phases/01-foundation/`
- 01-CONTEXT.md, 01-PLAN.md, 01-VERIFICATION.md

### Build / 메타
- `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`
- `gradle/wrapper/gradle-wrapper.properties`
- `app/build.gradle.kts`, `app/proguard-rules.pro`
- `app/src/main/AndroidManifest.xml`

### Kotlin source
- `MainActivity.kt`
- `prefs/A11yPrefs.kt`
- `ui/fragment/{Home,Menu,Checklist}Fragment.kt`
- `ui/view/AccessibilityBottomBar.kt`

### Resources
- layout: `activity_main`, `view_accessibility_bottom_bar`, `fragment_{home,menu,checklist}`
- values: `strings`, `dimens`, `colors`, `themes`
- drawable: `ic_a11y_{high_contrast,tts,zoom_in,zoom_out}`, `ic_launcher_{background,foreground}`
- mipmap-anydpi-v26: `ic_launcher{,_round}`

### Docs
- `README.md`, `docs/AVD-SETUP.md`

---

## Human Validation Items

다음 항목은 사용자 환경에서 수동 검증 필요:

1. **wrapper jar 생성:** `gradle wrapper --gradle-version 8.11.1` 1회 실행 (또는 Android Studio Sync).
2. **Gradle build 통과:** `./gradlew build` exit 0.
3. **AVD `hw.dPad=yes` 동작:** Numpad ↑↓←→/`5`로 포커스 이동/활성화 확인.
4. **README 체크박스 마킹:** Day 1 D-pad 동작 확인 후 검증 환경 섹션 `[x]` 처리.

---

## Pitfalls Status (M-3 차단)

| ID | Watched | Status |
|----|---------|--------|
| M-3 | 에뮬레이터 D-pad 미작동 | 🟡 docs/AVD-SETUP.md로 사전 차단. 사용자 수동 확인 1회로 closure. |

Phase 2/3 critical pitfalls(C-1 ~ C-5)는 본 Phase 범위 밖 — Phase 2/3 plan에서 다룬다.

---

## Verdict

| Item | Result |
|------|--------|
| 자동 산출물 정합성 (1·2·3·4) | ✅ pass |
| AVD 가이드 작성 (5) | ✅ pass |
| 사용자 환경 빌드/AVD 동작 검증 (1·5) | ⚠️ human_needed |

**Overall:** `human_needed` — 산출물은 모두 작성·정합성 통과. 사용자 환경에서 4개 수동 단계만 1회 실행 후 Phase 2 진입 가능.
