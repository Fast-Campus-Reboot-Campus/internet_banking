# Consultation Service

인터넷뱅킹 MVP의 챗봇·상담 서비스다. FastAPI 기반으로 챗봇 기능 실행, 상담 시작, 상담사 연결, 상담 메시지 이력을 제공한다.

## 실행

```powershell
cd services/consultation-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8001
```

## 주요 API

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/health` | 헬스 체크 |
| `GET` | `/chatbot/categories` | 챗봇 기능 카테고리 목록 |
| `GET` | `/chatbot/features` | 챗봇 기능 목록 |
| `GET` | `/chatbot/features/{feature_code}` | 챗봇 기능 상세 |
| `POST` | `/chatbot/features/{feature_code}/execute` | 챗봇 기능 실행 |
| `POST` | `/chatbot/consultations/start` | 챗봇 상담 시작 |
| `POST` | `/chatbot/consultations/{id}/messages` | 챗봇 상담 메시지 전송 |
| `GET` | `/chat/queue` | 상담 대기열 조회 |
| `POST` | `/chat/consultations/{id}/connect` | 상담사 상담 수락 |
| `POST` | `/chat/consultations/{id}/messages` | 상담사 메시지 전송 |
| `GET` | `/chat/consultations/{id}/messages` | 상담 메시지 이력 조회 |
| `POST` | `/chat/consultations/{id}/end` | 상담 종료 |

## 기능 코드

| 카테고리 | 기능 코드 |
|---|---|
| PRODUCT_ADVICE | `PRODUCT_GUIDE`, `RATE_GUIDE`, `JOIN_CONDITION`, `PRODUCT_COMPARE`, `TERMS_RAG`, `FAQ` |
| USER_FINANCE | `MY_ACCOUNTS`, `MY_PRODUCTS`, `CONTRACT_STATUS`, `MATURITY_SCHEDULE`, `INTEREST_HISTORY` |
| STAFF_SUPPORT | `STAFF_CUSTOMER`, `STAFF_CONTRACT`, `STAFF_ACCOUNT`, `STAFF_TRANSFER_FLOW`, `STAFF_CONSULTATION_HISTORY` |

## 설정

환경변수로 런타임 설정을 주입한다. `.env`와 백업 파일은 커밋하지 않는다.

| 변수 | 설명 |
|---|---|
| `CONSULTATION_DATABASE_URL` | 상담 서비스 DB 연결 문자열 |
| `CONSULTATION_KAFKA_ENABLED` | Kafka 이벤트 발행 사용 여부 |
| `CONSULTATION_KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap 서버 |

## 테스트

```powershell
cd services/consultation-service
python -m pytest tests/ -q
```

테스트 캐시, 가상환경, 로그, 백업 파일은 Git에 포함하지 않는다.
