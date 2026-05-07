# Android Kotlin 베리어프리 접근성 데모 앱 — Project Guide

> 위시켓 "키오스크 앱 베리어프리 기능 구현" 공고(마감 2026-05-20) 포트폴리오용 데모. 6일 안에 GitHub 공개 + 위시켓 재제출.

## Source of Truth (read in this order)

| File | Role |
|------|------|
| `.planning/PROJECT.md` | What This Is · Core Value · Constraints · Key Decisions |
| `.planning/REQUIREMENTS.md` | 32 v1 requirements + traceability + Phase 매핑 |
| `.planning/ROADMAP.md` | 5 phases (coarse) + dependency graph + day-to-phase calendar |
| `.planning/STATE.md` | current/next phase · top risks · open decisions |
| `.planning/research/SUMMARY.md` | Stack/Features/Architecture/Pitfalls 종합 (HIGH confidence) |
| `PRD.md` | 평가자 1차 시각의 P0/P1/P2 + Day 1-6 일정 |
| `android_barrier_free_demo_plan.md` | 최초 기획서 (참고용, PRD가 우선) |

`.planning/research/{STACK,FEATURES,ARCHITECTURE,PITFALLS}.md`는 SUMMARY가 압축본이므로 SUMMARY를 먼저 읽고 필요 시 원본 참조.

## GSD Workflow Conventions

- **Granularity:** coarse (5 phases) — `.planning/config.json`
- **Mode:** YOLO / auto-advance
- **Plan check + Verifier:** ON
- **모델 프로파일:** balanced (Sonnet 위주)
- **Commits:** `.planning/` 문서 git 추적 (`commit_docs: true`)
- **Atomic commits:** 각 phase 산출물은 즉시 커밋 (config / PROJECT / research / requirements / roadmap 별로 분리됨)

### Next-step commands

```
/gsd-plan-phase 1     # Phase 1 (Foundation) — Day 1 setup
/gsd-discuss-phase 1  # Phase 1 컨텍스트 점검 (선택)
```

이후 Phase 2 ~ 5는 동일 패턴 (`/gsd-plan-phase N` → `/gsd-execute-phase N` → `/gsd-verify-work` → `/gsd-transition`).

## Hard Constraints (do not violate without explicit approval)

- **Stack lock:** Kotlin 2.1.0 + AGP 8.7.3 + Gradle 8.11.1 + JDK 17 / minSdk 26 / compileSdk 35
- **AndroidX 5종만:** `appcompat:1.7.1`, `core-ktx:1.13.1`, `activity-ktx:1.9.3`, `fragment-ktx:1.8.5`, `constraintlayout:2.2.1`
- **금지:** Compose · `com.google.android.material` · Hilt/Dagger · Room/DataStore · Navigation Component · RxJava · `kotlinx-coroutines-android` · 시스템 `AccessibilityService` 등록 · AGP 9.x · Kotlin 2.3.x
- **UI:** XML Layout + Fragment + ViewBinding (Compose 금지)
- **Deadline:** 2026-05-15 push 완료, 2026-05-20 공고 마감

## Critical Pitfalls (Phase 작업 중 항상 의식)

| ID | Phase | One-liner |
|----|-------|-----------|
| C-1 | 2 | TTS `speak()` 전 `onInit` 가드 + pending 큐 |
| C-2 | 2 | `object TtsService` + applicationContext (recreate 좀비 회피) |
| C-3 | 3 | dispatchKeyEvent: DPAD/ENTER 화이트리스트 + ACTION_DOWN 한정, 외 `super` |
| C-4 | 3 | XML `nextFocusUp/Down/Left/Right` 명시 연결 (콘텐츠 ↔ 하단바 양방향) |
| C-5 | 3 | `state_focused` selector + `android:foreground` (ripple 충돌 회피) |
| P-3 | 5 | README 1스크롤에 매핑 표 필수 (평가자 30초 인지) |

상세는 `research/SUMMARY.md` §5, `research/PITFALLS.md` 전체.

## Communication

- 응답 언어: 한국어
- 코드 주석: 최소화. WHY가 비자명한 부분만 한 줄.
- 모든 Phase 산출물은 atomic commit으로 즉시 푸시 가능 상태 유지.

---
*CLAUDE.md initialized: 2026-05-07 after `/gsd-new-project --auto @PRD.md`*
