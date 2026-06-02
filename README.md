# Internet Banking MVP

작성자: 정혜영

AX풀뱅크 인터넷뱅킹 MVP는 개인뱅킹 화면, 예금/적금 상품 가입과 해지, 계좌조회, 계좌이체, 챗봇 상담, 현금흐름 기반 상품 추천을 하나의 시연 흐름으로 연결한 금융 서비스 프로젝트입니다.

이 저장소는 Next.js 프론트엔드와 Spring Boot/FastAPI 기반 백엔드 서비스를 함께 포함한 멀티 모듈 구조입니다.

---

## 프로젝트 구성

| 경로 | 기술 | 역할 |
|---|---|---|
| `web` | Next.js 14, TypeScript, Tailwind CSS | 개인/관리자 인터넷뱅킹 화면, 챗봇 UI |
| `services/deposit-service` | Java 17, Spring Boot, JPA | 예금상품, 계약, 계좌, 거래, 해지 |
| `services/consultation-service` | Python, FastAPI | 챗봇, 상담, 고객 금융정보 조회 |
| `common` | Java | 공통 예외, 보안, 유틸리티 |
| `infra` | Docker Compose | PostgreSQL, Redis, Prometheus, Grafana |

---

## 서비스 실행

### 전체 서비스 한 번에 시작

프로젝트 루트에서 PowerShell 실행:

```powershell
.\start-all.ps1
```

각 서비스가 별도 터미널 창으로 실행됩니다. 기동 완료까지 약 30초 소요됩니다.

| 서비스 | 포트 | URL |
|---|---|---|
| Web (Next.js) | 3001 | http://localhost:3001 |
| Deposit Service (Spring Boot) | 8082 | http://localhost:8082/api |
| Consultation Service (FastAPI) | 8087 | http://localhost:8087 |

### 개별 실행

```bash
# Web
cd web && npm run dev

# Deposit Service
./gradlew :services:deposit-service:bootRun

# Consultation Service
cd services/consultation-service
python -m uvicorn app.main:app --host 0.0.0.0 --port 8087 --reload
```

> **주의**: 세 서비스가 모두 실행 중이어야 이체, 해지, 챗봇 등 모든 기능이 정상 동작합니다.

---

## 주요 기능

### 개인뱅킹 사이트

#### 계좌조회
- 전체 계좌 목록 조회 (예금·적금·청약·입출금)
- 계좌별 잔액, 거래내역, 만기일 확인
- 계좌 상세에서 이체 바로가기

#### 계좌이체
- **당행/타행 선택**: 입금계좌 행에서 당행(내 계좌 드롭다운) / 타행(은행 선택 + 계좌번호 입력) 직접 선택
- **보안카드 입력**: 앞 2자리, 뒤 2자리 (숫자만 입력)
- **금융인증서 인증**: 전자서명 원문 확인 → YESKEY PIN 6자리 입력 → 이체 실행
- 출금 계좌와 동일한 계좌는 입금 대상에서 자동 제외

#### 예금/적금 가입
- 상품별 약관 동의, 가입 정보 입력, 계좌 비밀번호 설정
- 가입 완료 시 deposit-service DB에 즉시 저장
- 저장 실패 시 에러 안내 (localStorage 폴백 없음)

#### 예금/적금 해지
- 계좌 선택 → 입금 방식 선택(당행/타행/현금) → **보안카드 입력** → **금융인증서 PIN** → 해지 실행
- 당행 입금 시 해지 계좌 자신은 입금 계좌 목록에서 자동 제외
- 해지 후 계좌 목록 자동 갱신

---

### 챗봇 (AXful 상담 챗봇)

챗봇은 사이트 우측 하단 CHATBOT 버튼으로 접근합니다. 로그인 필요 기능은 챗봇 내 인라인 로그인 폼을 제공합니다.

#### 상품 안내 (로그인 불필요)
- 예금·적금·청약 상품 목록 조회
- 상품 비교: "예금과 적금 차이" 질문 시 개념 설명 + 현금흐름 기반 추천 동시 제공
- 금리 안내, 가입 조건 안내

#### 상품 추천 (로그인 필요)
- 가입 기간·금액·상품 유형·목적 단계별 입력 후 맞춤 추천
- 추천 결과에서 금리, 기간, 추천 이유 확인
- 후속 질문 (1위 이유, 각 상품 장점 등) 대화 지속 가능

#### 내 상품 조회 (로그인 필요)
- 가입된 전체 계좌 목록 조회 (deposit-service DB 기준)
- 입출금·예금·적금·청약 계좌별 잔액, 만기일 확인

