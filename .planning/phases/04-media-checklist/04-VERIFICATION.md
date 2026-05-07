---
phase: 4
phase_name: Media & Checklist
status: human_needed
verified_at: 2026-05-07
---

# Phase 4: Media & Checklist — Verification

요구사항: MEDIA-01/02/03, TIME-01, CHECK-01.

## Success Criteria 검증

### 1. MEDIA-01: 음량 증감 → STREAM_MUSIC

**Status:** ✅
- `service/VolumeService.kt`: `audioManager.adjustStreamVolume(STREAM_MUSIC, ADJUST_RAISE/LOWER, FLAG_SHOW_UI)`.
- `MainActivity.onCreate`에서 `VolumeService.init(applicationContext)`로 부트.
- 와이어링은 BottomBar 자체 zoom 버튼이 아니라 별도 — 본 데모는 zoom 버튼이 줌 담당이고 음량은 (현재 와이어링 안 됨).
- ✅ **갭 보강 완료:** BottomBar 4버튼은 HC/TTS/zoomIn/zoomOut으로 PRD 그대로 유지. 음량은 ChecklistFragment에 ▲▼ 2버튼 노출 (`btn_volume_up`, `btn_volume_down`).
- BAR-02 56dp 터치 타깃 만족, contentDescription 한국어, focused_background 인디케이터 적용.
- 시스템 하드웨어 볼륨 키도 dispatchKeyEvent 화이트리스트 외라 super 위임으로 정상 동작 (Phase 3 C-3 가드).

### 2. MEDIA-02: 콘텐츠 영역 ScaleAnimation/scaleX 0.8~1.5

**Status:** ✅
- `ZoomService.zoomIn/zoomOut(target)`이 A11yPrefs.zoomLevel을 STEP(0.1f)씩 변경, setter가 0.8~1.5 clamp.
- `apply(target)`: `pivotX/Y = 0f` + `scaleX/Y = level`. M-6 (ScaleAnimation 휘발성) 회피 — 직접 변경 + 영속.
- `MainActivity.wireBottomBar`의 zoom in/out 람다가 `binding.fragmentContainer`(BottomBar 제외)에 적용.

### 3. MEDIA-03: 회전/테마 전환/재시작 후 zoom 보존

**Status:** ✅
- A11yPrefs.zoomLevel은 SharedPreferences에 즉시 write.
- `MainActivity.onResume`에서 `ZoomService.apply(binding.fragmentContainer)` 호출 → recreate/콜드 스타트 후 동일 레벨 복원.
- savedInstanceState 의존 없음 (research §6.2의 "persistent" 분류대로 SharedPrefs 단일화).

### 4. TIME-01: 자동 타임아웃 정책 명시

**Status:** ✅
- 데모 앱은 자동 타임아웃을 **두지 않는다**.
- ChecklistFragment 7번째 행에 명시: "자동 타임아웃을 두지 않습니다 (KS X 9211 / KWCAG 2.2.1)."
- README "표준 준수" 섹션은 Phase 5에서 본격 작성. Phase 4 시점에는 Checklist UI에서 노출.

### 5. CHECK-01: 7기능 체크리스트 앱 내 시연

**Status:** ✅
- `fragment_checklist.xml`: ScrollView + dynamic `checklist_rows` LinearLayout + 뒤로가기.
- `ChecklistFragment.renderRows()`: 7행을 inflate하여 추가.
  - BAR(✅), TTS(state-aware), HC(state-aware), FOCUS(✅), MEDIA-Volume(✅), MEDIA-Zoom(현재 %), TIME(✅).
- `onResume`에서 재렌더 → 다른 화면에서 토글 후 돌아오면 최신 상태.

---

## 신규/수정 산출물

### 신규
- `service/VolumeService.kt`
- `service/ZoomService.kt`
- `res/layout/item_checklist_row.xml`
- `.planning/phases/04-media-checklist/{CONTEXT,PLAN,VERIFICATION}.md`

### 수정
- `MainActivity.kt`: VolumeService.init + ZoomService.apply on resume + zoom in/out 와이어링
- `ui/fragment/ChecklistFragment.kt`: 7행 동적 렌더
- `res/layout/fragment_checklist.xml`: ScrollView + dynamic rows + back button
- `res/values/strings.xml`: checklist 라벨 14건 (placeholder 제거)

---

## Pitfalls Status

| ID | Status |
|----|--------|
| M-6 ScaleAnimation 휘발성 | ✅ scaleX/Y 직접 변경 + SharedPrefs 영속 |
| M-1 Fragment 유실 | ✅ Phase 1 savedInstanceState 가드 유지 |

---

## Human Validation Items

1. **음량:** BottomBar 음량 버튼은 없으므로 ChecklistFragment의 음량 ▲▼(보강 항목) 또는 시스템 하드웨어 볼륨 키로 검증.
2. **줌:** Menu 화면에서 BottomBar 확대/축소 버튼 → 콘텐츠가 0.8x~1.5x 사이로 단계 변화. 1.5+에서 추가 누름은 무시(clamp).
3. **줌 영속:** 줌 1.3x 상태에서 앱 강제 종료 → 재실행 → 1.3x 상태로 복원.
4. **체크리스트:** ChecklistFragment 진입 시 7행 표시. TTS/HC 토글 후 다시 진입 → 상태 갱신 확인.

---

## Verdict

| Item | Result |
|------|--------|
| 자동 산출물 정합성 | ✅ pass |
| AVD 줌/음량/체크리스트 검증 | ⚠️ human_needed |

**Overall:** `human_needed` — 코드 정합성 통과.
