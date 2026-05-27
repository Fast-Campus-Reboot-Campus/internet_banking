# Kafka Payment 대시보드 해석 가이드

> 대상 대시보드: **Kafka Payment - 계좌이체 중간망**
> 대상 독자: 개발팀 전원 (결제계 담당 포함)
> **환경**: Docker Compose 기준 (`services/payment-service/docker-compose-kafka.yml`).

---

## 0. 접속 방법

| 도구 | URL | 계정 |
|------|-----|------|
| Grafana | `http://localhost:3000` | admin / admin |
| Prometheus | `http://localhost:9090` | 없음 |

대시보드 경로: **Dashboards → Kafka Payment - 계좌이체 중간망**

> payment-service, kafka-exporter, Prometheus가 모두 실행 중이어야 데이터가 표시된다.

---

## 1. 대시보드 구성

대시보드는 6개 섹션(Row)으로 구성된다.

| 섹션 | 핵심 패널 | 무엇을 보는가 |
|------|-----------|--------------|
| Consumer Lag | 그룹별 Lag, 합계 Stat | 메시지가 쌓이는지 여부 |
| 메시지 처리량 | 토픽 msg/s, 소비 건수, Outbox 발행 | 실시간 흐름 속도 |
| Outbox 적체 / 미완료 거래 | PENDING 수, 미완료 거래 수, 적체 추이 | 병목 위치 파악 |
| 이체 결과 / 처리시간 | 완료/실패 건수, p50/p95/p99, 성공률 | 거래 품질 |
| 장애 / 보상 트랜잭션 | 보상 건수, DLQ 유입, 중복 감지 | 장애 신호 |
| Broker / Partition 상태 | 활성 Broker, Under-Replicated, Consumer Group 멤버 수 | 인프라 안정성 |

---

## 2. Consumer Lag 섹션

### Consumer Lag (그룹별)
- **설명**: Kafka에 쌓인 미소비 메시지 수. Lag이 0이면 소비자가 실시간으로 따라가고 있는 것.
- **소비 그룹 구성**:

| 그룹 | 담당 토픽 | 역할 |
|------|----------|------|
| `payment-kftc` | `kftc.network.response` | KFTC 응답 처리 |
| `payment-bok` | `bok.network.response` | BOK 응답 처리 |
| `payment-internal` | `payment.internal.*` | 내부 이벤트 처리 |

| 등급 | Lag |
|------|-----|
| 정상 | 0 ~ 10 |
| 주의 | 10 ~ 500 |
| 위험 | > 500 (알림 발생) / > 2000 (critical 알림) |

- **Lag이 증가하는 주요 원인**:
  - payment-service 인스턴스 다운 또는 처리 지연
  - DB 커넥션 풀 고갈로 Consumer 처리 속도 저하
  - 외부망(KFTC/BOK) 응답 폭주

- **여러 그룹이 동시에 증가**하면: 인스턴스 자체 장애 가능성.
- **특정 그룹만 증가**하면: 해당 망(KFTC or BOK) 또는 소비 로직 문제.

---

## 3. 메시지 처리량 섹션

### 토픽 메시지 처리율 (msg/s)
- Kafka 내부 파티션 오프셋 증가율. 생산자가 메시지를 실제로 보내는 속도.
- 이 값이 0이면: 발신 요청이 없거나 Outbox 워커가 멈춘 것.

### Consumer 처리 건수 (5분)
- payment-service가 실제로 처리 완료한 Kafka 메시지 수. `payment_kafka_consume_total` 기준.
- 토픽 메시지 처리율과 비교하면:
  - **처리율 > 소비 건수**: 메시지가 들어오지만 소비 완료가 안 됨 → Lag 증가 신호.
  - **두 값이 비슷**: 정상.

### Outbox 발행 성공 / 실패 (5분)
- Outbox 워커가 DB → Kafka 발행한 건수.

| 상황 | 의미 |
|------|------|
| 성공만 있고 실패 없음 | 정상 |
| 실패가 간헐적으로 발생 | Kafka 일시 단절 또는 직렬화 오류 — 로그 확인 |
| 성공 = 0, 실패 지속 | Kafka 연결 불가 또는 워커 중단 — 즉시 확인 |

