# Phase 1: Foundation — Context

**Gathered:** 2026-05-07
**Status:** Ready for planning
**Mode:** Inline autonomous (smart-discuss equivalent)

<domain>
## Phase Boundary

Day 1에 빌드 가능한 Kotlin/AndroidX 앱 골격을 세우고, 이후 모든 Phase가 의존하는 영속화 계층(`A11yPrefs`)과 검증 환경(`hw.dPad=yes` AVD 가이드)을 미리 확보한다. 본 Phase의 코드는 Phase 2~4에서 import 만으로 활용 가능해야 하며, BottomBar 4버튼은 클릭 핸들러를 비워둔 채 Phase 2~4에서 와이어링한다.

요구사항: BUILD-01/02/03, BAR-01/02/03, DOC-04 (총 7건).

</domain>

<decisions>
## Implementation Decisions (locked)

### 패키지/저장소
- **패키지명:** `com.example.a11ydemo` — research §4 architecture skeleton 그대로. `applicationId`도 동일.
- **저장소명:** `android-barrier-free-kiosk-demo` (권장). Phase 5 GitHub push 시 최종 확정. PROJECT.md Open Decisions §1 매칭.
- **모듈:** 단일 `:app` 모듈. multi-module 비채택 (6일 압박, narrative 단순화).

### Gradle / 툴체인
- **Wrapper 버전:** Gradle 8.11.1 — `gradle/wrapper/gradle-wrapper.properties`만 작성. `gradle-wrapper.jar`는 Android Studio가 처음 열 때 또는 사용자가 `gradle wrapper --gradle-version 8.11.1`을 1회 실행해 생성. 텍스트 산출 한계로 jar는 코드베이스에 포함하지 않음 — README에 명시.
- **AGP:** 8.7.3 / **Kotlin:** 2.1.0 / **JDK:** 17. CLAUDE.md Hard Constraints 그대로.
- **Version Catalog (`libs.versions.toml`):** **불사용**. 단일 모듈에 의존성 5종이라 직접 `implementation("...")` 표기가 더 명확. narrative("외부 의존성 0 + AndroidX 5종") 가시성 우선.
- **`compileSdk`/`targetSdk`:** 35, **`minSdk`:** 26.

### 의존성 (locked, AndroidX 5종만)
```
androidx.appcompat:appcompat:1.7.1
androidx.core:core-ktx:1.13.1
androidx.activity:activity-ktx:1.9.3
androidx.fragment:fragment-ktx:1.8.5
androidx.constraintlayout:constraintlayout:2.2.1
```
**금지 (CLAUDE.md):** Compose, `com.google.android.material`, Hilt/Dagger, Room/DataStore, Navigation Component, RxJava, `kotlinx-coroutines-android`, ktlint/Detekt/Spotless.

### UI / 화면 구조
- **Activity:** 단일 `MainActivity` (theme owner + `dispatchKeyEvent` entry point — Phase 3에서 override).
- **Fragments:** `HomeFragment`(랜딩 + 메뉴 진입 버튼), `MenuFragment`(zoomable 콘텐츠 영역, Phase 4 zoom target), `ChecklistFragment`(Phase 4에서 7기능 표시 — Phase 1은 placeholder).
- **Fragment 전환:** `FragmentManager.beginTransaction().replace()` + 백스택. `NavigationComponent` 미사용. `FragmentContainerView` API 26+ 사용.
- **BottomBar:** `AccessibilityBottomBar` 커스텀 `LinearLayout` (`merge` 레이아웃 + ViewBinding). 4버튼 = 고대비/TTS/확대/축소. 56dp×56dp+ 터치 타깃, 항상 노출 (`activity_main.xml`의 `bottom` 앵커). 클릭 리스너는 setter로 노출하되 Phase 1에서는 빈 람다.
- **ViewBinding:** ON. Compose OFF.

