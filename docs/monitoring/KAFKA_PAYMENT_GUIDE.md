# Kafka Payment 대시보드 해석 가이드

> 대상 대시보드: **Kafka Payment - 계좌이체 중간망**
> 대상 독자: 개발팀 전원
> 환경: Docker Compose 기준 (`docker-compose.yml` — 메인 통합 환경)

---

## 이 가이드는 무엇인가요?

계좌이체 요청이 들어오면 payment-service는 KFTC(금융결제원) 또는 BOK(한국은행) 외부망을 통해 처리합니다. 이 과정에서 Kafka가 중간 메시지 버스 역할을 하는데, 이 대시보드는 그 전 과정이 지금 정상적으로 흘러가고 있는지 한눈에 확인할 수 있도록 만들어졌습니다.

---

## 1. 전체 흐름

```
이체 요청
    │
    ▼
payment-service
    │  이체 요청 처리
    ├──────────────────→ DB (Outbox에 메시지 저장)
    │                          │
    │                    Outbox 워커 (1초마다 실행)
    │                          │
    │                          ▼
    │                   Kafka로 메시지 발행
    │                          │
    │              ┌───────────┴───────────┐
    │              ▼                       ▼
    │         KFTC 클러스터           BOK 클러스터
    │         (10억 미만)            (10억 이상)
    │              │                       │
    │        외부망 응답 수신         외부망 응답 수신
    │              │                       │
    └──────────────┴───────────────────────┘
                   │
              이체 최종 완료 / 실패
```

이 흐름의 각 단계가 지금 얼마나 빠르게, 얼마나 정상적으로 처리되고 있는지를 대시보드에서 확인합니다.

---

## 2. 접속 방법

| 도구 | URL | 계정 |
|------|-----|------|
| Grafana (대시보드) | `http://localhost:3000` | admin / admin |
| Prometheus (알림 확인) | `http://localhost:9090/alerts` | 없음 |

대시보드 경로: Grafana 접속 → 왼쪽 메뉴 **Dashboards** → **Kafka Payment - 계좌이체 중간망**

> payment-service, kafka-exporter, Prometheus가 모두 실행 중이어야 데이터가 표시됩니다.
> 실행 방법은 맨 아래 **로컬 실행 방법** 참고.

---

## 3. 대시보드 구성

대시보드는 총 6개 화면으로 구성됩니다.

| 화면 | 무엇을 보는가 |
|------|--------------|
| Consumer Lag | 처리 못하고 쌓인 메시지가 얼마나 되는지 |
| 메시지 처리량 | 지금 얼마나 빠르게 처리되고 있는지 |
| Outbox 적체 / 미완료 거래 | 어느 단계에서 막혀있는지 |
| 이체 결과 / 처리시간 | 이체가 얼마나 잘, 빠르게 처리되고 있는지 |
| 장애 / 보상 트랜잭션 | 장애 신호가 있는지 |
| Broker 상태 | Kafka 서버 자체가 살아있는지 |

---

## 4. Consumer Lag 화면

**용어:**
- **Consumer Lag** — Kafka에 메시지가 쌓였는데 아직 처리 못한 개수. 택배로 비유하면 배달 대기 중인 물량. 0에 가까울수록 좋음.
- **Consumer Group** — 같은 토픽을 구독하는 payment-service 인스턴스 묶음.

Kafka에 쌓인 미처리 메시지 수를 보여줍니다. **이 숫자가 0에 가까울수록 정상**입니다.

### Consumer Group 구성

| 그룹 | 은행 | 역할 |
|------|------|------|
| `payment-kftc` | A은행 | KFTC(금융결제원)에서 온 응답 처리 |
| `payment-bok` | A은행 | BOK(한국은행)에서 온 응답 처리 |
| `payment-internal` | A은행 | 서비스 내부 이벤트 처리 |
| `payment-kftc-b` | B은행 | KFTC(금융결제원)에서 온 응답 처리 |
| `payment-bok-b` | B은행 | BOK(한국은행)에서 온 응답 처리 |
| `payment-internal-b` | B은행 | 서비스 내부 이벤트 처리 |

### 정상 기준