---

## 4. Outbox 적체 / 미완료 거래 섹션

### Outbox PENDING 적체 수
- DB `outbox_message` 테이블에서 `PENDING` 상태로 남아있는 메시지 수.
- Outbox 워커가 정상이면 0에 수렴해야 한다.

| 등급 | 수치 | 알림 |
|------|------|------|
| 정상 | 0 ~ 10 | — |
| 주의 | 10 ~ 50 | — |
| 경고 | > 50 (2분 지속) | warning 알림 |
| 위험 | > 200 (1분 지속) | critical 알림 |

- Outbox가 쌓이는 원인:
  - Outbox 워커 스케줄러 중단
  - Kafka 브로커 연결 불가
  - 요청 급증 (일시적 적체는 정상, 계속 오르면 이상)

### 미완료 거래 수 (PENDING/CLEARING)
- `payment_instruction` 테이블에서 COMPLETED/FAILED/CANCELED가 아닌 거래 수.
- 정상 상태에서는 진행 중인 거래들이 소수 존재하지만 일정 수준 이상이면 처리 지연 신호.

| 등급 | 수치 | 알림 |
|------|------|------|
| 정상 | 0 ~ 20 | — |
| 주의 | 20 ~ 100 | — |
| 위험 | > 100 (5분 지속) | warning 알림 |

- KFTC 응답 대기 타임아웃(기본 5분), BOK 응답 대기 타임아웃(기본 30초)이 지나면 자동으로 보상 트랜잭션 진행.
- 미완료 거래가 계속 누적되면: 외부망 응답 지연 또는 Consumer 처리 중단 확인.

### 적체 추이 (timeseries)
- PENDING과 미완료 거래를 한 그래프에서 시계열로 비교.
- 두 선이 함께 오르면: 전체 처리 파이프라인 지연.
- PENDING만 오르면: Outbox → Kafka 발행 단계 문제.
- 미완료만 오르면: Kafka → Consumer 처리 단계 문제.

---

## 5. 이체 결과 / 처리시간 섹션

### 이체 완료 / 실패 (5분)
- 5분 구간 내 COMPLETED/FAILED로 최종 확정된 거래 건수.

| 상황 | 의미 |
|------|------|
| 완료 > 0, 실패 ≈ 0 | 정상 (mock SUCCESS_RATE=1.0 환경) |
| 실패가 간헐적으로 발생 | KFTC/BOK 거절 또는 계좌 오류 — 정상 범위 내 |
| 실패 > 완료 | 외부망 거절률 급증 — KFTC/BOK 장애 또는 테스트 환경 |
| 완료 = 0 장시간 | 처리 파이프라인 중단 의심 |

### 이체 end-to-end 처리시간 (p50 / p95 / p99)
- API 요청 접수부터 KFTC/BOK 응답으로 COMPLETED 확정까지의 총 소요시간.
- `payment_instruction_duration_seconds_bucket` 기반 `histogram_quantile`.

| 등급 | p50 | p95 | p99 |
|------|-----|-----|-----|
| 정상 | < 1s | < 3s | < 5s |
| 주의 | 1–3s | 3–10s | 5–30s |
| 위험 | > 3s | > 10s | > 30s |

> 외부망 응답 지연이 포함된 값이므로 단순 HTTP API 응답시간보다 훨씬 높다.
> KFTC 타임아웃 기본 5분, BOK 30초 — 타임아웃 직전 완료된 거래가 p99를 끌어올릴 수 있다.

### 이체 성공률 (%)
- `completed / (completed + failed)` 비율.

| 등급 | 성공률 |
|------|--------|
| 정상 | > 95% |
| 주의 | 80–95% |
| 위험 | < 80% |

- 성공률이 급락하면 외부망 거절 급증 또는 계좌 한도/잔액 오류 — Outbox 상태와 같이 확인.

---

## 6. 장애 / 보상 트랜잭션 섹션

### 보상 트랜잭션 발생 (5분)
- KFTC/BOK 거절로 출금 취소(B-5) 보상이 실행된 건수. `payment_compensation_total` 기준.

