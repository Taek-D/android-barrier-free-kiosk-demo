# Feature Landscape: Android Kotlin 베리어프리 키오스크 데모 앱

**Domain:** 공공 키오스크(무인정보단말기, 韓 KIOSK) 접근성 — Android Kotlin 데모 (포트폴리오 목적)
**Researched:** 2026-05-07
**Confidence:** HIGH (regulatory) / MEDIUM (Android-specific feature priority — derived from KS X 9211 + KWCAG + Android best-practice cross-reference)

---

## Context Snapshot

이 데모의 1차 평가자는 **위시켓 공고 클라이언트**이며, 실제 키오스크 사용자(시각·청각·지체·인지 장애인 + 고령자)는 2차 사용자다. 따라서 "feature가 필요한가?"의 기준은 두 갈래:

1. **공고 평가자가 30~90초 안에 'Android Accessibility 만들 줄 안다'고 결론짓게 하는가** — 포트폴리오 실용 기준
2. **KS X 9211 / 장차법 시행령 / 과기부 무인정보단말기 접근성 검증기준(별표 5) / KWCAG 2.2 모바일 항목과 매핑되는가** — 도메인 신뢰성 기준

두 기준 모두를 만족시키는 feature가 P0이고, 한쪽만 만족시키는 feature는 P1, 둘 다 약하면 P2 또는 anti-feature이다.

### 핵심 규제 레퍼런스 (이후 표에서 참조)

| Reference | What it covers | 적용 |
|---|---|---|
| **KS X 9211:2022** 무인정보단말기 접근성 지침 | HW+SW (화면 대비, 버튼 간격, 음성안내, 응답시간) | 키오스크 도메인 표준 |
| **장차법 시행령 별표 3 + 과기부 고시 별표 5** "무인정보단말기 접근성 검증 기준" | 10원칙 47지표 — 점자, 음성안내, 휠체어 접근, 자막 등 | 법적 의무 (2024.1.28~2026.1.28 단계 시행) |
| **KWCAG 2.2** | 4원칙 14지침 33검사항목 (모바일 앱 포함) | SW 측 일반 기준 |
| **WCAG AA** | 색대비 4.5:1, 포커스 가시성 등 | 위 표준들의 기반 |

---

## 1. Table Stakes (P0) — Must Have

> **기준:** 없으면 (a) 평가자가 "Accessibility 못 만든다"로 분류하거나, (b) KS X 9211 / 검증기준 / KWCAG에서 명시 요구.
> **Demo scope 주의:** 데모 앱이라 HW(점자 키패드, 휠체어 클리어런스, 음성안내 스피커 등) 항목은 SW 측면 시뮬레이션/언급으로 갈음한다.

| Feature | Why Expected (규제·평가) | Complexity | PRD 매핑 | 의존성 |
|---|---|---|---|---|
| **F-1. 항상 노출되는 접근성 컨트롤 진입점 (`AccessibilityBottomBar`)** | KS X 9211 "기능 진입의 일관성"; 모든 화면에서 접근성 토글 노출 — 평가자 30초 시각 임팩트 1순위 | **Low** (XML include) | P0-1 ✅ | Activity/Fragment 골격 |
| **F-2. 포커스 이동 시 음성안내 (TTS, 중복억제 토글)** | 검증기준 별표 5 "시각장애 음성안내"; KWCAG 2.2 4.1.2; Android `TextToSpeech` 표준 | **Med** (비동기 init, 큐잉, 중복억제 500ms) | P0-2 ✅ | TTS init guard |
| **F-3. 고대비 테마 토글 (WCAG AA 4.5:1+)** | 검증기준 "저시력 대비"; KWCAG 1.4.3; KS X 9211 "화면 대비"; `recreate()` 후 전체 반영 | **Low~Med** (`themes_high_contrast.xml` + state 보존) | P0-3 ✅ | savedInstanceState/SharedPrefs |
| **F-4. 외부 키패드 방향키 포커스 이동 (DPAD + ENTER)** | 검증기준 "지체장애 대체입력"; KS X 9211 "물리적 입력 보조"; `dispatchKeyEvent` + `focusSearch()` | **Med** (focusSearch 폴백 디버그가 가장 어려움 — Day 4 위험) | P0-4 ✅ | Focusable 뷰 그래프 |
| **F-5. 가시적 포커스 인디케이터 (≥3dp stroke, `state_focused`)** | WCAG 2.4.7 (포커스 가시성) — 한국·국제 공통; KWCAG 2.2 신규 항목으로 강화 | **Low** (drawable selector) | P0-5 ✅ | F-4와 함께 검증 |
| **F-6. 모든 인터랙티브 뷰에 `contentDescription` (한국어)** | 검증기준 "대체텍스트"; KWCAG 1.1.1; 평가자가 코드 grep으로 즉시 확인 | **Low** (XML 속성) | P0-1 (암묵) ⚠️ **명시 필요** | F-2와 직결 |
| **F-7. 최소 터치 타깃 56dp+ (KS X 9211 권고: 손가락 18mm)** | KS X 9211 버튼 간격/크기; Material 권고 48dp 상회 | **Low** (XML `minWidth/minHeight`) | P0-1 ✅ | — |
| **F-8. README 1:1 매핑 표 (공고 항목 ↔ 코드 위치 ↔ GIF)** | 평가자 30초 스캔 — 데모의 핵심 가치 전달 채널 | **Med** (스크린샷/GIF 캡쳐 + 표 정리) | P0-6 ✅ | 모든 feature 완성 후 |