| 등급 | Lag 수치 |
|------|---------|
| 정상 | 0 ~ 10 |
| 주의 | 10 ~ 500 |
| 위험 | 500 초과 (알림 발생) |

### Lag이 증가하는 주요 원인은 무엇인가요?

- payment-service가 다운되거나 처리 속도가 느려진 경우
- DB 연결이 부족해서 처리가 밀리는 경우
- KFTC/BOK에서 응답이 한꺼번에 몰려오는 경우

**여러 그룹이 동시에 증가**하면 → payment-service 자체 문제일 가능성이 높습니다.
**특정 그룹만 증가**하면 → 해당 외부망(KFTC 또는 BOK) 문제일 가능성이 높습니다.

---

## 5. 메시지 처리량 화면

지금 얼마나 많은 메시지가 처리되고 있는지 보여줍니다.

### 토픽 메시지 처리율 (msg/s)
- 초당 처리되는 메시지 수입니다.
- **이 값이 0이면**: 이체 요청이 없거나 Outbox 워커가 멈춘 것입니다.

### Consumer 처리 건수 (5분)
- payment-service가 각 Kafka 토픽에서 5분간 소비한 메시지 수입니다. 토픽별로 구분되어 표시됩니다.
- 메시지가 Kafka에 발행은 됐는데 이 값이 증가하지 않으면 Consumer가 멈춘 것입니다.

### Outbox 발행 성공 / 실패 (5분)
- DB에서 Kafka로 메시지를 보낸 결과입니다.

| 상황 | 의미 |
|------|------|
| 성공만 있고 실패 없음 | 정상 |
| 실패가 간헐적으로 발생 | Kafka 일시 단절 가능성 — 로그 확인 |
| 성공 = 0이고 실패가 계속 | Kafka 연결 불가 — 즉시 확인 필요 |

---

## 6. Outbox 적체 / 미완료 거래 화면

**용어:**
- **Outbox** — payment-service가 Kafka로 메시지를 보내기 전에 DB에 먼저 저장해두는 임시 보관함. DB에 저장 후 워커가 Kafka로 발행함.

어느 단계에서 처리가 막히고 있는지 파악할 수 있습니다.

### Outbox PENDING 적체 수
- DB의 임시 보관함(Outbox)에서 아직 Kafka로 못 보낸 메시지 수입니다.
- 정상이라면 거의 0에 가까워야 합니다.

| 등급 | 수치 | 알림 |
|------|------|------|
| 정상 | 0 ~ 10 | — |
| 주의 | 10 ~ 50 | — |
| 경고 | 50 초과 (2분 지속) | ⚠️ warning 알림 |
| 위험 | 200 초과 (1분 지속) | 🔴 critical 알림 |

적체가 쌓이는 주요 원인은 다음과 같습니다.
- Outbox 워커가 중단된 경우
- Kafka 브로커 연결이 끊긴 경우
- 요청이 갑자기 몰린 경우 (일시적이면 정상)

### 미완료 거래 수
- 아직 완료/실패/취소가 되지 않은 이체 건수입니다.
- 진행 중인 거래가 소수 있는 것은 정상이지만, 계속 쌓이면 처리 지연 신호입니다.

| 등급 | 수치 | 알림 |
|------|------|------|
| 정상 | 0 ~ 20 | — |
| 주의 | 20 ~ 100 | — |
| 위험 | 100 초과 (5분 지속) | ⚠️ warning 알림 |

> KFTC 응답 대기 최대 5분, BOK 응답 대기 최대 30초. 이 시간이 지나면 타임아웃이 감지되어 로그에 기록됩니다 (자동 보상 없음 — 운영자 직접 확인 필요).

### 두 지표를 함께 보는 방법
- **두 지표가 함께 오름**: 전체 파이프라인이 막힌 것
- **Outbox만 오름**: DB → Kafka 발행 단계가 막힌 것
- **미완료만 오름**: Kafka → payment-service 처리 단계가 막힌 것

---

## 7. 이체 결과 / 처리시간 화면

**용어:**
- **p50 / p95 / p99** — 처리시간 분포. p95 = 100건 중 느린 5건을 제외한 나머지의 최대 처리시간. 숫자가 낮을수록 빠름.

이체가 얼마나 잘 처리되고 있는지 보여줍니다.

