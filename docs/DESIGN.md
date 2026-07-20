# DragonEggSV 설계 문서

> DragonEggSV(드래곤 알 서바이벌) 서버 플러그인의 전체 설계. 현재 저장소는
> 인챈트 최대레벨 + 모루 페널티만 구현된 `DEench` 플러그인이며, 이 문서는
> 그것을 서버 전체 룰을 굴리는 종합 플러그인으로 확장하기 위한 설계다.
>
> **상태 표기**: ✅ 구현됨 · 🟡 부분 구현 · ⬜ 미구현 · ⚠️ 결정 필요

---

## 1. 게임 개요

- MC 시간 기준 **99일** 동안 서버가 진행되고, **100일차가 되는 순간 드래곤 알을
  보유한 유저가 우승**한다.
- 월드보더: 오버월드 `1000×1000`, 네더 `500×500`, 엔드 `400×400`.
- 엔딩(크레딧)은 등장하지 않고, 엔드 외부섬(엔드 게이트웨이) 포탈은 무효화된다.

핵심 오브젝트는 **단 하나뿐인 드래곤 알**이며, 플러그인의 대부분은 "누가 알을
가지고 있는가"와 "알 보유자에게 어떤 페널티가 붙는가"를 중심으로 돈다.

---

## 2. 아키텍처

기존 단일 리스너 구조를 매니저/서비스 단위로 분리한다. 플러그인 ID(`DEench`)와
메인 클래스(`me.qmftm.dEench.DEench`)는 하위 호환을 위해 유지한다.

```
me.qmftm.dEench
├── DEench.java                 # 메인: 설정 로드 + 각 모듈 wiring
├── config/
│   └── PluginConfig.java       # config.yml 접근 래퍼
├── game/                       # 게임 진행 / 승리
│   ├── GameClock.java          # 경과 일수 추적, 100일차 판정
│   ├── WinService.java         # 우승 판정/선언
│   └── WorldBorderService.java # 차원별 월드보더 설정
├── egg/                        # ★ 드래곤 알 코어 (우선 구현)
│   ├── EggManager.java         # 알 상태/보유자 추적 + 영속화
│   ├── EggState.java           # 상태 enum + 데이터
│   ├── FootprintService.java   # 발자국 트레일
│   ├── DeathAltarService.java  # 사망 제단 + 신호기 빔
│   ├── DimensionLockListener.java
│   ├── EnderEffectService.java # 엔더맨 이펙트
│   ├── BeaconAmbienceService.java # 설치된 알 근처 신호기 소리
│   └── VillagerTradeLimiter.java  # 하루 20회 거래 제한
├── enchant/                    # 인챈트 (기존 DEench 로직)
│   ├── AnvilListener.java      # ✅ 모루 로직 + 최대레벨 상승
│   └── MaxEnchantNameService.java # ⬜ 최대레벨 장비 이름 빨강
├── items/                      # 아이템 룰
│   ├── BannedItemListener.java # 사용 금지 아이템
│   ├── BannedEnchantListener.java # 수선/무한 금지
│   ├── PotionCarryLimiter.java # 포션 2개 제한
│   ├── GoldenAppleTweak.java   # 황금사과 재생 4→2초
│   ├── NetheriteTemplateRecipe.java
│   └── DragonRespawnXp.java    # 재소환 5000 경험치
└── rules/                      # 환경/규칙
    ├── ExplosionDamageListener.java # 침대/정박기/엔드수정 1/4
    ├── XpDropListener.java     # 사망 시 경험치 전량 드롭
    ├── InfoLeakListener.java   # 좌표/채팅/데스메시지/발전과제 금지
    └── EndPortalListener.java  # 엔딩 미등장 + 외부섬 포탈 무효
```

각 매니저는 `DEench#onEnable`에서 생성·등록하고, 공유 상태(알 정보, 게임 시계)는
매니저 인스턴스로 주입한다.

---

## 3. 데이터 모델 & 영속화

99일짜리 장기 서버이므로 재시작에도 상태가 유지되어야 한다.

- **`data.yml`** (또는 `egg.yml`): 알 상태와 게임 진행을 저장.
  ```yaml
  game:
    started: true
    start-fulltime: 0        # 게임 시작 시점의 오버월드 fullTime
  egg:
    state: HELD              # HELD | PLACED | ALTAR | DROPPED | NONE
    holder: <uuid>           # 현재 보유자 (HELD일 때)
    first-holder: <uuid>     # 최초 획득자 (엔드→오버월드 예외용)
    location:                # PLACED/ALTAR/DROPPED일 때 알의 위치
      world: overworld
      x: 0
      y: 0
      z: 0
    altar-expire: <epoch-ms> # 제단/신호기 철거 예정 시각
  footprints: [ ... ]        # 발자국 목록 (아래 참고)
  ```
