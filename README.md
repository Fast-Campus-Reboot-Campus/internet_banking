# Internet Banking MVP

작성자: 정혜영

AX풀뱅크 인터넷뱅킹 MVP는 개인뱅킹 화면, 예금/적금 상품 가입과 해지, 계좌조회, 계좌이체, 챗봇 상담, 현금흐름 기반 상품 추천을 하나의 시연 흐름으로 연결한 금융 서비스 프로젝트입니다.

이 저장소는 Next.js 프론트엔드와 Spring Boot/FastAPI 기반 백엔드 서비스를 함께 포함한 멀티 모듈 구조입니다. 예금 서비스는 계좌, 계약, 거래, 상품 추천을 담당하고, 상담 서비스는 챗봇과 고객 금융정보 조회를 담당합니다.

---

## 프로젝트 구성

| 경로 | 기술 | 역할 |
|---|---|---|
| `web` | Next.js 14, TypeScript, Tailwind CSS | 개인/관리자 인터넷뱅킹 화면, 챗봇 UI |
| `services/deposit-service` | Java 17, Spring Boot, JPA | 예금상품, 계약, 계좌, 거래, 해지 |
| `services/consultation-service` | Python, FastAPI | 챗봇, 상담, 고객 금융정보 조회 |
| `common` | Java | 공통 예외, 보안, 유틸리티 |
| `infra` | Docker Compose | PostgreSQL, Redis, Prometheus, Grafana |
| `docs` | Markdown, Postman | 설계 문서와 API 테스트 자료 |

---

## 주요 기능

### 개인뱅킹 프론트엔드

- 개인뱅킹 메인, 계좌조회, 거래내역 조회, 계좌상세 화면
- 예금/적금 상품 목록, 상품 상세, 가입, 해지, 해지결과 조회
- 계좌이체 입력, 확인, 금융인증서 인증, 결과 화면
- 챗봇 위젯을 통한 내 계좌/내 상품 조회, 상품 추천, 해지 보조 흐름
- 로그인/로그아웃 상태와 상담 API 프록시 연동

### 계좌이체

- 계좌상세 또는 계좌목록에서 `이체`를 누르면 선택한 계좌가 출금계좌로 고정됩니다.
- 출금계좌는 입출금 계좌만 허용합니다.
- 정기예금과 적금 계좌에는 이체 버튼을 표시하지 않습니다.
- 확인 화면에서 금융인증서 전자서명과 PIN 입력 후 이체가 실행됩니다.
- 이체 결과와 최근 이체 내역을 화면에 반영합니다.

### 예금/적금 해지

- 해지 대상 계좌를 선택하거나 계좌조회 화면에서 바로 해지 화면으로 진입할 수 있습니다.
- 지급 방식은 다음 세 가지를 지원합니다.
  - 당행 계좌 입금
  - 타행 계좌 입금
  - 현금 수령
- 해지 버튼 클릭 시 금융인증서 전자서명 원문을 보여준 뒤 PIN 입력을 완료해야 해지가 실행됩니다.
- 당행 계좌 입금 선택 시 해지금액이 선택한 입출금 계좌에 반영됩니다.
- 타행 입금과 현금 수령 선택 시 불필요한 당행 계좌 잔액 변경을 하지 않습니다.

### 챗봇/상담

- 챗봇에서 상품 안내, 금리 안내, 가입 조건, 내 계좌, 내 상품, 해지 흐름을 지원합니다.
- 고객 인증이 필요한 기능은 로그인 상태를 확인합니다.
- 상담 서비스는 FastAPI로 구현되어 프론트엔드 API 라우트에서 프록시합니다.
- 챗봇 해지 플로우도 지급 방식 선택과 당행 입금계좌 선택을 지원합니다.

### 예금 서비스

- 상품, 계약, 계좌, 거래 도메인을 관리합니다.
- 계약 생성 시 계좌가 함께 생성됩니다.
- 계약 해지 시 선택한 당행 입금계좌 ID를 받을 수 있습니다.
- 입출금 상품, 예금, 적금, 청약 계좌를 프론트에서 구분할 수 있도록 응답 데이터를 매핑합니다.

---

## 실행 방법

### 1. 프론트엔드 실행

```powershell
cd web
npm install
npm run dev
```

기본 주소:

```text
http://localhost:3001
```

### 2. 예금 서비스 실행

```powershell
.\gradlew :services:deposit-service:bootRun
```

기본 주소:

```text
http://localhost:8082
```

프론트엔드에서 사용할 API 주소는 `web/.env.local` 또는 환경변수로 지정할 수 있습니다.

```env
NEXT_PUBLIC_DEPOSIT_API_URL=http://localhost:8082/api
```

### 3. 상담 서비스 실행

```powershell
cd services/consultation-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8090
```

