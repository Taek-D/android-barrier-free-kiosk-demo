# Domain Pitfalls: Android Kotlin 키오스크 접근성 데모

**Domain:** Android (Kotlin, View/XML, API 26+) 베리어프리 접근성 — 인앱 컨트롤러 패턴
**Researched:** 2026-05-07
**Confidence:** HIGH (Context7 + 공식 Android 문서 + cvs-health 공식 레퍼런스 저장소 + 한국 KS X 9211 표준 직접 인용)
**Goal:** 6일 데모 일정 안에서 발생 확률이 높고, 평가자에게 즉시 노출되는 결함을 차단한다.

---

## Critical Pitfalls (반드시 차단 — 발생 시 데모가 무너짐)

### Pitfall C-1: TTS `speak()` 호출이 `OnInitListener` 콜백 전에 일어남

**What goes wrong**
`TextToSpeech` 생성자는 비동기다. Activity `onCreate`에서 `tts = TextToSpeech(this, listener)` 직후 곧바로 `tts.speak(...)`를 호출하면 엔진이 아직 바인딩되지 않은 상태라 `ERROR(-1)`이 반환되고 첫 포커스 안내가 통째로 묵음이 된다. 시연 GIF를 찍으면 평가자에게 "TTS 미작동"으로 보인다.

**Why it happens**
TTS는 별도 시스템 서비스(예: Google TTS)에 IPC 바인딩되며 `onInit(SUCCESS)` 호출까지 보통 100~800ms가 소요된다.

**Warning signs**
- Logcat에 `speak failed: not bound to TTS engine`
- 첫 화면 진입 시 첫 안내만 묵음, 두 번째 포커스부터는 정상

**Prevention** (Phase 2 — TTS 구현 시점에 즉시 적용)
```kotlin
class TtsService(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready = false
    private val pending = mutableListOf<String>()

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val r = tts.setLanguage(Locale.KOREAN)
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.language = Locale.US // 명시적 폴백
        }
        ready = true
        pending.forEach { tts.speak(it, TextToSpeech.QUEUE_FLUSH, null, it) }
        pending.clear()
    }

    fun speakIfChanged(text: String) {
        if (!ready) { pending += text; return }
        // 중복 억제 로직...
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }
}
```

**Phase mapping:** Day 2 (TtsService 구현). 가드 없이 Day 4까지 가면 Day 5 시연 영상 촬영 시 발견되어 재작업.

---

### Pitfall C-2: TTS 인스턴스 누수 → Activity recreate 시 엔진 좀비

**What goes wrong**
고대비 토글이 `Activity.recreate()`를 부르는데, 매번 새 Activity가 새 `TextToSpeech` 인스턴스를 만들고 이전 인스턴스가 `shutdown()` 되지 않으면 TTS 엔진 바인딩이 좀비로 남고, 시연 중 토글을 두세 번 누르면 첫 인스턴스가 발화 중인 텍스트와 새 인스턴스의 안내가 겹친다. LeakCanary가 잡으면 GitHub Issue처럼 보이지만, 더 큰 문제는 시연 영상에서 음성이 겹쳐 들리는 것이다.

