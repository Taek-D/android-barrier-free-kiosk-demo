# Architecture Patterns

**Domain:** Android Kotlin 키오스크 접근성(베리어프리) 데모 앱 (포트폴리오)
**Researched:** 2026-05-07
**Constraints:** Kotlin + AndroidX View/XML (Compose 금지), Min SDK 26, 외부 의존성 0, 단독 1인 6일, 백엔드/네트워킹/영속성 없음 (SharedPreferences만 허용)
**Optimization target:** 평가자(GitHub README 30초 스캔) 가독성 + 1:1 공고 매핑 + 6일 안에 완성

---

## 1. Recommended Architecture (한눈에)

```
┌──────────────────────────────────────────────────────────────────────┐
│ MainActivity (single host)                                           │
│ ─ Theme owner: setTheme() + recreate()                              │
│ ─ KeyEvent entry: dispatchKeyEvent() → FocusNavigator.handle()      │
│ ─ Service holder: TtsService, ThemeService, VolumeService 인스턴스   │
│ ─ FragmentContainerView + AccessibilityBottomBar (always visible)   │
└─────────────┬─────────────────────────┬──────────────────────────────┘
              │                         │
   ┌──────────▼──────────┐    ┌─────────▼──────────────────┐
   │ NavHost (Fragment)  │    │ AccessibilityBottomBar     │
   │ ─ HomeFragment      │    │ (custom View)              │
   │ ─ MenuFragment      │    │ ─ 4 buttons: HC/TTS/+/−    │
   │ ─ ChecklistFragment │    │ ─ delegates to services    │
   │ each: own focus     │    │ ─ never owns state itself  │
   │ order via XML       │    └────────────────────────────┘
   └─────────────────────┘
              │
              ▼
   ┌────────────────────────────────────────────────────────────┐
   │ Services (singletons / Activity-scoped objects)            │
   │ ─ TtsService     : TTS lifecycle + speakIfChanged          │
   │ ─ ThemeService   : isHighContrast flag + recreate trigger  │
   │ ─ VolumeService  : AudioManager STREAM_MUSIC adjust        │
   │ ─ FocusNavigator : KeyEvent → focusSearch dispatcher       │
   └────────────────────────────────────────────────────────────┘
              │
              ▼
   ┌────────────────────────────────────────────────────────────┐
   │ Persistence: SharedPreferences (single file: "a11y_prefs") │
   │ ─ key_high_contrast : Boolean                              │
   │ ─ key_tts_enabled   : Boolean                              │
   └────────────────────────────────────────────────────────────┘
```

**Single-rule mental model the evaluator should grasp in 30s:**
- **MainActivity** owns process-wide accessibility *state and entry points*.
- **Fragments** own *content and per-screen focus order* (declared in XML).
- **Services** own *side effects* (TTS speaking, audio volume, theme application).
- **FocusNavigator** is a stateless utility — pure function from KeyEvent → focused-View change.
- **AccessibilityBottomBar** is a "dumb" custom View — emits intents to MainActivity, never holds state.

---

## 2. Component Boundaries & State Ownership

