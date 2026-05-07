# Phase 5: Ship — Context

**Gathered:** 2026-05-07
**Mode:** Inline autonomous

<domain>
## Phase Boundary

평가자 90초 인지 전환을 위해 README 1스크롤을 완성한다. P0~P5 portfolio pitfall 모두 차단. 실제 GitHub push / Notion / 위시켓 재제출은 사용자 환경 액션 — 본 Phase 산출물은 README + .planning 마감 + 산출물 인벤토리.

Requirements: DOC-01/02/03/05/06/07.

</domain>

<decisions>
## Implementation Decisions (locked)

### README.md 본 구성 (1스크롤 = ~150줄 안)

순서 (P-3 차단):
1. **타이틀 + 한 줄 설명** (10s 인지)
2. **GIF placeholder + MP4 링크** (자막 GIF 자리 — 사용자가 OBS/ScreenToGif 등으로 캡처해 추가)
3. **공고 요구사항 7행 ↔ 코드 위치 매핑 표** (P-3 핵심)
4. **WPF ↔ Android 이식 매핑 표**
5. **표준 준수 4행 표** (KS X 9211 / KWCAG 5.1.5 / 5.4.7 / 6.1.x — P-5 차단)
6. **Scope** (시스템 AccessibilityService 미등록 헤지 — DOC-03)
7. **검증 환경** (DOC-04, AVD, Numpad, Google TTS 한국어)
8. **빌드 & 실행** (`gradle wrapper` 1회 + `./gradlew build`)
9. **의존성 표** (외부 0 + AndroidX 5종 강조)
10. **Out of scope / v2 로드맵 링크**
11. **저작권 + 한 줄**

### 시각 자료 정책 (P-1, P-2 차단)
- GIF는 **자막 오버레이** 필수. 평가자에게 오디오 부재 회피.
- 3dp focus stroke는 **빨간 화살표 + 캡션** 권장. 라이트/HC 좌우 비교 캡처.
- 본 Phase에서는 placeholder 텍스트로 자리만 마련. 실제 캡처는 사용자가 AVD에서 수행.

### 저장소 / 메타 (P-4 차단)
- 권장 저장소명: `android-barrier-free-kiosk-demo`.
- description (한국어): "위시켓 키오스크 베리어프리 공고용 Android 네이티브 데모. 외부 의존성 0, AndroidX 5종만으로 구현한 4기능(고대비·TTS·키패드·Focus Indicator) + 음량/줌/체크리스트."
- topics: `android`, `kotlin`, `accessibility`, `barrier-free`, `kiosk`, `wcag`, `kwcag`.
- 본 Phase 산출물에 위 메타를 README "GitHub 저장소 메타" 섹션으로 박제 → 사용자가 push 시 그대로 사용.

### .planning 산출물 정리
- STATE.md: 5/5 완료로 갱신.
- ROADMAP.md: 진행 표 ✓ 갱신.
- PROJECT.md: Active → Validated 이동(시연 검증 가능 항목).
- 5개 phase 디렉토리(`01..05`) 그대로 유지 — git 추적.

### 사용자 액션 체크리스트 (Phase 5 외부)
- [ ] `gradle wrapper --gradle-version 8.11.1` 1회.
- [ ] `./gradlew build` exit 0.
- [ ] AVD D-pad 동작 + TTS 발화 + HC 토글 + 줌 영속 1회 검증.
- [ ] AVD에서 GIF/MP4 캡처 (자막 오버레이) → README `[GIF]` placeholder 교체.
- [ ] GitHub 새 저장소 생성 (`android-barrier-free-kiosk-demo`, public, description + topics).
- [ ] `git remote add origin && git push -u origin master`.
- [ ] Notion 프로젝트 DB에 한 페이지 작성 (링크 + 한 줄 설명 + 매핑 표 복사).
- [ ] 위시켓 공고에 지원서 재제출 (저장소 + Notion 링크).
- [ ] 모두 ≤ 2026-05-15.

이 7단계는 README "Release Checklist" 섹션으로 박제.

</decisions>

<deferred>
- v2: 시스템 AccessibilityService 등록, Compose 변형, Magnification API 전체 뷰포트, 다국어 i18n.
</deferred>