### 이체 완료 / 실패 (5분)
- 5분간 최종 완료되거나 실패한 이체 건수입니다.

| 상황 | 의미 |
|------|------|
| 완료 > 0, 실패 거의 없음 | 정상 |
| 실패가 간헐적으로 발생 | KFTC/BOK 거절 또는 계좌 오류 — 정상 범위 |
| 실패 > 완료 | 외부망 장애 가능성 — 즉시 확인 필요 |
| 완료 = 0이 오래 지속 | 처리 파이프라인 중단 의심 |

### 이체 처리시간 (p50 / p95 / p99)
- 이체 요청부터 최종 완료까지 걸린 시간입니다.
- p50은 중간값, p95는 상위 5% 제외한 최대값, p99는 상위 1% 제외한 최대값입니다.

| 등급 | p50 | p95 | p99 |
|------|-----|-----|-----|
| 정상 | 1초 미만 | 3초 미만 | 5초 미만 |
| 주의 | 1~3초 | 3~10초 | 5~30초 |
| 위험 | 3초 초과 | 10초 초과 | 30초 초과 |

> 외부망 응답 대기 시간이 포함된 값이므로 일반 API 응답시간보다 높게 나오는 것이 정상입니다.

### 이체 성공률 (%)
- 전체 처리된 이체 중 완료된 비율입니다.

| 등급 | 성공률 |
|------|--------|
| 정상 | 95% 이상 |
| 주의 | 80 ~ 95% |
| 위험 | 80% 미만 |

---

## 8. 장애 / 보상 트랜잭션 화면

**용어:**
- **보상 트랜잭션** — KFTC/BOK 거절·정산오류 또는 Kafka 발행 실패로 이미 출금된 금액을 다시 돌려주는 처리. 정상적인 비즈니스 흐름이지만 너무 많으면 외부망·인프라 장애 신호.
- **DLQ (Dead Letter Queue)** — 여러 번 재시도했는데도 처리 실패한 메시지를 따로 모아두는 큐. 여기에 메시지가 쌓이면 즉시 확인 필요.

장애 신호를 감지하는 화면입니다.

### 보상 트랜잭션 발생 (5분)
- KFTC/BOK 거절 또는 Kafka 발행 실패로 출금을 다시 돌려준 건수입니다.
- 보상 자체는 정상적인 비즈니스 흐름이지만, **5분간 5건을 넘으면 외부망·인프라 장애 가능성**이 있습니다.

| 표시값 | 의미 |
|--------|------|
| `F2_KFTC` | KFTC가 이체를 거절해서 출금 취소 처리한 건수 |
| `F3_BOK` | BOK가 이체를 거절해서 출금 취소 처리한 건수 |
| `F4_KFTC` | KFTC 요청 메시지 Kafka 발행 실패로 출금 취소 처리한 건수 |
| `F4_BOK` | BOK 요청 메시지 Kafka 발행 실패로 출금 취소 처리한 건수 |
| `F7_KFTC` | KFTC 정산 중 오류로 보상 처리한 건수 |
| `F7_BOK` | BOK 정산 중 오류로 보상 처리한 건수 |

### DLQ 유입 수 (5분)
- 반복 시도했지만 끝내 처리 실패한 메시지가 쌓인 수입니다.
- **정상이라면 항상 0이어야 합니다. 1건이라도 발생하면 즉시 확인이 필요합니다.**

DLQ에 메시지가 쌓이는 주요 원인은 다음과 같습니다.
- 메시지 형식 오류 (코드 변경으로 인한 호환성 문제)
- 처리 오류가 반복되어 재시도 한도 초과
- DB 장애로 인한 처리 실패 지속

대응 순서: DLQ 메시지 내용 확인 → 원인 파악 → 수정 후 재처리 또는 폐기 결정

### 중복 거래 감지 수 (5분)
- 동일한 요청이 중복으로 들어온 횟수입니다.
- 재시도 로직으로 인한 간헐적 발생은 정상입니다.
- 급증하면 클라이언트 오류 또는 비정상 접근을 의심해야 합니다.

