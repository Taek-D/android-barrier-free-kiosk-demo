# Technology Stack — Android Kotlin 베리어프리 키오스크 데모

**Project:** Android Kotlin 베리어프리 접근성 데모 앱 (위시켓 공고 포트폴리오)
**Researched:** 2026-05-07
**Mode:** Ecosystem (Stack dimension)
**Constraints:** Kotlin + XML View only, Compose 금지, AndroidX 외 0 dependencies, Min SDK 26
**Overall confidence:** HIGH

---

## TL;DR

Pin Kotlin 2.1.x (NOT 2.3.20 latest), AGP 8.7.x with Gradle 8.11.x (NOT AGP 9.x), Java 17 toolchain, compileSdk 35, targetSdk 35, minSdk 26. AndroidX surface = `appcompat:1.7.1` + `core-ktx:1.13.x` + `activity:1.9.x` + `fragment:1.8.x` + `constraintlayout:2.2.1`. Material Components is **optional** — include `material:1.12.0` only if you use `Theme.Material3.*` parents in `themes.xml`. Use **in-app controller pattern** (no `AccessibilityService` system registration); for portfolio honesty, label this clearly in README so the evaluator does not mistake it for a missing requirement.

---

## Stack Recommendations

### Build Toolchain (pin these in `gradle/libs.versions.toml`)

| Component | Recommended Version | Latest in 2026 | Rationale |
|---|---|---|---|
| **Android Gradle Plugin (AGP)** | **8.7.3** | 9.1.1 (Apr 2026) | AGP 9.x is NEW, requires Gradle 9.1 + adds breaking DSL changes (Kotlin/Android target reorganization). For a 6-day demo with min SDK 26, the safest, most-documented path is AGP 8.7.x — the LTS-feel release on Android Studio Ladybug. AGP 9 buys nothing for a View-only Kotlin app and risks Day-1 build hell. |
| **Gradle Wrapper** | **8.11.1** | 9.x | Matches AGP 8.7.x (AGP 8.7 supports Gradle 8.9–8.x). Avoid 9.x entirely. |
| **Kotlin** | **2.1.0** | 2.3.20 (Mar 2026) | 2.1.x is the tested compiler for AGP 8.7. Kotlin 2.3.20 is fine in isolation but pairs cleanly only with AGP 9 — and brings nothing this demo uses (no destructuring, no compiler plugin features needed). Pin 2.1.0 to avoid version-skew warnings. |
| **JDK (toolchain)** | **17 (Temurin)** | 17–26 supported | AGP 8.7 requires JDK 17. Set `kotlin { jvmToolchain(17) }` and `compileOptions { sourceCompatibility/targetCompatibility = JavaVersion.VERSION_17 }`. JDK 21 also works but 17 is the conservative pick. |
| **compileSdk** | **35** | 36 / 36.1 | API 35 (Android 15) is the most-tested compile target as of May 2026 and matches Play Store minimum target floor. compileSdk 36 requires AGP 8.9+ — out of band for our toolchain pin. |
| **targetSdk** | **35** | 35 / 36 | Android 15. Demo is not shipped to Play Store, so the May-2026 targetSdk-36 deadline is irrelevant. 35 keeps behavior changes from API 36 (predictive back, edge-to-edge enforcement) out of scope. |
| **minSdk** | **26** | — | Android 8.0 Oreo. Per PRD constraint. Confirmed compatible with every library below. |
| **buildToolsVersion** | **35.0.0** | 36.0.0 | Pairs with compileSdk 35. |