**P0 Coverage check vs PRD:** PRD P0-1~P0-6은 위 F-1~F-5, F-8에 모두 매핑됨. **갭: F-6 (`contentDescription` 명시)과 F-7 (56dp 최소 타깃)이 PRD에 암묵으로만 존재** — README/구현 시 명시 권장. PRD P0-1 acceptance criteria에 "각 56dp+, contentDescription 설정"으로 한 줄 들어 있으나, 모든 인터랙티브 뷰로 일반화된 별개 acceptance가 없다.

---

## 2. Should Have / Differentiators (P1)

> **기준:** P0보다 한 단계 약하지만, 있으면 평가자가 "단순 따라하기가 아니라 도메인 이해가 있다"고 인지. 규제 요구이거나 KS X 9211 추천 항목.

| Feature | Why Valuable | Complexity | PRD 매핑 |
|---|---|---|---|
| **F-9. 시스템 음량 증감 (`AudioManager.STREAM_MUSIC`)** | KS X 9211 "음량 조절"; 청각 보조 사용자에 직접 가치 | **Low** | P1-1 ✅ |
| **F-10. 콘텐츠 영역 확대/축소 (ScaleAnimation)** | 검증기준 "확대 기능"; 전체 뷰포트 zoom은 P2로 분리 (PRD 결정과 일치) | **Low~Med** | P1-2 ✅ |
| **F-11. 앱 내 7기능 체크리스트 시연 (`ChecklistFragment`)** | 평가자에게 self-demo — 클라이언트가 코드 안 봐도 동작 확인 가능 | **Low** | P1-3 ✅ |
| **F-12. SharedPreferences로 고대비/TTS 상태 영속화** | 키오스크 재시작 후 직전 사용자 보조설정 유지 — 키오스크 도메인 신호 | **Low** | P2-1 (PRD가 P2로 둠) ⚠️ **승격 권장** |
| **F-13. 자막/시각적 알림 (시스템 사운드와 시각 큐 동기화)** | 검증기준 "청각장애 자막"; 키오스크 도메인에서 자주 누락 — 차별화 포인트 | **Med** (TTS 발화 동시 텍스트 토스트/오버레이) | **PRD 갭** ⚠️ 신규 추가 검토 |
| **F-14. 응답시간 충분(KS X 9211): 다이얼로그/세션 타임아웃 가시화 또는 비활성화** | KS X 9211 "응답시간"; KWCAG 2.2.1; 키오스크에서 결제·주문 흐름 중단 방지 | **Low** (timeout 없음 명시 또는 연장 버튼) | **PRD 갭** ⚠️ 데모에선 "타임아웃 없음" 명시로 해결 가능 |
| **F-15. 모션 감소 모드 / 애니메이션 끄기** | KWCAG 2.3.3 (3회 깜박임), 인지·전정장애 배려 | **Low** (`Settings.Global.ANIMATOR_DURATION_SCALE` 존중) | **PRD 갭** ⚠️ 명시 권장 |