| Component | Owns | Does NOT own | Survives across recreate()? |
|---|---|---|---|
| **MainActivity** | KeyEvent dispatch, current theme application, service instances, Fragment back stack host, BottomBar instance | Per-screen layout, focus order details, user-visible content | No (recreated). State restored via SharedPreferences + savedInstanceState. |
| **HomeFragment / MenuFragment / ChecklistFragment** | Per-screen XML layout, per-screen focus order (`android:nextFocus*` + `android:focusable`), per-screen contentDescription | Theme, TTS engine, volume, key dispatch | No (recreated with Activity). FragmentManager restores back stack automatically if `setRetainInstance` is NOT used (we don't use it). |
| **AccessibilityBottomBar (custom View)** | 4 button layout, contentDescription, click→callback wiring | Whether TTS is on, whether HC is on, current volume value | Re-inflated. Visual state must be re-derived from services on `onAttachedToWindow`. |
| **TtsService** | TextToSpeech instance, `lastSpoken`/`lastSpokenTime` debounce state, `isEnabled` toggle, `isInitialized` guard | What text to speak (caller decides), focus listeners (attached by Fragments) | If declared as `object` (singleton) → survives. **Recommended.** Must `shutdown()` on Application terminate (or accept leak for demo). |
| **ThemeService** | `isHighContrast: Boolean`, `apply(activity)` method | Activity lifecycle, recreate timing | `object` singleton — survives recreate. Persists via SharedPreferences for cold start. |
| **VolumeService** | AudioManager reference, step size constant | Volume value (read live from AudioManager — no local cache) | `object` singleton, stateless beyond AudioManager handle. |
| **FocusNavigator** | Nothing (pure dispatcher) | All state | Stateless `object` — N/A. |

**Why singletons (`object`) for services?**
- Survive `Activity.recreate()` automatically — `lastSpoken` debounce isn't reset on theme toggle.
- Zero DI overhead — no Hilt/Koin pulls in dependencies (violates "0 external libs" constraint).
- Demo-scale; no testability sacrifice that matters in 6 days.
- Trade-off: Application-scoped lifetime means TTS engine is never explicitly shut down. For a demo, acceptable; document in README under "Known limitations" if asked.

---

## 3. Data Flow Per Accessibility Feature

### 3.1 Theme Toggle (High Contrast)

```
User taps HC button on AccessibilityBottomBar
   │
   ▼
BottomBar.onClick → callback to MainActivity (interface or lambda)
   │
   ▼
MainActivity.onHighContrastClicked()
   │
   ├─► ThemeService.toggle()         (flips isHighContrast, writes SharedPreferences)
   │
   └─► activity.recreate()            (Android destroys & rebuilds Activity + Fragments)
         │
         ▼
   MainActivity.onCreate() runs again
         │
         ├─► ThemeService.apply(this) BEFORE setContentView()  ← critical order
         │     (reads SharedPreferences, calls setTheme(R.style.Theme_AccessKit_HighContrast))
         │
         ├─► setContentView(R.layout.activity_main)
         │
         └─► FragmentManager auto-restores back stack & current Fragment
```

**Critical ordering rule:** `setTheme()` MUST be called before `setContentView()` in `onCreate()`. Otherwise the theme applies to nothing.

### 3.2 TTS on Focus

```
User presses DPAD_DOWN (or taps)
   │
   ▼
View.requestFocus() (or focusSearch result)
   │
   ▼
View fires OnFocusChangeListener (attached by Fragment.onViewCreated)
   │
   ▼
TtsService.speakIfChanged(view.contentDescription ?: view.text ?: tag)
   │
   ├─► If !isEnabled → return
   ├─► If !isInitialized → return (guard against pre-init focus events)
   ├─► If text == lastSpoken && now - lastSpokenTime < 500ms → return (debounce)
   └─► tts.speak(text, QUEUE_FLUSH, null, "tts_focus_<id>")
```

**Who attaches listeners?** Each Fragment in `onViewCreated()` walks its focusable views and calls `TtsService.attachToView(v)`. Centralized in a small extension function `View.attachA11ySpeak()` to avoid copy-paste.

**Why not in MainActivity?** Fragments know which views are focusable in their layout; MainActivity shouldn't reach into Fragment view trees.

### 3.3 KeyEvent → Focus Movement

```
External keypad / emulator D-pad press
   │
   ▼
Android dispatches to Activity.dispatchKeyEvent(event)
   │
   ▼
MainActivity.dispatchKeyEvent(event)  ← OVERRIDDEN here, not in Fragments
   │
   ├─► If event.action != ACTION_DOWN → super.dispatchKeyEvent(event)
   │
   └─► FocusNavigator.handle(currentFocus, event.keyCode)
         │
         ├─► DPAD_UP/DOWN/LEFT/RIGHT → currentFocus.focusSearch(direction)?.requestFocus()
         ├─► DPAD_CENTER / ENTER     → currentFocus.performClick()
         └─► returns true if handled, else false → falls back to super
```

**Why MainActivity (not per-Fragment)?** Single source of truth; one place to read for evaluators; works regardless of which Fragment is currently shown. `currentFocus` is Activity-wide and naturally points into whichever Fragment view tree is currently focused.

**Why not register a custom KeyListener on root view?** `dispatchKeyEvent` is the documented Android entry point and matches WPF `KeyboardFocusBehavior` 1:1 in the README mapping table — better portfolio narrative.

### 3.4 Volume Adjust

```
User taps + or − button on BottomBar (or hardware volume keys — out of scope)
   │
   ▼
BottomBar callback → MainActivity → VolumeService.adjust(+1 / −1)
   │
   ▼
audioManager.adjustStreamVolume(STREAM_MUSIC, RAISE/LOWER, FLAG_SHOW_UI)
```

Stateless. No ViewModel needed.

### 3.5 ScaleAnimation (Content Zoom)

Owned by the Fragment that hosts the zoomable region (e.g., `MenuFragment`). MainActivity's BottomBar zoom buttons emit a callback; current Fragment (queried via `supportFragmentManager.primaryNavigationFragment` or a simple interface) reacts.

```
BottomBar.zoomIn → MainActivity.onZoomIn()
   │
   ▼
(currentFragment as? Zoomable)?.zoomIn()    ← interface
   │
   ▼
Fragment runs ScaleAnimation on its content root
```

**Interface:** `interface Zoomable { fun zoomIn(); fun zoomOut() }` — implemented by Fragments that have a zoomable region. HomeFragment may no-op.

---

## 4. Suggested Package Layout (Validated + Refined)

The plan §4-1 layout is **good and should be kept**. Two refinements:

```
app/src/main/java/com/example/a11ydemo/
├── MainActivity.kt                       ← single Activity, theme + key entry
│
├── ui/
│   ├── fragment/
│   │   ├── HomeFragment.kt
│   │   ├── MenuFragment.kt               ← also implements Zoomable
│   │   └── ChecklistFragment.kt          ← evidence screen for evaluators
│   │
│   └── view/
│       └── AccessibilityBottomBar.kt     ← custom View, exposes 4 callbacks
│
├── service/                              ← side-effect singletons
│   ├── TtsService.kt
│   ├── ThemeService.kt
│   └── VolumeService.kt
│
├── accessibility/                        ← pure focus/key utilities
│   ├── FocusNavigator.kt
│   └── A11yViewExt.kt                    ← View.attachA11ySpeak() etc. ★ NEW
│
└── prefs/                                ← single file, single concern  ★ NEW
    └── A11yPrefs.kt                      ← SharedPreferences wrapper
```

```
app/src/main/res/
├── layout/
│   ├── activity_main.xml                  (FragmentContainerView + <include bottom_bar>)
│   ├── view_accessibility_bottom_bar.xml  ← merge/include layout
│   ├── fragment_home.xml
│   ├── fragment_menu.xml
│   └── fragment_checklist.xml
│
├── drawable/
│   ├── focused_background.xml             (state_focused selector, 3dp+ stroke)
│   └── ic_*.xml                           (button icons)
│
├── values/
│   ├── colors.xml
│   ├── strings.xml                        (ALL contentDescription here, not inline)
│   ├── styles.xml
│   ├── themes.xml                         (Theme.AccessKit — base)
│   └── themes_high_contrast.xml           (Theme.AccessKit.HighContrast — WCAG AA)
│
└── (no values-night/)                     ← we drive theme manually, not by system
```

**Why two new packages:**
- `accessibility/A11yViewExt.kt` keeps the per-Fragment `attachToView` boilerplate as a single 5-line extension. Easier scan, easier reuse.
- `prefs/A11yPrefs.kt` (a tiny `object` with `var isHighContrast` / `var isTtsEnabled` that read/write SharedPreferences) means MainActivity's `onCreate` is 2 lines instead of 8 of `getSharedPreferences(...).getBoolean(...)` noise.

**Package naming for evaluator clarity:** `service/` and `accessibility/` are intentionally named to be self-explanatory in a GitHub file tree. An evaluator scrolling the repo sees the right boxes immediately and matches them to the 7 requirements.

---

## 5. Build Order (6-Day Implementation Sequence)

The build order is dictated by dependencies between components and by what unblocks visual verification earliest.

```
┌─ Day 1 ─────────────────────────────────────────────────────────────┐
│ 1. Project skeleton: MainActivity, 3 empty Fragments, nav stub      │
│ 2. AccessibilityBottomBar custom View (visual only, no callbacks)   │
│ 3. activity_main.xml with FragmentContainerView + bottom bar        │
│ 4. A11yPrefs (SharedPreferences wrapper) — needed by ThemeService   │
│ DoD: app builds, navigates Home/Menu/Checklist, bottom bar visible  │
└─────────────────────────────────────────────────────────────────────┘

┌─ Day 2 ─ TTS first (because focus listeners depend on it) ──────────┐
│ 5. TtsService (init, isInitialized guard, speakIfChanged debounce)  │
│ 6. A11yViewExt.attachA11ySpeak() extension                          │
│ 7. contentDescription on every focusable view in 3 Fragments        │
│ 8. BottomBar TTS toggle button → TtsService.isEnabled               │
│ DoD: tap-focus on any view → audible label; toggle silences         │
└─────────────────────────────────────────────────────────────────────┘

┌─ Day 3 ─ Theme + recreate (highest risk for state loss) ────────────┐
│ 9.  themes.xml + themes_high_contrast.xml (WCAG AA verified)        │
│ 10. ThemeService.apply() + toggle() + recreate()                    │
│ 11. MainActivity.onCreate: ThemeService.apply(this) BEFORE setCV()  │
│ 12. Verify Fragment back stack survives recreate                    │
│ DoD: HC toggle flips colors instantly across all 3 screens          │
└─────────────────────────────────────────────────────────────────────┘

┌─ Day 4 ─ Focus navigation (key risk: emulator D-pad quirks) ────────┐
│ 13. FocusNavigator.handle() (pure function, easy to unit-test if    │
│     time permits)                                                   │
│ 14. MainActivity.dispatchKeyEvent override                          │
│ 15. focused_background.xml selector + apply via theme attr or       │
│     android:background on focusables                                │
│ 16. Per-Fragment XML focus order: android:nextFocusDown/Up/Left/    │
│     Right + android:focusable="true" + initial focus                │
│ DoD: D-pad arrows traverse all focusables in logical order;         │
│      ENTER triggers click; focus ring visible at all times          │
└─────────────────────────────────────────────────────────────────────┘

┌─ Day 5 ─ Remaining features ────────────────────────────────────────┐
│ 17. VolumeService + BottomBar +/− buttons                           │
│ 18. Zoomable interface + MenuFragment.zoomIn/Out (ScaleAnimation)   │
│ 19. ChecklistFragment: 7-item list with live state from services    │
│ DoD: all 7 requirements visibly working; ChecklistFragment shows    │
│      green check on each                                            │
└─────────────────────────────────────────────────────────────────────┘

┌─ Day 6 ─ Polish & ship ─────────────────────────────────────────────┐
│ 20. README with 1:1 mapping table + GIFs                            │
│ 21. GitHub push + Notion + 위시켓 재제출                              │
└─────────────────────────────────────────────────────────────────────┘
```

**Critical "build order" insights:**

1. **A11yPrefs before ThemeService**: ThemeService reads HC flag in `onCreate` *before* `setContentView`. Prefs wrapper must exist by Day 1.
2. **TtsService before contentDescription pass**: Day 2 TTS attach is the forcing function that reveals every missing `contentDescription`. Doing them in the opposite order means a second pass.
3. **FocusNavigator scaffold before per-Fragment focus order**: ⚠️ **Important.** If you write XML `nextFocusDown/Up/Left/Right` attributes before `dispatchKeyEvent` is wired, you can't visually verify the focus chain — `focusSearch()` returns the right view but nothing triggers `requestFocus()`. Reverse order = silent breakage.
4. **Theme recreate before VolumeService/Zoom**: Recreate is the highest-risk lifecycle interaction. If state-restoration is broken, you want to find out on Day 3 when there's still buffer, not on Day 5.
5. **ChecklistFragment last**: It reads state from all services, so building it last means no rework when service signatures evolve.

---

## 6. Lifecycle Gotchas & Mitigations

### 6.1 `recreate()` and Fragment back stack

**The gotcha:** Calling `Activity.recreate()` rebuilds the Activity, but `FragmentManager` restores the back stack from its saved state. If your Fragments use `setRetainInstance(true)` or hold references to Activity-only objects (e.g., direct `View` references to BottomBar), you get crashes or stale views.

**Mitigation:**
- ✅ Do NOT use `setRetainInstance(true)` (deprecated anyway).
- ✅ Fragments must NOT hold long-lived references to Activity views. Re-acquire in `onViewCreated`.
- ✅ Each Fragment re-attaches TTS focus listeners in `onViewCreated()` — listeners are tied to the new view tree.
- ✅ Use `FragmentContainerView` (not `FrameLayout`) in `activity_main.xml` — required for FragmentManager state restoration to work cleanly.
- ✅ Service singletons (`object`) hold no `Activity` reference. If you must pass a Context, take `applicationContext`.

### 6.2 `savedInstanceState` vs `SharedPreferences` — which for what?

| State | Mechanism | Why |
|---|---|---|
| `isHighContrast` toggle | **SharedPreferences (A11yPrefs)** | Must survive cold start. Recreate triggered by us — pref already written before `recreate()`. |
| `isTtsEnabled` toggle | **SharedPreferences** | Same rationale. |
| Currently focused view ID | **savedInstanceState (automatic)** | Android's standard view-state save handles this if views have stable `android:id`. ⚠️ Make sure all focusables have IDs. |
| Current Fragment | **FragmentManager auto-save** | Free if you use FragmentContainerView + the standard `supportFragmentManager.beginTransaction().replace().addToBackStack()`. |
| ScaleAnimation zoom level | **savedInstanceState** in MenuFragment (override `onSaveInstanceState`) | Transient UI state, no need to persist across cold start. |
| Volume | **Nothing** | Read live from `AudioManager`. |

**The trap to avoid:** Don't try to persist HC state in `savedInstanceState` only. If the user kills the app and reopens, they expect HC mode to remain. SharedPreferences is the durable store; `savedInstanceState` is a redundant cache.

### 6.3 TextToSpeech init race

**The gotcha:** `TextToSpeech` init is async. Focus events can fire before init completes (especially the *first* focus on app launch).

**Mitigation:** `TtsService` keeps `isInitialized = false` until `onInit(SUCCESS)`. `speakIfChanged` returns silently if not initialized. Optionally enqueue the most recent pre-init request and replay on init success — but for a 6-day demo, dropping the very first utterance is acceptable.

### 6.4 Theme attribute resolution timing

**The gotcha:** If you call `getColor(R.color.x)` inside a Fragment expecting a theme-resolved color, you get the *resource* color, not the theme-attribute color. After HC toggle + recreate, hardcoded colors won't update.

**Mitigation:** Use `?attr/colorPrimary`-style theme attributes in XML and `resolveAttribute` in code. Define both `Theme.AccessKit` and `Theme.AccessKit.HighContrast` to override the same attr names.

### 6.5 Custom View on theme switch

**The gotcha:** `AccessibilityBottomBar` inflates from XML. After `recreate()`, it's re-inflated fresh — but if it cached drawables/colors in field initializers using direct resource refs, those become stale.

**Mitigation:** Read theme-resolved values in `onAttachedToWindow` (called after the View is attached to the new themed Activity), not in `init {}`.

### 6.6 Focus initial state after recreate

**The gotcha:** After `recreate()`, focus is often lost (no view has focus) until the user presses a D-pad key. For a demo recording this looks broken.

**Mitigation:** In each Fragment's `onViewCreated`, call `firstFocusableView.requestFocus()` if `savedInstanceState == null` OR if no view currently has focus. Use `view.post { ... }` to defer until layout is complete.

### 6.7 `dispatchKeyEvent` swallowing keys

**The gotcha:** Returning `true` from `dispatchKeyEvent` for keys you didn't intend to handle blocks the system. E.g., handling `ACTION_DOWN` but not `ACTION_UP` for the same keycode breaks long-press.

**Mitigation:** Only intercept `ACTION_DOWN` for known DPAD/ENTER keycodes; explicitly return `super.dispatchKeyEvent(event)` for everything else (including ACTION_UP). FocusNavigator returns `Boolean` indicating "I handled this"; MainActivity falls through to super on `false`.

---

## 7. Patterns to Follow

### Pattern 1: "Activity = entry points, Fragments = content, Services = effects"
**What:** A strict three-layer separation where layer N never reaches into layer N+1's internals.
**When:** Throughout the app.
**Why:** Lets an evaluator open any one file and understand its job in 5 seconds.

### Pattern 2: Singleton `object` services for demo-scale side effects
**What:** `object TtsService`, `object ThemeService`, etc. — Kotlin singletons.
**When:** When DI is overkill and the lifetime is process-wide.
**Why:** Survives `recreate()` for free, no library dependency, evaluator can find "the TTS code" at one path.

### Pattern 3: Theme attributes over hardcoded colors
**What:** XML uses `?attr/textColorOnSurface` not `@color/black`. Two themes redefine the same attrs.
**When:** Anywhere a color/dimen differs between normal and HC theme.
**Why:** `recreate()` correctly re-applies; no per-View update logic.

### Pattern 4: All `contentDescription` in `strings.xml`
**What:** No inline `android:contentDescription="버튼"` strings.
**When:** Always.
**Why:** Easier audit (one file to scan); future i18n; signals professionalism to evaluator.

### Pattern 5: Pure-function `FocusNavigator`
**What:** `FocusNavigator.handle(currentFocus: View?, keyCode: Int): Boolean` — no fields, no Context.
**When:** Stateless utilities.
**Why:** Trivially testable, no lifecycle concerns, reusable from any Activity.

---

## 8. Anti-Patterns to Avoid

### Anti-Pattern 1: ViewModel for a 6-day no-network demo
**What:** Adding `androidx.lifecycle:lifecycle-viewmodel-ktx` and per-Fragment ViewModels.
**Why bad:** Pulls dependency, adds a layer that holds zero data (no network, no DB), distracts from the accessibility narrative.
**Instead:** Singleton `object` services + SharedPreferences. State lives where the side effect lives.

### Anti-Pattern 2: Per-Fragment `dispatchKeyEvent`
**What:** Letting each Fragment override key handling.
**Why bad:** Duplicate logic; ambiguity about which Fragment "wins"; harder to point an evaluator at "the keypad code."
**Instead:** One override in MainActivity → FocusNavigator. `currentFocus` is Activity-wide.

### Anti-Pattern 3: Theme switch via `AppCompatDelegate.setDefaultNightMode`
**What:** Treating high-contrast as "dark mode."
**Why bad:** Conflates user OS dark-mode preference with app HC mode; evaluator might assume you "just used dark mode." Reduces portfolio narrative — you want to show explicit theme management.
**Instead:** Custom `setTheme(R.style.Theme_AccessKit_HighContrast)` + `recreate()` as already planned.

### Anti-Pattern 4: TTS instance per-Fragment
**What:** Each Fragment creates its own `TextToSpeech`.
**Why bad:** Each costs an init round-trip (~hundreds of ms), engines compete, debounce state is lost across navigation.
**Instead:** One `object TtsService` for the whole process.

### Anti-Pattern 5: Accessibility code mixed into Fragment business logic
**What:** TTS speak calls scattered inside button click handlers.
**Why bad:** Evaluator scrolling Fragment code can't find the accessibility story.
**Instead:** All speaking goes through `TtsService`. Fragments only call `view.attachA11ySpeak()`. Click handlers don't speak.

### Anti-Pattern 6: Ignoring the `?:` null safety on `currentFocus`
**What:** `currentFocus.focusSearch(...)` without null-check.
**Why bad:** On first launch with no focused view, NPE crash on first key press — the worst possible first impression.
**Instead:** `currentFocus ?: return super.dispatchKeyEvent(event)` at the top of the override.

---

## 9. Scalability Considerations

This is a portfolio demo, not a product, so "scale" here means evaluator scan-time and future portfolio reuse, not user load.

| Concern | At demo (this project) | Future reuse (Phase 2) | Real kiosk product |
|---|---|---|---|
| Number of Fragments | 3 | 5–10 | 20+, navigation graph |
| Persistence | SharedPreferences (2 keys) | + DataStore | Room + remote sync |
| DI | None (singletons) | None | Hilt |
| TTS engine sharing | One singleton | One singleton | Service-bound TTS for cross-process |
| Accessibility audit | Manual + ChecklistFragment | Espresso a11y scanner | Continuous CI a11y checks |

**Key implication for THIS demo:** the 3-Fragment / singleton-service architecture is intentionally as small as it can be while still demonstrating each capability. Resist the urge to add MVVM/Hilt/Coroutines patterns the demo doesn't need.

---

## 10. Concrete Class/File Skeletons (for Day 1)

```kotlin
// MainActivity.kt — the spine of the app
class MainActivity : AppCompatActivity(),
    AccessibilityBottomBar.Listener {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeService.apply(this)            // 1. theme BEFORE super
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        TtsService.init(applicationContext)  // 2. init TTS once
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.nav_host, HomeFragment())
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        FocusNavigator.handle(currentFocus, event)
            ?: super.dispatchKeyEvent(event)

    // BottomBar callbacks
    override fun onToggleHighContrast() { ThemeService.toggle(this) }
    override fun onToggleTts()          { TtsService.isEnabled = !TtsService.isEnabled }
    override fun onZoomIn()             { (currentFragment() as? Zoomable)?.zoomIn() }
    override fun onZoomOut()            { (currentFragment() as? Zoomable)?.zoomOut() }
    // (volume + / − also here)

    private fun currentFragment(): Fragment? =
        supportFragmentManager.findFragmentById(R.id.nav_host)
}
```

```kotlin
// FocusNavigator.kt — pure function
object FocusNavigator {
    /** Returns true if handled, false to let super handle, null to defer to super entirely. */
    fun handle(focused: View?, event: KeyEvent): Boolean? {
        if (event.action != KeyEvent.ACTION_DOWN) return null
        val target = focused ?: return null
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP    -> target.focusSearch(View.FOCUS_UP)?.requestFocus()    ?: false
            KeyEvent.KEYCODE_DPAD_DOWN  -> target.focusSearch(View.FOCUS_DOWN)?.requestFocus()  ?: false
            KeyEvent.KEYCODE_DPAD_LEFT  -> target.focusSearch(View.FOCUS_LEFT)?.requestFocus()  ?: false
            KeyEvent.KEYCODE_DPAD_RIGHT -> target.focusSearch(View.FOCUS_RIGHT)?.requestFocus() ?: false
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER      -> target.performClick()
            else -> null
        }
    }
}
```

These two files alone communicate ~70% of the architectural story to a 30-second skimmer. Keep them short and well-named.

---

## 11. Quality Gate Self-Check

- [x] **Components clearly defined with state ownership** — §2 table covers each component, what it owns, what it doesn't, and recreate() survival.
- [x] **Data flow direction explicit for each accessibility feature** — §3 sub-sections diagram theme toggle, TTS-on-focus, key→focus, volume, zoom.
- [x] **Build order implications noted** — §5 Day-by-Day with explicit dependency callouts ("FocusNavigator before XML focus order", "A11yPrefs before ThemeService", "TTS before contentDescription pass").
- [x] **Lifecycle gotchas surfaced** — §6 covers recreate + back stack, savedInstanceState contract, TTS init race, theme attr timing, custom view re-inflation, focus restoration, dispatchKeyEvent swallowing.

---

## Sources

- Project files (HIGH confidence — source of truth):
  - `.planning/PROJECT.md`
  - `PRD.md`
  - `android_barrier_free_demo_plan.md` §4-1, §7
- Android framework knowledge (HIGH confidence — long-stable APIs since API 14–21):
  - `Activity.recreate()`, `setTheme()` ordering rule, `FragmentContainerView`, `dispatchKeyEvent`, `View.focusSearch()`, `TextToSpeech.OnInitListener`, `AudioManager.adjustStreamVolume`, drawable state selectors. These are foundational APIs unchanged since the listed API levels and well-documented in d.android.com.
- WPF→Android mapping rationale: derived from plan §8 (HIGH — author's own prior work).

**Confidence:** HIGH overall. The architectural choices are conservative, AndroidX-only, and use the simplest mechanism that satisfies each requirement — minimizing the chance that a 6-month-old Android API change has invalidated the approach. Two areas worth re-verifying with Context7 if time allows during implementation: (a) latest `FragmentContainerView` state-restoration behavior on API 26 minimum, (b) any deprecation notices on `TextToSpeech.speak(String, int, HashMap)` vs the newer `Bundle` overload (we use the Bundle overload above).
