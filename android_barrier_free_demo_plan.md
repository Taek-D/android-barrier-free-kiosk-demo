# Android Kotlin 베리어프리 접근성 데모 앱 — 사이드 프로젝트 기획서

> 작성일: 2026-05-07
> 연관 공고: 키오스크 앱 베리어프리 기능 구현 (위시켓)
> 공고 마감일: 2026-05-20
> 목표 완료일: 2026-05-15 (마감 5일 전)

---

## 1. 프로젝트 배경 및 목적

### 연관 공고 요약

| 항목 | 내용 |
|---|---|
| 공고명 | 키오스크 앱 베리어프리 기능 구현 |
| 예산 | 1,000만 원 |
| 기간 | 60일 (6월 26일 납품) |
| 핵심 스택 | Android (Kotlin) |
| 지원 자격 | Android(Kotlin) 커스텀 UI 및 Accessibility Service 제어 경험이 풍부한 분 |
| 현장 조건 | 주 1회 서울 서초구 신사역 오프라인 미팅 |

### 포트폴리오 갭 분석

현재 보유한 ♿ 배리어프리 키오스크 접근성 데모(WPF/C#)는 접근성 설계 철학과 구현 원리를 증명하지만, 플랫폼이 Android Kotlin이 아니다.

공고 지원 자격 "Android(Kotlin) 경험이 풍부한 분"을 충족하려면 **Android Kotlin 네이티브 코드 증거**가 필요하다.

이 사이드 프로젝트는 기존 WPF 데모에서 검증된 접근성 4항목(TTS·고대비·키패드·Focus Indicator)을 Android Kotlin으로 이식하여, 공고의 모든 기능 요구사항(2-1~2-3)에 1:1 대응하는 포트폴리오를 만드는 것이 목표다.

---

## 2. 구현 목표 (공고 요구사항 1:1 매핑)

| 공고 요구사항 | 이 프로젝트 구현 항목 |
|---|---|
| 접근성 네비게이션 바 (하단 바) | CustomBottomAccessibilityBar View 컴포넌트 |
| 음성 안내 On/Off | TextToSpeech API + OnFocusChangeListener |
| 고대비 On/Off | styles.xml 테마 전환 + recreate() |
| 화면 확대/축소 | ScaleAnimation 또는 ViewCompat zoom |
| 음량 조절 | AudioManager.STREAM_MUSIC 증감 |
| 키패드 방향키 → 포커스 이동 | dispatchKeyEvent override + nextFocusDown/Up/Left/Right |
| Focus Indicator | drawable selector (focused_background.xml) |

---

## 3. 기술 스택

| 분류 | 선택 | 이유 |
|---|---|---|
| 언어 | Kotlin | 공고 요구사항 직접 대응 |
| UI | XML Layout (View 기반) | 기존 코틀린 코드베이스가 Compose가 아닐 가능성 높음. Fragment 기반 인수 시나리오 재현 |
| TTS | android.speech.tts.TextToSpeech | 네이티브 SDK, 별도 의존성 없음 |
| 테마 | styles.xml + AppCompatDelegate | 런타임 테마 전환 표준 패턴 |
| 키이벤트 | Activity.dispatchKeyEvent | 하드웨어 키패드 이벤트 캡처 진입점 |
| 오디오 | AudioManager | 시스템 볼륨 제어 표준 API |
| 빌드 | Gradle (Kotlin DSL) | 최신 Android 빌드 표준 |
| 최소 SDK | API 26 (Android 8.0) | 공공 키오스크 타깃 기준 |

**외부 라이브러리: 없음** — 공고 클라이언트가 기존 코드베이스에 외부 의존성 추가를 꺼릴 수 있으므로, 네이티브 SDK만으로 구현하여 이식성을 증명한다.

---

## 4. 구현 범위 (MVP 기준)

### 4-1. 앱 구조

```
app/
├── ui/
│   ├── MainActivity.kt              ← 키이벤트 진입점 + 테마 전환 호스트
│   ├── fragment/
│   │   ├── HomeFragment.kt          ← 데모 메인 화면
│   │   ├── MenuFragment.kt          ← 버튼/리스트 포커스 탐색 데모
│   │   └── ChecklistFragment.kt    ← 구현 항목 체크리스트 (포트폴리오 시연용)
│   └── view/
│       └── AccessibilityBottomBar.kt ← 커스텀 하단 접근성 바
├── service/
│   ├── TtsService.kt               ← TextToSpeech 래퍼 (중복 억제 포함)
│   ├── ThemeService.kt             ← 고대비/기본 테마 전환
│   └── VolumeService.kt            ← AudioManager 볼륨 제어
├── accessibility/
│   └── FocusNavigator.kt           ← 방향키 KeyEvent → 포커스 이동 매핑
└── res/
    ├── layout/
    ├── drawable/
    │   ├── focused_background.xml  ← Focus Indicator (4dp stroke)
    │   └── high_contrast_bg.xml
    ├── values/
    │   ├── themes.xml              ← 기본 테마
    │   └── themes_high_contrast.xml ← 고대비 테마
    └── values-night/               ← (참고용)
```

### 4-2. 핵심 구현 항목 상세

#### A. AccessibilityBottomBar (하단 접근성 바)

- 항상 화면 최하단에 고정 노출 (CoordinatorLayout 또는 ConstraintLayout 고정)
- 버튼 4개: 고대비 On/Off / TTS On/Off / 확대 / 축소
- 각 버튼 최소 56dp × 56dp (터치 타겟)
- contentDescription 필수 설정

#### B. TtsService (포커스 기반 TTS)

```kotlin
// 핵심 구현 패턴 (WPF SpeakOnFocusBehavior 이식)
class TtsService(context: Context) : TextToSpeech.OnInitListener {
    private var lastSpoken = ""
    private var lastSpokenTime = 0L

    fun speakIfChanged(text: String) {
        val now = System.currentTimeMillis()
        if (text == lastSpoken && now - lastSpokenTime < 500) return  // 중복 억제
        lastSpoken = text
        lastSpokenTime = now
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_focus")
    }

    fun attachToView(view: View) {
        view.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus && isEnabled) {
                val label = v.contentDescription?.toString()
                    ?: v.tag?.toString()
                    ?: v::class.simpleName
                speakIfChanged(label ?: return@setOnFocusChangeListener)
            }
        }
    }
}
```

#### C. ThemeService (고대비 테마 전환)

```kotlin
object ThemeService {
    var isHighContrast = false

    fun toggle(activity: AppCompatActivity) {
        isHighContrast = !isHighContrast
        // 테마를 바꾸고 Activity 재시작으로 전체 적용
        activity.setTheme(
            if (isHighContrast) R.style.Theme_AccessKit_HighContrast
            else R.style.Theme_AccessKit
        )
        activity.recreate()
    }
}
```

고대비 테마 명도 대비율: WCAG AA 기준 4.5:1 이상 충족 (배경 #000000 / 텍스트 #FFFFFF 또는 #FFFF00)

#### D. FocusNavigator (키패드 방향키 포커스 이동)

```kotlin
// MainActivity에서 dispatchKeyEvent override
override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (event.action == KeyEvent.ACTION_DOWN) {
        val focused = currentFocus ?: return super.dispatchKeyEvent(event)
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN  -> focused.focusSearch(View.FOCUS_DOWN)?.requestFocus()
            KeyEvent.KEYCODE_DPAD_UP    -> focused.focusSearch(View.FOCUS_UP)?.requestFocus()
            KeyEvent.KEYCODE_DPAD_LEFT  -> focused.focusSearch(View.FOCUS_LEFT)?.requestFocus()
            KeyEvent.KEYCODE_DPAD_RIGHT -> focused.focusSearch(View.FOCUS_RIGHT)?.requestFocus()
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER -> { focused.performClick(); return true }
            else -> return super.dispatchKeyEvent(event)
        }
        return true
    }
    return super.dispatchKeyEvent(event)
}
```

#### E. Focus Indicator (drawable selector)

```xml
<!-- res/drawable/focused_background.xml -->
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true">
        <shape>
            <stroke android:width="3dp" android:color="@color/focus_indicator" />
            <corners android:radius="6dp" />
        </shape>
    </item>
    <item>
        <shape>
            <corners android:radius="6dp" />
        </shape>
    </item>
</selector>
```

---

## 5. 예상 개발 일정 (6일)

| 일차 | 작업 내용 | 산출물 |
|---|---|---|
| Day 1 | 프로젝트 셋업, 기본 구조, MainActivity + 3개 Fragment 뼈대, AccessibilityBottomBar 레이아웃 | 빌드 성공하는 뼈대 앱 |
| Day 2 | TtsService 구현 (중복 억제 포함), 모든 버튼/텍스트에 contentDescription 설정, 포커스 TTS 연결 | TTS 동작 확인 |
| Day 3 | 고대비 테마 2종(themes.xml / themes_high_contrast.xml) 작성, ThemeService 전환 로직, recreate() 처리 | 고대비 전환 동작 확인 |
| Day 4 | FocusNavigator + dispatchKeyEvent, Focus Indicator drawable, nextFocusDown/Up 설정, 키보드 시뮬레이션 테스트 | 키패드 포커스 이동 동작 확인 |
| Day 5 | VolumeService (AudioManager), 화면 확대/축소 (ScaleAnimation), ChecklistFragment 구현 항목 정리 | 모든 기능 동작 완성 |
| Day 6 | README 작성, 스크린샷/GIF 캡처, GitHub push, Notion DB 업로드 | 포트폴리오 등록 완료 |

**버퍼:** Day 7~8 (예상치 못한 에뮬레이터 이슈, 키이벤트 처리 트러블슈팅 대비)

---

## 6. 완료 기준 (Definition of Done)

- [ ] `./gradlew build` — 빌드 오류 없음
- [ ] `./gradlew connectedAndroidTest` 또는 에뮬레이터에서 전체 기능 수동 테스트 통과
- [ ] TTS: 버튼/텍스트 포커스 시 음성 출력, 동일 항목 재포커스 시 중복 억제 확인
- [ ] 고대비: 전환 버튼 탭 → 전체 화면 테마 전환, WCAG AA 4.5:1 이상 대비율
- [ ] 키패드 방향키: 에뮬레이터 D-pad 또는 키보드 방향키로 포커스 이동 확인
- [ ] Focus Indicator: 포커스된 요소에 3dp 이상 테두리 노출
- [ ] 하단 접근성 바: 모든 화면에서 항상 노출
- [ ] 음량 조절: 증감 버튼 동작 확인
- [ ] GitHub 저장소 공개 push 완료
- [ ] README에 기능별 스크린샷 또는 GIF 1개 이상 포함
- [ ] Notion 프로젝트 DB 업로드 완료
- [ ] 위시켓 지원서 재작성 요청

---

## 7. 리스크 및 대응

| 리스크 | 발생 가능성 | 대응 |
|---|---|---|
| recreate() 호출 시 Fragment 상태 유실 | 중 | savedInstanceState 또는 SharedPreferences로 테마 상태 보존 |
| 에뮬레이터에서 하드웨어 키패드 시뮬레이션 불완전 | 중 | D-pad 에뮬레이션 + 실제 디바이스 테스트 권장 (실 기기 없으면 README에 에뮬레이터 D-pad 기준으로 명시) |
| TTS 초기화 딜레이 (OnInitListener 비동기) | 낮 | isInitialized 플래그로 guard 처리 |
| 고대비 테마에서 특정 시스템 컴포넌트(Dialog, Toast) 미적용 | 낮 | MaterialAlertDialogBuilder로 커스텀 다이얼로그 교체 |
| 화면 확대/축소 — 전체 뷰포트 vs 특정 영역 범위 불명확 | 중 | MVP에서는 특정 콘텐츠 영역 ScaleAnimation으로 구현. 전체 뷰포트 zoom은 Phase 2로 분리 |

---

## 8. WPF 기존 데모 → Android Kotlin 이식 대응표

포트폴리오 지원서에서 "동일 원리 경험"을 주장할 때 활용한다.

| WPF 구현 | Android Kotlin 이식 | 이식 난이도 |
|---|---|---|
| System.Speech.Synthesis.SpeechSynthesizer | android.speech.tts.TextToSpeech | ⭐⭐ (API 구조 유사) |
| SpeakOnFocusBehavior (중복 억제) | OnFocusChangeListener + 시간/텍스트 중복 체크 | ⭐⭐ |
| ResourceDictionary semantic token | styles.xml + @attr 참조 | ⭐⭐⭐ |
| Theme.HighContrast.xaml | themes_high_contrast.xml + recreate() | ⭐⭐⭐ |
| LayoutModeService (테마/레이아웃 분리) | 별도 LayoutModeService object | ⭐⭐ |
| KeyboardFocusBehavior (방향키) | dispatchKeyEvent + focusSearch() | ⭐⭐⭐ |
| FocusVisualStyle | drawable state_focused selector | ⭐⭐ |
| AccessibleButton (50px+) | minWidth/minHeight 56dp + contentDescription | ⭐ |
| ModeToolbar (하단 컨트롤러) | Custom View + include layout | ⭐⭐ |

난이도 기준: ⭐ 쉬움 / ⭐⭐ 보통 / ⭐⭐⭐ 학습 필요

---

## 9. 포트폴리오 등록 시 강조 포인트

1. **동일 접근성 기능을 두 플랫폼(WPF + Android)에서 모두 구현** → 플랫폼 종속이 아닌 접근성 설계 철학 보유를 증명
2. **WPF 데모의 핵심 설계 결정(테마/레이아웃 분리, TTS 중복 억제)을 Android에서도 동일하게 적용** → 단순 API 따라하기가 아닌 아키텍처 이해 증명
3. **공고의 기능 요구사항 2-1~2-3을 README 체크리스트에 1:1 매핑** → 클라이언트가 GitHub에서 30초 안에 확인 가능
4. **외부 라이브러리 없이 네이티브 SDK만 사용** → 기존 코드베이스에 의존성 추가 없이 이식 가능함을 어필

---

## 10. 완성 후 다음 단계

1. GitHub 저장소 공개 push: `github.com/Taek-D/android-barrier-free-demo` (예상 URL)
2. Notion 프로젝트 DB에 업로드
3. 위시켓 지원서 재작성 요청 (이 기획서와 함께 공고 URL 재첨부)
4. 지원서에서 이 프로젝트를 Portfolio 1번 + 유사 경험 섹션에 반영
