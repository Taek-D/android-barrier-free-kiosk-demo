# Project State

**Project:** Android Kotlin 베리어프리 접근성 데모 앱
**Last Updated:** 2026-05-07 (Phase 1 complete — 산출물 작성 + 커밋 완료, 사용자 환경 수동 검증 대기)

---

## Project Reference

- **PROJECT.md:** `.planning/PROJECT.md` (core value, constraints, key decisions)
- **REQUIREMENTS.md:** `.planning/REQUIREMENTS.md` (32 v1 requirements + traceability)
- **ROADMAP.md:** `.planning/ROADMAP.md` (5 phases, coarse granularity)
- **Research:** `.planning/research/SUMMARY.md` (HIGH confidence, build order §6)
- **PRD:** `PRD.md` (P0/P1/P2 + Day 1-6 timeline §8)

**Core Value:** 평가자가 GitHub README와 시연 GIF를 90초 안에 훑고 "이 사람 Android Accessibility 진짜 만들 줄 안다"고 결론짓게 하는 것. 다른 모든 가치보다 이 인지 전환이 우선한다.

**Hard Deadline:** 2026-05-15 (GitHub push + Notion + 위시켓 재제출). 공고 마감 2026-05-20.

---

## Current Position

| Field | Value |
|-------|-------|
| **Current phase** | Phase 3: Focus & Keypad 🔴 |
| **Next phase** | Phase 3 (in-progress) |
| **Current plan** | Phase 2 산출물 commit 완료. Phase 3 plan 작성 단계. |
| **Status** | Phase 2 ✅ artifacts complete (human_needed: 발화/HC 토글 1회 수동) |
| **Progress** | 2 / 5 phases complete |

```
[████████░░░░░░░░░░░░] 40% (2/5 phases)
```

---

## Phase Pipeline

| # | Phase | Status | Risk | UI |
|---|-------|--------|------|-----|
| 1 | Foundation | ✅ Complete (human-needed) | 🟢 Low | yes |
| 2 | TTS + Theme | ✅ Complete (human-needed) | 🟡 Med | yes |
| 3 | Focus & Keypad | In progress | 🔴 **HIGH** | yes |
| 4 | Media & Checklist | Not started | 🟢 Low | yes |
| 5 | Ship | Not started | 🟡 Med | no |

---

## Performance Metrics

| Metric | Value |
|--------|-------|
| v1 requirements | 32 |
| Phases | 5 (coarse) |
| Coverage | 100% (32/32 mapped) |
| Plans created | 0 |
| Plans complete | 0 |
| Calendar days budgeted | 6 + 2 buffer |
| Days elapsed | 0 |
| Days remaining to hard deadline | 8 (to 2026-05-15) |

---

## Accumulated Context

### Key Decisions (locked in PROJECT.md / SUMMARY)

- XML Layout (View/Fragment) 채택, Compose 배제 — 클라이언트 코드베이스 호환성 우선
- 외부 의존성 0 (AndroidX 5종만, Material 미포함)
- AGP 8.7.3 + Gradle 8.11.1 + Kotlin 2.1.0 + JDK 17 + minSdk 26 / target 35
- 테마 전환 = `recreate()` + `setTheme()` BEFORE `setContentView()`
- TTS 중복 억제 = 시간(500ms) + 텍스트 비교 (`speakIfChanged`)
- 화면 확대/축소 = 콘텐츠 영역 ScaleAnimation (전체 뷰포트 Magnification은 Phase 2)
- 검증 환경 = Android Studio Emulator + `hw.dPad=yes` AVD + Numpad 매핑 + Google TTS 한국어
- F-12 SharedPreferences 영속화 P0 승격 (Phase 2 내 처리)
- F-14 타임아웃 정책 P1 추가 (Phase 4 TIME-01)
- 시스템 `AccessibilityService` 미등록 — README "Scope" 섹션으로 의도된 범위 한정 명시

### Open Decisions (Day 1)

1. **저장소명** — `android-barrier-free-kiosk-demo` (권장) vs `android-accessibility-kiosk-kotlin`
2. **AVD 프로필 사양** — `hw.dPad=yes` + Numpad 매핑 + Google TTS 한국어 사전 설치 확정
3. **시연 영상 형식** — 결정됨: GIF (자막) 인라인 + MP4 링크 둘 다

### Open Decisions (발주처 확인 필요 — 지원 후 위시켓 메시지)

4. "Accessibility Service 제어 경험" — 시스템 서비스 strict 여부. README "Scope" 헤지로 보험.
5. 화면 확대/축소 — 콘텐츠 영역 vs 전체 뷰포트 (PRD는 콘텐츠 영역 결정)

### Top Risks

- 🔴 **Phase 3 (Day 4)** — Critical pitfall 5개 중 3개(C-3/C-4/C-5)가 집중. 와이어링 순서: FocusNavigator → dispatchKeyEvent → XML `nextFocus*`. 역순 = silent breakage.
- 🟡 **Phase 5 (Day 6)** — Portfolio pitfall P-1~P-5 (GIF 자막, stroke 가시성, 1스크롤 매핑 표, 저장소명, KWCAG 인용)
- 🟡 **Phase 2 TTS init race** (C-1) — `@Volatile ready` + pending 큐 누락 시 첫 안내 묵음
- 🟡 **Phase 2 recreate 좀비 TTS** (C-2) — `object TtsService` + applicationContext

### Blockers

(none)

### Todos (rolling)

- [ ] `/gsd-plan-phase 1`로 Phase 1 plan 분해 (next action)
- [ ] Day 1 셋업 시 저장소명 확정
- [ ] Day 1 셋업 시 `hw.dPad=yes` AVD 동작 검증 (Phase 3 위험 차단)

---

## Session Continuity

**Last session:** 2026-05-07 — Roadmap 초기화 (`gsd-roadmapper`)
- Read PROJECT.md / REQUIREMENTS.md / research/SUMMARY.md / config.json / PRD.md §8
- Derived 5 phases from research SUMMARY §6 build order (coarse granularity)
- Mapped all 32 v1 requirements (100% coverage, 0 orphans)
- Wrote ROADMAP.md, STATE.md; updated REQUIREMENTS.md Traceability

**Next session:** Run `/gsd-plan-phase 1` to decompose Phase 1 (Foundation) into executable plans.

---

*State initialized: 2026-05-07*
