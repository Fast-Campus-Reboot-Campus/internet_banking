# 챗봇 상담 모니터링 가이드

> 대상 대시보드: **챗봇 상담 모니터링**
> 대상 독자: 개발팀 전원
> 환경: 로컬 직접 설치 기준 (`consultation-service` + Prometheus + Grafana)

---

## 이 가이드는 무엇인가요?

고객이 챗봇 상담을 시작하면 consultation-service는 시나리오·LLM·기능 실행 중 하나의 방식으로 응답하고, 필요 시 상담사에게 이관합니다. 이 대시보드는 챗봇이 지금 정상적으로 작동하는지, LLM 응답이 느려지거나 오류가 발생하진 않는지, Kafka 이벤트가 제대로 흐르는지 한눈에 확인할 수 있도록 만들어졌습니다.

---

## 1. 용어 설명

| 용어 | 쉬운 설명 |
|------|----------|
| **process_method** | 챗봇이 메시지를 처리한 방식. SCENARIO(버튼 선택), BP003_GPT(LLM 응답), BP002_LLM(이관 안내), FEATURE_*(기능 실행), AGENT_TRANSFER(오류로 인한 이관) |
| **이관 (Handoff)** | 챗봇이 처리하지 못하거나 고객이 요청해서 실제 상담사에게 연결되는 것 |
| **만족도 점수** | 상담 종료 시 고객이 입력하는 1~5점 평점 |
| **LLM** | 챗봇이 자연어로 응답하기 위해 호출하는 AI 언어 모델 (OpenAI GPT) |
| **RAG** | LLM 응답 시 상품 정보 등 DB 데이터를 컨텍스트로 함께 전달해 더 정확한 답변을 유도하는 방식 |
| **p50 / p95** | 응답시간 분포. p95 = 100건 중 느린 5건을 제외한 나머지의 최대 응답시간 |

---

## 2. 접속 방법

| 도구 | URL | 계정 |
|------|-----|------|
| Grafana (대시보드) | `http://localhost:3000` | admin / admin |
| Prometheus (알림 확인) | `http://localhost:9090/alerts` | 없음 |
| consultation-service 메트릭 원본 | `http://localhost:8087/metrics` | 없음 |

대시보드 경로: Grafana → **Dashboards** → **챗봇 상담 모니터링**

---

## 3. 대시보드 구성

| 섹션 | 패널 | 설명 |
|------|------|------|
| 세션 현황 | 활성 세션 수, 세션 시작, 상담사 이관율, 평균 만족도 | 챗봇 사용 현황 한눈에 |
| 메시지 라우팅 | 처리 방식별 추이, 처리 방식 분포 | 어떤 방식으로 응답하고 있는지 |
| LLM | 응답시간 P50/P95, 오류율, 오류 추이 | LLM 호출 성능 및 안정성 |
| Kafka | 이벤트 발행률, 이벤트 소비율 | 이벤트 파이프라인 상태 |
| HTTP | 요청률, 응답시간 P95, 5xx 오류율 | API 레이어 전반 상태 |
| 세션 종료 | 세션 종료 추이, 만족도 점수 분포 | 상담 완결 및 고객 만족도 |

---

## 4. 세션 현황 섹션

### 활성 세션 수
- 현재 진행 중인 챗봇 상담 세션 수.
- 서비스 재시작 시 0으로 초기화된다.

| 등급 | 수치 |
|------|------|
| 정상 | 0 ~ 200 |
| 주의 | 200 초과 (알림 발생) |

### 세션 시작 (건/분)
- 분당 새로 시작된 챗봇 세션 수.
- 갑자기 0으로 떨어지면 서비스 다운 또는 업스트림(프론트엔드) 장애 의심.

### 상담사 이관율
- 전체 메시지 처리 중 상담사 이관이 발생한 비율.
- 높을수록 챗봇이 스스로 처리하지 못하는 문의가 많다는 뜻.

| 등급 | 비율 |
|------|------|
| 정상 | 30% 미만 |
| 주의 | 30% 이상 (알림 발생) |

> **참고**: 서비스 재시작 직후에는 이전 카운터 누적치와 섞여 100%를 초과하는 이상값이 보일 수 있다. 일정 시간이 지나면 정상화된다.

### 평균 만족도
- 상담 종료 시 입력된 만족도 점수의 평균 (1~5점).
- 3.5 미만이면 챗봇 응답 품질 점검 필요.

---

## 5. 메시지 라우팅 섹션

### 메시지 처리 방식별 추이
- 시간축으로 어떤 방식으로 응답했는지 보여준다.

| process_method | 의미 |
|---------------|------|
| BP003_GPT | LLM이 자연어로 응답 — 정상적인 상담 흐름 |
| BP002_LLM | 상담사 이관 안내 (고객 요청 이관) |
| SCENARIO | 시나리오 버튼 흐름으로 처리 |
| FEATURE_* | 잔액 조회 등 기능 실행으로 처리 |
| AGENT_TRANSFER | LLM 오류로 인한 자동 이관 — 주의 필요 |