| 레이블 | 의미 |
|--------|------|
| `F2_KFTC` | KFTC REJECT → 역분개 4건 + 출금 취소 |
| `F3_BOK` | BOK SETTLEMENT_REJECT → 역분개 4건 + 출금 취소 |
| `F7_KFTC` | KFTC SETTLEMENT_NOTIFY 비정상 코드 → 보상 |
| `F7_BOK` | BOK SETTLEMENT_COMPLETED 비정상 코드 → 보상 |

- 보상이 발생하는 것 자체는 비정상이 아니다. 외부망 거절은 정상적인 비즈니스 결과.
- **주의**: 5분간 > 5건 → critical 알림 (보상 급증 = 외부망 장애 가능성).
- 보상이 발생했는데 성공률은 정상이면: 거절 후 재시도가 성공한 것.

### DLQ 유입 수 (5분)
- Dead Letter Queue에 쌓인 메시지 수. `payment_kafka_dlq_total` 기준.
- **DLQ 유입 = 즉시 대응 필요**. 정상 상태에서는 0이어야 한다.

| cluster | DLQ 토픽 |
|---------|----------|
| `kftc` | `kftc.network.response.dlq` |
| `bok` | `bok.network.response.dlq` |

- DLQ 유입 원인:
  - 메시지 파싱 오류 (스키마 변경, 필드 누락)
  - 최대 재시도 초과 (Consumer 처리 오류가 반복)
  - DB 장애로 Consumer 트랜잭션 실패 지속
- **대응**: DLQ 토픽 메시지 내용 확인 → 원인 수정 → 재처리 또는 폐기 결정.

### 중복 거래 감지 수 (5분)
- 동일 멱등키로 중복 요청이 들어온 횟수. `payment_idempotency_duplicate_total` 기준.
- 재시도 로직이 있는 클라이언트에서 간헐적으로 발생하는 것은 정상.
- 급증하면: 클라이언트 재시도 루프 오류 또는 자동화 공격 의심.

---

## 7. Broker / Partition 상태 섹션

### 활성 Broker 수
- KFTC/BOK/Internal 클러스터별 정상 동작 중인 Broker 수.
- 로컬 개발 환경에서는 각 클러스터당 Broker 1개. 1이면 정상, 0이면 클러스터 다운.

### Under Replicated Partition
- 복제가 정상적으로 완료되지 않은 파티션 수. 0이어야 정상.
- 0이 아니면: Broker 장애 또는 네트워크 문제로 레플리카 동기화 실패 → Kafka 안정성 위협.
- 로컬 환경(단일 Broker)에서는 항상 0 (복제 설정 없음).

### Consumer Group 멤버 수
- 각 Consumer Group에 활성화된 Consumer 인스턴스 수.
- payment-service-a/b 두 인스턴스가 뜨면 2, 하나면 1.
- **0이 되면**: Consumer가 완전히 빠져나간 것 → Lag이 쌓이기 시작하는 시점.

---

## 8. 이상 징후 패턴별 확인 순서

### Consumer Lag이 계속 증가한다
1. **Consumer Group 멤버 수** 확인 → 0이면 payment-service 재기동
2. **미완료 거래 수** 확인 → DB 처리 지연 여부 확인
3. **DB 커넥션 풀** 확인 (Service Overview 대시보드)
4. `docker logs payment-service-a` 로 Consumer 오류 확인

### 이체 성공률이 갑자기 떨어졌다
1. **보상 트랜잭션** 급증 여부 확인 — F2/F3 레이블 확인
2. **DLQ 유입** 여부 확인
3. 외부망(KFTC/BOK) 장애 가능성 → 운영팀 확인
4. mock 환경이라면 SUCCESS_RATE 설정 확인

### Outbox가 계속 쌓인다
1. **Outbox 발행 실패** 여부 확인 — failure가 있으면 Kafka 연결 문제
2. **활성 Broker 수** 확인 → 0이면 클러스터 재기동
3. `docker logs payment-service-a` 에서 `OutboxPublisher` 오류 확인

