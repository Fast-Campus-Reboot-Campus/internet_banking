# 서버 배포 가이드

이 문서는 axfulbank 프로젝트를 네이버 클라우드 서버에 배포하는 방법을 설명합니다.

---

## 서버 정보

| 항목 | 내용 |
|---|---|
| 클라우드 | 네이버 클라우드 플랫폼 (NCP) |
| 공인 IP | 101.79.17.205 |
| OS | Ubuntu 24.04 |
| 접속 포트 | 8080 (백엔드 API) |

---

## 서버 접속 방법

```bash
ssh root@101.79.17.205
```

비밀번호는 네이버 클라우드 콘솔에서 확인합니다.
> 콘솔 → Server → 서버 선택 → 서버 관리 및 설정 변경 → 관리자 비밀번호 확인

---

## 배포 구성

로컬 개발용 `docker-compose.yml`과 별도로 서버 전용 `docker-compose.server.yml`을 사용합니다.

**포함된 서비스:**

| 서비스 | 포트 | 설명 |
|---|---|---|
| gateway-service | 8080 | API 진입점 (이걸 통해 외부 접속) |
| customer-service | 8081 | 고객/인증 |
| deposit-service | 8082 | 예금/계좌 |
| loan-service | 8083 | 대출 |
| payment-service | 8084 | 이체/결제 |
| master-service | 8085 | 공통 데이터 |
| DB 6개 + Redis + Kafka | - | 인프라 |

모니터링, AI 에이전트, Langfuse 등은 서버 자원 절약을 위해 제외했습니다.

---

## 처음 배포할 때

### 1단계 — 서버 준비

```bash
# 서버 접속 후
git clone https://github.com/Fast-Campus-Reboot-Campus/internet_banking.git
cd internet_banking
mkdir -p /app/logs
```

### 2단계 — DB + 인프라 먼저 띄우기

앱 서비스가 DB에 연결해야 하므로 DB를 먼저 실행합니다.

```bash
docker compose -f docker-compose.server.yml up -d \
  customer-db deposit-db loan-db common-db payment-db master-db redis kafka
```

모두 healthy 상태가 될 때까지 기다립니다.

```bash
docker compose -f docker-compose.server.yml ps
```

### 3단계 — 앱 서비스 빌드 및 실행

처음 빌드는 10~20분 정도 걸립니다 (Gradle이 Java 코드를 컴파일합니다).

```bash
docker compose -f docker-compose.server.yml up -d --build \
  customer-service deposit-service loan-service payment-service master-service
```

### 4단계 — gateway 실행

모든 앱 서비스가 healthy 상태가 된 후 gateway를 실행합니다.

```bash
docker compose -f docker-compose.server.yml up -d --build gateway-service
```

### 5단계 — 접속 확인

```bash
curl http://localhost:8080/actuator/health
```

외부에서는 `http://101.79.17.205:8080` 으로 접속합니다.

---

## 코드 업데이트 후 재배포

main 브랜치에 변경사항이 머지된 경우:

```bash
git pull origin main

# 변경된 서비스만 재빌드 (예: loan-service만 변경된 경우)
docker compose -f docker-compose.server.yml up -d --build loan-service
```

---

## 알려진 문제

### loan-service Flyway 버그

`V35__seed_admin_demo_data.sql`이 존재하지 않는 테이블에 데이터를 넣으려 해서 loan-service 기동이 실패합니다.

**임시 조치**: `docker-compose.server.yml`에 `SPRING_FLYWAY_TARGET: "34"` 설정으로 V35 이후 시드 데이터를 건너뜁니다. 앱 기능에는 영향 없고 어드민 화면의 데모 데이터만 없는 상태입니다.

**해결 방법**: loan-service 담당자가 아래 4개 테이블의 CREATE TABLE 마이그레이션을 추가하면 됩니다.
- `review_advisory_rule`
- `review_advisory_report`
- `ai_audit_opinion`
- `advisory_document`

추가 완료 후 `docker-compose.server.yml`에서 `SPRING_FLYWAY_TARGET: "34"` 줄을 제거하면 됩니다.

---

## 서비스 중단 및 재시작

```bash
# 전체 중단
docker compose -f docker-compose.server.yml down

# 전체 중단 + 데이터 삭제 (초기화)
docker compose -f docker-compose.server.yml down -v

# 특정 서비스만 재시작
docker compose -f docker-compose.server.yml restart loan-service
```