### KFTC 마감배치 정산 실패 (최근 24h)
- KFTC 일일 마감배치에서 건별 격리·재시도 후에도 끝내 정산에 실패한 누적 건수입니다.
- **정상이라면 0이어야 합니다. 1건이라도 발생하면 출금은 됐으나 KFTC 정산이 안 된 거래가 있다는 뜻으로 운영자가 직접 확인해야 합니다.**
- 마감배치는 매일 오전 11시(KST)에 실행됩니다. 이 시각 이후에 값이 변화하며, 그 외 시간에는 변화가 없습니다.

---

## 9. Broker 상태 화면

**용어:**
- **Broker** — Kafka 서버. 메시지를 저장하고 전달하는 역할.

Kafka 서버 자체의 상태를 보여줍니다.

### 활성 Broker 수
- 현재 정상 동작 중인 Kafka 서버 수입니다.
- 로컬 환경에서는 클러스터당 1개. **0이 되면 해당 클러스터 전체 다운**입니다.

### Under Replicated Partition
- 메시지 복제가 정상적으로 완료되지 않은 파티션 수입니다.
- **항상 0이어야 정상**입니다. 0이 아니면 Kafka 안정성에 문제가 생긴 것입니다.
- 로컬 환경(Broker 1개)에서는 복제 설정이 없어 항상 0입니다.

### Consumer Group 멤버 수
- 각 Consumer Group에서 현재 동작 중인 payment-service 인스턴스 수입니다.
- **0이 되면 메시지를 처리할 서비스가 없다는 뜻**이므로 Consumer Lag이 쌓이기 시작합니다.

---

## 10. 이상 징후 발생 시 확인 순서

### Consumer Lag이 계속 증가한다
1. **Consumer Group 멤버 수** 확인 → A은행 그룹(`payment-kftc` 등)이 0이면 `ib-payment-service` 재기동, B은행 그룹(`payment-kftc-b` 등)이 0이면 `ib-payment-service-b` 재기동
2. **미완료 거래 수** 확인 → 함께 증가하면 DB 처리 지연 가능성
3. **DB 커넥션 풀** 확인 (Service Overview 대시보드)
4. `docker logs ib-payment-service` 또는 `docker logs ib-payment-service-b` 로 오류 로그 확인

### 이체 성공률이 갑자기 떨어졌다
1. **보상 트랜잭션** 급증 여부 확인 — F2/F3(외부망 거절), F7(정산오류), F4(Kafka 발행 실패) 순으로 확인
2. **DLQ 유입** 여부 확인
3. 외부망(KFTC/BOK) 장애 가능성 → 운영팀 확인
4. 테스트 환경이라면 Mock Responder의 SUCCESS_RATE 설정 확인

### Outbox가 계속 쌓인다
1. **Outbox 발행 실패** 패널 확인 — failure 값이 있으면 Kafka 연결 문제
2. **활성 Broker 수** 확인 → 0이면 해당 클러스터 재기동
3. `docker logs ib-payment-service` 에서 OutboxPublisher 오류 확인

### DLQ에 메시지가 유입됐다
1. DLQ 토픽 메시지 내용 확인 (`kftc.network.response.dlq` 또는 `bok.network.response.dlq`)
2. 메시지 형식 오류인지 → 코드 변경 이력 확인
3. 처리 오류인지 → 해당 거래 ID로 DB 상태 확인
4. 원인 수정 후 재처리 또는 폐기 결정

### 처리시간 p99가 급증했다
1. **Consumer Lag** 확인 → Lag이 높으면 Consumer 처리 병목
2. **Outbox 적체** 확인 → 적체가 있으면 발행 지연
3. **DB 커넥션 풀** 확인 (Service Overview 대시보드)
4. KFTC 타임아웃(5분), BOK 타임아웃(30초)에 근접한 거래가 있는지 확인

---

## 11. 알림 규칙

이상 상황이 되면 자동으로 감지하는 규칙입니다. 알림 상태는 `http://localhost:9090/alerts` 에서 확인할 수 있습니다.

> Slack 알림은 `infra/alertmanager/alertmanager.yml` 설정 완료 시 자동으로 발송됩니다. 초기 설정 방법은 통합 모니터링 가이드(`UNIFIED_MONITORING_GUIDE.md`)를 참고하세요.