- **`config.yml`**: 튜닝 값 (아래 §7).
- 저장 시점: 알 상태 변경 시 즉시 + 주기적 flush + `onDisable`.

> ⚠️ **결정 필요**: 발자국이 많아질 수 있어 별도 파일 분리 또는 in-memory + 만료
> 정리(재시작 시 소실 허용)로 갈지. 기본 제안은 **in-memory + 주기 정리**(발자국은
> 5일 만료라 재시작 소실 허용).

---

## 4. ★ 드래곤 알 코어 (우선 구현)

### 4.1 알 상태 추적 (`EggManager`)

바닐라에서 드래곤 알은 최초 엔더 드래곤 처치 시 **정확히 1개** 생성된다. 이 유일한
알의 위치/소유를 상태 기계로 추적한다.

```
NONE  ──(드래곤 첫 처치)──▶  PLACED (엔드 스폰 기둥 위)
PLACED ──(플레이어 채취)──▶  HELD
HELD  ──(설치)──▶ PLACED
HELD  ──(사망)──▶ ALTAR (제단 위)
HELD  ──(드롭/디스펜스)──▶ DROPPED (아이템 엔티티)
ALTAR/DROPPED/PLACED ──(획득)──▶ HELD
```

추적 방법:
- `PlayerPickupItemEvent` / `InventoryClickEvent` / `BlockPlaceEvent` /
  `BlockBreakEvent` / `PlayerDeathEvent` / `PlayerDropItemEvent`에서 알(
  `Material.DRAGON_EGG`)의 이동을 감지해 상태·보유자 갱신.
- 알 아이템에 `PersistentDataContainer` 마커를 심어 "이 서버의 그 알"임을 식별
  (복제 어뷰징 방지, 여러 알 대비).
- **최초 획득자**(`first-holder`)는 `NONE→HELD` 최초 전이 시 1회 기록하고 고정.

### 4.2 알 보유자 페널티

아래는 **보유자(HELD 상태의 holder)** 에게만 적용:

1. **발자국 트레일** (`FootprintService`) ⬜
   - 반복 태스크(예: 0.5초 간격)로 보유자 위치를 샘플링, 일정 거리 이동 시 발자국
     1개 기록: `(위치, 바라보는 방향, 생성 fullTime)`.
   - 렌더 태스크가 근처 온라인 유저에게 파티클로 표시. **밤(worldTime 13000~23000)
     에는 숨김**, **생성 후 5 MC일(=5×24000틱) 경과 시 만료**.
   - ⚠️ **결정 필요**: 표현 방식 — (a) 파티클(가벼움, 비영구) vs (b) 디스플레이
     엔티티/블록(더 뚜렷하지만 무거움). 기본 제안 **(a) 파티클** + 방향 표시용
     화살표형 파티클 배치.

2. **사망 제단 + 신호기 빔** (`DeathAltarService`) ⬜
   - 보유자 사망 시 `PlayerDeathEvent`에서 알이 인벤토리로 드롭되는 대신 사망 위치
     근처에 **제단 구조물**을 생성하고 그 위에 알을 안치(→ `ALTAR` 상태).
   - **보라색 신호기 빔**을 실시간 **100분** 유지: 신호기 블록 + 최소 피라미드
     베이스(발동용) + 위쪽에 **보라색 유리**(빔 색상) 배치. 만료 시 철거하며
     **덮어쓴 원본 블록을 복원**(교체 블록 스냅샷 저장).
   - ⚠️ **결정 필요**: 제단 구조물 형태(재질/크기). 기본 제안 — 3×3 석영/흑요석
     플랫폼 + 중앙 신호기 + 철블록 1층 베이스 + 보라 유리. 스카이 액세스가 막힌
     지형/지하 사망 시 빔이 안 뜰 수 있어 대체안(디스플레이 빔) 필요할 수 있음.

3. **차원 이동 금지** (`DimensionLockListener`) ⬜
   - 보유자의 `PlayerPortalEvent`/차원 이동 텔레포트를 취소.
   - **예외**: `player == first-holder && from == THE_END && to == OVERWORLD`
     이면 허용(최초 획득자가 알을 엔드 밖으로 반출 가능).

4. **엔더맨 이펙트** (`EnderEffectService`) ⬜
   - ⚠️ **결정 필요**: "엔더맨 이펙트"의 정의. 후보:
     (a) 물/비에 닿으면 데미지, (b) 보라 파티클 상시 표출, (c) 워프 사운드,
     (d) 위 조합. 기본 제안 **(a)+(b)** (엔더맨처럼 물/비 피해 + 엔더 파티클).

5. **설치된 알 근처 신호기 소리** (`BeaconAmbienceService`) ⬜
   - `PLACED` 상태일 때 주기적으로 근처 유저에게 신호기 앰비언트 사운드 재생(위치
     노출용).