**Differentiator priority for portfolio:** F-12 (영속화), F-13 (자막), F-14 (타임아웃 명시) 중 **하나만 추가**해도 평가자에게 "도메인 이해" 신호가 강해진다. 6일 일정에서는 **F-12 (Low, 1~2시간) 승격이 ROI 가장 높음** — `recreate()` 시 어차피 상태 보존 이슈를 풀어야 하므로 부수적 비용.

---

## 3. Anti-Features — Deliberately NOT Build

> **기준:** 6일 데모 범위에서 시간을 잡아먹고, 평가자에게 임팩트 없거나 오히려 "데모 vs 프로덕트 혼동"을 일으키는 항목.

| Anti-Feature | Why Avoid | What to Do Instead |
|---|---|---|
| **풀 키오스크 비즈니스 로직 (결제·주문·메뉴 도메인)** | 데모 목적 = 접근성 4기능 시연. 도메인 로직은 시간만 잡아먹음 | "Mock 메뉴 화면" 1개로 갈음, 명시적으로 README에 비-범위 표기 (PRD 이미 결정) |
| **Jetpack Compose UI** | 클라이언트 코드베이스가 View/XML 일 가능성 높음 — Compose는 호환성 마이너스 | XML Layout 통일 (PRD 결정과 일치). Compose 변형은 Phase 2 선택 어필 자산 |
| **AccessibilityService 시스템 등록 (`<service android:name>` + accessibility config)** | 시스템 차원 통합은 "앱 내 접근성 컨트롤"과 다른 기능. 시스템 권한 요구로 데모 흐름 끊김 | 인앱 컨트롤러로 한정, README에 "Phase 2"로 명시 (PRD 결정과 일치) |
| **Magnification API (전체 뷰포트 확대)** | API 26+에서 `AccessibilityService` 권한 필요 — 시연 흐름이 시스템 설정으로 빠짐 | 콘텐츠 영역 ScaleAnimation으로 갈음, "Phase 2" 명시 (PRD 결정과 일치) |
| **BLE/외부 키패드 페어링** | 시스템 차원 통합 + HW 의존 — 데모 검증 불가 | XML `dispatchKeyEvent`로 일반 HW 키보드 이벤트 처리만 시연 (PRD 결정과 일치) |
| **유닛테스트 커버리지 목표** | 6일 안에 시연 영상 ROI > 테스트 커버리지 ROI | espresso/로보틱 테스트 1~2개만 README 어필 용도 (선택) (PRD 결정과 일치) |
| **점자 키패드 / 휠체어 클리어런스 / 점자블록 / 점자라벨** | HW 영역 — Android SW 데모로 검증 불가 | README "이 데모의 비-범위(HW 항목)" 섹션에서 명시적으로 언급, 검증기준 별표 5 인지하고 있음을 어필 |
| **Material Dialog/Toast 고대비 일관성** | edge case — 메인 흐름 평가에 영향 적음 | P2 유지 (PRD와 일치). 선택적으로 toast 스타일만 1줄 |
| **다국어 i18n (영어 UI)** | 평가자 = 한국어 클라이언트. 영어는 시간만 소모 | 한국어 단일, README만 영어 요약 추가 가능 |
| **다크모드 ≠ 고대비** | 다크모드와 고대비는 다른 개념. 둘 다 만들면 혼란 | 고대비 1개만 (검정배경+노랑/흰색 텍스트, 4.5:1+). 다크모드는 Phase 2 |
| **음성 명령(Voice Input)** | STT는 Android `SpeechRecognizer` 사용 시 Google 서비스 의존 → "외부 의존 0" 원칙 위배 | Phase 2로 분리. STT는 OS 표준 입력으로 상정 |
| **AccessibilityNodeInfo 커스텀 ViewGroup** | 데모 범위에서 표준 View로 충분. 커스텀 NodeInfo는 평가자 주의 분산 | 표준 View + `contentDescription`으로 충족 |

