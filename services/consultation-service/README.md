# Consultation Service

작성자: 정혜영

AXful Bank 챗봇 상담 서비스입니다.
고객의 현금흐름을 분석해 맞춤 금융상품을 추천하고, 이체·해지·상담사 연결을 지원합니다.

---

## 목차

- [서비스 구조](#서비스-구조)
- [주요 기능](#주요-기능)
- [100점 채점 모델](#100점-채점-모델)
- [현금흐름 분석](#현금흐름-분석)
- [나이 기반 상품 필터](#나이-기반-상품-필터)
- [API 엔드포인트](#api-엔드포인트)
- [환경변수](#환경변수)
- [로컬 실행](#로컬-실행)

---

## 서비스 구조

```
consultation-service/
├── app/
│   ├── main.py          # FastAPI 애플리케이션 진입점
│   ├── config.py        # 환경변수 설정 (Pydantic Settings)
│   ├── services.py      # 핵심 비즈니스 로직
│   ├── models.py        # SQLAlchemy ORM 모델
│   ├── schemas.py       # Pydantic 요청/응답 스키마
│   ├── llm.py           # LLM 어댑터 및 Intent 분류기
│   ├── rag.py           # RAG 엔진 (상품 설명서 검색)
│   ├── kafka.py         # Kafka 이벤트 발행
│   └── features/        # 피처별 실행기
├── requirements.txt
└── Dockerfile
```

---

## 주요 기능

### 챗봇 상담

| 기능 | feature_code | 설명 |
|------|-------------|------|
| 상품 안내 | PRODUCT_GUIDE | 예금·적금·청약 상품 목록 및 금리 안내 |
| **현금흐름 추천** | CASH_FLOW_RECOMMEND | 고객 입출금 패턴 분석 → 맞춤 상품 추천 |
| **조건 맞춤 추천** | PRODUCT_SEARCH | 기간·금액·유형 입력 → 100점 모델로 추천 |
| 금리 안내 | RATE_GUIDE | 기간별 기본금리 + 우대금리 조회 |
| 가입 조건 안내 | JOIN_CONDITION | 대상·채널·기간·금액 조건 안내 |
| 내 현금 흐름 | MY_CASH_FLOW | 고객 입출금 거래 내역 조회 |
| 내 상품 | MY_PRODUCTS | 가입된 예금·적금·청약 상품 조회 |
| 챗봇 이체 | — | 계좌 간 이체 (보안카드·금융인증서 인증) |
| 상담사 연결 | — | 대기 큐 → 직원 연결 |

### 직원 지원

| 기능 | feature_code |
|------|-------------|
| 고객 정보 조회 | STAFF_CUSTOMER |
| 고객 계약 조회 | STAFF_CONTRACT |
| 고객 계좌 조회 | STAFF_ACCOUNT |
| 고객 거래 내역 | STAFF_CASH_FLOW |
| 이체 흐름 추적 | STAFF_TRANSFER_FLOW |
| 상담 이력 조회 | STAFF_CONSULTATION_HISTORY |

---

## 100점 채점 모델

`PRODUCT_SEARCH`와 `CASH_FLOW_RECOMMEND` 양쪽에서 동일한 채점 함수(`_rank_products`)를 사용합니다.

### 채점 항목

| 항목 | 배점 | 계산 방식 |
|------|------|-----------|
| **재정 적합도** | 40점 | 예금: `min(투자가능잔액/최소가입금액, 5)/5 × 40`<br>적금: `min(월잉여금/(최소납입액×2), 5)/5 × 40`<br>페르소나 일치 시 ×1.3 가산 |
| **수익성 (ROI)** | 30점 | 고객 실제 납입 가능 금액 기준 만기 이자 → 정규화<br>예금: `투자가능잔액 × 금리 × targetPeriod / 12`<br>적금: `월잉여금 × targetPeriod × 금리 × (targetPeriod+1) / 24` |
| **유동성 매칭** | 20점 | 거래빈도 ≥10 → 단기(12개월 이하) 선호<br>거래빈도 ≤5 → 장기(24개월 이상) 선호<br>중도해지 가능 시 +0.2 가산 |
| **부가 혜택** | 10점 | 비과세 가능 7점 + 중도해지 가능 3점 |

> 합계 100점 만점, 내림차순 정렬 후 TOP 3 추천  
> 동점 시 실제 적용 금리(기본 + 우대) 높은 상품 우선

### 페르소나 분류

| 유형 | 조건 | 우대 상품 유형 |
|------|------|---------------|
| 지출 과다형 | 월잉여금 ≤ 0 | 예금 |
| 저축 성장형 | 잔액 < 월잉여금 × 12 | 적금 |
| 목돈 보유형 | 잔액 ≥ 월잉여금 × 12 | 예금 |

### 우대금리 수치·조건 표시

상품 추천 카드에 우대금리 수치(`+X%`)와 조건 텍스트를 함께 표시합니다.

- **데이터 출처**: deposit DB의 `banking_deposit_product_interest_rates` 테이블에서 `rate_type = 'PREFERENTIAL'`인 행을 상품별로 집계
  - 우대금리 수치: `SUM(rate)` → 카드에 `+X%` 형식 표시
  - 우대금리 조건: `STRING_AGG(condition_description)` → 카드에 조건 텍스트 표시
- **fallback**: DB에 조건 데이터가 없으면 상품명 키워드 기반 조건 사용

| 키워드 | fallback 조건 |
|--------|--------------|
| 내맘대로 | 자동이체 설정 |
| 자유적금 | 자동이체 설정 |
| 맑은하늘 | 맑은하늘 앱 설치 후 인증코드 등록 |
| 직장인우대 | 급여이체 실적 등록 |
| 달러 | 달러 환전 실적 보유 |
| 청년도약 | 소득 요건 충족 확인 |
| 수퍼정기 | 비대면 가입 |
| 정기예금 | 비대면(인터넷·스타뱅킹) 가입 |
| 꿈적금 | 만기 유지 |
| 함께적금 | 2인 이상 공동 가입 |

표시 예시:
```
🎁 우대금리 +0.6% 조건: 자동이체 설정 우대
```

---

### 금리 반영 방식

`base_interest_rate` 대신 `banking_deposit_product_interest_rates` 테이블에서 고객 선택 기간에 맞는 **BASE + PREFERENTIAL 금리를 합산**해 실제 적용금리를 사용합니다.

```
targetPeriod = clamp(사용자 입력기간 or 12, min_period_month, max_period_month)
```

---

## 현금흐름 분석

`_analyze_customer_cash_flow()` 함수 집계 기준:

| 항목 | 처리 방식 |
|------|----------|
| 분석 기간 | 최근 3개월 거래만 포함 |
| 테스트 계좌 제외 | 30일 이상 거래 이력 있는 계좌만 분석 대상 |
| 내부 이체 제외 | TRANSFER 타입 전체 제외 |
| 내부 이체 감지 | counterparty_account_id / 계좌번호 / 당일 동일금액 DEPOSIT+WITHDRAW 쌍 |
| 잔액 구분 | `total_balance`: 전체 ACTIVE 합계 (참고용)<br>`investable_balance`: `is_withdrawable=True` 계좌만 (ROI 계산 기준) |

응답 `data[0].cf_debug` 필드에 계산 근거가 포함됩니다:

```
분석계좌=1개 | 기준=최근3개월 | 전체ACTIVE잔액=13,006,030원 |
투자가능잔액(출금가능)=12,906,030원 | 외부입금=9,600,000원 |
외부출금=2,172,000원 | 월잉여금=2,476,000원
```

---

## 나이 기반 상품 필터

`customer-service` API 연동으로 고객 나이를 조회해 가입 자격 없는 상품을 필터링합니다.

```
GET {CUSTOMER_SERVICE_URL}/api/v1/customers/me
Header: X-Customer-Id: {customer_id}
Response: { data: { birthDate: "19900101", ... } }
```

| target_group | 조건 | 처리 |
|-------------|------|------|
| 청년층 (id=3, 만 19~34세) | 나이 > 34 또는 나이 < 19 | 추천 제외 |
| 현역병 (id=4) | 상품명 키워드 기반 | 추천 제외 |
| 일반고객 (id=1) | 모든 나이 | 추천 포함 |

> customer-service 연동 실패(타임아웃 2초) 시 상품명 키워드 필터로 자동 fallback

---

## API 엔드포인트

### 챗봇

| Method | Path | 설명 |
|--------|------|------|
| POST | `/chatbot/consultations/start` | 챗봇 상담 시작 |
| POST | `/chatbot/consultations/{id}/messages` | 메시지 전송 |
| POST | `/chatbot/features/{code}/execute` | 피처 직접 실행 |
| GET | `/chatbot/features` | 피처 목록 조회 |
| POST | `/chatbot/transfer` | 챗봇 이체 실행 |

### 상담사 채팅

| Method | Path | 설명 |
|--------|------|------|
| GET | `/chat/queue` | 대기 큐 조회 |
| POST | `/chat/consultations/{id}/connect` | 상담사 연결 |
| POST | `/chat/consultations/{id}/messages` | 메시지 전송 |
| POST | `/chat/consultations/{id}/end` | 상담 종료 |

### 기타

| Method | Path | 설명 |
|--------|------|------|
| GET | `/health` | 헬스체크 → `{"status":"UP"}` |
| GET | `/metrics` | Prometheus 메트릭 |

---

## 환경변수

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `CONSULTATION_DATABASE_URL` | **(필수)** | PostgreSQL 연결 URL (`postgresql+psycopg://...`) |
| `CONSULTATION_KAFKA_ENABLED` | `false` | Kafka 이벤트 발행 활성화 여부 |
| `CONSULTATION_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka 브로커 주소 |
| `CONSULTATION_OPENAI_API_KEY` | `""` | OpenAI API 키 (LLM 추천 활성화 시 필요) |
| `CONSULTATION_OPENAI_MODEL` | `gpt-4o-mini` | 사용할 LLM 모델명 |
| `CONSULTATION_CUSTOMER_SERVICE_URL` | `http://localhost:8081` | customer-service 주소 (나이 조회) |
| `CONSULTATION_LANGFUSE_ENABLED` | `false` | Langfuse LLM 트레이싱 활성화 여부 |

---

## 로컬 실행

### 가상환경 설정

```bash
cd services/consultation-service
python -m venv .venv

# Windows
.venv\Scripts\activate

# macOS / Linux
source .venv/bin/activate

pip install -r requirements.txt
```

### 실행 (Windows)

```bat
start-8087.bat
```

### 직접 실행

```bash
CONSULTATION_KAFKA_ENABLED=false \
CONSULTATION_DATABASE_URL=postgresql+psycopg://deposit:deposit@localhost:5432/deposit_db \
uvicorn app.main:app --host 0.0.0.0 --port 8087 --reload
```

### 헬스체크 확인

```bash
curl http://localhost:8087/health
# {"status":"UP"}
```

### Docker Compose

```bash
docker-compose up consultation-service
```

서비스 포트: **8087**
