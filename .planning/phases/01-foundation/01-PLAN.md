# Phase 1: Foundation — Plan

**Phase:** 01 — Foundation
**Goal:** Day 1에 빌드 가능한 Kotlin/AndroidX 골격 + `A11yPrefs` 영속화 + AVD 가이드 + README placeholder
**Risk:** 🟢 Low (M-3만 watch)
**Requirements:** BUILD-01/02/03, BAR-01/02/03, DOC-04

---

## Task Breakdown

순서가 중요한 곳은 의존성 화살표로 명시. 그 외는 병렬 가능.

```
T1 (Gradle 골격) ──> T2 (Manifest + Theme) ──> T3 (MainActivity + Fragments)
                                            └─> T4 (BottomBar 커스텀 뷰)
T5 (A11yPrefs) ──────────────────────────────> T3 (MainActivity.onCreate에서 init)
T6 (strings/dimens/colors) ──> T2, T4
T7 (Adaptive icon placeholder) ──> T2
T8 (AVD 가이드) (independent)
T9 (README placeholder) (independent)
T10 (VERIFICATION.md + manual checks)
```

---

### T1. Gradle 골격
- `settings.gradle.kts` — pluginManagement(google + mavenCentral) + dependencyResolutionManagement(FAIL_ON_PROJECT_REPOS) + `include(":app")` + `rootProject.name = "AndroidBarrierFreeKioskDemo"`.
- `build.gradle.kts` (root) — `plugins {}`에 AGP 8.7.3, Kotlin 2.1.0 `apply false`.
- `gradle.properties` — JVM args + AndroidX + Kotlin code style + non-transitive R class OFF (단일 모듈) + Jetifier OFF.
- `gradle/wrapper/gradle-wrapper.properties` — `distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip`.
- `app/build.gradle.kts` — `com.android.application` + `org.jetbrains.kotlin.android`. `compileSdk=35 / minSdk=26 / targetSdk=35`. ViewBinding ON. JDK 17. AndroidX 5종 implementation. `applicationId "com.example.a11ydemo"` + versionCode 1 / versionName "0.1.0".
- `app/proguard-rules.pro` — empty (release minify OFF in Phase 1).

### T2. Manifest + 기본 테마
- `app/src/main/AndroidManifest.xml` — `<application>` `android:theme="@style/Theme.A11yDemo"` + `MainActivity` launcher intent + `android:exported="true"`.
- `res/values/themes.xml` — `Theme.A11yDemo` parent `Theme.AppCompat.Light.NoActionBar`. 색상 attr는 colors.xml 참조. 고대비 variant는 Phase 2.

### T3. MainActivity + 3 Fragment
- `MainActivity.kt` — `AppCompatActivity`. `onCreate` 순서: `A11yPrefs.init(applicationContext)` → `super.onCreate(...)` → `setContentView`. `savedInstanceState == null` 가드로 HomeFragment commit. `onBackPressed`는 default. Phase 3에서 `dispatchKeyEvent` override 추가 (TODO 주석으로 자리 표시).
- `HomeFragment.kt` — TextView(앱 소개) + Button("메뉴 열기") → MenuFragment 전환, Button("체크리스트") → ChecklistFragment 전환. Fragment 전환은 `parentFragmentManager.beginTransaction().replace().addToBackStack().commit()`.
- `MenuFragment.kt` — 줌 대상 컨테이너(`@+id/menu_zoom_target`) + sample 메뉴 버튼 4~6개(예: "아메리카노", "라떼", "녹차", "주문 확인"). Phase 4에서 zoom 적용.
- `ChecklistFragment.kt` — placeholder TextView "Phase 4에서 7기능 체크리스트 표시" + 뒤로가기 버튼.

### T4. AccessibilityBottomBar 커스텀 뷰
- `ui/view/AccessibilityBottomBar.kt` — `LinearLayout(horizontal)` 서브클래스. `init`에서 `LayoutInflater.from(context).inflate(R.layout.view_accessibility_bottom_bar, this, true)`. ImageButton 4개를 ID로 lookup하고 setter 4개 (`setOnTtsClick`, `setOnHighContrastClick`, `setOnZoomInClick`, `setOnZoomOutClick`)를 노출. 클릭 핸들러는 Phase 2~4에서 attach.
- `res/layout/view_accessibility_bottom_bar.xml` — `<merge>` root, 4개 ImageButton 균등 분배 (`layout_weight=1`). `minWidth=56dp / minHeight=56dp / android:contentDescription=@string/...`. `android:background="?selectableItemBackground"`.
- `res/drawable/ic_a11y_*.xml` — 단순 vector placeholder 4종(circle/square/plus/minus 식별 가능 수준). 디자인은 v2.
- `activity_main.xml` — ConstraintLayout. `FragmentContainerView`(`app:defaultNavHost="false"`, layout_constraintBottom_toTopOf=BottomBar, top=parent) + `AccessibilityBottomBar`(layout_constraintBottom_toBottomOf=parent, height=`@dimen/a11y_bottom_bar_height`).