**Anti-feature 패턴 요약:** "HW", "시스템 권한", "외부 의존", "도메인 로직"이 키워드. 이 4개에 걸리면 cut.

---

## 4. Feature Dependencies

```
F-1 (BottomBar)  ─┬─→ F-2 (TTS)
                  ├─→ F-3 (고대비)
                  ├─→ F-9 (음량)
                  └─→ F-10 (확대/축소)

F-2 (TTS) ────────── F-6 (contentDescription)   [F-2가 읽는 텍스트 = F-6 값]
F-4 (DPAD) ───────── F-5 (Focus Indicator)      [둘이 함께 검증되어야 의미]
F-3 (고대비) ──────── F-12 (영속화)              [recreate() 시 상태 복원]
                      F-7 (56dp 타깃)            [F-1, F-4와 함께]

모든 F → F-8 (README 매핑 표)                   [최종 산출물]
F-11 (Checklist) ── 모든 F의 시각적 self-demo
```

**임계 경로 (Day 단위 PRD 일정 기준):**
- Day 1: 골격 + F-1
- Day 2: F-2 + F-6 (병렬)
- Day 3: F-3 + F-12 (승격 시)
- Day 4: F-4 + F-5 (가장 위험 — 에뮬레이터 D-pad 디버깅)
- Day 5: F-9, F-10, F-11
- Day 6: F-8 (README + GIF) — 모든 선행 완료 필요

---

## 5. PRD Cross-Check Table

| PRD 항목 | Feature ID | 상태 |
|---|---|---|
| P0-1 AccessibilityBottomBar 4버튼 56dp+ contentDescription | F-1 + F-7 + F-6 | ✅ 매핑 |
| P0-2 TTS speakIfChanged 500ms 중복억제 토글 | F-2 | ✅ 매핑 |
| P0-3 고대비 테마 + recreate() WCAG AA | F-3 | ✅ 매핑 |
| P0-4 dispatchKeyEvent DPAD + focusSearch | F-4 | ✅ 매핑 |
| P0-5 focused_background.xml 3dp+ stroke | F-5 | ✅ 매핑 |
| P0-6 README 매핑 표 + GIF | F-8 | ✅ 매핑 |
| P1-1 AudioManager STREAM_MUSIC | F-9 | ✅ 매핑 |
| P1-2 ScaleAnimation 콘텐츠 영역 | F-10 | ✅ 매핑 |
| P1-3 ChecklistFragment | F-11 | ✅ 매핑 |
| P2-1 SharedPreferences 영속화 | F-12 | ⚠️ **P1 승격 권장 (ROI Best)** |
| P2-2 전체 뷰포트 Magnification | (anti) | ✅ Out of scope (anti-feature) |
| P2-3 AccessibilityService 시스템 등록 | (anti) | ✅ Out of scope (anti-feature) |
| P2-4 Material Dialog 고대비 | — | ✅ P2 유지 |
| **PRD 미언급** F-6 contentDescription 일반화 | F-6 | ⚠️ **명시화 권장** (P0-1 acceptance에 한 줄로만 존재) |
| **PRD 미언급** F-13 청각장애 자막/시각 큐 | F-13 | ⚠️ **검토 권장** — 검증기준 별표 5 청각장애 항목 |
| **PRD 미언급** F-14 응답시간/타임아웃 정책 | F-14 | ⚠️ **README 한 줄 명시 권장** — KS X 9211 항목 |
| **PRD 미언급** F-15 모션 감소 존중 | F-15 | 선택 — KWCAG 2.2 |

**Three flagged gaps (recommended action):**

1. **F-6 (contentDescription) 명시화** — PRD acceptance 또는 README 코드 매핑 표에 별도 줄. 비용 0.
2. **F-12 (영속화) P2 → P1 승격** — `recreate()` 시 어차피 상태 복원 이슈 다뤄야 하므로 부수 비용. ROI 최고.
3. **F-13 (청각장애 자막) 또는 F-14 (타임아웃 명시) 중 1개 추가** — 검증기준 별표 5 청각장애 항목 커버리지 신호. F-14가 더 저렴 (README 한 줄 + 코드 1줄).