6. **주민 거래 하루 20회 제한** (`VillagerTradeLimiter`) ⬜
   - 보유자의 거래 완료를 카운트(Paper `PlayerPurchaseEvent`), MC일마다 리셋,
     20회 초과 시 취소.

7. **안개 끄기 금지** ⚠️
   - 안개/렌더거리는 **클라이언트 설정**이라 서버 완전 강제 불가. 근사안:
     보유자 `Player#setViewDistance`를 낮게 강제하거나 어둠(DARKNESS) 효과 부여.
   - ⚠️ **결정 필요**: 리소스팩/모드 병행 전제인지, 아니면 근사 강제로 충분한지.

8. **사망 시 경험치 전량 드롭** (전역일 가능성 높음, §6 참고)

> ⚠️ **스코프 해석 결정 필요**: 원문의 "드래곤 알 페널티" 목록 중
> `경험치 전량 드롭`·`엔더드래곤 재소환 5000 경험치`는 문맥상 **전역 룰**로 보인다.
> 발자국·제단·차원잠금·엔더이펙트·신호기소리·거래제한만 **보유자 한정**으로 해석.
> 이 해석이 맞는지 확인 필요.

### 4.3 승리 판정 (`GameClock` + `WinService`) ⬜

- 게임 시작 시 오버월드 `fullTime`을 `start-fulltime`으로 저장.
- 경과 일수 `= (현재 fullTime − start-fulltime) / 24000`.
- **100일차 시작 = 경과 99일 도달 시점**에 알 보유자를 우승자로 선언(방송/게임 정지).
- 보유자가 없거나(NONE/DROPPED/ALTAR) 알이 무주공산이면 규칙 확정 필요.
  - ⚠️ **결정 필요**: 100일차 순간 알이 보유자 손에 없을 때(제단/설치/드롭 상태)의
    우승 처리.
- 관리자용 `/DE game start|pause|status` 명령으로 시작 시점/진행을 제어.

---

## 5. 인챈트 (기존 DEench 확장)

- ✅ 최대레벨 상승: 날카로움 5→10, 보호 4→7 (config로 조정 가능, `/DE config overench`).
- ✅ 모루 39레벨 페널티 삭제(비용 39 캡).
- 🟡 **최대레벨 인챈트 강조** — 현재는 *로어*를 진빨강으로 표시. 룰은 "최대레벨
  인챈트가 부여된 장비의 **이름**이 빨간색"이므로 **아이템 이름 자체를 빨강**으로
  바꾸는 `MaxEnchantNameService`로 보완 필요.
  - ⚠️ **결정 필요**: 기존 로어 강조를 유지+이름 빨강 추가할지, 이름 빨강만 둘지.
- ⬜ **사용 금지 인챈트**: 수선(Mending)/무한(Infinity). 인챈트 테이블/모루/거래/
  전리품에서 부여 차단 + 기존 부여분 무효화 정책 결정 필요.

---

## 6. 아이템 & 전역 룰

| 룰 | 구현 접근 | 상태 |
|---|---|---|
| 네더라이트 형판 조합법 추가 | 커스텀 `ShapedRecipe` 등록(형판 대신 네더라이트) | ⬜ ⚠️레시피 확정 |
| 포션 최대 2개 휴대 | 인벤토리 변경 감지, 초과 픽업/이동 취소 | ⬜ ⚠️"포션" 범위 |
| 황금사과 재생 4→2초 | 소비/효과 이벤트에서 REGENERATION 지속 재설정 | ⬜ |
| 사용 금지 아이템 | 상호작용/장착/투척/발동 이벤트 취소 | ⬜ |
| 엔더드래곤 재소환 5000 경험치 | 재소환 처치 감지 후 추가 XP 지급(기본 500→5000) | ⬜ |
| 폭발 데미지 1/4 (침대/정박기/엔드수정) | `EntityDamageEvent`에서 해당 폭발원 데미지 ×0.25 | ⬜ |
| 사망 시 경험치 전량 드롭 | `PlayerDeathEvent#setDroppedExp` 전량 계산 | ⬜ |

**사용 금지 아이템 세부** (각각 별도 취소 로직):
겉날개(장착 차단) · 엔더진주 투척 한정(투척만 차단, 소지는 허용) · 셜커상자 ·
엔더상자 · 방패(사용/막기 차단) · 불사의 토템(`EntityResurrectEvent` 취소) ·
삼지창(사용/투척 차단).
> ⚠️ **결정 필요**: 각 금지 아이템을 "소지 불가"까지 갈지, "사용만 불가"로 둘지.

---

## 7. 환경 / 정보 차단 룰

- **좌표 금지**: 게임룰 `reducedDebugInfo=true`로 F3 좌표 숨김. (완전 차단은
  클라 의존 — 근사.)
