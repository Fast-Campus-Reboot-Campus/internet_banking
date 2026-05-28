# Grafana 대시보드 해석 가이드

> 대상 대시보드: **Internet Banking - Service Overview**
> 대상 독자: 개발팀 전원 (운영 모니터링 담당 포함)
> **환경**: 로컬 직접 설치 기준. Docker 전환 시 실행 방법이 달라지나 URL과 대시보드 해석 방법은 동일하다.

---

## 0. 접속 방법

| 도구 | URL | 계정 |
|------|-----|------|
| Grafana | `http://localhost:3000` | 초기: admin / admin (첫 로그인 시 변경 요청) |
| Prometheus | `http://localhost:9090` | 없음 (인증 없음) |

대시보드 경로: **Dashboards → Internet Banking - Service Overview**

> Prometheus와 각 서비스가 실행 중이어야 데이터가 표시된다.
> 서비스 실행 방법은 각 서비스 README 또는 로컬 개발 가이드를 참고한다.

---

## 1. 대시보드 구성

대시보드는 5개 섹션으로 구성된다.

| 섹션 | 패널 | 설명 |
|------|------|------|
| HTTP | 요청 수, 에러율, 응답시간 | 외부 요청 흐름 |
| JVM | Heap, GC, 스레드 | 애플리케이션 런타임 상태 |
| 시스템 / DB | CPU, 커넥션 풀, 쿼리 지연 | 인프라 수준 상태 |
| 인증 / 보안 | 로그인 성공/실패, JWT 실패, 회원가입 | 보안 이벤트 추적 |
| 서버 자원 | Disk 사용량, Network I/O | 호스트 OS 수준 자원 (windows_exporter) |

상단 **서비스** 드롭다운으로 서비스를 전환한다.
> 인증/보안 · 서버 자원 섹션은 `customer-service` 선택 시에만 의미 있는 데이터가 표시된다.

### 모니터링 대상 서비스

| 서비스 | 포트 | 역할 |
|--------|------|------|
| customer-service | 8081 | 고객 인증 및 계좌 관리 |
| deposit-service | 8082 | 예금 상품 관리 |
| loan-service | 8083 | 대출 심사 및 관리 |
| payment-service | 8084 | 이체 및 결제 처리 |
| master-service | 8085 | 공통 마스터 데이터 |
| ai-service | 8086 | 자동 심사 AI 모듈 |

드롭다운에 서비스 이름이 보이지 않으면 해당 서비스가 실행되지 않은 것이다.

---

## 2. HTTP 섹션

### HTTP 요청 수 (req/s)
- **설명**: 초당 처리된 요청 수. URI별로 구분된다.
- **정상**: 서비스 특성에 따라 다름. 급격한 급등/급락이 없으면 정상.
- **주의**: 평소 대비 2배 이상 급등 → 트래픽 폭증 or DDoS 의심.
- **주의**: 갑자기 0으로 떨어짐 → 서비스 다운 or 네트워크 단절 의심.

### HTTP 에러율 (%)
- **설명**: 4xx(클라이언트 오류), 5xx(서버 오류) 비율.

| 등급 | 4xx | 5xx |
|------|-----|-----|
| 정상 | < 5% | < 1% |
| 주의 | 5–15% | 1–3% |
| 위험 | > 15% | > 3% |

- **4xx 증가**: 잘못된 요청, 인증 실패 증가 — 로그인 실패 공격 가능성 확인.
- **5xx 증가**: 서버 내부 오류 — 즉시 애플리케이션 로그 확인 필요.

### API 응답시간 (p50 / p95 / p99)
- **설명**: 응답시간의 백분위수. p95 = 상위 5%를 제외한 최대 응답시간.

| 등급 | p50 | p95 | p99 |
|------|-----|-----|-----|
| 정상 | < 100ms | < 500ms | < 1s |
| 주의 | 100–300ms | 500ms–2s | 1–5s |
| 위험 | > 300ms | > 2s | > 5s |

- p50은 낮은데 p99가 높으면: 특정 요청(무거운 쿼리, 외부 API 호출)에서 병목 발생.
- 전체 백분위가 함께 오르면: DB 커넥션 풀이나 CPU 포화 가능성.

---

## 3. JVM 섹션

### JVM Heap 메모리
- **설명**: JVM이 사용하는 힙 메모리. `사용 중` vs `최대` 비교.