### DLQ에 메시지가 유입됐다
1. DLQ 토픽 (`kftc.network.response.dlq` 또는 `bok.network.response.dlq`) 메시지 내용 확인
2. 파싱 오류인지 → 스키마 변경 확인
3. 처리 오류인지 → 해당 clearingNo/bokReferenceNo로 DB 상태 확인
4. 원인 수정 후 DLQ 메시지 재처리 또는 폐기 결정

### 처리시간 p99가 급증했다
1. **Consumer Lag** 확인 → Lag이 높으면 Consumer 처리 병목
2. **Outbox 적체** 확인 → 적체가 있으면 발행 지연
3. **DB 커넥션 풀** 확인 (Service Overview 대시보드)
4. KFTC 타임아웃 기본 5분(300s)에 근접하면 → 외부망 응답 지연 가능성

---

## 9. 알림 규칙

`infra/prometheus/alerts.yml` 의 `kafka-consumer-lag` 및 `kafka-payment` 그룹.

### kafka-consumer-lag 그룹

| 알림명 | 조건 | 지연 | 심각도 |
|--------|------|------|--------|
| KafkaConsumerLagHigh | 그룹+토픽별 Lag > 500 | 3분 | warning |
| KafkaConsumerLagCritical | 그룹+토픽별 Lag > 2000 | 1분 | critical |

### kafka-payment 그룹

| 알림명 | 조건 | 지연 | 심각도 |
|--------|------|------|--------|
| OutboxBacklogHigh | Outbox PENDING > 50 | 2분 | warning |
| OutboxBacklogCritical | Outbox PENDING > 200 | 1분 | critical |
| KafkaDlqIncrease | DLQ 유입 > 0 | 즉시 | critical |
| CompensationRateHigh | 5분간 보상 > 5건 | 즉시 | critical |
| IncompletePaymentHigh | 미완료 거래 > 100 | 5분 | warning |

> `for: 0m` 인 규칙(DLQ, 보상)은 조건 충족 즉시 FIRING 전환.
> 나머지는 지정 시간 동안 지속될 때만 FIRING — 일시적 스파이크는 무시.

알림 상태 확인: `http://localhost:9090/alerts`

---

## 10. "No data" 표시 시 대처법

### Consumer Lag 패널에 No data
- kafka-exporter가 실행 중인지 확인:
  ```
  docker ps | grep kafka-exporter
  ```
- Prometheus Targets(`http://localhost:9090/targets`)에서 `kafka-exporter-*` 가 UP인지 확인
- kafka-exporter 컨테이너가 Kafka 브로커에 연결할 수 있는지 네트워크 확인

### payment-service 지표 패널에 No data
1. `http://localhost:9090/targets` 에서 `payment-service` 가 UP인지 확인
2. `http://localhost:8080/actuator/prometheus` 에서 해당 메트릭명 직접 검색
3. 메트릭이 있는데 Grafana에 없으면: datasource UID 문제 → `import_dashboard.py` 재실행

### 히스토그램 패널 (p50/p95/p99)에 No data
- `payment_instruction_duration_seconds_bucket` 메트릭이 존재하는지 확인:
  ```
  curl http://localhost:8080/actuator/prometheus | grep duration_seconds_bucket
  ```
- 없으면: `application.yml` 의 `percentiles-histogram.payment.instruction.duration: true` 설정 확인 후 서비스 재기동

---

## 11. 관련 파일 위치

| 파일 | 역할 |
|------|------|
| `infra/prometheus/alerts.yml` | Kafka/Payment 알림 규칙 |
| `infra/prometheus/prometheus.yml` | 스크레이프 대상 (kafka-exporter, payment-service) |
| `infra/grafana/provisioning/dashboards/kafka-payment.json` | 대시보드 정의 |
| `services/payment-service/src/main/java/com/bank/payment/config/PaymentMetrics.java` | 커스텀 메트릭 등록 |
| `services/payment-service/docker-compose-kafka.yml` | Kafka 3클러스터 + kafka-exporter |
| `services/payment-service/docker-compose-mock.yml` | Mock KFTC/BOK 응답기 (테스트용) |
