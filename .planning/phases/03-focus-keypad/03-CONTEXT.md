# Phase 3: Focus & Keypad — Context

**Gathered:** 2026-05-07
**Status:** Ready for planning
**Mode:** Inline autonomous
**Risk:** 🔴 HIGH (C-3, C-4, C-5)

<domain>
## Phase Boundary

외부 키패드(에뮬 D-pad) 기반 방향키 네비게이션. Critical pitfall 5개 중 3개 집중. 와이어링 순서 엄격: 1) FocusNavigator pure function, 2) dispatchKeyEvent 화이트리스트, 3) XML nextFocus* 양방향 연결, 4) state_focused selector + android:foreground.

Requirements: FOCUS-01~05.

</domain>

<decisions>
## Implementation Decisions (locked)

### dispatchKeyEvent 정책 (C-3 가드)
- **위치:** `MainActivity.dispatchKeyEvent`.
- **화이트리스트:** `KEYCODE_DPAD_UP/DOWN/LEFT/RIGHT/CENTER`, `KEYCODE_ENTER`, `KEYCODE_NUMPAD_ENTER`.
- **ACTION 한정:** `event.action == ACTION_DOWN`만 처리. UP/DOWN 분리.
- **나머지 키:** 즉시 `super.dispatchKeyEvent(event)` 위임 (BACK, EditText 입력, VOLUME, KEYCODE_HOME 등).
- **DPAD_CENTER/ENTER:** `currentFocus?.performClick()` 후 true 반환.
- **DPAD_UP/DOWN/LEFT/RIGHT:** `FocusNavigator.move(currentFocus, dir)` 호출. 결과가 새 뷰면 `requestFocus()` 후 true. null이면 super 위임 (시스템 기본 focus search).

### FocusNavigator (pure function, 단순화)
- **위치:** `accessibility/FocusNavigator.kt`.
- **API:** `fun move(currentFocus: View?, direction: Int): View?`. 내부적으로는 `currentFocus.focusSearch(direction)`을 그대로 사용.
- **추가 책임:** 없음. XML `nextFocus*` 명시 연결로 모든 wraparound이 처리되므로 navigator는 wrapper 수준 + 미래 확장 슬롯.
- **목적:** 테스트 가능한 분리 + 향후 커스텀 정책 hook(예: 첫 진입 시 `firstFocusable` 반환) 자리.
- 별도 메서드 `findFirstFocusable(root: View): View?` 제공 — recreate 후 초기 포커스 복원에 사용.

### XML nextFocus* 와이어링 (C-4 가드)
- **콘텐츠 ↔ BottomBar 양방향:**
  - HomeFragment: `btn_open_checklist`(맨 아래)의 `nextFocusDown` → BottomBar 첫 버튼(`btn_high_contrast`).
  - MenuFragment: `menu_item_confirm`(맨 아래)의 `nextFocusDown` → BottomBar.
  - ChecklistFragment: placeholder TextView 아래에 "뒤로가기" 버튼 추가 → `nextFocusDown` → BottomBar.
  - BottomBar 첫 버튼의 `nextFocusUp` → 콘텐츠 마지막 포커서블 (Fragment마다 다름) — 동적이므로 코드에서 `requestFocus()` 처리. XML 정적 연결은 같은 fragment ID 참조 불가.
  - **간소화 결정:** XML에서는 콘텐츠 → BottomBar `nextFocusDown`만 정적 연결. BottomBar → 콘텐츠 복귀(`nextFocusUp`)는 코드에서 fragment 의존 동적 처리: BottomBar의 첫 버튼이 포커스된 상태에서 UP DPAD 시 dispatchKeyEvent가 가로채 `findLastFocusableInContent()` 결과로 이동.
- **BottomBar 내부 좌우:** XML에서 `nextFocusLeft/Right`로 4 버튼 cyclic 연결.

### dispatchKeyEvent UP DPAD 특수 처리 (C-4 보강)
- 현재 포커스가 BottomBar 자식이고 KEYCODE_DPAD_UP인 경우 → 콘텐츠 영역 마지막 포커서블에 requestFocus.
- 그 외 UP은 `super.dispatchKeyEvent` 위임 (focusSearch가 자동 처리).

### state_focused selector (C-5)
- **위치:** `res/drawable/focused_background.xml` — 4-state layer-list.
- **state_focused="true":** stroke 3dp `@color/a11y_focus_stroke` (light: red `#D32F2F`, HC: yellow `#FFFF00`) 위 transparent. `<corners android:radius="6dp"/>`.
- **default:** transparent.
- **충돌 회피:** Material ripple 미사용(외부 의존성 0). 그러나 `?selectableItemBackground` 자체에 ripple drawable이 들어가 있을 수 있음. `android:foreground="@drawable/focused_background"` (API 23+) 사용 → background와 layer 분리.
- **적용 대상:** BottomBar 4 ImageButton + Home/Menu/Checklist의 모든 Button.
- **HC 변형:** `res/drawable/focused_background.xml`에서 stroke color를 attr `?attr/a11yFocusStroke`로 참조 → themes.xml 두 테마에서 attr 매핑.

### 초기 포커스 복원
- `MainActivity.onResume` 또는 Fragment `onViewCreated`에서 `view.post { firstFocusable.requestFocus() }`. recreate 후에도 동작 (savedInstanceState != null이어도 system이 자동 복원하지만 안 되는 경우 명시 fallback).

### Phase 1/2 변경 요약
- `MainActivity.kt`: `dispatchKeyEvent` override 추가, BottomBar UP DPAD 콘텐츠 복귀 helper, `FocusNavigator` 활용.
- 모든 fragment_*.xml: 마지막 콘텐츠 버튼에 `nextFocusDown="@id/btn_high_contrast"` 추가, BottomBar로 명시 연결.
- 모든 Button/ImageButton: `android:foreground="@drawable/focused_background"`, `android:focusable="true"`.

</decisions>

<code_context>
- BottomBar 4버튼 ID: btn_high_contrast, btn_tts, btn_zoom_in, btn_zoom_out (Phase 1 확정).
- HomeFragment: btn_open_menu(top), btn_open_checklist(bottom).
- MenuFragment: menu_item_americano/latte/green_tea/confirm.
- ChecklistFragment: 현재 Title + Placeholder. 뒤로가기 버튼 추가 필요.
</code_context>

<specifics>
## Critical Pitfall Watches

| ID | One-liner |
|----|-----------|
| C-3 | dispatchKeyEvent: 화이트리스트 + ACTION_DOWN + 외 super 위임. BACK/EditText/VOLUME 보호. |
| C-4 | XML nextFocusDown(content→BottomBar) 정적 + dispatchKeyEvent UP(BottomBar→content) 동적 양방향. |
| C-5 | android:foreground + state_focused selector + attr ?a11yFocusStroke로 HC 색상 동시 처리. |

</specifics>

<deferred>
- Phase 4: Volume/Zoom 와이어링 (BottomBar의 zoom 2버튼).
- Phase 5: 평가자 가시성 — 3dp stroke를 README 캡처에 빨간 화살표로 강조.

</deferred>