### 상태 영속화 (`A11yPrefs`)
- **위치:** `com.example.a11ydemo.prefs.A11yPrefs`
- **API (locked, Phase 2~4가 import만으로 사용):**
  - `fun init(context: Context)` — `applicationContext` 전용. recreate 좀비 방지(C-2).
  - `var ttsEnabled: Boolean` (default `false`)
  - `var highContrastEnabled: Boolean` (default `false`)
  - `var zoomLevel: Float` (default `1.0f`, range 0.8f~1.5f) — Phase 4에서 회전/recreate 보존 시 savedInstanceState로 갈음 가능 (research §6.2). Phase 1은 read/write API만 노출.
- **Storage:** `SharedPreferences("a11y_prefs", Context.MODE_PRIVATE)`. DataStore 비채택 (외부 의존성 narrative).
- **Init 위치:** `MainActivity.onCreate` 첫 줄에서 `A11yPrefs.init(applicationContext)`. 앱 클래스 별도 생성하지 않음 (간결성).

### contentDescription / 문자열
- **strings.xml:** 모든 라벨/contentDescription을 strings.xml에 등록. 하드코딩 금지(BAR-03 grep 검증).
- **언어:** 한국어 단일 (v1 Out of Scope: i18n).

### Theme / 색상
- **Phase 1 themes.xml:** `Theme.A11yDemo` (light, default). 고대비 variant 및 색상 21:1 팔레트는 Phase 2.
- **Material 미포함:** parent 테마는 `Theme.AppCompat.Light.NoActionBar`. ActionBar 비표시(키오스크 풀스크린 느낌).
- **icon:** 어댑티브 아이콘 placeholder. `mipmap-anydpi-v26/ic_launcher.xml` + `drawable/ic_launcher_background.xml` + `drawable/ic_launcher_foreground.xml` (단색 placeholder). 실제 아이콘 디자인은 v2.

### 검증 환경 (DOC-04)
- **AVD 가이드 위치:** `docs/AVD-SETUP.md` — README placeholder에서 링크. Pixel 6 API 35 + `hw.dPad=yes` + Numpad 매핑 + Google TTS 한국어 사전 설치 단계.
- **수동 검증:** Day 1 마무리 시 ↑↓←→/Enter로 시스템 포커스 이동 동작 확인 후 README에 "검증 완료 (Day 1)" 마크.

### README placeholder (DOC-04 + DOC-01 대비)
- 1스크롤 골격(한 줄 설명 → GIF placeholder → 매핑 표 stub → WPF↔Android 표 stub → 검증 환경 섹션 → 표준 인용 stub) — Phase 5에서 채워질 자리만 마련.

</decisions>

<code_context>
## Existing Code Insights

- 코드베이스 비어있음 (Day 0). `.planning/`, `PRD.md`, `CLAUDE.md`, `android_barrier_free_demo_plan.md`만 존재.
- `.gitignore`에 `*.iml`, `.gradle/`, `build/`, `local.properties`, `.idea/` 등록 완료.
- Git 초기화됨, master branch.

</code_context>

<specifics>
## Specific Pitfalls Watched (M-3 차단)

- **M-3 (D-pad 미작동):** AVD 생성 시 `hw.dPad=yes`를 *config.ini* 기준으로 검증. Day 4 시작 전에 차단되어야 한다.
- **사전 차단:** `docs/AVD-SETUP.md`에 단계별 + screenshot placeholder 문구 + "Day 1 종료 시점에 ↑↓←→/Enter로 임의의 시스템 다이얼로그(예: 설정) 포커스 이동을 1회 수동 확인" DoD 문구 명시.

</specifics>

<deferred>
## Deferred to Later Phases

- Phase 2: TTS attach, themes_high_contrast.xml, contentDescription 누락 보강 패스.
- Phase 3: dispatchKeyEvent override, FocusNavigator, focused selector drawable, XML `nextFocus*` 와이어링.
- Phase 4: ChecklistFragment 7기능 cells, ScaleAnimation/scaleX·scaleY zoom binding, VolumeService.
- Phase 5: README 본문, GIF/MP4, GitHub push, Notion, 위시켓 재제출.

</deferred>