**Confidence: HIGH** — Verified against [Android Developers AGP release notes](https://developer.android.com/build/releases/agp-9-1-0-release-notes) and [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html).

> **NOTE on the "use latest" temptation:** The researcher initially considered AGP 9.1.1 + Kotlin 2.3.20 (both stable as of May 2026). Rejected because: (a) breaking DSL changes in AGP 9 ([migration guide](https://blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9/)) cost time on a 6-day clock with zero feature payoff for View-based code, and (b) the eventual client codebase will likely be on AGP 8.x — using a bleeding-edge toolchain in a "portability proof" demo is self-defeating.

---

### AndroidX Dependencies (the entire allowlist)

All versions confirmed minSdk-compatible (every library below supports API ≤ 21).

| Library | Version | Purpose | Required? | Rationale |
|---|---|---|---|---|
| `androidx.appcompat:appcompat` | **1.7.1** | `AppCompatActivity`, theme attribute resolution, `setTheme()` + `recreate()` patterns | **MUST** | Backbone for `Theme.AppCompat.*` parents that drive theme switching. Provides `AppCompatDelegate.setDefaultNightMode()` if you ever want to layer night-mode on top of the high-contrast toggle. |
| `androidx.core:core-ktx` | **1.13.1** | Kotlin extensions on `Context`, `View`, `Bundle`, etc. | **MUST** | Tiny, ubiquitous, used implicitly by appcompat. Pin 1.13.1 (compileSdk 35-compatible) instead of 1.18.0 (which targets compileSdk 36.1) to match toolchain. |
| `androidx.activity:activity-ktx` | **1.9.3** | `OnBackPressedDispatcher`, `viewModels()` (if needed), modern `Activity` lifecycle | **MUST** | Required transitively by fragment 1.8.x. Pin to 1.9.3 (compileSdk-35-clean) over 1.12.4. |
| `androidx.fragment:fragment-ktx` | **1.8.5** | `FragmentManager`, `commit { … }` DSL, `viewModels()` | **MUST** | The PRD explicitly uses `HomeFragment` / `MenuFragment` / `ChecklistFragment`. KTX gives idiomatic Kotlin transactions. |
| `androidx.constraintlayout:constraintlayout` | **2.2.1** | `ConstraintLayout` for the bottom-bar + content split | **MUST** | Standard for the always-visible bottom bar pattern (anchor bottom bar, anchor `FragmentContainerView` above it). Min API 14 — comfortably below 26. |
| `com.google.android.material:material` | **1.12.0** *(optional)* | `MaterialButton`, `MaterialCardView`, focus ripple defaults | **OPTIONAL — consider dropping** | If you keep `Theme.AppCompat.*` (or `Theme.MaterialComponents.*` only when needed) you can omit Material entirely. The demo's "external libraries = 0" narrative is *cleaner* without it. **Recommendation: omit.** Use plain `Button` + your own `state_focused` selector — that is exactly what you need to demonstrate. If you must include it, 1.12.0 is the safe pin (1.13.0 came out late 2025 and is also fine). |

**Total surface if Material is dropped: 5 AndroidX artifacts. Zero non-AndroidX dependencies. This is exactly the "portability proof" the PRD targets.**

**Confidence: HIGH** — Versions cross-checked on [Maven Central via mvnrepository](https://mvnrepository.com/artifact/androidx.appcompat/appcompat) and [Android Developers AndroidX releases](https://developer.android.com/jetpack/androidx/versions).

---

### Suggested `gradle/libs.versions.toml`

```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
appcompat = "1.7.1"
coreKtx = "1.13.1"
activity = "1.9.3"
fragment = "1.8.5"
constraintlayout = "2.2.1"

[libraries]
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-activity-ktx = { group = "androidx.activity", name = "activity-ktx", version.ref = "activity" }
androidx-fragment-ktx = { group = "androidx.fragment", name = "fragment-ktx", version.ref = "fragment" }
androidx-constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

### Suggested `app/build.gradle.kts` (excerpt)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.barrierfree"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.barrierfree"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }

    buildFeatures {
        viewBinding = true     // Strongly recommended — see ARCHITECTURE.md
        compose = false        // Explicit. Compose is forbidden.
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    // Material intentionally omitted to honor "0 external libs" narrative.
}
```

---

### AccessibilityService vs In-App Controller — DECISION

**Recommendation: in-app controller only. Do NOT register a system `AccessibilityService`. Confidence: HIGH.**

| Dimension | In-app controller (RECOMMENDED) | System `AccessibilityService` |
|---|---|---|
| Maps to PRD requirement | ✅ All 7 features (TTS-on-focus, theme, D-pad, focus indicator, volume, scale, bottom bar) implementable via plain `View` APIs | ❌ Overkill — service-level APIs (`AccessibilityEvent` interception, global gestures) target *other* apps, not your own UI |
| Time to demo | Hours | Days (manifest, service config XML, runtime permission flow, Settings deep-link, user re-enable after reboot) |
| Portfolio narrative | "I implemented in-app accessibility controls cleanly" | Risk: evaluator sees `BIND_ACCESSIBILITY_SERVICE` permission and assumes it is misused (Google flags this aggressively per [BrowserStack: Impact of Accessibility Permission](https://www.browserstack.com/guide/accessibility-permission-in-android)) |
| Job-posting fit | Posting says "Accessibility Service 제어 경험" — ambiguous Korean phrasing covers BOTH custom-in-app a11y work AND system-level service. Demonstrating the in-app surface plus a *clearly-labeled Phase 2 stub* for system service is the safer read | If evaluator interprets it strictly as system service, only this satisfies — but you can hedge with a README section |
| Demo-ability on emulator | ✅ Works out of the box | ⚠ Requires Settings → Accessibility → enable per-install, which is awkward in a 30-second README scan |

**Hedge for the portfolio README:**
Add an explicit "Scope" section:
> *"This demo implements **in-app accessibility controls** — the same APIs that an `AccessibilityService` consumes (`View.contentDescription`, `OnFocusChangeListener`, `dispatchKeyEvent`), exercised from the host app. A separate Phase 2 branch (planned, not in MVP) registers a system `AccessibilityService` to broadcast these events platform-wide."*

This reframes the omission as a *deliberate scoping choice*, not a gap.

**Sources:** [Android Developers — Create an accessibility service](https://developer.android.com/guide/topics/ui/accessibility/service), [Android Developers — Principles for improving app accessibility](https://developer.android.com/guide/topics/ui/accessibility/principles).

---

## What NOT to Use

| Excluded | Why | What to do instead |
|---|---|---|
| **Jetpack Compose** | PRD constraint — client codebase likely View-based; Compose import in a "portability" demo undermines the narrative | XML layouts + ViewBinding |
| **Hilt / Dagger** | DI framework adds an opaque external dependency; demo has 3 services (`TtsService`, `ThemeService`, `VolumeService`) — manual constructor injection or `object` singletons are simpler and obviously correct | Plain `object` for services or pass via Activity |
| **Room** | No persistent data in MVP; `SharedPreferences` covers theme/TTS-on state if P2-1 lands | `getSharedPreferences()` directly |
| **RxJava / Coroutines extras (`kotlinx-coroutines-android`)** | TTS callbacks are simple `OnInitListener` and `UtteranceProgressListener` — no async orchestration needed. Coroutines core is bundled with Kotlin stdlib but the `-android` artifact is a *separate* dependency you should not import | Plain callbacks; if you really need a coroutine, use the stdlib `kotlin.coroutines` (no extra dep) |
| **Third-party TTS wrappers** (e.g., `tts-utils`, etc.) | Whole point of the demo is "I drove `android.speech.tts.TextToSpeech` directly" | Native `TextToSpeech` API |
| **AGP 9.x / Gradle 9.x** | Breaking DSL migration on a 6-day clock; zero payoff for View-only code | AGP 8.7.3 + Gradle 8.11.1 |
| **Kotlin 2.3.x** | Pair-tested with AGP 9 only; mismatched Kotlin/AGP versions emit warnings that distract evaluators reading the build log | Kotlin 2.1.0 |
| **Compose-only AndroidX libraries** (`compose-runtime`, `compose-ui`, `compose-material3`, `activity-compose`) | Forbidden by PRD | n/a |
| **Material Components for Android** (`com.google.android.material:material`) | Optional. Adds a transitive dep that muddies the "0 external libs (AndroidX only)" claim. Strictly speaking, `com.google.android.material` is *not* `androidx.*` despite shipping alongside AndroidX | Use `Theme.AppCompat.*` parents and plain `Button` |
| **Navigation Component** (`androidx.navigation:*`) | 3 fragments + manual `FragmentManager` swaps is trivial; Navigation adds a graph XML, SafeArgs plugin, and another moving part | `supportFragmentManager.commit { replace(R.id.content, …) }` |
| **ViewPager2 / RecyclerView** | Not needed for the 3-fragment + bottom-bar demo. If `ChecklistFragment` lists 7 items, a `LinearLayout` inside a `ScrollView` is faster to write and easier to focus-debug than RecyclerView | Plain `LinearLayout` + `ScrollView` |
| **Crashlytics / Firebase / analytics** | Demo, not a production app. Adds external deps. | None |
| **Detekt / ktlint / spotless** | Lint plugins are noise on a 6-day demo. Android Studio's built-in inspections suffice | Built-in IDE inspections |
| **Coroutines `lifecycle-runtime-ktx` / `lifecycle-viewmodel-ktx`** | Lifecycle observation is not in MVP scope. If you add `repeatOnLifecycle` later, prefer it via plain Activity callbacks for now | `onStart`/`onStop` overrides |
| **System `AccessibilityService`** registration in the manifest | See decision section above — wrong tool for an in-app demo, triggers Play-Store-style permission scrutiny that confuses evaluators | In-app controller pattern |

---

## Confidence Levels

| Recommendation | Level | Justification |
|---|---|---|
| AGP 8.7.3 + Gradle 8.11.1 | HIGH | Verified against Android Developers release notes; widely deployed; documented compatibility. |
| Kotlin 2.1.0 | HIGH | Stable since late 2024; canonical pairing with AGP 8.7. |
| compileSdk/targetSdk 35, minSdk 26 | HIGH | API 35 = Android 15, well-supported by AGP 8.7; minSdk 26 confirmed by every library above. |
| `appcompat 1.7.1` + `core-ktx 1.13.1` + `activity 1.9.3` + `fragment 1.8.5` + `constraintlayout 2.2.1` | HIGH | All cross-verified on Maven Central; min API ≤ 21 for each. |
| Drop Material Components | MEDIUM | Defensible narrative but evaluator preference unknown — if they expect Material defaults (ripple, elevation), plain Button looks "older". Either choice works; I lean drop. |
| In-app controller, no system AccessibilityService | HIGH | Matches PRD scope, posting requirement language is ambiguous enough to cover, hedged via README "Scope" section. |
| ViewBinding ON, no Navigation Component, no DI | HIGH | 3-fragment app does not justify the additional moving parts; ViewBinding pays for itself even at this size. |

---

## Open Questions

1. **Does the evaluator expect Material visual style?** If they open a screenshot and the buttons look "stock pre-Material," that may cost a point regardless of the cleaner-deps narrative. Mitigation: include 1 screenshot with Material parent theme and 1 without; pick whichever looks better. Owner: 본인. Deadline: Day 5 (visual polish).
2. **Is "Accessibility Service 제어 경험" in the posting strictly the system service?** Cannot resolve without contacting the client. The README "Scope" hedge is the cheapest insurance. Owner: 본인 (지원 후 위시켓 메시지). Deadline: post-submission.
3. **Should AGP/Kotlin pin shift to 8.9 + 2.1.20 to enable compileSdk 36?** Only if the evaluator's environment is on Android Studio Narwhal/Otter (Spring 2026). Mitigation: include `gradle.properties` snippet showing how to bump; do not bump in the committed code. Owner: 본인. Deadline: Day 6 README.
4. **Stable `androidx.fragment` exact version — 1.8.5 vs 1.8.9?** Fragment 1.8.x has had multiple patch releases; the latest is 1.8.9 per Aug 2025 search. 1.8.5 is conservative and proven. Either is HIGH confidence. Mitigation: pin 1.8.9 if you want the most recent; 1.8.5 if you want maximum stability with appcompat 1.7.1. **Default: 1.8.5.**

---

## Sources

- [Android Gradle plugin 9.1.1 release notes — Android Developers](https://developer.android.com/build/releases/agp-9-1-0-release-notes) — verified 2026-04 release & Gradle 9.1 / JDK 17 hard requirement
- [Android Gradle plugin 9.0.1 release notes — Android Developers](https://developer.android.com/build/releases/agp-9-0-0-release-notes)
- [About Android Gradle plugin — Android Developers](https://developer.android.com/build/releases/about-agp)
- [Update your Kotlin projects for AGP 9.0 — JetBrains Blog](https://blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9/) — confirms Kotlin/AGP version pairing
- [Kotlin 2.3.20 Released — JetBrains Blog](https://blog.jetbrains.com/kotlin/2026/03/kotlin-2-3-20-released/)
- [Kotlin release process — kotlinlang.org](https://kotlinlang.org/docs/releases.html)
- [Gradle Compatibility Matrix](https://docs.gradle.org/current/userguide/compatibility.html) — JDK 17 minimum for Gradle 9
- [Java versions in Android builds — Android Developers](https://developer.android.com/build/jdks)
- [AndroidX releases — Android Developers](https://developer.android.com/jetpack/androidx/versions)
- [Appcompat releases — Android Developers](https://developer.android.com/jetpack/androidx/releases/appcompat) — 1.7.1 stable
- [Maven: androidx.appcompat:appcompat](https://mvnrepository.com/artifact/androidx.appcompat/appcompat)
- [Fragment releases — Android Developers](https://developer.android.com/jetpack/androidx/releases/fragment)
- [Maven: androidx.fragment:fragment-ktx](https://mvnrepository.com/artifact/androidx.fragment/fragment-ktx)
- [Maven: androidx.core:core-ktx](https://mvnrepository.com/artifact/androidx.core/core-ktx)
- [Maven: androidx.activity:activity-ktx](https://mvnrepository.com/artifact/androidx.activity/activity-ktx)
- [ConstraintLayout releases — Android Developers](https://developer.android.com/jetpack/androidx/releases/constraintlayout) — 2.2.1 stable
- [Material Components for Android releases — GitHub](https://github.com/material-components/material-components-android/releases) — 1.13.0 stable, 1.12.0 LTS-feel
- [Meet Google Play's target API level requirement — Android Developers](https://developer.android.com/google/play/requirements/target-sdk)
- [Create an accessibility service — Android Developers](https://developer.android.com/guide/topics/ui/accessibility/service) — basis for in-app-controller decision
- [Principles for improving app accessibility — Android Developers](https://developer.android.com/guide/topics/ui/accessibility/principles)
- [Impact of Accessibility Permission in Android Apps — BrowserStack](https://www.browserstack.com/guide/accessibility-permission-in-android) — Play Store policy scrutiny on `BIND_ACCESSIBILITY_SERVICE`