### T5. A11yPrefs
- `prefs/A11yPrefs.kt` — `object`. `private lateinit var prefs: SharedPreferences`. `fun init(context: Context)` idempotent (이미 초기화된 경우 no-op). 3개 var(`ttsEnabled`, `highContrastEnabled`, `zoomLevel`)는 getter/setter에서 prefs 직접 read/write. zoomLevel은 setter에서 0.8f~1.5f clamp.
- 단위 검증: 인스턴스 메서드 호출 시 `applicationContext`만 사용해 액티비티 누수 방지(C-2 spirit).

### T6. strings / dimens / colors
- `res/values/strings.xml` — 앱 이름, Fragment 타이틀, BottomBar 4개 contentDescription, sample 메뉴 라벨, 체크리스트 placeholder 등. 모두 한국어.
- `res/values/dimens.xml` — `a11y_bottom_bar_height=72dp` (56dp 버튼 + 8dp 상하 padding 여유), `a11y_min_touch=56dp`, `a11y_focus_stroke=3dp` (Phase 3 사전 등록).
- `res/values/colors.xml` — Phase 1 light palette만(brand primary/onPrimary, surface, onSurface, focus_color stub). 21:1 고대비 팔레트는 Phase 2.

### T7. Adaptive icon placeholder
- `res/mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` — `<adaptive-icon>` foreground/background 참조.
- `res/drawable/ic_launcher_background.xml` — solid color rectangle.
- `res/drawable/ic_launcher_foreground.xml` — vector "A11" 텍스트 placeholder (단색).

### T8. AVD 가이드
- `docs/AVD-SETUP.md` — Pixel 6 / API 35 / `hw.dPad=yes` 설정법(Android Studio AVD Manager → Show Advanced → Hardware → DPad: yes), Numpad 키 매핑, Google TTS 한국어 데이터 설치 단계, Day 1 DoD 체크리스트.

### T9. README placeholder
- `README.md` — 한 줄 설명 + GIF placeholder 자리 + 매핑 표 stub + 표준 준수 stub + Scope stub + 검증 환경(완성) + 빌드 방법(`gradle wrapper --gradle-version 8.11.1` 1회 + `./gradlew build`).

### T10. VERIFICATION
- `.planning/phases/01-foundation/01-VERIFICATION.md` — Phase 1 success criteria 1~5 자동/수동 검증 결과. 자동(grep `contentDescription`, build) + 수동(AVD D-pad 동작) 분리.

---

## Risks

- **R-1 (gradle-wrapper.jar 부재):** 텍스트 도구로 jar 생성 불가. 대응: README/AVD 가이드에 1회용 `gradle wrapper --gradle-version 8.11.1` 또는 Android Studio "Sync Project" 안내. BUILD-01은 사용자 환경에서 검증 (autonomous는 산출물까지).
- **R-2 (어댑티브 아이콘 미생성 시 빌드 fail):** `mipmap-anydpi-v26` + `ic_launcher_background/foreground` drawable 모두 작성하면 회피. 회피책 수동 검증.
- **R-3 (M-3 D-pad 미동작):** Day 4 위험 차단. 대응: T8 AVD 가이드 + Day 1 종료 시 사용자 수동 확인 마킹.

---

## Definition of Done (Phase 1)

1. `app/`, root Gradle, wrapper.properties, AVD 가이드, README placeholder 모든 파일 생성됨.
2. CONTEXT.md/PLAN.md/VERIFICATION.md 작성 + commit.
3. `app/src/main/java/.../prefs/A11yPrefs.kt` import만으로 Phase 2 ThemeService/TtsService가 토글 영속화 가능.
4. `view_accessibility_bottom_bar.xml`이 모든 Fragment에서 동일 위치/크기로 노출됨 (activity_main에 단일 인스턴스).
5. grep `contentDescription|android:text` 검증으로 인터랙티브 뷰 전부 매칭(Phase 1 화면 한정).
6. `docs/AVD-SETUP.md`에 `hw.dPad=yes` 단계 + Day 1 수동 DoD 체크박스 명시.