- **채팅 금지(정보 누출 방지)**: 일반 채팅(`AsyncChatEvent`) 차단 +
  데스메시지 제거(`event.deathMessage(null)`) + 발전과제 방송
  게임룰 `announceAdvancements=false`.
  - ⚠️ **결정 필요**: 채팅을 전면 금지인지, 데스/발전과제/좌표 등 "정보성"만
    막고 일반 대화는 허용인지.
- **엔딩 미등장**: 엔드 퇴장 포탈 통과 시 크레딧을 띄우지 않고 처리.
- **엔드 외부섬 포탈 무효**: 엔드 게이트웨이 텔레포트 취소(이동 안 함).
- **월드보더**: 시작 시 차원별 크기 설정(중심/크기 config).

### config.yml (확장안)
```yaml
overench:            # ✅ 이미 존재
  sharpness: 10
  protection: 7
game:
  days-to-win: 99
worldborder:
  overworld: 1000
  nether: 500
  end: 400
egg:
  footprint-days: 5
  altar-beacon-minutes: 100
  villager-trades-per-day: 20
golden-apple:
  regen-seconds: 2
potions:
  max-carry: 2
dragon-respawn-xp: 5000
explosion-damage-multiplier: 0.25
banned-enchants: [ mending, infinity ]
banned-items: [ elytra, ender_pearl_throw, shulker_box, ender_chest, shield, totem_of_undying, trident ]
rules:
  reduced-debug-info: true
  disable-chat: true
  hide-death-messages: true
  disable-advancement-broadcast: true
```

---

## 8. 구현 로드맵 & 진행 상황

- ✅ **Phase 1 — 드래곤 알 코어**: `EggManager`(상태추적+영속화), 차원 이동 금지,
  엔더 파티클, 발자국(디스플레이), 사망 제단+보라 신호기 빔, 설치 알 신호기 소리,
  주민거래 20회/일, 승리 판정(보스바).
- ✅ **Phase 2 — 전역/환경 룰**: 월드보더(1000/500/400), 게임룰(좌표 숨김/발전과제
  방송 끔), 채팅·데스메시지 차단, 경험치 전량 드롭, 폭발 데미지 1/4, 황금사과 2초,
  포션 2개 제한, 엔딩 미등장/엔드 게이트웨이 무효.
- ✅ **Phase 3 — 아이템/인챈트**: 금지 아이템(겉날개·엔더진주 투척·셜커·엔더상자·
  방패·토템·삼지창), 금지 인챈트(수선·무한 신규 획득 차단), 네더라이트 형판 조합법,
  재소환 드래곤 5000 XP, 최대레벨 장비 이름 빨강.

각 Phase는 독립 커밋으로 진행했고, Paper 1.21.8에 대해 빌드 검증 후 푸시했다.

> **주의**: 컴파일 검증만 완료. **인게임 실서버 테스트가 필요**하며, 특히 발자국
> 회전각/밤 숨김, 제단 신호기 빔(지하/천장막힘 시), 최대레벨 이름 되돌림, 방패·
> 엔더진주 사용 차단 등 이벤트 기반 동작은 실측 튜닝이 필요할 수 있다.

---

## 9. 결정 목록

### 확정됨 (사용자 확인)

1. **"1일" 정의 & 승리**: MC 하루(24000틱)=1일. 플러그인이 자동 판정하며, **보스바에
   현재 날짜(Day N)와 하루 진행도**를 표시한다. 100일차 도달 시 알 보유자 우승.
2. **엔더맨 이펙트**: 알 보유자에게 **엔더맨 파티클(포탈 파티클)이 지속적으로** 표출.
3. **경험치 드롭 / 재소환 XP**: **전역 룰**(모든 플레이어에 적용).
4. **정보 차단**: **전부 금지** — 발전과제 알림 / 데스메시지 / 채팅 / 좌표(F3).
5. **발자국**: **디스플레이 엔티티(TextDisplay)** 로 2초마다 발밑에 `^` 형태를 찍고,
   그 `^`가 플레이어가 **바라보는 방향**을 향하도록 회전. 5 MC일 지속, 밤엔 숨김.

### 아직 확정 필요 (기본안으로 진행, 추후 조정)

- **100일차 순간 알이 보유자 손에 없을 때**의 우승 처리 → 기본: "보유자 없음" 방송.
- **제단 구조물 형태** & 지하/천장막힘 시 신호기 빔 대체안.
- **안개 끄기 강제** 수준(클라 의존 — 근사만).
- **금지 아이템** 처리 수준(소지 불가 vs 사용만 불가) → 기본: 사용만 불가.
- **금지 인챈트/네더라이트 형판** 세부(기존 부여분 무효화 여부, 레시피 구성).
- **최대레벨 강조**: 로어 유지 여부 + 이름 빨강 추가 방식.