| 등급 | 사용률 |
|------|--------|
| 정상 | < 70% |
| 주의 | 70–85% |
| 위험 | > 85% |

- 사용률이 지속적으로 우상향하면: **메모리 누수** 의심 → Heap dump 분석 필요.
- 주기적으로 올랐다 내려오는 패턴: 정상 (GC 동작).

### GC 일시정지 시간
- **설명**: Garbage Collection으로 인해 애플리케이션이 멈춘 시간.

| 등급 | 일시정지 |
|------|----------|
| 정상 | < 200ms |
| 주의 | 200ms–1s |
| 위험 | > 1s |

- GC가 자주, 오래 발생하면 Heap이 부족하다는 신호 → 메모리 설정 조정 필요.
- p99 응답시간이 높을 때 GC 그래프도 높으면: GC가 응답 지연 원인.

### JVM 스레드 수
- **설명**: 활성 스레드 / 데몬 스레드 수.

| 등급 | 활성 스레드 (Spring Boot 기본) |
|------|-------------------------------|
| 정상 | 20–100 |
| 주의 | 100–200 |
| 위험 | > 200 또는 지속 증가 |

- 스레드가 계속 증가하고 줄지 않으면: **스레드 누수** 또는 블로킹 I/O 의심.

---

## 4. 시스템 / DB 섹션

### CPU 사용률
- **설명**: 프로세스 CPU (`process_cpu_usage`) vs 시스템 전체 CPU (`system_cpu_usage`).

| 등급 | 사용률 |
|------|--------|
| 정상 | < 60% |
| 주의 | 60–80% |
| 위험 | > 80% |

- 프로세스 CPU만 높고 시스템 CPU는 낮으면: 해당 서비스만 과부하.
- 시스템 CPU가 전반적으로 높으면: 호스트 자체의 자원 부족.

### DB 커넥션 풀 (HikariCP)
- **설명**: 활성(사용 중) / 유휴(대기 중) / 대기(획득 대기) / 최대 커넥션 수.

| 등급 | 기준 |
|------|------|
| 정상 | 대기 = 0, 활성 < 최대의 70% |
| 주의 | 대기 > 0 가끔 발생 |
| 위험 | 대기 지속 발생, 활성 = 최대 |

- 대기(pending)가 0이 아니면: 커넥션 부족 → `maximum-pool-size` 조정 또는 쿼리 최적화 필요.
- 유휴만 많고 활성이 거의 없으면: 풀 크기가 과도하게 설정된 것.

### DB 쿼리 지연시간 (p95)
- **설명**: 커넥션 획득 시간 + 실제 쿼리 사용 시간의 p95.

| 등급 | 커넥션 획득 | 쿼리 실행 |
|------|------------|----------|
| 정상 | < 10ms | < 100ms |
| 주의 | 10–100ms | 100ms–500ms |
| 위험 | > 100ms | > 500ms |

- 커넥션 획득이 느리면: 풀 고갈 신호.
- 쿼리 실행이 느리면: 인덱스 누락, 락 경합, 대용량 쿼리 의심.

---

## 4-1. 인증 / 보안 섹션

> `customer-service` 에 등록된 Micrometer 커스텀 카운터를 시각화한다.
> 메트릭 소스: `LoginService`, `RegisterService`, `JwtAuthenticationFilter`, `SecurityConfig`

### 로그인 성공 / 실패 (5분)
- **설명**: 5분 구간 동안 발생한 로그인 결과를 reason 별로 집계.

| 레이블 | 의미 |
|--------|------|
| 로그인 성공 | 정상 인증 완료 |
| 로그인 실패 (전체) | reason 불문 전체 실패 |
| 계정 잠금 | 실패 임계치 초과로 잠금된 계정 시도 |
| 비밀번호 오류 | 비밀번호 불일치 |

- **주의**: 비밀번호 오류가 짧은 시간 안에 급증 → 브루트포스 공격 가능성.
- 계정 잠금이 여러 계정에서 동시에 발생 → 자격증명 스터핑 공격 의심.

### JWT 실패 / 권한 거부 (5분)
- **JWT 검증 실패**: 만료·위변조·잘못된 형식의 토큰 시도 횟수.
- **권한 거부 (403)**: 인증은 됐으나 해당 리소스에 접근 권한 없음.

| 등급 | JWT 실패 (5분) | 권한 거부 (5분) |
|------|--------------|----------------|
| 정상 | < 10 | < 5 |
| 주의 | 10–50 | 5–20 |
| 위험 | > 50 | > 20 |