기본 주소:

```text
http://localhost:8090
```

프론트엔드 환경변수:

```env
NEXT_PUBLIC_CONSULTATION_API_URL=http://localhost:8090
```

### 4. 인프라 실행

```powershell
docker compose up -d
```

주요 인프라:

- PostgreSQL
- Redis
- Prometheus
- Grafana

---

## 주요 화면 흐름

### 계좌상세에서 이체

1. 계좌조회 화면에서 입출금 계좌를 선택합니다.
2. 계좌상세 화면에서 `이체`를 누릅니다.
3. 이체 입력 화면의 출금계좌번호가 선택한 계좌 하나로 고정됩니다.
4. 입금기관, 입금계좌번호, 이체금액, 계좌비밀번호를 입력합니다.
5. 확인 화면에서 보안매체 입력 후 금융인증서 PIN을 입력합니다.
6. 이체 결과 화면에서 처리 결과를 확인합니다.

### 예금/적금 해지

1. 계좌조회 또는 예금해지 메뉴에서 해지할 예금/적금 계좌를 선택합니다.
2. 해지계좌 비밀번호를 입력합니다.
3. 지급 방식을 선택합니다.
4. 당행 계좌 입금이면 입금받을 입출금 계좌를 선택합니다.
5. 타행 계좌 입금이면 은행과 계좌번호를 입력합니다.
6. 현금 수령이면 별도 계좌 입력 없이 진행합니다.
7. 해지 버튼을 누르면 금융인증서 전자서명 원문이 표시됩니다.
8. PIN 6자리를 입력하면 해지가 완료됩니다.

---

## API 요약

### Deposit API

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/api/products` | 예금/적금 상품 목록 조회 |
| `GET` | `/api/products/{productId}` | 상품 상세 조회 |
| `POST` | `/api/contracts` | 예금/적금 계약 생성 |
| `GET` | `/api/contracts` | 고객 계약 목록 조회 |
| `PATCH` | `/api/contracts/{contractId}/terminate` | 계약 해지 |
| `GET` | `/api/accounts` | 고객 계좌 목록 조회 |
| `GET` | `/api/transactions` | 거래내역 조회 |
| `GET` | `/api/products/recommend-agent` | 현금흐름 기반 상품 추천 |

### Consultation API

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/chatbot/categories` | 챗봇 카테고리 목록 |
| `GET` | `/chatbot/features` | 챗봇 기능 목록 |
| `POST` | `/chatbot/features/{feature_code}/execute` | 챗봇 기능 실행 |
| `POST` | `/chatbot/consultations/start` | 상담 시작 |
| `POST` | `/chatbot/consultations/{id}/messages` | 상담 메시지 전송 |
| `POST` | `/chatbot/transfer` | 챗봇 기반 이체 실행 |

---

## 검증 명령

### 프론트엔드 린트

```powershell
cd web
npx next lint --file "app/(personal)/transfer/account/page.tsx" --file "app/(personal)/transfer/confirm/page.tsx" --file "app/(personal)/transfer/result/page.tsx"
npx next lint --file "app/(personal)/products/deposit/inquiry/terminate/page.tsx" --file "lib/deposit-api.ts"
```

### 프론트엔드 전체 린트

```powershell
cd web
npm run lint
```

### 프론트엔드 타입 체크

```powershell
cd web
npx tsc --noEmit --pretty false
```

### 예금 서비스 테스트

```powershell
.\gradlew :services:deposit-service:test
```

### 상담 서비스 테스트

```powershell
cd services/consultation-service
pytest
```

---

## 개발 메모

- 입출금 계좌만 이체 출금계좌로 사용할 수 있습니다.
- 정기예금/적금 계좌는 계좌조회와 해지 대상에는 표시되지만 이체 버튼은 표시하지 않습니다.
- 계좌상세에서 이체로 진입하면 URL의 `from` 파라미터를 기준으로 출금계좌를 고정합니다.
- 해지 화면은 지급 방식에 따라 필요한 입력만 보여줍니다.
- 금융인증서 모달은 이체와 해지 모두에서 전자서명 원문 확인 후 PIN 입력 방식으로 동작합니다.
- 상품 ID 기반으로 입출금성 예금 상품을 판정하여 계좌가 잘못 숨겨지지 않도록 처리합니다.

---

## 최근 반영 내용

- 계좌상세에서 선택 계좌로 출금계좌 고정
- 입출금 계좌만 이체 가능하도록 제한
- 정기예금/적금의 이체 버튼 제거
- 예금/적금 해지 지급 방식 3종 추가
- 해지 시 금융인증서 인증 단계 추가
- 당행 계좌 입금 해지 시 대상 계좌 잔액 반영
- 챗봇의 내 계좌/내 상품/해지 흐름과 프론트 화면 연동 보강