#### 챗봇 이체 (로그인 필요)
- 내 상품 목록에서 입출금 계좌의 **이체** 버튼 클릭
- **당행**: 내 계좌 목록에서 선택 (출금 계좌 자동 제외)
- **타행**: 은행 선택 + 계좌번호 직접 입력
- 이체 금액, 메모 입력 → 내용 확인
- **보안카드** (앞·뒤 2자리) → **금융인증서 전자서명 원문 확인** → **PIN 6자리** → 이체 실행
- 이체 성공 후 내 상품 목록 자동 갱신

#### 챗봇 해지 (로그인 필요)
- 내 상품 목록에서 **해지** 버튼 클릭
- 입금 방식 선택 (당행 계좌 입금 / 타행 계좌 입금 / 현금 수령)
  - 당행: 내 전체 계좌에서 선택 (해지 계좌 자동 제외)
  - 타행: 은행 선택 + 계좌번호 입력
- **보안카드** → **금융인증서 원문 확인** → **PIN 6자리** → 해지 실행
- 해지 후 내 상품 목록 자동 갱신

#### 상담원 연결
- 상담원 연결 버튼 또는 대화 중 상담원 요청 시 라이브 채팅으로 연결

---

## 아키텍처

```
[브라우저]
    │
    ▼
[Next.js Web :3001]
    ├─ /api/consultation/* → proxy → Consultation Service :8087
    └─ /api/v1/*           → proxy → Deposit Service :8082 (또는 직접)

[Consultation Service :8087] ── FastAPI, PostgreSQL
    ├─ 챗봇 대화 처리 (intent 분류 → feature 실행 → LLM 폴백)
    ├─ 상품 추천 (PRODUCT_COMPARE, CASH_FLOW_RECOMMEND, PRODUCT_SEARCH)
    ├─ 챗봇 이체 (deposit_accounts 직접 쿼리)
    └─ Kafka 이벤트 발행 (KAFKA_ENABLED=true 시 활성화)

[Deposit Service :8082] ── Spring Boot, JPA, PostgreSQL
    ├─ 계좌/계약/거래 CRUD
    ├─ 상품 가입 (createContract)
    └─ 해지 처리 (terminate: 해지계좌 CLOSE + 입금계좌 잔액 증가)
```

---

## Kafka 검증 (선택)

챗봇-Kafka 연동을 검증하려면:

### 1. Kafka 브로커 실행

```bash
cd services/consultation-service
docker-compose up -d zookeeper kafka kafka-ui
```

Kafka UI: http://localhost:8090

### 2. Kafka 활성화

`services/consultation-service/.env`:
```
CONSULTATION_KAFKA_ENABLED=true
```

### 3. Consultation 서비스 재시작

```bash
cd services/consultation-service
python -m uvicorn app.main:app --host 0.0.0.0 --port 8087 --reload
```

### 4. 챗봇 동작 후 토픽 확인

http://localhost:8090 에서 아래 토픽에 메시지 수신 확인:

| 토픽 | 이벤트 |
|---|---|
| `consultation.chatbot.events` | ChatbotConsultationStarted, ChatbotMessageHandled, ChatbotAgentTransferRequested |
| `consultation.chat.events` | 상담사 채팅 이벤트 |

> 검증 완료 후 `.env`를 `CONSULTATION_KAFKA_ENABLED=false`로 원복하면 기존 기능에 영향 없습니다.

---

## 환경 변수

### Consultation Service (`services/consultation-service/.env`)

| 변수 | 기본값 | 설명 |
|---|---|---|
| `CONSULTATION_DATABASE_URL` | `postgresql+psycopg://deposit:deposit@localhost:5432/deposit_db` | DB 연결 |
| `CONSULTATION_KAFKA_ENABLED` | `false` | Kafka 이벤트 발행 활성화 |
| `CONSULTATION_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka 브로커 주소 |
| `CONSULTATION_OPENAI_API_KEY` | - | LLM 폴백용 OpenAI 키 |

### Deposit Service

`services/deposit-service/src/main/resources/application.yml` 참조. 기본값으로 `localhost:5432/deposit_db` 사용.

---

## 데이터베이스

PostgreSQL (포트 5432, DB: `deposit_db`, 사용자: `deposit`)

주요 테이블:

| 테이블 | 설명 |
|---|---|
| `deposit_banking_products` | 예금/적금/청약 상품 목록 |
| `deposit_contracts` | 고객 가입 계약 |
| `deposit_accounts` | 계좌 (잔액, 상태) |
| `deposit_transactions` | 거래 내역 |
| `chatbot_consultations` | 챗봇 상담 이력 |
| `chat_message_history` | 챗봇 메시지 이력 |
| `consultations` | 전체 상담 이력 |
