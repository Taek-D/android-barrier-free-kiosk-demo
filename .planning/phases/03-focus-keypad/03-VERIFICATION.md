---
phase: 3
phase_name: Focus & Keypad
status: human_needed
verified_at: 2026-05-07
risk: HIGH
---

# Phase 3: Focus & Keypad — Verification

요구사항: FOCUS-01~05.

## Success Criteria 검증

### 1. FOCUS-01: D-pad ↑↓←→로 모든 Fragment 화면에서 포커스 이동

**Status:** ✅ structural / ⚠️ runtime
- ✅ `MainActivity.dispatchKeyEvent`가 KEYCODE_DPAD_UP/DOWN/LEFT/RIGHT를 ACTION_DOWN에서만 처리.
- ✅ `FocusNavigator.move(currentFocus, direction)` = `currentFocus.focusSearch(direction)`로 위임 → 시스템 표준 동작.
- ✅ 결과 view가 있으면 `requestFocus()` + `return true`. 없으면 `super.dispatchKeyEvent` 위임.
- ✅ 각 Fragment `onViewCreated`에서 `view.post { findFirstFocusable.requestFocus() }`로 첫 진입 포커스 보장.

### 2. FOCUS-02: ENTER/DPAD_CENTER로 클릭

**Status:** ✅
- `KEYCODE_DPAD_CENTER`, `KEYCODE_ENTER`, `KEYCODE_NUMPAD_ENTER` 세 코드 모두 → `currentFocus?.performClick()`.

### 3. FOCUS-03: BACK/EditText/VOLUME 등 비방향키는 super 위임 (C-3)

**Status:** ✅
- dispatchKeyEvent의 `else -> super.dispatchKeyEvent(event)` 분기로 화이트리스트 외 모든 keyCode가 시스템에 위임됨.
- `event.action != KeyEvent.ACTION_DOWN` 가드도 super 위임 → ACTION_UP 이벤트 흡수 안 함.
- ⚠️ 사용자 환경: BACK 키로 Fragment popBackStack, 볼륨 키로 시스템 음량 제어 동작 확인 필요.

### 4. FOCUS-04: 콘텐츠 ↔ BottomBar 양방향 (C-4)

**Status:** ✅
- ✅ XML 정적 (콘텐츠 → BottomBar):
  - `fragment_home.xml#btn_open_checklist@nextFocusDown=@id/btn_high_contrast`
  - `fragment_menu.xml#menu_item_confirm@nextFocusDown=@id/btn_high_contrast`
  - `fragment_checklist.xml#btn_checklist_back@nextFocusDown=@id/btn_high_contrast`
- ✅ 코드 동적 (BottomBar → 콘텐츠):
  - `MainActivity.handleUp`: `currentFocus`가 BottomBar 자식이면 `FocusNavigator.findLastFocusable(binding.fragmentContainer)`로 콘텐츠 마지막 포커서블 복귀.
- ✅ BottomBar 내부 좌우 cyclic: `view_accessibility_bottom_bar.xml`에서 4 ImageButton의 `nextFocusLeft/Right` 양방향 연결.

### 5. FOCUS-05: state_focused 3dp stroke + ripple 충돌 회피 (C-5)

**Status:** ✅
- ✅ `res/drawable/focused_background.xml`: state_focused 시 `<stroke android:width="@dimen/a11y_focus_stroke" android:color="?attr/a11yFocusStroke"/>`. width=3dp, color는 테마 attr 분기.
- ✅ `attrs.xml`에 `a11yFocusStroke` 추가. light=`@color/a11y_focus_stroke`(red `#D32F2F`), HC=`@color/a11y_hc_primary`(yellow `#FFFF00` on black = 19.56:1).
- ✅ 모든 Button/ImageButton에 `android:foreground="@drawable/focused_background"` + `android:focusable="true"` 적용 (grep 매칭 확인).
- ✅ background는 `?selectableItemBackground` 유지 → ripple과 layer 분리(API 23+).

### 6. recreate 후 초기 포커스 복원

**Status:** ✅ structural
- 모든 Fragment `onViewCreated`에서 `view.post { firstFocusable.requestFocus() }` 호출.
- ThemeService.toggle → recreate → MainActivity 재생성 → savedInstanceState != null 가드로 Fragment 중복 commit 회피, 시스템이 복원한 Fragment의 `onViewCreated`가 다시 실행되어 포커스 복원.

---

## Pitfalls Status

| ID | Watched | Status |
|----|---------|--------|
| C-3 | dispatchKeyEvent 모든 키 흡수 | ✅ 화이트리스트 + ACTION_DOWN 한정 + 외 super 위임 |
| C-4 | 콘텐츠 → BottomBar 점프 후 복귀 불가 | ✅ XML nextFocusDown 정적 + handleUp 동적 양방향 |
| C-5 | selector vs ripple 충돌 | ✅ android:foreground (API 23+) + state_focused selector 분리 |

---

## 신규/수정 산출물

### 신규
- `accessibility/FocusNavigator.kt`
- `res/drawable/focused_background.xml`
- `.planning/phases/03-focus-keypad/{CONTEXT,PLAN,VERIFICATION}.md`

### 수정
- `MainActivity.kt` — dispatchKeyEvent override + handleUp + isInBottomBar
- `res/layout/{fragment_home,fragment_menu,fragment_checklist,view_accessibility_bottom_bar}.xml` — focusable + foreground + nextFocus*
- `res/values/{attrs.xml, themes.xml, themes_high_contrast.xml, strings.xml}` — a11yFocusStroke attr 매핑 + checklist_back
- `ui/fragment/{Home,Menu,Checklist}Fragment.kt` — view.post requestFocus 추가; ChecklistFragment 뒤로가기 핸들러

---

## Human Validation Items

1. **D-pad 이동:** AVD Numpad 8/2/4/6으로 모든 화면에서 포커스가 자연스럽게 이동.
2. **클릭:** Numpad 5/Enter로 포커스된 버튼이 클릭됨 (Fragment 전환, TTS/HC 토글).
3. **콘텐츠 → BottomBar 복귀:** 콘텐츠 마지막 버튼에서 ↓ → BottomBar 첫 버튼. BottomBar 임의 버튼에서 ↑ → 콘텐츠 마지막 버튼.
4. **BACK/볼륨:** BACK 키로 Fragment popBackStack, 볼륨 하드웨어 키로 시스템 음량 변경.
5. **3dp stroke:** 포커스된 버튼에 빨간(또는 HC 노랑) 3dp stroke 시각 확인. recreate 후에도 유지.
6. **재시작:** 앱 재시작 → 첫 진입 시 자동 포커스 복원.

---

## Verdict

| Item | Result |
|------|--------|
| 자동 산출물 정합성 (C-3/C-4/C-5 + FOCUS-01~05) | ✅ pass |
| AVD D-pad/recreate/BACK 검증 | ⚠️ human_needed |

**Overall:** `human_needed` — 코드 정합성 통과. AVD 1회 manual run으로 closure.
