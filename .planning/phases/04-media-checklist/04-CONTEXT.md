# Phase 4: Media & Checklist — Context

**Gathered:** 2026-05-07
**Status:** Ready for planning
**Mode:** Inline autonomous

<domain>
## Phase Boundary

음량/줌 미디어 컨트롤 + ChecklistFragment 7기능 표시 + 타임아웃 정책 명시. 앞 Phase 서비스 시그니처가 안정화된 뒤 마지막으로 Checklist 와이어링.

Requirements: MEDIA-01/02/03, TIME-01, CHECK-01.

</domain>

<decisions>
## Implementation Decisions (locked)

### VolumeService (MEDIA-01)
- **위치:** `service/VolumeService.kt` `object`.
- **API:**
  - `fun init(context: Context)` — `applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager`. idempotent.
  - `fun increment()` — `audioManager.adjustStreamVolume(STREAM_MUSIC, ADJUST_RAISE, FLAG_SHOW_UI)`.
  - `fun decrement()` — `ADJUST_LOWER`.
- **Stream:** `AudioManager.STREAM_MUSIC` (research §F-9).
- **시각 피드백:** `FLAG_SHOW_UI`로 시스템 볼륨 슬라이더 노출 → 평가자 검증 가시성.

### ZoomService (MEDIA-02/03, M-6 가드)
- **위치:** `service/ZoomService.kt` `object`.
- **API:**
  - `fun zoomIn(target: View)` — A11yPrefs.zoomLevel + 0.1f → setter clamp(0.8f~1.5f) → `apply(target)`.
  - `fun zoomOut(target: View)` — - 0.1f → 동일.
  - `fun apply(target: View)` — `target.scaleX = level; target.scaleY = level; target.pivotX = 0f; target.pivotY = 0f`.
  - `fun current(): Float` — A11yPrefs.zoomLevel.
- **영속화 정책:** 줌 레벨은 SharedPreferences(A11yPrefs)에 저장. research §6.2와 약간 다른 결정 — 회전/recreate에서 savedInstanceState도 가능하지만, "재시작 후에도 유지"가 도메인 신호로 더 강해 SharedPrefs 단일화. M-6의 "ScaleAnimation 휘발성"은 scaleX/scaleY 직접 변경 + 영속 read로 회피.
- **호출자:** `MainActivity.wireBottomBar`의 zoom in/out 람다에서 `MenuFragment`가 활성일 때만 `menuZoomTarget`에 적용. 다른 fragment에서는 toast/안내 없이 무시 (단순화). 또는 모든 fragment의 root scale.
- **간소화:** 모든 fragment의 root에 적용하면 BottomBar 자신은 제외해야 한다. 결정: BottomBar는 Activity 루트에 있고 scale 대상은 `binding.fragmentContainer`만 → BottomBar는 scale되지 않고 원본 56dp 유지.

### ChecklistFragment 7기능 표시 (CHECK-01)
- 7행: BAR / TTS / HC / FOCUS / MEDIA-Volume / MEDIA-Zoom / TIME.
- 각 행: 라벨 + 상태(✅/⚠️/❌) + 짧은 설명.
- 자동 검증 가능한 항목:
  - BAR: BottomBar 보이면 ✅
  - TTS: A11yPrefs.ttsEnabled
  - HC: A11yPrefs.highContrastEnabled
  - FOCUS: 항상 ✅ (코드 정합성)
  - Volume: 항상 ✅ (서비스 wire)
  - Zoom: A11yPrefs.zoomLevel != 1.0f이면 "현재 N%"
  - TIME: 항상 ✅ (자동 타임아웃 없음 명시)
- 초간단 LinearLayout + 7개 row include. CHECK-01 핵심은 "앱 내에서 한 화면으로 시연·확인" — 동적 상태 read만 잘 되면 충분.

### TIME-01 정책
- 데모 앱은 **자동 타임아웃을 두지 않는다** (가장 단순 정책). KS X 9211 응답시간 / KWCAG 2.2.1 대응.
- README.md 표준 준수 섹션에 1줄 명시: "본 데모는 자동 타임아웃을 두지 않습니다 (KS X 9211 / KWCAG 2.2.1)."
- ChecklistFragment의 TIME 행에서 동일 문구 표시.

### Phase 1~3 변경 요약
- `MainActivity.wireBottomBar`: zoom in/out 람다 채움. 활성 Fragment 판별: `supportFragmentManager.findFragmentById(R.id.fragment_container)`. MenuFragment면 `menuZoomTarget`에 적용. 그 외에는 fragment_container 자체에 적용해도 됨 (보편적).
- 결정: zoom in/out은 **항상 `binding.fragmentContainer`에 적용** (모든 fragment 공통). BottomBar는 별도 루트에 있어 영향 없음.
- VolumeService.init은 MainActivity.onCreate에서 추가.

### Resources
- strings: 7개 checklist 라벨 + 짧은 설명.
- layout: `fragment_checklist.xml` 재작성 — Phase 3에서 추가한 뒤로가기는 유지하면서 7행 추가.

</decisions>

<code_context>
- A11yPrefs.zoomLevel은 Phase 1에서 정의된 그대로 사용 (0.8~1.5 clamp).
- MenuFragment의 menuZoomTarget은 Phase 1에서 이미 LinearLayout으로 정의됨.
- BottomBar zoom 2버튼은 Phase 2에서 빈 람다로 와이어링 자리 확보됨.
</code_context>

<specifics>
## Pitfalls Watch

- **M-6 (ScaleAnimation 휘발성):** scaleX/scaleY 직접 변경 + A11yPrefs read로 영속.
- **M-1 (Fragment 유실):** savedInstanceState 가드는 Phase 1에서 이미 처리.
</specifics>

<deferred>
- Phase 5: README "표준 준수" 섹션에 KS X 9211/KWCAG 2.2.1 응답시간 명시.
</deferred>