### 회원가입 현황 (5분)
- **회원가입 완료**: 성공적으로 가입된 신규 고객 수.
- **중복 아이디**: 이미 존재하는 loginId로 가입 시도 횟수.
- 중복 아이디 시도가 매우 높으면: 자동화된 계정 생성 시도(봇) 의심.

---

## 4-2. 서버 자원 섹션 (windows_exporter)

> `localhost:9182` 에서 수집하는 Windows 호스트 메트릭.
> windows_exporter가 실행 중이지 않으면 이 섹션은 "No data"로 표시된다.

### Disk 사용량 (C:)
- C: 드라이브의 전체 용량 대비 사용 중인 용량.

| 등급 | 사용률 |
|------|--------|
| 정상 | < 70% |
| 주의 | 70–85% |
| 위험 | > 85% |

### Network I/O
- NIC별 수신(RX) / 송신(TX) 처리량 (bytes/s).
- 평소 대비 수신이 급증하면: 대용량 트래픽 유입 or 데이터 수집 공격.
- 송신이 급증하면: 데이터 유출 또는 외부 의존성 폭증 가능성 확인.

---

## 5. 이상 징후 패턴별 확인 순서

### 응답이 느려졌다는 제보
1. **API 응답시간** p95/p99 확인
2. **DB 커넥션 풀** 대기 여부 확인
3. **DB 쿼리 지연시간** 확인
4. **GC 일시정지 시간** 확인
5. **CPU 사용률** 확인

### 에러가 갑자기 증가했다
1. **HTTP 에러율** 4xx/5xx 구분
2. 5xx면 → 해당 서비스 로그 즉시 확인
3. 4xx면 → 특정 URI 패턴 확인 (인증 공격 여부)

### 서비스가 다운된 것 같다
1. **HTTP 요청 수** 가 0인지 확인
2. Prometheus Targets 페이지(`http://localhost:9090/targets`)에서 UP/DOWN 확인
3. 서비스 프로세스 상태 확인

### 메모리가 계속 증가한다
1. **JVM Heap** 사용률 추이 확인 (1시간, 24시간 비교)
2. **GC 일시정지** 빈도 및 시간 확인
3. 줄어들지 않으면 메모리 누수 — Heap dump 후 MAT(Memory Analyzer Tool) 분석

### 배포 직후 확인
배포 후 5~10분간 다음 순서로 확인한다.

1. **HTTP 요청 수** — 요청이 정상적으로 들어오고 있는지
2. **HTTP 에러율** — 5xx 에러가 배포 전보다 증가했는지
3. **API 응답시간** p95 — 배포 전과 비교해 튀는 구간이 없는지
4. **JVM Heap** — 기동 후 안정화되는지 (초기에 높다가 내려오는 것은 정상)
5. **DB 커넥션 풀** — 커넥션이 정상 범위 안에 있는지

> 시간 범위를 **Last 15m** 또는 **Last 30m** 으로 설정하고 배포 전후를 비교한다.

---

## 6. 시간 범위 활용

| 상황 | 권장 시간 범위 |
|------|--------------|
| 평상시 모니터링 | Last 1h |
| 장애 발생 직후 | Last 15m ~ 30m |
| 트렌드 분석 | Last 24h ~ 7d |
| 배포 전후 비교 | 배포 시점 ±30m |

우측 상단 시간 선택기에서 변경 가능하다.

---

## 7. "No data" 표시 시 대처법

패널에 데이터가 없을 때 단계별로 확인한다.

### 1단계: 서비스 드롭다운 확인
- 상단 **서비스** 드롭다운이 비어있거나 선택이 안 되어 있으면 → 드롭다운에서 서비스 선택
- 드롭다운 자체가 열리지 않으면 → 해당 서비스가 실행 중이지 않거나 Prometheus가 수집을 못한 것

### 2단계: Prometheus 수집 상태 확인
`http://localhost:9090/targets` 접속 → 해당 서비스가 **UP** 인지 확인

| 상태 | 조치 |
|------|------|
| UP | Prometheus는 정상, Grafana datasource 문제로 넘어감 |
| DOWN | 서비스 실행 여부 확인, `prometheus.yml` 포트 설정 확인 |
| 목록에 없음 | `prometheus.yml` scrape_configs에 서비스 추가 여부 확인 |