- **AGENT_TRANSFER가 급증하면**: LLM 오류율도 함께 확인할 것.

### 메시지 처리 방식 분포 (1h)
- 최근 1시간 누적 분포를 파이 차트로 보여준다.
- BP003_GPT 비중이 압도적으로 높은 것이 정상.

---

## 6. LLM 섹션

### LLM 응답시간 P50/P95
- LLM 호출부터 응답 수신까지 걸린 시간.

| 등급 | P50 | P95 |
|------|-----|-----|
| 정상 | 3초 미만 | 10초 미만 |
| 주의 | 3~5초 | 10~20초 (알림 발생) |
| 위험 | 5초 초과 | 20초 초과 |

- P95만 급등하면: 특정 긴 질문에서 지연 — 프롬프트 길이 확인.
- 전체가 함께 느려지면: OpenAI API 서버 이슈 또는 네트워크 문제.

### LLM 오류율
- LLM 호출 중 에러가 발생한 비율.

| 등급 | 비율 |
|------|------|
| 정상 | 0% |
| 주의 | 5% 미만 |
| 위험 | 10% 이상 (알림 발생) |

- 오류 발생 시 자동으로 상담사 이관으로 전환된다.
- 오류율이 높으면: OpenAI API 키 유효성, 잔여 크레딧, 네트워크 연결 확인.

### LLM 오류 발생 추이
- 오류가 특정 시점에 몰려 발생하면: 외부 API 장애.
- 지속적으로 분산 발생하면: 특정 입력 패턴이 API 오류를 유발하는 것.

---

## 7. Kafka 섹션

> `CONSULTATION_KAFKA_ENABLED=false`이면 이 섹션은 항상 No data이다.

### Kafka 이벤트 발행률 (topic / status)
- 챗봇 이벤트가 Kafka 토픽으로 정상 발행되고 있는지 확인.
- `status=success`만 보여야 정상. `status=error`가 나타나면 즉시 확인 필요.

| 토픽 | 발행 이벤트 |
|------|-----------|
| `consultation.chatbot.events` | 세션 시작, 메시지 처리, 이관 요청 |
| `consultation.chat.events` | 상담사 채팅 종료 |

### Kafka 이벤트 소비율 (event_type / status)
- consultation-service가 구독 중인 토픽(예: `deposit.contract.events`)에서 메시지를 소비한 현황.
- `status=success`가 정상.

---

## 8. HTTP 섹션

### HTTP 요청률 (endpoint별)
- 각 API 엔드포인트의 초당 요청 수.
- `/chatbot/consultations/start`가 주요 지표.

### HTTP 응답시간 P95 (endpoint별)
- p95 응답시간이 2초를 넘으면 사용자 체감 성능 저하.

### HTTP 5xx 오류율
- 서버 내부 에러 비율. 0이 정상.
- Kafka 토픽 미생성 등 환경 문제 시 5xx 급등 가능.

---

## 9. 세션 종료 섹션

### 세션 종료 추이
- 세션이 종료되는 패턴 확인. 시작 추이와 비교해 대칭적이어야 정상.

### 만족도 점수 분포 (1h 누적)
- 각 점수대에 쌓인 종료 건수.

| 상황 | 해석 |
|------|------|
| ≤5.0 > ≤4.0 | 5점이 가장 많음 — 정상 |
| ≤2.0에 쏠림 | 챗봇 응답 품질 문제 — 즉시 확인 |
| 전체 0 | 상담이 종료되지 않거나 점수 미입력 |

---

## 10. 이상 징후 패턴별 확인 순서

### 챗봇 응답이 갑자기 느려졌다
1. **LLM 응답시간 P95** 확인 — 10초 초과 여부
2. **LLM 오류율** 확인 — 오류로 인한 재시도 여부
3. OpenAI API 상태 페이지 확인
4. consultation-service 로그에서 timeout 에러 확인

### 상담사 이관이 급증했다
1. **메시지 처리 방식 추이**에서 AGENT_TRANSFER 비중 확인
2. **LLM 오류율** 확인 — LLM 실패로 인한 강제 이관 여부
3. LLM 오류가 없으면: 고객이 직접 이관 요청하는 케이스 증가 (BP002_LLM 비중 확인)

### Kafka 발행 에러가 발생했다
1. Kafka 브로커 상태 확인: `docker ps --filter "name=kafka"`
2. 토픽 존재 여부 확인: `docker exec payment-kftc-kafka kafka-topics.sh --list --bootstrap-server localhost:9092`
3. 토픽이 없으면 생성: `docker exec payment-kftc-kafka kafka-topics.sh --create --topic <토픽명> --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1`

### 만족도가 갑자기 낮아졌다
1. **LLM 응답시간** 확인 — 응답이 너무 느리면 불만족 증가
2. **AGENT_TRANSFER 비중** 확인 — 오류 이관이 잦으면 불만족 증가
3. 챗봇 시나리오 또는 LLM 프롬프트 최근 변경 이력 확인