**Why it happens**
`TextToSpeech$Connection`이 익명 `ITextToSpeechCallback.Stub`을 보유하며 Activity Context를 강참조한다 ([kiwix-android #827](https://github.com/kiwix/kiwix-android/issues/827), [FolioReader #481](https://github.com/FolioReader/FolioReader-Android/issues/481)).

**Warning signs**
- 고대비 토글 2회 이상 후 음성이 겹침
- LeakCanary가 `TextToSpeech` 보유한 Activity 누수 보고

**Prevention** (Phase 2/3 양쪽에서 강제)
1. `TtsService`는 `applicationContext`로만 생성 — Activity Context 절대 금지.
2. `TtsService`를 싱글턴(`object TtsService` 또는 Application 보유)으로 만들고, recreate 시 인스턴스를 재사용.
3. 만약 Activity-scoped로 갈 거면 `onDestroy()`에서 반드시 `tts.stop(); tts.shutdown()`.

```kotlin
class App : Application() {
    val ttsService by lazy { TtsService(this) } // applicationContext 사용
}
```

**Phase mapping:** Day 2 (생성), Day 3 (recreate 도입 시 재검증).

---

### Pitfall C-3: `dispatchKeyEvent`가 `super` 호출을 빼먹어 EditText/시스템 키가 죽음

**What goes wrong**
DPAD 핸들링 로직이 모든 KeyEvent에 `return true`를 반환하면 EditText 텍스트 입력, BACK 키, VOLUME 키, 시스템 IME 액션이 모두 차단된다. 평가자가 에뮬레이터에서 BACK 키를 눌렀는데 앱이 안 닫히면 즉시 "버그 있는 데모"로 분류된다.

**Why it happens**
`dispatchKeyEvent`는 액티비티의 모든 키이벤트의 첫 진입점이다. 처리하지 않은 이벤트는 반드시 `super.dispatchKeyEvent(event)`로 위임해야 시스템 포커스 탐색 + 기본 동작이 살아난다 ([Wan Xiao 2019](https://medium.com/@wanxiao1994/how-android-dispatch-keyevent-and-perform-focus-navigation-8565327bd12e)).

**Warning signs**
- BACK 키가 안 먹음
- 시연용 ChecklistFragment에 EditText가 있다면 글자 입력이 안 됨
- VOLUME 버튼이 P1-1 VolumeService 외에서도 안 먹음

**Prevention** (Phase 4 — FocusNavigator 구현 시)
```kotlin
override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (event.action == KeyEvent.ACTION_DOWN) {
        val handled = focusNavigator.handle(event) // DPAD_*만 처리
        if (handled) return true
    }
    return super.dispatchKeyEvent(event) // 나머지는 시스템에 위임
}

// FocusNavigator는 DPAD_UP/DOWN/LEFT/RIGHT/CENTER + ENTER만 매칭
fun handle(e: KeyEvent): Boolean = when (e.keyCode) {
    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> { /* focusSearch */ true }
    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { /* click */ true }
    else -> false
}
```

**Phase mapping:** Day 4. 화이트리스트 방식 (특정 키만 처리)을 처음부터 적용.

---

### Pitfall C-4: 포커스가 하단 접근성 바로 "튀어버림" — 시각적 함정

**What goes wrong**
`AccessibilityBottomBar`가 화면 하단에 항상 노출되는데, 콘텐츠 영역에서 DPAD_DOWN을 누르면 포커스가 바로 하단 4버튼 중 하나로 점프하고 다시 위로 못 올라온다. 또는 콘텐츠 어디서 DOWN을 눌러도 같은 버튼이 잡혀서 평가자가 "이게 진짜 포커스 이동인가?" 의심하게 된다.

**Why it happens**
`focusSearch(FOCUS_DOWN)`은 시각적 위치(bounds) 기준으로 가장 가까운 포커서블 뷰를 찾는다. 하단 바가 항상 콘텐츠보다 아래라서 콘텐츠의 마지막 행 외 어느 위치에서 DOWN을 눌러도 같은 버튼으로 점프한다. 또한 `nextFocusUp/Down/Left/Right`가 명시되지 않으면 휴리스틱이 예상과 다르게 동작한다 ([Android Keyboard navigation guide](https://developer.android.com/develop/ui/views/touch-and-input/keyboard-input/navigation)).

**Warning signs**
- 콘텐츠 첫 행에서 DOWN 한 번 누르면 바로 하단 바
- 하단 바에서 UP 누르면 콘텐츠로 못 돌아오거나 의도 외 위치로 이동

**Prevention** (Phase 4)
1. 콘텐츠와 하단 바를 명시적 `nextFocus*`로 연결:
   ```xml
   <Button android:id="@+id/btn_last_content_row"
       android:nextFocusDown="@id/btn_high_contrast" />
   <Button android:id="@+id/btn_high_contrast"
       android:nextFocusUp="@id/btn_last_content_row"
       android:nextFocusRight="@id/btn_tts" />
   ```
2. 하단 바를 제외한 영역의 마지막 행에서만 하단 바로 진입하도록 만들거나, `focusSearch` 호출 전 현재 포커스가 콘텐츠 안에 있는지 검사 후 직접 매핑.
3. 시연 영상에 "콘텐츠 행 → 행 → 마지막 → 하단 바 → 다시 위" 흐름을 명시적으로 보여줌.

**Phase mapping:** Day 4. 늦으면 Day 5 시연 영상에서 평가자에게 가장 잘 노출되는 결함이 된다.

---

### Pitfall C-5: `state_focused` 셀렉터가 동작하지 않음 — Material 버튼/Ripple 충돌

**What goes wrong**
`@drawable/focused_background` selector를 `android:background`에 지정했는데 포커스 시 stroke가 안 보인다. P0-5 미구현으로 직결.

**Why it happens (3가지 원인)**
1. **Material 버튼 사용 시:** `MaterialButton`은 ripple drawable을 background로 이미 보유. 셀렉터를 background에 다시 넣으면 ripple이 사라지거나 셀렉터가 무시됨 ([cvs-health: Custom Focus Indicators](https://github.com/cvs-health/android-view-accessibility-techniques/blob/main/doc/dynamicbehaviors/CustomFocusIndicators.md)).
2. **selector item 순서:** `<item android:drawable="@drawable/normal"/>` (state 없음, default)을 위에 놓으면 모든 상태에서 default가 매칭되어 focus state가 무시됨. State item이 default보다 위에 있어야 함.
3. **Rounded corner + stroke clipping:** `<corners android:radius="12dp"/>` + `<stroke width="3dp"/>`가 있으면 stroke가 corner에서 잘려 보임 ([material-components #1329](https://github.com/material-components/material-components-android/issues/1329)).

**Warning signs**
- 에뮬레이터 Tab 키로 포커스 이동 시 stroke 미표시
- `state_pressed`만 동작, `state_focused` 미동작
- Material `Button` 사용 중

**Prevention** (Phase 4 — Day 4 동시 구현)
1. **`android:foreground` 사용 (API 23+, 본 프로젝트 minSDK=26 충족):**
   ```xml
   <Button
       android:foreground="@drawable/focused_overlay"
       android:background="@drawable/btn_normal" />
   ```
   foreground는 ripple과 충돌하지 않고 항상 위에 그려짐.
2. **Selector 순서 검증:**
   ```xml
   <selector>
       <item android:state_focused="true" android:drawable="@drawable/btn_focused"/>
       <item android:drawable="@drawable/btn_normal"/> <!-- 반드시 마지막 -->
   </selector>
   ```
3. **Stroke ≥ 3dp + 고대비 색상** (KWCAG 2.2 포커스 가시성 강화 충족, 명도 대비 3:1 이상).
4. **`android.widget.Button` 사용** (MaterialButton 회피) 또는 MaterialButton 사용 시 `app:strokeWidth`/`app:strokeColor` ColorStateList 방식.

**Phase mapping:** Day 4. selector XML과 layout 검증을 동시에. Day 6 README 스크린샷에서 stroke가 명확히 보여야 함.

---

## Moderate Pitfalls (시연 품질에 영향, 우회 가능)

### Pitfall M-1: `recreate()` 후 Fragment 상태 유실 — 토글 후 화면이 홈으로 돌아감

**What goes wrong**
Day 5 시연 시: ChecklistFragment에서 항목 3개 체크 → 고대비 토글 → recreate 후 ChecklistFragment가 사라지고 HomeFragment가 보임. 또는 진행 중이던 dialog가 사라짐.

**Why it happens**
`recreate()`는 새 Activity 인스턴스를 만든다. Fragment back stack은 `FragmentManager` 자동 복원이 동작하지만, MainActivity가 `onCreate`에서 무조건 `replace(HomeFragment)`를 부르면 복원된 Fragment 위에 덮어쓴다 ([Fragment State Loss](https://www.androiddesignpatterns.com/2013/08/fragment-transaction-commit-state-loss.html)).

**Warning signs**
- 고대비 토글 후 항상 홈 화면으로 돌아감
- 다이얼로그가 토글 후 사라짐

**Prevention** (Phase 3 — Day 3 ThemeService 도입 시)
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(themeService.currentThemeRes())
    super.onCreate(savedInstanceState)
    setContentView(...)
    if (savedInstanceState == null) {
        // 최초 실행에서만 초기 Fragment 추가
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, HomeFragment()).commit()
    }
}
```
또한 P0-3에서 Fragment 내부 상태(체크박스 상태 등)는 `onSaveInstanceState`에 직접 저장.

**Phase mapping:** Day 3. PRD Open Question도 동일 사항 명시.

---

### Pitfall M-2: 한국어 TTS 데이터 미설치 에뮬레이터에서 침묵

**What goes wrong**
새 에뮬레이터 이미지에 Google TTS 한국어 음성 데이터가 없으면 `setLanguage(Locale.KOREAN)`이 `LANG_MISSING_DATA`를 반환하고 polished 시연 GIF에서 음성이 안 나온다.

**Why it happens**
TTS 언어 데이터는 별도 다운로드. 시스템 설정 → 일반 → 언어 → TTS 설정에서 한국어 다운로드 필요 ([Android TTS Korean docs](https://medium.com/@kayushi07/implementing-text-to-speech-in-kotlin-multiplatform-a-complete-guide-for-android-ios-80dfc69b4961)).

**Warning signs**
- `setLanguage` 반환값이 `-1` (LANG_MISSING_DATA) 또는 `-2` (LANG_NOT_SUPPORTED)
- Logcat에 `TextToSpeech: setLanguage failed`

**Prevention** (Phase 2 + Phase 6)
1. 코드에서 `LANG_MISSING_DATA` 시 `Locale.US` 폴백 + `Toast`로 사용자 안내.
2. README "검증 환경" 섹션에 명시:
   > 본 데모는 Pixel 6 API 33 에뮬레이터 + Google TTS 한국어 음성 데이터(설정 → 시스템 → 언어 → 텍스트 음성 변환 → Google → 음성 데이터 → 한국어) 설치 환경에서 검증되었습니다.
3. 시연 GIF는 한국어 음성 출력 후 캡처. 백업으로 영어 폴백 GIF도 준비.

**Phase mapping:** Day 2 코드 + Day 6 README.

---

### Pitfall M-3: 에뮬레이터 D-pad 미작동 — 키패드 시연 검증 자체가 불가

**What goes wrong**
Day 4 작업 후 키이벤트 시연을 위해 에뮬레이터에서 화살표 키를 눌러도 반응 없음. PC 외부 키보드를 연결했는데 호스트 화살표 키가 에뮬레이터 DPAD로 매핑되지 않음.

**Why it happens**
1. AVD 디바이스 프로필이 D-pad 미지원 (대부분의 modern 폰 프로필이 그렇다).
2. AVD 설정에 "Enable Keyboard Input"이 꺼져 있음.
3. 호스트 키 → DPAD 매핑이 일부 OS에서 다름.

**Warning signs**
- 화살표 키 입력해도 KEYCODE_DPAD_* 콜백 안 옴
- `getDevices()` 호출 시 D-pad 디바이스 미존재

**Prevention** (Phase 4 — Day 4 시작 시 즉시 검증)
1. **AVD 생성 시 D-pad 지원 디바이스 사용:** "Android TV" 프로필 또는 Phone 프로필에서 Advanced → "DPad" enabled. 또는 hardware profile XML 직접 편집:
   ```
   hw.dPad=yes
   hw.keyboard=yes
   ```
2. **에뮬레이터 키 매핑** ([Android Studio Extended controls](https://developer.android.com/studio/run/emulator-extended-controls)):
   - `Numpad 4/8/6/2` → DPAD_LEFT/UP/RIGHT/DOWN
   - `Numpad 5` → DPAD_CENTER
   - `Tab` → 다음 포커스 (포커스 가시화 검증용으로 추가 활용 가능)
3. **백업 검증법:** 코드에 임시로 floating debug 버튼을 두어 onClick에서 `dispatchKeyEvent(KeyEvent(ACTION_DOWN, KEYCODE_DPAD_DOWN))`을 호출. 키패드 없이도 로직 검증 가능.
4. **README "검증 환경"에 명시:** "Android Studio Emulator (Pixel 4 API 33) + Numpad 키 매핑으로 검증" — PRD Risk와 일치.

**Phase mapping:** Day 4 시작 30분 안에 검증 → 안 되면 Day 4 일정 자체가 위험. Day 1 셋업 시 미리 D-pad AVD 생성을 권장한다.

---

### Pitfall M-4: TTS 큐 모드 — `QUEUE_ADD` 사용 시 안내 폭주

**What goes wrong**
포커스를 빠르게 5번 이동하면 5개 안내가 순서대로 모두 재생되어 마지막 포커스에 도달했을 때도 첫 번째 안내가 들리는 중. 시연 영상에서 "이거 lag 같은데" 인상.

**Why it happens**
`QUEUE_ADD`는 모든 발화를 큐에 추가. 포커스 이동 속도 > 발화 속도 시 누적.

**Prevention** (Phase 2 — TTS 첫 구현 시)
- 포커스 안내는 `TextToSpeech.QUEUE_FLUSH` 사용. 새 안내가 즉시 이전을 끊는다.
- 단 사용자가 의도적으로 누른 버튼 액션 안내(예: "고대비 모드 켜짐")는 끊기면 안 되니 별도 발화 채널에서 `QUEUE_ADD`.
- PRD 명시 "동일 텍스트 500ms 내 재발화 억제"는 동일 텍스트 중복만 억제, 빠른 이동 시 다른 텍스트는 FLUSH로 처리해야 함.

**Phase mapping:** Day 2.

---

### Pitfall M-5: 고대비 테마에서 명도 대비 4.5:1 미달 — KWCAG/WCAG AA 실패

**What goes wrong**
"고대비 테마"라고 라벨링했는데 검은 배경(#000) + 진한 회색 텍스트(#666)면 대비 5.74:1로 통과지만, 보라/파랑 액센트(#3F51B5 on #000 = 2.6:1) 같은 디자이너 본능이 들어가면 KWCAG 2.2 명도 대비 4.5:1 (작은 텍스트), 3:1 (큰 텍스트/UI 컴포넌트) 미달.

**Why it happens**
대비를 "느낌"으로 결정. PRD가 "WCAG AA 4.5:1 이상"을 명시하지만 실제 검증을 안 함.

**Warning signs**
- 검은 배경에 채도 낮은 컬러 텍스트 사용
- 액센트 컬러를 "예쁘게" 고르려는 충동

**Prevention** (Phase 3 — Day 3 themes_high_contrast.xml 작성 시)
1. **단일 컬러 팔레트:** 배경 `#000000`, 텍스트 `#FFFFFF`, 포커스 stroke `#FFFF00` (yellow on black 19.56:1), 액센트 `#FFD700`. 디자인 욕심 차단.
2. **검증 도구:** Android Studio Layout Inspector → Accessibility tab의 "Contrast" 진단. 또는 [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/) 수동 검증.
3. README 스크린샷 옆에 명시: "배경/텍스트 명도 대비 21:1 (WCAG AAA), 포커스 stroke/배경 19.56:1".
4. KWCAG 2.2 ([a11ykr](https://a11ykr.github.io/kwcag22/)) 5.1.5 검증: 큰 텍스트(18pt+/14pt bold+) 3:1, 일반 텍스트 4.5:1.

**Phase mapping:** Day 3 + Day 6 README 검증 캡처.

---

### Pitfall M-6: ScaleAnimation이 영구적이지 않음 — 토글 후 원본 크기 복귀

**What goes wrong**
`ScaleAnimation`은 **시각적 변환만** 적용하고 View 크기 자체는 안 변함. 애니메이션 종료 후 reset되어 사용자가 "확대했는데 다음 화면은 다시 작아짐" 경험.

**Why it happens**
`Animation`은 transient. 영구 적용은 `View.scaleX/scaleY` 또는 `LayoutParams` 변경 필요.

**Prevention** (Phase 5 — Day 5 ScaleAnimation 구현 시)
```kotlin
// 권장: ObjectAnimator 또는 직접 scaleX/scaleY
contentArea.scaleX = currentScale
contentArea.scaleY = currentScale
contentArea.pivotX = 0f; contentArea.pivotY = 0f
```
또는 ScaleAnimation 사용 시 `setFillAfter(true)` + `setFillEnabled(true)`. 단 fill 옵션은 visual만 유지하고 hit test는 원래 크기라 포커스 인디케이터 위치와 어긋날 수 있다.

**Warning signs**
- 확대 버튼 누른 후 다른 화면 이동 → 원래 크기
- 포커스 stroke 위치가 시각적 콘텐츠와 어긋남

**Phase mapping:** Day 5.

---

## Minor Pitfalls (인지하지 않으면 디버깅 시간 낭비)

### Pitfall m-1: `contentDescription` 이 시각 라벨과 다르면 KS X 9211 위반
KS X 9211은 시각 정보와 음성 안내가 동일 정보를 제공해야 함을 요구한다 ([음성 가이드라인](https://www.kioskui.or.kr/index.do?menu_id=00001048)). "고대비" 버튼의 `contentDescription`이 "테마 변경"이면 안 된다. 라벨과 정확히 일치시킬 것.

**Phase mapping:** Day 2.

### Pitfall m-2: TTS On/Off 토글 시 즉시 침묵하지 않음
사용자가 TTS off를 눌러도 큐에 쌓인 발화가 끝까지 재생됨. `tts.stop()`을 toggle off 시점에 호출.

**Phase mapping:** Day 2.

### Pitfall m-3: `focusable="true"` 누락
`Button`은 기본적으로 focusable이지만 `LinearLayout`/`FrameLayout`을 카드 클릭 영역으로 쓸 때 `android:focusable="true"` `android:focusableInTouchMode="false"`를 빼먹으면 DPAD 이동에서 빠진다.

**Phase mapping:** Day 4.

### Pitfall m-4: `android.R.attr.colorControlActivated` 의존
시스템 테마의 포커스 색상에 의존하면 고대비 테마 적용 시 포커스 stroke 색이 의도와 달라진다. 항상 직접 색상 ColorStateList 정의.

**Phase mapping:** Day 3 + Day 4.

### Pitfall m-5: SharedPreferences 미사용 시 첫 화면이 항상 라이트
P2-1로 분류되어 있지만 평가자가 앱을 두 번 실행할 수 있다. 두 번째 실행 시 고대비가 꺼져 있으면 "왜 매번 다시 설정?" 인상. P0로 격상 권장 (5줄 코드).

**Phase mapping:** Day 3 (3분 추가 작업).

### Pitfall m-6: TouchScreen 진입점에서 포커스가 보이지 않음
`focusableInTouchMode=false`인 일반 Button은 터치하면 포커스가 사라짐. 키패드 시연 영상은 처음부터 키만 사용해서 캡처. 터치 후 키 입력 혼합 영상을 찍으면 포커스 stroke가 안 보이는 구간이 생김.

**Phase mapping:** Day 6.

---

## Phase-Specific Warning Matrix

| Day | Phase Topic | Likely Pitfall | Mitigation |
|-----|-------------|----------------|------------|
| Day 1 | 프로젝트 셋업 | D-pad 지원 AVD 미생성 → Day 4에 발견 | Day 1에 미리 hw.dPad=yes AVD 생성 (M-3) |
| Day 2 | TtsService | C-1 init race, C-2 leak, M-2 LANG_MISSING_DATA, M-4 QUEUE mode, m-1 라벨 일치, m-2 stop on toggle | applicationContext + ready 가드 + Locale 폴백 + QUEUE_FLUSH + stop() |
| Day 3 | 고대비 테마 | M-1 Fragment 유실, M-5 명도 대비 미달, m-4 시스템 색상 의존, m-5 SharedPreferences | savedInstanceState != null 가드 + 21:1 팔레트 + ColorStateList + SharedPreferences |
| Day 4 | FocusNavigator + drawable | C-3 super 누락, C-4 포커스 점프, C-5 selector 무효, M-3 에뮬레이터 키, m-3 focusable | 화이트리스트 + nextFocus* 명시 + foreground + Numpad 매핑 |
| Day 5 | Volume + ScaleAnimation + Checklist | M-6 ScaleAnimation 휘발성 | scaleX/scaleY 직접 |
| Day 6 | README + GIF | P-1 시각 명료성, P-2 자막, P-3 매핑 표, m-6 터치 혼용 | (아래 Portfolio Pitfalls) |

---

## Portfolio Presentation Pitfalls (Day 6 — 평가자 90초 인지 전환에 직결)

### Pitfall P-1: GIF가 음성 안내 컨텍스트를 잃음

**What goes wrong**
포커스 안내 TTS가 핵심 기능인데 GIF에 오디오가 없다. 평가자는 stroke만 보고 "포커스 시각화는 되는데 음성 안내는 진짜 되나?" 의심.

**Prevention**
1. GIF 위에 자막 오버레이 추가 (예: "TTS: 고대비 모드 버튼" 텍스트가 stroke 옆에 등장).
2. README 별도 섹션에 MP4 영상 링크 (오디오 포함). YouTube/Loom 임베드.
3. Logcat 캡처도 같이 첨부 — `D/TtsService: speak: "고대비 모드 버튼"` 라인이 보이면 의심 차단.

**Phase mapping:** Day 6.

---

### Pitfall P-2: 스크린샷에서 포커스 stroke가 안 보임

**What goes wrong**
스크린샷이 고해상도지만 stroke 3dp가 화면에서 1~2px로 작아 보여 평가자가 "포커스 인디케이터 어디?" 라며 스크롤 넘김.

**Prevention**
1. 스크린샷 + 포커스 영역 빨간 화살표 + "← 3dp focus stroke (KWCAG 2.2 5.1.5 명도 대비 19.56:1)" 캡션.
2. 동일 위치 라이트/고대비 비교 스크린샷 좌우 배치.
3. 스크린샷 자체 크기를 충분히 크게 (1080×1920 → README에서 1/2 축소 정도).

**Phase mapping:** Day 6.

---

### Pitfall P-3: README 30초 테스트 실패 — 평가자가 매핑 표를 못 찾음

**What goes wrong**
README 첫 화면(스크롤 없이 보이는 영역)에 공고 요구사항 ↔ 코드 매핑 표가 안 보인다. 평가자가 스크롤하면서 "이 사람 뭘 했나" 찾는 단계에서 이미 떠난다.

**Prevention** (README 구조 확정)
README 구조 권장:
```
1. (스크롤 없이 보임) 한 줄 설명: "위시켓 키오스크 베리어프리 공고 매칭 데모 — Android Kotlin"
2. 시연 GIF 1개 (오디오 자막 오버레이 버전)
3. 공고 요구사항 ↔ 코드 매핑 표 (7행, 각 행에 GitHub 코드 라인 perma-link)
4. WPF → Android 이식 매핑 표 (PRD 핵심 자산)
5. 검증 환경 (에뮬레이터 + Numpad)
6. 빌드 방법 (3줄)
7. 라이선스
```
첫 4섹션이 모두 1스크롤 안에 들어와야 함.

**Phase mapping:** Day 6. README 초안을 Day 1에 placeholder로 만들어두면 매일 채워가기 쉬움.

---

### Pitfall P-4: GitHub 저장소명/description이 모호 → 검색 조회 실패

**What goes wrong**
저장소명이 `accessibility-test`나 `myapp` 같으면 위시켓 매니저가 URL만 보고 무엇인지 모른다.

**Prevention**
- 저장소명: `android-barrier-free-kiosk-demo` 또는 `android-accessibility-kiosk-kotlin`
- Description: "Android Kotlin 키오스크 접근성(베리어프리) 데모 — TTS · 고대비 · DPAD 포커스 · Focus Indicator (외부 의존성 0)"
- Topics: `android` `kotlin` `accessibility` `kiosk` `barrier-free` `tts` `wcag` `kwcag`

**Phase mapping:** Day 1 + Day 6.

---

### Pitfall P-5: KWCAG/KS X 9211 인용 누락 — "표준을 모른다" 인상

**What goes wrong**
README가 "WCAG AA"만 언급하고 KWCAG 2.2, KS X 9211(무인정보단말기 접근성 지침)을 인용 안 함. 한국 키오스크 발주처는 한국 표준을 우선시.

**Prevention** (Day 6 README "Compliance" 섹션 신설)
```
## 표준 준수
| 기능 | 관련 표준 | 검증 방법 |
|---|---|---|
| 음성 안내 | KS X 9211 (음성 가이드라인 6.x) | 모든 포커스/액션 contentDescription 일치 검증 |
| 고대비 | KWCAG 2.2 5.1.5 (명도 대비) | 21:1 검증 스크린샷 |
| 포커스 인디케이터 | KWCAG 2.2 5.4.7 (포커스 가시성) | stroke ≥ 3dp, 명도 대비 19.56:1 |
| 키패드 조작 | KWCAG 2.2 6.1.x (키보드 접근성) | DPAD UP/DOWN/LEFT/RIGHT/CENTER 동작 영상 |
```

**Phase mapping:** Day 6.

---

## Korean Regulatory Failure Modes (Summary)

KS X 9211 검증 항목 중 본 데모의 인앱 컨트롤러 구현이 차단해야 하는 실패 모드 ([WebWatch 시험평가](http://www.kwacc.or.kr/Accessibility/Kiosk), [별표5 검증 기준](https://www.law.go.kr/LSW/flDownload.do?flSeq=157698277)):

| 실패 모드 | 본 데모의 차단 방법 | 위치 |
|-----------|---------------------|------|
| 시각 정보와 음성 안내 불일치 | `contentDescription` ≡ 시각 라벨 정책 | Pitfall m-1 |
| 음성 안내 제어 불가 | TTS On/Off 토글 + 즉시 stop | Pitfall m-2 |
| 포커스 비가시 | 3dp stroke + 19:1 대비 + foreground 사용 | C-5 |
| 키보드 단독 조작 불가 | DPAD UP/DOWN/LEFT/RIGHT/CENTER + ENTER 모두 처리 | C-3, C-4 |
| 명도 대비 미달 | 21:1 팔레트 + 검증 도구 | M-5 |
| 개인정보 음성 노출 | (본 데모 범위 밖이지만 README에 인지 명시 권장) | Day 6 |

---

## Sources

### HIGH confidence (공식 표준/Android 공식 문서/Context7급)
- [Android Developers: Support keyboard navigation](https://developer.android.com/develop/ui/views/touch-and-input/keyboard-input/navigation)
- [Android Developers: TextToSpeech.OnInitListener](https://developer.android.com/reference/android/speech/tts/TextToSpeech.OnInitListener)
- [Android Developers: Saving state with fragments](https://developer.android.com/guide/fragments/saving-state)
- [Android Developers: Emulator Extended controls](https://developer.android.com/studio/run/emulator-extended-controls)
- [Android Developers: TV Focus system](https://developer.android.com/design/ui/tv/guides/styles/focus-system)
- [cvs-health: Custom Focus Indicators (Android Views)](https://github.com/cvs-health/android-view-accessibility-techniques/blob/main/doc/dynamicbehaviors/CustomFocusIndicators.md) — Anthropic 권장 a11y 레퍼런스 저장소
- [KS X 9211 무인정보단말기 접근성 지침 (e나라표준인증)](https://standard.go.kr/KSCI/standardIntro/getStandardSearchView.do?menuId=919&topMenuId=502&upperMenuId=503&ksNo=KSX9211)
- [무인정보단말기 접근성 검증 기준 별표5 (보건복지부 고시)](https://www.law.go.kr/LSW/flDownload.do?flSeq=157698277)
- [한국형 웹 콘텐츠 접근성 지침(KWCAG) 2.2](https://a11ykr.github.io/kwcag22/)

### MEDIUM confidence (검증된 커뮤니티/이슈 트래커)
- [Fragment Transactions & Activity State Loss — Android Design Patterns](https://www.androiddesignpatterns.com/2013/08/fragment-transaction-commit-state-loss.html)
- [Wan Xiao: How Android dispatch KeyEvent and perform focus navigation](https://medium.com/@wanxiao1994/how-android-dispatch-keyevent-and-perform-focus-navigation-8565327bd12e)
- [Iwona Pękała: Keyboard focus indicator in native Android applications](https://medium.com/@iwona.pekala/keyboard-focus-indicator-in-native-android-applications-f474e062a794)
- [Appt: Accessibility focus indicator on Android](https://appt.org/en/docs/android/samples/accessibility-focus-indicator)
- [kiwix-android #827: TextToSpeech memory leak](https://github.com/kiwix/kiwix-android/issues/827)
- [FolioReader-Android #481: Memory leak in TextToSpeech activity](https://github.com/FolioReader/FolioReader-Android/issues/481)
- [material-components-android #1329: ShapeableImageView rounded corners stroke clipping](https://github.com/material-components/material-components-android/issues/1329)
- [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)

### Domain reference
- [한국디지털접근성진흥원 키오스크 시험평가](http://www.kwacc.or.kr/Accessibility/Kiosk)
- [WebWatch 키오스크 접근성](http://www.webwatch.or.kr/KA/KA_Intro.html?MenuCD=410)
- [무인정보단말기 UI 플랫폼 (음성 가이드라인)](https://www.kioskui.or.kr/index.do?menu_id=00001048)
