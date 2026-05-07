---
phase: 5
phase_name: Ship
status: human_needed
verified_at: 2026-05-07
---

# Phase 5: Ship — Verification

요구사항: DOC-01/02/03/05/06/07.

## Success Criteria 검증

### 1. DOC-01: README 첫 1스크롤 = 한 줄 + GIF + 매핑 표 + WPF↔Android 표
**Status:** ✅ structural — README 상단 5섹션이 1스크롤 안 (한 줄 설명 / 뱃지 / GIF placeholder / 7행 매핑 / WPF 매핑).

### 2. DOC-02: 표준 준수 (KS X 9211 / KWCAG 5.1.5·5.4.7·6.1.x / WCAG AA / 별표 5)
**Status:** ✅ — README "표준 준수" 4행 표.

### 3. DOC-03: Scope (시스템 AccessibilityService 미등록 명시)
**Status:** ✅ — README "Scope" 섹션 첫 항목.

### 4. DOC-04: 검증 환경 (재확인)
**Status:** ✅ — README "검증 환경" + `docs/AVD-SETUP.md` 링크.

### 5. DOC-05: GitHub 공개 + description + topics
**Status:** ⚠️ human_needed — README "GitHub 저장소 메타"에 한국어 description + 7개 topics 박제. 실제 push는 사용자 환경.

### 6. DOC-06: GIF + MP4 임베드
**Status:** ⚠️ human_needed — README placeholder 자리 + Release Checklist 4번 항목.

### 7. DOC-07: Notion DB + 위시켓 재제출
**Status:** ⚠️ human_needed — Release Checklist 8·9번 항목. ≤ 2026-05-15.

---

## Pitfall Status (Portfolio P-1 ~ P-5)

| ID | Status |
|----|--------|
| P-1 GIF 오디오 부재 | ✅ "자막 오버레이 + MP4 별도" 정책 README + Checklist |
| P-2 3dp stroke 안 보임 | ✅ "라이트/HC 좌우 비교 + 빨간 화살표" 정책 Checklist 5번 |
| P-3 1스크롤 매핑 표 | ✅ README 상단 7행 매핑 표 |
| P-4 저장소명 | ✅ `android-barrier-free-kiosk-demo` README 박제 |
| P-5 KWCAG/KS X 9211 인용 | ✅ 4행 표 + 별표 5 |

---

## Human Validation Items (= Release Checklist)

README "Release Checklist" 9 항목 = 사용자가 ≤ 2026-05-15에 수행할 액션:
1. gradle wrapper jar 생성
2. ./gradlew build exit 0
3. AVD 1회 동작 검증
4. GIF/MP4 캡처 + placeholder 교체
5. Focus 비교 스크린샷
6. GitHub 저장소 생성 + description/topics
7. git push
8. Notion 페이지 작성
9. 위시켓 지원서 재제출

---

## Verdict

| Item | Result |
|------|--------|
| 자동 산출물 (DOC-01~04) | ✅ pass |
| 사용자 환경 액션 (DOC-05/06/07) | ⚠️ human_needed (Release Checklist) |

**Overall:** `human_needed` — 코드/문서 산출물은 모두 작성 완료. 평가자 가시성은 GIF 캡처 후 readme 교체 1회 + GitHub push로 closure.