---

## 11. 알림 규칙

| 알림명 | 조건 | 지연 | 심각도 |
|--------|------|------|--------|
| ChatbotActiveSessions | 활성 세션 > 200 | 5분 | warning |
| ChatbotHandoffRateHigh | 이관율 > 30% | 3분 | warning |
| ChatbotLlmErrorHigh | LLM 오류율 > 10% | 2분 | critical |
| ChatbotLlmResponseSlow | LLM P95 > 10초 | 3분 | warning |
| ChatbotKafkaPublishErrorHigh | Kafka 발행 오류율 > 5% | 2분 | critical |

알림 상태 확인: `http://localhost:9090/alerts`

---

## 12. 로컬 실행 방법

```powershell
# 1. consultation-service 기동
cd "c:\Users\jaho3\OneDrive\바탕 화면\AX_FULL_Bank\internet_banking\services\consultation-service"
python -m uvicorn app.main:app --host 0.0.0.0 --port 8087

# 2. Kafka 토픽 생성 (KAFKA_ENABLED=true 시 필요)
docker exec payment-kftc-kafka kafka-topics.sh --create --topic consultation.chatbot.events --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
docker exec payment-kftc-kafka kafka-topics.sh --create --topic consultation.chat.events --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

### 테스트 요청 전송

```powershell
# 세션 시작
$s = Invoke-RestMethod -Method Post -Uri "http://localhost:8087/chatbot/consultations/start" -ContentType "application/json" -Body '{"entry_screen":"MAIN"}'
$id = $s.chatbot_consultation_id

# LLM 호출 (자연어 메시지)
Invoke-RestMethod -Method Post -Uri "http://localhost:8087/chatbot/consultations/$id/messages" -ContentType "application/json" -Body '{"message":"적금 금리 알려줘"}'

# 상담사 이관
Invoke-RestMethod -Method Post -Uri "http://localhost:8087/chatbot/consultations/$id/messages" -ContentType "application/json" -Body '{"message":"상담사 연결", "button_value":"AGENT"}'

# 상담 종료 (만족도 포함)
$chat = Invoke-RestMethod -Method Get -Uri "http://localhost:8087/chat/queue"
$chatId = ($chat | Where-Object { $_.chatbot_consultation_id -eq $id }).chat_consultation_id
Invoke-RestMethod -Method Post -Uri "http://localhost:8087/chat/consultations/$chatId/end" -ContentType "application/json" -Body '{"satisfaction_score": 5}'
```

---

## 13. "No data" 표시 시 대처법

| 패널 | No data 원인 | 조치 |
|------|-------------|------|
| LLM 오류율 / 오류 추이 | LLM 에러 없음 | 정상 — 에러 없으면 당연히 No data |
| Kafka 이벤트 발행/소비 | KAFKA_ENABLED=false | `.env`에서 true로 변경 후 재시작 |
| HTTP 5xx 오류율 | 서버 에러 없음 | 정상 |
| LLM 응답시간 | 요청이 적거나 방금 시작됨 | 메시지 몇 건 더 보낸 후 확인 |
| 전체 패널 | Prometheus 미수집 | `http://localhost:9090/targets`에서 consultation-service UP 여부 확인 |

---

## 14. 관련 파일 위치

| 파일 | 역할 |
|------|------|
| `infra/prometheus/alerts.yml` | 알림 규칙 (chatbot 그룹) |
| `infra/prometheus/prometheus.yml` | 스크레이핑 대상 (consultation-service 포함) |
| `infra/grafana/provisioning/dashboards/chatbot.json` | 대시보드 정의 파일 |
| `services/consultation-service/app/metrics.py` | 메트릭 정의 |
| `services/consultation-service/app/main.py` | 세션·메시지·만족도 계측 |
| `services/consultation-service/app/llm.py` | LLM 응답시간·오류·토큰 계측 |
| `services/consultation-service/app/kafka.py` | Kafka 발행·소비 계측 |
| `services/consultation-service/.env` | 환경 변수 (API 키, Kafka 설정 등) |

---

## 15. 관련 가이드

| 문서 | 내용 |
|------|------|
| [DASHBOARD_GUIDE.md](DASHBOARD_GUIDE.md) | Service Overview 대시보드 — HTTP, JVM, DB, 인증/보안 |
| [KAFKA_PAYMENT_GUIDE.md](KAFKA_PAYMENT_GUIDE.md) | Kafka Payment 대시보드 — Consumer Lag, Outbox, DLQ |
| [ML_LOAN_REVIEW_GUIDE.md](ML_LOAN_REVIEW_GUIDE.md) | ML 대출 심사 모니터링 — 추론 성능, 드리프트 감지 |
| [INFRA_VERSIONS.md](INFRA_VERSIONS.md) | 모니터링 인프라 버전 및 설정 파일 위치 |