---

## 6. MVP Recommendation (6일 일정 기준)

### Build (P0 + 승격된 P1)
1. F-1 BottomBar
2. F-2 TTS + F-6 contentDescription (분리 명시)
3. F-3 고대비 + **F-12 영속화 (승격)**
4. F-4 DPAD + F-5 Focus Indicator
5. F-7 56dp 최소 타깃 (모든 P0 화면)
6. F-9 음량
7. F-10 확대/축소
8. F-11 Checklist
9. F-8 README

### Add if time (Day 7~8 버퍼)
- **F-14 타임아웃 명시** (README 한 줄 + Activity 1줄) — 가장 저렴한 차별화
- **F-13 자막** (TTS 발화와 동시 시각 토스트) — 청각장애 신호

### Defer (Phase 2)
- F-2-extended: AccessibilityService 시스템 등록
- F-10-extended: Magnification API 전체 뷰포트
- F-15: 모션 감소 + Material Dialog 고대비

### Explicit Anti-features (README "비-범위" 섹션)
- 결제/주문/메뉴 도메인
- Compose
- BLE 페어링
- HW 항목 (점자, 휠체어 클리어런스, 점자블록) — "검증기준 별표 5의 HW 영역은 본 SW 데모의 범위 외임을 인지" 식 한 줄

---

## 7. Confidence & Sources

**HIGH confidence:**
- 규제 매핑 (KS X 9211, 장차법 시행령, 과기부 별표 5, KWCAG 2.2, WCAG AA)
- Android API 표준 (TextToSpeech, dispatchKeyEvent, focusSearch, AudioManager)
- PRD와의 1:1 매핑 (PRD 직접 인용)

**MEDIUM confidence:**
- F-12 / F-13 / F-14 승격·신규 추가 권고 — 6일 일정 내 ROI 추정. 실제 작업 속도에 따라 조정 필요.
- "평가자 30초 스캔" 모델 — PRD 가설을 본 연구가 따랐을 뿐, 실증 데이터 없음.

**Sources:**
- [KS X 9211 무인정보단말기 접근성 지침 (e-나라표준)](https://standard.go.kr/KSCI/standardIntro/getStandardSearchView.do?menuId=919&topMenuId=502&upperMenuId=503&ksNo=KSX9211)
- [무인정보단말기 접근성 검증 기준 별표 5 (국가법령정보센터)](https://www.law.go.kr/LSW/flDownload.do?flSeq=157698277)
- [장애인차별금지 및 권리구제 등에 관한 법률 시행령](https://www.law.go.kr/lsInfoP.do?lsiSeq=249465&viewCls=lsRvsDocInfoR)
- [한국형 웹 콘텐츠 접근성 지침(KWCAG) 2.2](https://a11ykr.github.io/kwcag22/)
- [한국디지털접근성진흥원 — 키오스크 시험평가 소개](http://www.kwacc.or.kr/Accessibility/Kiosk?category=WA)
- [Android Developers — Principles for improving app accessibility](https://developer.android.com/guide/topics/ui/accessibility/principles)
- [BrowserStack — Android Accessibility Guidelines](https://www.browserstack.com/guide/android-accessibility-guidelines)
- [TPGi — South Korea's Disability Discrimination Act: Kiosk, Web, Mobile](https://vispero.com/resources/south-koreas-disability-discrimination-act-kiosk-web-mobile-accessibility/)
- [복지부 보도자료 — 키오스크 및 모바일앱 장애인 접근성 정당한 편의 제공](https://www.mohw.go.kr/board.es?mid=a10503010100&bid=0027&act=view&list_no=375590)
- [SMART CITY KOREA — Barrier-Free Kiosk](https://smartcity.go.kr/en/2024/12/13/%EB%B2%A0%EB%A6%AC%EC%96%B4%ED%94%84%EB%A6%AC%ED%82%A4%EC%98%A4%EC%8A%A4%ED%81%AC/)
- [UXKM — 무인정보단말기 접근성 체크리스트](https://uxkm.io/accessibility/a11y/06-a11yCheck/03-checkKioskPrinciple)
