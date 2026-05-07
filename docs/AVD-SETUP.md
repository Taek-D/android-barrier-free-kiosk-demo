# AVD 셋업 가이드 — `hw.dPad=yes` + 한국어 TTS

본 데모는 **Android Studio Emulator**의 D-pad를 키보드 Numpad로 입력해 **Phase 3 키패드 네비게이션**을 검증합니다. **Day 4 위험 차단을 위해 Day 1 종료 시점에 D-pad 동작을 1회 수동 확인**하세요.

---

## 1. AVD 생성

1. Android Studio → **Device Manager → Create Virtual Device**.
2. **Hardware:** Pixel 6 (또는 폰 카테고리 임의).
3. **System Image:** API 35 (Android 15) — Google APIs (Google TTS 한국어 사용 위해 권장).
4. **AVD 이름:** `a11y_demo_dpad_api35`.
5. **Show Advanced Settings** 클릭.
6. **Hardware → DPad: `yes`** 로 변경 (필수).
7. **Finish** 후 첫 부팅.

또는 `~/.android/avd/<name>.avd/config.ini`에서 직접 다음 항목 확인/추가:

```ini
hw.dPad=yes
hw.keyboard=yes
hw.mainKeys=yes
```

---

## 2. Numpad ↔ D-pad 매핑 확인

에뮬레이터 실행 후:

- Num Lock **OFF** 상태에서 키보드 Numpad
  - `8` = ↑ DPAD_UP
  - `2` = ↓ DPAD_DOWN
  - `4` = ← DPAD_LEFT
  - `6` = → DPAD_RIGHT
  - `5` = DPAD_CENTER (선택/확인)
- 또는 에뮬레이터 우측 패널 **Extended Controls → Directional pad** 활성화 후 마우스로 클릭.

---

## 3. Google TTS 한국어 데이터 설치

1. 에뮬레이터 → **설정(Settings) → 시스템 → 언어 및 입력 → 텍스트 음성 변환 출력 (TTS)**.
2. 기본 엔진을 **Google 텍스트 음성 변환 엔진**으로 설정.
3. **언어 → 한국어 (대한민국)** 선택. 음성 데이터 다운로드.
4. 미설치 상태일 경우 앱은 `Locale.US`로 폴백합니다 (TTS-05).

---

## 4. Day 1 DoD — 수동 확인 체크리스트

- [ ] AVD `a11y_demo_dpad_api35` 부팅 성공 (`./gradlew installDebug` 또는 Android Studio Run).
- [ ] 앱 실행 → 홈 화면에서 Numpad `2`(↓) 누름 → 시스템 포커스가 "메뉴 열기" / "체크리스트 열기" 버튼으로 이동.
- [ ] Numpad `5` 누름 → 포커스된 버튼 클릭 동작.
- [ ] 위 동작을 README 검증 환경 섹션에 "Day 1 D-pad 동작 확인 완료 (YYYY-MM-DD)"로 마킹.

위 4개가 모두 ✓ 상태여야 Day 4 (Phase 3) 작업이 안전하게 시작됩니다 (M-3 차단).

---

## 5. 트러블슈팅

| 증상 | 원인 | 대응 |
|------|------|------|
| Numpad 눌러도 포커스 이동 안 됨 | `hw.dPad=no` | 위 §1 6번 단계 확인. AVD 재생성. |
| 한국어 TTS 묵음 | Google TTS 한국어 미설치 | §3 단계 수행. 앱은 Locale.US로 폴백. |
| Numpad가 숫자 입력으로 잡힘 | Num Lock ON | Num Lock OFF. |
| `./gradlew` 명령 없음 | wrapper 미생성 | `gradle wrapper --gradle-version 8.11.1` 1회 실행 (또는 Android Studio Sync). |

---

*Last updated: 2026-05-07 — Phase 1 산출물.*
