# Phase 3: Focus & Keypad — Plan

**Risk:** 🔴 HIGH. 와이어링 순서 절대 준수: T1 → T2 → T3 → T4. 역순 = silent breakage.

## Tasks

### T1. `accessibility/FocusNavigator.kt`
- `object FocusNavigator`.
- `fun move(currentFocus: View?, direction: Int): View?` = `currentFocus?.focusSearch(direction)`.
- `fun findFirstFocusable(root: View): View?` — DFS, isFocusable && !isInTouchMode 자식 우선. 미사용 시 root 자체 반환.
- `fun findLastFocusableInContent(root: View): View?` — 콘텐츠 영역에서 BottomBar 제외하고 마지막 포커서블.

### T2. `MainActivity.dispatchKeyEvent` (C-3 화이트리스트)
- `override fun dispatchKeyEvent(event: KeyEvent): Boolean`.
- ACTION_DOWN 외 즉시 super.
- DPAD_UP 특수 처리: `currentFocus`가 `accessibility_bottom_bar`의 자식이면 `FocusNavigator.findLastFocusableInContent(binding.fragmentContainer).requestFocus()` → true.
- DPAD_DOWN/LEFT/RIGHT: `FocusNavigator.move(currentFocus, FOCUS_*)` → 결과 있으면 requestFocus + true. 없으면 super.
- DPAD_CENTER / ENTER / NUMPAD_ENTER: `currentFocus?.performClick()` true.
- 그 외 키: 즉시 super.

### T3. XML nextFocus* 정적 연결 (C-4)
- `fragment_home.xml#btn_open_checklist` → `android:nextFocusDown="@id/btn_high_contrast"`.
- `fragment_menu.xml#menu_item_confirm` → `android:nextFocusDown="@id/btn_high_contrast"`.
- `fragment_checklist.xml`에 "뒤로가기" 버튼 추가 → `android:nextFocusDown="@id/btn_high_contrast"`.
- `view_accessibility_bottom_bar.xml`: 4버튼 cyclic — `btn_high_contrast.nextFocusRight=btn_tts`, ..., `btn_zoom_out.nextFocusRight=btn_high_contrast`. 좌측은 역순.

### T4. focused selector + foreground (C-5)
- `res/drawable/focused_background.xml`:
  ```xml
  <selector>
    <item android:state_focused="true">
      <shape android:shape="rectangle">
        <stroke android:width="@dimen/a11y_focus_stroke" android:color="?attr/a11yFocusStroke"/>
        <corners android:radius="6dp"/>
        <solid android:color="@android:color/transparent"/>
      </shape>
    </item>
    <item><shape android:shape="rectangle"><solid android:color="@android:color/transparent"/></shape></item>
  </selector>
  ```
- `attrs.xml`: `<attr name="a11yFocusStroke" format="color"/>` 추가.
- `themes.xml`: light에서 `a11yFocusStroke=@color/a11y_focus_stroke` (red), HC에서 `=@color/a11y_hc_primary` (yellow).
- 모든 Button/ImageButton에 `android:foreground="@drawable/focused_background"` + `android:focusable="true"`.

### T5. ChecklistFragment 뒤로가기 버튼
- `fragment_checklist.xml`에 `Button` 추가 (`@string/checklist_back`). onClick → `parentFragmentManager.popBackStack()`.
- ChecklistFragment.kt에서 binding으로 setOnClickListener.

### T6. 초기 포커스 복원
- 각 Fragment `onViewCreated`에서 `view.post { FocusNavigator.findFirstFocusable(view)?.requestFocus() }`.
- recreate 후에도 동일 호출.

### T7. strings.xml 보강
- `checklist_back` = "뒤로가기".

### T8. 03-VERIFICATION.md
- FOCUS-01~05 자동 + 수동 검증 분리.

## DoD

1. dispatchKeyEvent 화이트리스트 외 키 전부 super 위임 — code inspect.
2. XML nextFocusDown 콘텐츠 → BottomBar 3개 fragment 모두 명시.
3. android:foreground + focused_background.xml + a11yFocusStroke attr 매핑 light/HC.
4. recreate 후 first focusable 자동 포커스 (fragment lifecycle hook).
5. 사용자 환경: D-pad ↑↓←→ 동작 + ENTER 클릭 + BACK/VOLUME 정상 + 3dp stroke 시각.
