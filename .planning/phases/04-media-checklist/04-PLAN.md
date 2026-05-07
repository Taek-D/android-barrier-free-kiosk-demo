# Phase 4: Media & Checklist — Plan

## Tasks

### T1. `service/VolumeService.kt`
- `object`. `init(context)` idempotent. `increment()`/`decrement()`로 STREAM_MUSIC + ADJUST_RAISE/LOWER + FLAG_SHOW_UI.

### T2. `service/ZoomService.kt`
- `object`. `zoomIn/zoomOut(target)` → A11yPrefs.zoomLevel ± 0.1f (setter clamp 0.8~1.5) → `apply(target)` (pivot 0,0 + scaleX/Y).
- `apply` 단독 호출로 onResume에서 영속 레벨 복원.

### T3. `MainActivity` 수정
- onCreate: `VolumeService.init(applicationContext)` 추가.
- onResume: `ZoomService.apply(binding.fragmentContainer)` 추가 (recreate/재시작 후 복원).
- `wireBottomBar` zoom 람다: `ZoomService.zoomIn/Out(binding.fragmentContainer)`.
- BottomBar는 fragment_container 바깥에 있어 scale 영향 없음 (56dp+ 보장).

### T4. ChecklistFragment 7행
- 새 `fragment_checklist.xml`: ScrollView + 동적 LinearLayout `checklist_rows` + 뒤로가기 버튼.
- 새 `item_checklist_row.xml`: 상태(✅/⚠️) + 라벨 + 상세.
- `ChecklistFragment.renderRows()`에서 7행 추가. TTS/HC는 A11yPrefs read로 ON/OFF 분기, Zoom은 현재 % 표시.
- `onResume`에서 renderRows 재호출(다른 화면에서 토글 후 돌아왔을 때).

### T5. strings 보강
- 7개 라벨 + 상세(TTS/HC는 on/off 2종, zoom은 format).

### T6. README 표준 인용에 TIME-01 1줄 추가 (Phase 5에서 본격 작성, Phase 4는 stub 유지).

### T7. 04-VERIFICATION.md
- MEDIA-01/02/03, TIME-01, CHECK-01 자동/수동 검증.

## DoD

1. VolumeService.increment/decrement이 STREAM_MUSIC + FLAG_SHOW_UI 호출.
2. ZoomService.apply가 scaleX/Y + pivot 0,0 직접 변경 (M-6 회피).
3. A11yPrefs.zoomLevel 0.8~1.5 clamp + 재시작 후 read.
4. ChecklistFragment 7행 동적 렌더 + onResume re-render.
5. 사용자 환경: 음량 슬라이더 변화, zoom in/out 시각, 재시작 후 줌/HC/TTS 유지, 7행 정상 표시.