| 알림 이름 | 조건 | 언제 발동 | 심각도 |
|-----------|------|----------|--------|
| KafkaConsumerLagHigh | 미처리 메시지 > 500건 | 3분 지속 시 | ⚠️ warning |
| KafkaConsumerLagCritical | 미처리 메시지 > 2,000건 | 1분 지속 시 | 🔴 critical |
| OutboxBacklogHigh | Outbox PENDING > 50건 | 2분 지속 시 | ⚠️ warning |
| OutboxBacklogCritical | Outbox PENDING > 200건 | 1분 지속 시 | 🔴 critical |
| KafkaDlqIncrease | DLQ 유입 1건 이상 | 즉시 | 🔴 critical |
| CompensationRateHigh | 5분간 보상 트랜잭션 > 5건 | 즉시 | 🔴 critical |
| IncompletePaymentHigh | 미완료 거래 > 100건 | 5분 지속 시 | ⚠️ warning |

> 즉시 발동하는 알림(DLQ, 보상)은 조건이 충족되는 순간 바로 발생합니다.
> 나머지는 일시적인 스파이크를 무시하고 지속될 때만 발생합니다.

---

## 12. 알림 테스트 방법

알림이 실제로 잘 동작하는지 확인하고 싶을 때 사용합니다.

1. `.env` 파일에서 `SUCCESS_RATE=0.0` 으로 수정
2. mock-responder 재시작
```bash
docker compose --profile mock up -d mock-responder
```
3. 이체 요청 6건 이상 전송 (Swagger: `http://localhost:8084/swagger-ui.html`)
   - `X-Auth-Token-Id` 헤더는 20자 이내로 설정 (VARCHAR(20) 제약)
4. `http://localhost:9090/alerts` 에서 `CompensationRateHigh` FIRING 확인
5. 테스트 완료 후 `.env`에서 `SUCCESS_RATE=1.0` 으로 원복 후 재시작
```bash
docker compose --profile mock up -d mock-responder
```

---

## 13. "No data" 표시 시 대처법

### Consumer Lag 패널에 No data
```bash
# kafka-exporter가 실행 중인지 확인
docker ps | grep kafka-exporter
```
- Prometheus `http://localhost:9090/targets` 에서 `kafka-exporter-*` 가 UP인지 확인

### payment-service 지표 패널에 No data
1. `http://localhost:9090/targets` 에서 payment-service 가 UP인지 확인
2. `http://localhost:8084/actuator/prometheus` 에서 해당 지표 이름으로 직접 검색
3. 지표는 있는데 Grafana에 표시되지 않으면 → datasource 연결 문제 → 대시보드 재임포트 필요

### 처리시간 패널 (p50/p95/p99)에 No data
```bash
# bucket 지표가 있는지 확인
curl http://localhost:8084/actuator/prometheus | grep duration_seconds_bucket
```
- 결과가 없으면 이체 요청이 한 건도 처리된 적 없는 것입니다. Timer 메트릭은 첫 번째 이체를 처리한 후에야 `/actuator/prometheus`에 나타납니다. 모의 이체를 한 건 실행한 후 다시 확인하세요.

### Consumer Lag이 음수로 표시된다 (-6, -12 등)
- 실제 문제가 아닙니다. kafka-exporter가 `committed offset - latest offset` 계산 시 consumer가 `auto.offset.reset=latest`로 시작하면 음수가 나올 수 있습니다.
- 대시보드 쿼리에 `clamp_min(..., 0)`이 적용되어 있으므로 정상적으로 0으로 표시됩니다.
- Prometheus에서 `kafka_consumergroup_lag` 원본값이 0이면 실제 backlog 없음으로 정상입니다.

---

## 14. 로컬 실행 방법

메인 `docker-compose.yml`에 payment-service, Kafka, Prometheus, Grafana가 모두 통합되어 있습니다.

```bash
# 1. 전체 스택 기동 (프로젝트 루트에서)
docker compose up -d

# 2. Mock Responder 기동 (mock 프로파일)
docker compose --profile mock up -d mock-responder
# SUCCESS_RATE, DELAY_MS는 .env 파일에서 조정 (기본값: SUCCESS_RATE=1.0, DELAY_MS=300)
```