### 3단계: Grafana datasource 확인
`http://localhost:3000/connections/datasources` → Prometheus datasource → **Save & test**
- "Successfully queried the Prometheus API" 메시지가 나와야 정상
- 실패하면 URL이 `http://localhost:9090` 으로 설정되어 있는지 확인


---

### 4단계: 대시보드 재import
위 3단계가 모두 정상인데도 No data이면 datasource 매핑 문제일 수 있다.
1. 현재 대시보드 삭제 (Dashboards → 체크박스 → Delete)
2. **Dashboards → New → Import → Upload JSON file**
3. `infra/grafana/provisioning/dashboards/internet-banking.json` 업로드
4. Import 화면에서 Prometheus datasource 선택 후 Import

---

## 8. 알림 규칙

### 8-1. Prometheus 알림 규칙 (`infra/prometheus/alerts.yml`)

Prometheus가 직접 평가하는 레코딩 규칙. `http://localhost:9090/alerts` 에서 상태 확인 가능.

| 그룹 | 알림명 | 조건 | 지연 | 심각도 |
|------|--------|------|------|--------|
| service-availability | ServiceDown | `up == 0` | 1m | critical |
| http-errors | HighErrorRate5xx | 5xx 비율 > 5% | 2m | critical |
| http-errors | HighErrorRate4xx | 4xx 비율 > 20% | 5m | warning |
| api-performance | SlowApiResponse | p95 응답시간 > 2초 | 5m | warning |
| jvm | JvmHeapHigh | Heap 사용률 > 90% | 5m | warning |
| auth-security | BruteForceLoginDetected | 5분간 로그인 실패 > 20회 | 즉시 | critical |
| auth-security | HighJwtInvalidRate | 5분간 JWT 실패 > 50회 | 즉시 | warning |
| database | DbConnectionPoolExhausted | 대기 커넥션 > 5 | 2m | critical |

> **`for: 0m`** 인 규칙(BruteForce, JwtInvalid)은 조건 충족 즉시 FIRING으로 전환된다.
> 나머지 규칙은 지정 시간 동안 지속될 때만 FIRING으로 전환되어 일시적 스파이크를 무시한다.

### 8-2. Grafana Managed Alerts

Grafana가 자체적으로 Prometheus를 쿼리하여 평가하는 알림 (Prometheus Alertmanager와 별개).

| 알림명 | 조건 | 알림 채널 |
|--------|------|----------|
| Service Down | `up < 1` | Slack DM |
| Brute Force Login Detected | 5분간 로그인 실패 > 20 | Slack DM |

알림 상태 확인: Grafana → **Alerting → Alert rules**

### 8-3. Slack DM 알림

- **Contact point**: `Slack-DM` (Incoming Webhook 사용)
- **Webhook URL**: Slack 앱 설정에서 확인 (`internet-banking-alerts` 앱)
- **라우팅**: 모든 알림 → Slack-DM (기본 정책)
- **반복 주기**: 최초 알림 후 1시간 간격으로 재전송 (FIRING 상태 지속 시)
- **알림 형식**: `[FIRING] 알림명` 또는 `[RESOLVED] 알림명`

알림 설정 확인: Grafana → **Alerting → Contact points**

> 알림 메시지에 한국어를 사용하면 Slack에서 깨질 수 있다. 영어로 작성 권장.

### 8-4. 알림이 오지 않을 때 체크리스트

1. Prometheus rules 로드 확인: `http://localhost:9090/rules` → alerts.yml 그룹이 보이는지
2. Prometheus lifecycle 활성화 확인: `curl -X POST http://localhost:9090/-/reload` 응답 확인
   - 실패하면 Prometheus 재시작 시 `--web.enable-lifecycle` 플래그 필요
3. Grafana alert rule datasource UID 확인: Alerting → Alert rules → 편집 → datasource 드롭다운에 Prometheus가 선택되어 있는지
4. Slack Webhook URL 유효성 확인: `curl -X POST <webhook-url> -H "Content-Type: application/json" -d '{"text":"test"}'`

---

## 9. 관련 가이드

| 문서 | 내용 |
|------|------|
| [KAFKA_PAYMENT_GUIDE.md](KAFKA_PAYMENT_GUIDE.md) | Kafka Payment 대시보드 — Consumer Lag, Outbox, 보상 트랜잭션, DLQ 해석 |
| [INFRA_VERSIONS.md](INFRA_VERSIONS.md) | 모니터링 인프라 버전 및 설정 파일 위치 |