> **주의**: payment-service는 내부적으로 8084 포트를 사용합니다 (Swagger: `http://localhost:8084/swagger-ui.html`).
> 이체 요청 시 `X-Auth-Token-Id` 헤더는 **20자 이내**로 설정해야 합니다 (VARCHAR(20) 제약).

### Mock Responder SUCCESS_RATE 옵션

| 값 | 용도 |
|----|------|
| `1.0` | 전부 성공 — 정상 흐름 확인 |
| `0.3` | 30% 성공 — 보상 트랜잭션 발생 확인 |
| `0.0` | 전부 실패 — 알림 발동 테스트 |

### DLQ 토픽 생성 확인

`docker compose up` 시 `payment-topic-init` 서비스가 자동으로 18개 토픽(DLQ 포함)을 생성하므로 수동 생성은 필요 없습니다.

토픽이 정상 생성됐는지 확인하려면:

```bash
docker exec ib-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka:29092 --list | grep dlq
```

---

## 15. 관련 파일 위치

| 파일 | 역할 |
|------|------|
| `infra/prometheus/alerts.yml` | 알림 규칙 정의 |
| `infra/prometheus/prometheus.yml` | 데이터 수집 대상 설정 |
| `infra/grafana/provisioning/dashboards/kafka-payment.json` | 대시보드 정의 파일 |
| `services/payment-service/src/main/java/com/bank/payment/config/PaymentMetrics.java` | 결제 관련 지표 수집 코드 |
| `services/payment-service/src/main/java/com/bank/payment/config/KftcKafkaConfig.java` | KFTC Kafka Consumer/DLQ 설정 |
| `services/payment-service/src/main/java/com/bank/payment/config/BokKafkaConfig.java` | BOK Kafka Consumer/DLQ 설정 |
| `services/payment-service/mock/responder.py` | Mock Responder 코드 (테스트 전용) |
| `docker-compose.yml` | 전체 스택 통합 실행 설정 |

### 지표별 코드 연결 위치 요약

| 지표 | 메서드 | 호출 위치 |
|------|--------|----------|
| `payment_instruction_completed_total` | `paymentCompleted()` | `PaymentOrchestratorImpl`(자행 즉시·예약이체 성공), `KftcNetworkResponseConsumer`, `BokNetworkResponseConsumer` |
| `payment_instruction_failed_total` | `paymentFailed()` | `PaymentOrchestratorImpl`(검증 실패·보상 완료 등 12개 경로), `KftcNetworkResponseConsumer`, `BokNetworkResponseConsumer` |
| `payment_instruction_duration_seconds` | `durationTimer.record()` | `paymentCompleted()` 내부 |
| `payment_instruction_incomplete` | DB Gauge | `PaymentMetrics` 생성자 (스크레이프 시 DB 조회) |
| `payment_outbox_pending` | DB Gauge | `PaymentMetrics` 생성자 (스크레이프 시 DB 조회) |
| `payment_outbox_publish_total` | `outboxPublished()`, `outboxFailed()` | `OutboxPublisher` |
| `payment_kafka_consume_total` | `consumed(topic)` | `KftcNetworkResponseConsumer`, `BokNetworkResponseConsumer`, `KftcNetworkRequestConsumer` |
| `payment_kafka_dlq_total` | `dlq(cluster)` | `KftcKafkaConfig` recoverer, `BokKafkaConfig` recoverer |
| `payment_compensation_total` | `compensation(type)` | `KftcNetworkResponseConsumer`, `BokNetworkResponseConsumer`, `OutboxPublisher` |
| `payment_idempotency_duplicate_total` | `idempotencyDuplicate()` | `PaymentOrchestratorImpl` |
| `payment_kftc_settlement_failed_total` | `kftcSettlementFailed()` | `KftcSettlementBatchWorker` (알림 미연결 — 대시보드 "KFTC 마감배치 정산 실패" 패널로 확인) |

> **참고**: `paymentCompleted()` 는 자행 이체(즉시·예약)와 타행 이체(KFTC/BOK 정산) 모두에서 호출됩니다. `paymentFailed()` 역시 검증 실패·보상 트랜잭션 완료·KFTC/BOK 거절 등 모든 실패 경로를 커버합니다. 이를 통해 이체 성공률(`completed / (completed + failed)`)이 이체 유형에 관계없이 정확하게 계산됩니다.
